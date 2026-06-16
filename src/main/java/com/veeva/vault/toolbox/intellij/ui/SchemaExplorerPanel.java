package com.veeva.vault.toolbox.intellij.ui;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.ide.CopyPasteManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.SearchTextField;
import com.intellij.ui.treeStructure.Tree;
import com.intellij.util.ui.JBUI;
import com.veeva.vault.toolbox.intellij.metadata.MetadataIndex;
import com.veeva.vault.toolbox.intellij.metadata.MetadataIndexImpl;
import com.veeva.vault.toolbox.intellij.metadata.MetadataService;
import com.veeva.vault.toolbox.intellij.metadata.model.AttributeMeta;
import com.veeva.vault.toolbox.intellij.metadata.model.FieldMeta;
import com.veeva.vault.toolbox.intellij.metadata.model.PicklistValueMeta;
import com.veeva.vault.toolbox.intellij.metadata.model.RelationshipMeta;
import com.veeva.vault.toolbox.intellij.project.ToolboxProject;
import icons.ToolboxIcons;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

/**
 * Tool-window tab that browses the connected vault's cached schema (objects, picklists, and MDL
 * component types) from the {@link MetadataService} index. Beyond browsing, it offers the
 * IDE-native actions a browser cannot: copying an API name and inserting it at the caret of the
 * active editor. The tree rebuilds automatically whenever the cached snapshot changes (connect,
 * refresh, or a lazy attribute load).
 */
public class SchemaExplorerPanel extends JPanel {

    private static final SimpleDateFormat AS_OF_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm");

    private final Project project;
    private final ToolboxProject toolboxProject;
    private final MetadataService metadataService;

    private final Tree tree;
    private final DefaultTreeModel treeModel;
    private final ToolboxTreeNode rootNode = new ToolboxTreeNode("Schema", true, ToolboxIcons.Stack);
    private final SearchTextField filterField = new SearchTextField();
    private final JLabel statusLabel = new JLabel();

    /**
     * Constructs the panel.
     *
     * @param toolboxProject the current toolbox project
     */
    public SchemaExplorerPanel(ToolboxProject toolboxProject) {
        super(new BorderLayout());
        this.toolboxProject = toolboxProject;
        this.project = toolboxProject.getProject();
        this.metadataService = MetadataService.getInstance(project);

        treeModel = new DefaultTreeModel(rootNode);
        tree = new Tree(treeModel);
        tree.setRootVisible(false);
        tree.setOpaque(false);
        tree.setCellRenderer(new ToolboxTreeNodeRenderer());

        tree.getSelectionModel().setSelectionMode(javax.swing.tree.TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION);
        SchemaMouseListener mouseListener = new SchemaMouseListener();
        tree.addMouseListener(mouseListener);
        tree.addMouseMotionListener(mouseListener);

        tree.addTreeExpansionListener(new javax.swing.event.TreeExpansionListener() {
            @Override
            public void treeExpanded(javax.swing.event.TreeExpansionEvent event) {
                if (event.getPath().getLastPathComponent() instanceof ToolboxTreeNode node) {
                    maybeTriggerLazyLoad(node);
                }
            }

            @Override
            public void treeCollapsed(javax.swing.event.TreeExpansionEvent event) {
            }
        });

        add(createToolbar(), BorderLayout.NORTH);
        add(new JScrollPane(tree), BorderLayout.CENTER);

        statusLabel.setBorder(JBUI.Borders.empty(2, 6));
        statusLabel.setFont(statusLabel.getFont().deriveFont(statusLabel.getFont().getSize() - 1f));
        add(statusLabel, BorderLayout.SOUTH);

        filterField.addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { rebuild(); }
            @Override public void removeUpdate(DocumentEvent e) { rebuild(); }
            @Override public void changedUpdate(DocumentEvent e) { rebuild(); }
        });

        if (metadataService != null) {
            metadataService.addChangeListener(this::rebuild);
        }
        SchemaExplorerService.getInstance(project).register(this);
        rebuild();
    }

    /**
     * Creates the toolbar component.
     *
     * @return the toolbar component
     */
    private JComponent createToolbar() {
        DefaultActionGroup group = new DefaultActionGroup();
        group.add(new AnAction("Refresh Schema", "Reload schema from the connected vault", AllIcons.Actions.Refresh) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                if (metadataService != null) {
                    metadataService.refreshAsync(true);
                }
            }
        });
        ActionToolbar toolbar = ActionManager.getInstance().createActionToolbar("SchemaExplorerToolbar", group, true);
        toolbar.setTargetComponent(this);

        filterField.getTextEditor().getEmptyText().setText("Filter objects, picklists, component types…");

        JPanel north = new JPanel(new BorderLayout());
        north.add(filterField, BorderLayout.CENTER);
        north.add(toolbar.getComponent(), BorderLayout.EAST);
        return north;
    }

    /**
     * Rebuilds the tree model from the metadata index.
     */
    private void rebuild() {
        MetadataIndex index = metadataService != null ? metadataService.getIndex() : MetadataIndexImpl.EMPTY;
        Set<String> expanded = captureExpanded();
        rootNode.removeAllChildren();

        if (index == null || !index.isReady()) {
            ToolboxTreeNode info = leaf(NodeKey.info(), "<html><a href=''>Connect to a vault to load schema</a></html>", ToolboxIcons.Disconnected);
            rootNode.add(info);
            statusLabel.setText("Not connected");
            treeModel.reload();
            tree.setRootVisible(false);
            return;
        }

        String filter = filterField.getText() == null ? "" : filterField.getText().trim().toLowerCase();

        ToolboxTreeNode objects = category(NodeKey.category("objects"), "Objects", ToolboxIcons.Database);
        int objectCount = addObjects(objects, index, filter);

        ToolboxTreeNode picklists = category(NodeKey.category("picklists"), "Picklists", ToolboxIcons.SlidersHorizontal);
        int picklistCount = addPicklists(picklists, index, filter);

        ToolboxTreeNode componentTypes = category(NodeKey.category("componenttypes"), "Component Types", ToolboxIcons.Puzzle);
        int typeCount = addComponentTypes(componentTypes, index, filter);

        addCategoryIfVisible(objects, objectCount, filter);
        addCategoryIfVisible(picklists, picklistCount, filter);
        addCategoryIfVisible(componentTypes, typeCount, filter);

        treeModel.reload();
        tree.setRootVisible(false);

        if (!filter.isEmpty()) {
            expandAllCategories();
        } else {
            restoreExpansion(rootNode, expanded, "");
        }
        updateStatus(index);
    }

    /**
     * Adds objects to the parent node.
     *
     * @param parent the parent node
     * @param index the metadata index
     * @param filter the current filter string
     * @return the number of objects added
     */
    private int addObjects(ToolboxTreeNode parent, MetadataIndex index, String filter) {
        int count = 0;
        for (String name : sortedNames(index.objectNames())) {
            if (!matches(name, filter)) {
                continue;
            }
            ToolboxTreeNode objectNode = category(NodeKey.object(name), name, ToolboxIcons.Component);
            if (index.fieldsLoaded(name)) {
                ToolboxTreeNode fields = category(NodeKey.folder(name + "/fields"), "Fields", ToolboxIcons.Folder);
                for (FieldMeta field : sortedByName(index.fieldsFor(name), FieldMeta::getName)) {
                    if (field.getName() != null) {
                        fields.add(leaf(NodeKey.field(field.getName()), fieldLabel(field), ToolboxIcons.Code));
                    }
                }
                fields.setText("Fields (" + fields.getChildCount() + ")");
                objectNode.add(fields);

                if (!index.relationshipsFor(name).isEmpty()) {
                    ToolboxTreeNode relationships = category(NodeKey.folder(name + "/relationships"), "Relationships", ToolboxIcons.Folder);
                    for (RelationshipMeta relationship : sortedByName(index.relationshipsFor(name), RelationshipMeta::getName)) {
                        if (relationship.getName() != null) {
                            relationships.add(leaf(NodeKey.relationship(relationship.getName()),
                                    relationshipLabel(relationship), ToolboxIcons.Link));
                        }
                    }
                    relationships.setText("Relationships (" + relationships.getChildCount() + ")");
                    objectNode.add(relationships);
                }
            } else {
                objectNode.add(leaf(NodeKey.placeholder(name), "Loading fields…", ToolboxIcons.Code));
            }
            parent.add(objectNode);
            count++;
        }
        return count;
    }

    /**
     * Adds picklists to the parent node.
     *
     * @param parent the parent node
     * @param index the metadata index
     * @param filter the current filter string
     * @return the number of picklists added
     */
    private int addPicklists(ToolboxTreeNode parent, MetadataIndex index, String filter) {
        int count = 0;
        for (String name : sortedNames(index.picklistNames())) {
            if (!matches(name, filter)) {
                continue;
            }
            ToolboxTreeNode picklistNode = category(NodeKey.picklist(name), name, ToolboxIcons.SlidersHorizontal);
            if (index.valuesLoaded(name)) {
                for (PicklistValueMeta value : index.valuesFor(name)) {
                    if (value.getName() != null) {
                        picklistNode.add(leaf(NodeKey.value(value.getName()), valueLabel(value), ToolboxIcons.Json));
                    }
                }
            } else {
                picklistNode.add(leaf(NodeKey.placeholder(name), "Loading values…", ToolboxIcons.Json));
            }
            parent.add(picklistNode);
            count++;
        }
        return count;
    }

    /**
     * Returns names sorted case-insensitively for stable, alphabetical display.
     *
     * @param names the collection of names
     * @return a sorted list of names
     */
    private List<String> sortedNames(Collection<String> names) {
        List<String> list = new ArrayList<>(names);
        list.sort(String.CASE_INSENSITIVE_ORDER);
        return list;
    }

    /**
     * Returns items sorted alphabetically by name (case-insensitive, nulls last).
     *
     * @param items the items to sort
     * @param nameOf the function to extract the name
     * @param <T> the type of item
     * @return a sorted list of items
     */
    private <T> List<T> sortedByName(Collection<T> items, Function<T, String> nameOf) {
        List<T> list = new ArrayList<>(items);
        list.sort(Comparator.comparing(nameOf, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)));
        return list;
    }

    /**
     * Gets the label for a field.
     *
     * @param field the field meta
     * @return the label
     */
    private String fieldLabel(FieldMeta field) {
        StringBuilder sb = new StringBuilder(field.getName());
        if (field.getType() != null) {
            sb.append(" : ").append(field.getType());
        }
        if (field.getReferencedObject() != null) {
            sb.append(" → ").append(field.getReferencedObject());
        } else if (field.getPicklist() != null) {
            sb.append(" → ").append(field.getPicklist());
        }
        return sb.toString();
    }

    /**
     * Gets the label for a relationship.
     *
     * @param relationship the relationship meta
     * @return the label
     */
    private String relationshipLabel(RelationshipMeta relationship) {
        String target = relationship.getReferencedObject() != null ? " → " + relationship.getReferencedObject() : "";
        return relationship.getName() + target;
    }

    /**
     * Gets the label for a picklist value.
     *
     * @param value the picklist value meta
     * @return the label
     */
    private String valueLabel(PicklistValueMeta value) {
        return value.getLabel() != null ? value.getName() + " — " + value.getLabel() : value.getName();
    }

    /**
     * Adds component types to the parent node.
     *
     * @param parent the parent node
     * @param index the metadata index
     * @param filter the current filter string
     * @return the number of component types added
     */
    private int addComponentTypes(ToolboxTreeNode parent, MetadataIndex index, String filter) {
        int count = 0;
        for (String typeName : sortedNames(index.componentTypeNames())) {
            if (!matches(typeName, filter)) {
                continue;
            }
            ToolboxTreeNode typeNode = category(NodeKey.type(typeName), typeName, ToolboxIcons.Puzzle);
            if (index.attributesLoaded(typeName)) {
                for (AttributeMeta attribute : sortedByName(index.attributesFor(typeName), AttributeMeta::getName)) {
                    if (attribute.getName() != null) {
                        typeNode.add(leaf(NodeKey.attribute(attribute.getName()), attribute.getName(), ToolboxIcons.Code));
                    }
                }
            } else {
                typeNode.add(leaf(NodeKey.placeholder(typeName), "Loading attributes…", ToolboxIcons.Code));
            }
            parent.add(typeNode);
            count++;
        }
        return count;
    }

    /**
     * Adds a category node if it's visible based on the filter.
     *
     * @param category the category node
     * @param count the count of items in the category
     * @param filter the filter string
     */
    private void addCategoryIfVisible(ToolboxTreeNode category, int count, String filter) {
        if (!filter.isEmpty() && count == 0) {
            return;
        }
        category.setText(category.getText() + " (" + count + ")");
        rootNode.add(category);
    }

    /**
     * Checks if a name matches the filter.
     *
     * @param name the name
     * @param filter the filter
     * @return true if it matches
     */
    private boolean matches(String name, String filter) {
        return filter.isEmpty() || (name != null && name.toLowerCase().contains(filter));
    }

    /**
     * Updates the status label.
     *
     * @param index the metadata index
     */
    private void updateStatus(MetadataIndex index) {
        String asOf = index.fetchedEpochMillis() > 0 ? AS_OF_FORMAT.format(new Date(index.fetchedEpochMillis())) : "—";
        statusLabel.setText(index.objectNames().size() + " objects · "
                + index.picklistNames().size() + " picklists · "
                + index.componentTypeNames().size() + " types — as of " + asOf);
    }

    /**
     * Creates a category node.
     *
     * @param key the node key
     * @param label the label
     * @param icon the icon
     * @return the category node
     */
    private ToolboxTreeNode category(NodeKey key, String label, Icon icon) {
        ToolboxTreeNode node = new ToolboxTreeNode(key, true, icon);
        node.setText(label);
        return node;
    }

    /**
     * Creates a leaf node.
     *
     * @param key the node key
     * @param label the label
     * @param icon the icon
     * @return the leaf node
     */
    private ToolboxTreeNode leaf(NodeKey key, String label, Icon icon) {
        ToolboxTreeNode node = new ToolboxTreeNode(key, false, icon);
        node.setText(label);
        return node;
    }

    /**
     * Captures the currently expanded nodes.
     *
     * @return a set of expanded node keys
     */
    private Set<String> captureExpanded() {
        Set<String> keys = new HashSet<>();
        for (int row = 0; row < tree.getRowCount(); row++) {
            TreePath path = tree.getPathForRow(row);
            if (path != null && tree.isExpanded(path)) {
                keys.add(pathKey(path));
            }
        }
        return keys;
    }

    /**
     * Generates a stable key for a tree path.
     *
     * @param path the tree path
     * @return the stable key
     */
    private String pathKey(TreePath path) {
        StringBuilder sb = new StringBuilder();
        for (Object component : path.getPath()) {
            if (component instanceof ToolboxTreeNode node && node.getUserObject() instanceof NodeKey key) {
                sb.append('/').append(key.stableId());
            }
        }
        return sb.toString();
    }

    /**
     * Restores the expansion state of the tree.
     *
     * @param parent the parent node
     * @param expandedKeys the set of expanded keys
     * @param prefix the current prefix
     */
    private void restoreExpansion(ToolboxTreeNode parent, Set<String> expandedKeys, String prefix) {
        for (int i = 0; i < parent.getChildCount(); i++) {
            if (!(parent.getChildAt(i) instanceof ToolboxTreeNode child)
                    || !(child.getUserObject() instanceof NodeKey key)) {
                continue;
            }
            String id = prefix + "/" + key.stableId();
            if (expandedKeys.contains(id)) {
                tree.expandPath(new TreePath(child.getPath()));
                maybeTriggerLazyLoad(child);
                restoreExpansion(child, expandedKeys, id);
            }
        }
    }

    /**
     * Expands all category nodes.
     */
    private void expandAllCategories() {
        for (int i = 0; i < rootNode.getChildCount(); i++) {
            if (rootNode.getChildAt(i) instanceof ToolboxTreeNode child) {
                tree.expandPath(new TreePath(child.getPath()));
            }
        }
    }

    /**
     * Triggers a lazy load if necessary for the given node.
     *
     * @param node the tree node
     */
    private void maybeTriggerLazyLoad(ToolboxTreeNode node) {
        if (metadataService == null || !(node.getUserObject() instanceof NodeKey key)) {
            return;
        }
        switch (key.kind) {
            case TYPE:
                metadataService.ensureComponentTypeAttributesLoaded(key.name);
                break;
            case OBJECT:
                metadataService.ensureObjectFieldsLoaded(key.name);
                break;
            case PICKLIST:
                metadataService.ensurePicklistValuesLoaded(key.name);
                break;
            default:
                break;
        }
    }

    private class SchemaMouseListener extends MouseAdapter {
        private boolean isDragging = false;
        private int dragStartRow = -1;

        @Override
        public void mouseClicked(MouseEvent e) {
            if (SwingUtilities.isLeftMouseButton(e) && e.getClickCount() == 1) {
                TreePath path = tree.getPathForLocation(e.getX(), e.getY());
                if (path != null) {
                    if (path.getLastPathComponent() instanceof ToolboxTreeNode node) {
                        if (node.getUserObject() instanceof NodeKey key) {
                            if (key.kind == NodeKey.Kind.INFO && !toolboxProject.isConnected()) {
                                toolboxProject.requestLoginTabSwitch();
                            }
                        }
                    }
                }
            }
        }

        @Override
        public void mouseMoved(MouseEvent e) {
            TreePath path = tree.getPathForLocation(e.getX(), e.getY());
            boolean isLink = false;
            if (path != null) {
                if (path.getLastPathComponent() instanceof ToolboxTreeNode node) {
                    if (node.getUserObject() instanceof NodeKey key) {
                        if (key.kind == NodeKey.Kind.INFO && !toolboxProject.isConnected()) {
                            isLink = true;
                        }
                    }
                }
            }
            if (isLink) {
                tree.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            } else {
                tree.setCursor(Cursor.getDefaultCursor());
            }
        }

        @Override
        public void mousePressed(MouseEvent e) {
            handlePopup(e);
            if (!e.isPopupTrigger() && SwingUtilities.isLeftMouseButton(e)) {
                isDragging = true;
                dragStartRow = tree.getRowForLocation(e.getX(), e.getY());
            }
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            handlePopup(e);
            isDragging = false;
            dragStartRow = -1;
        }

        @Override
        public void mouseDragged(MouseEvent e) {
            if (isDragging && dragStartRow >= 0) {
                int currentRow = tree.getClosestRowForLocation(e.getX(), e.getY());
                if (currentRow >= 0) {
                    tree.setSelectionInterval(dragStartRow, currentRow);
                    Rectangle bounds = tree.getRowBounds(currentRow);
                    if (bounds != null) {
                        tree.scrollRectToVisible(bounds);
                    }
                }
            }
        }

        /**
         * Handles the popup menu for the tree.
         *
         * @param e the mouse event
         */
        private void handlePopup(MouseEvent e) {
            if (!e.isPopupTrigger()) {
                return;
            }
            TreePath path = tree.getPathForLocation(e.getX(), e.getY());
            if (path != null && !tree.isPathSelected(path)) {
                tree.setSelectionPath(path);
            }

            boolean single = tree.getSelectionCount() <= 1;
            NodeKey key = selectedActionableKey();
            FieldSelection fieldSelection = selectedFieldSelection();
            if (key == null && fieldSelection == null) {
                return;
            }
            DefaultActionGroup group = new DefaultActionGroup();

            if (single && key != null && key.kind == NodeKey.Kind.OBJECT) {
                group.add(new AnAction("Open in VQL Console") {
                    @Override
                    public void actionPerformed(@NotNull AnActionEvent a) {
                        openVql(selectFrom("id, name__v", key.name));
                    }
                });

                group.add(new AnAction("Query All Fields in VQL Console") {
                    @Override
                    public void actionPerformed(@NotNull AnActionEvent a) {
                        if (metadataService != null) {
                            MetadataIndex index = metadataService.getIndex();
                            if (!index.fieldsLoaded(key.name)) {
                                com.intellij.openapi.progress.ProgressManager.getInstance().run(
                                    new com.intellij.openapi.progress.Task.Modal(project, "Loading Metadata", false) {
                                        @Override
                                        public void run(@org.jetbrains.annotations.NotNull com.intellij.openapi.progress.ProgressIndicator indicator) {
                                            indicator.setIndeterminate(true);
                                            indicator.setText("Retrieving metadata for " + key.name + "...");
                                            metadataService.ensureObjectFieldsLoadedSync(key.name);
                                        }
                                    }
                                );
                            }
                        }
                        MetadataIndex currentIndex = metadataService != null ? metadataService.getIndex() : MetadataIndexImpl.EMPTY;
                        openVql(selectFrom(allFieldColumns(currentIndex, key.name), key.name));
                    }
                });
            }

            if (fieldSelection != null) {
                String label = "Query Selected Field" + (fieldSelection.fields.size() == 1 ? "" : "s")
                        + " in VQL Console (" + fieldSelection.fields.size() + ")";
                group.add(new AnAction(label) {
                    @Override
                    public void actionPerformed(@NotNull AnActionEvent a) {
                        openVql(selectFrom(String.join(", ", fieldSelection.fields), fieldSelection.object));
                    }
                });
            }

            if (single && key != null) {
                String definitionFolder = definitionFolderFor(key);
                if (definitionFolder != null) {
                    group.add(new AnAction("Open Definition (extracted MDL)") {
                        @Override
                        public void actionPerformed(@NotNull AnActionEvent a) {
                            openDefinition(definitionFolder, key.name);
                        }
                    });
                }

                group.add(new AnAction("Find Usages in MDL") {
                    @Override
                    public void actionPerformed(@NotNull AnActionEvent a) {
                        MdlUsageSearch.findInMdl(project, tree, key.name);
                    }
                });
            }

            if (group.getChildrenCount() > 0) {
                com.intellij.openapi.actionSystem.ActionPopupMenu menu = ActionManager.getInstance().createActionPopupMenu("SchemaExplorerPopup", group);
                menu.getComponent().show(tree, e.getX(), e.getY());
            }
        }
    }

    private static final class FieldSelection {
        private final String object;
        private final List<String> fields;

        FieldSelection(String object, List<String> fields) {
            this.object = object;
            this.fields = fields;
        }
    }

    /**
     * Returns the selected field nodes as a {@link FieldSelection} when they are all fields of a
     * single object; {@code null} otherwise (mixed kinds, mixed objects, or no fields selected).
     *
     * @return the selected field selection or null
     */
    private FieldSelection selectedFieldSelection() {
        TreePath[] paths = tree.getSelectionPaths();
        if (paths == null) {
            return null;
        }
        String object = null;
        List<String> fields = new ArrayList<>();
        for (TreePath path : paths) {
            if (!(path.getLastPathComponent() instanceof ToolboxTreeNode node)
                    || !(node.getUserObject() instanceof NodeKey key) || key.kind != NodeKey.Kind.FIELD) {
                return null;
            }
            String owner = objectOf(node);
            if (owner == null) {
                return null;
            }
            if (object == null) {
                object = owner;
            } else if (!object.equals(owner)) {
                return null;
            }
            fields.add(key.name);
        }
        return fields.isEmpty() ? null : new FieldSelection(object, fields);
    }

    /**
     * Returns the object name owning a field node (field → Fields folder → object), or null.
     *
     * @param fieldNode the field node
     * @return the object name or null
     */
    private String objectOf(ToolboxTreeNode fieldNode) {
        TreeNode folder = fieldNode.getParent();
        TreeNode objectNode = folder != null ? folder.getParent() : null;
        if (objectNode instanceof ToolboxTreeNode node
                && node.getUserObject() instanceof NodeKey key && key.kind == NodeKey.Kind.OBJECT) {
            return key.name;
        }
        return null;
    }

    /**
     * Gets all field columns for an object.
     *
     * @param index the metadata index
     * @param objectName the object name
     * @return a comma-separated string of columns
     */
    private String allFieldColumns(MetadataIndex index, String objectName) {
        List<String> names = new ArrayList<>();
        for (FieldMeta field : index.fieldsFor(objectName)) {
            if (field.getName() != null) {
                names.add(field.getName());
            }
        }
        return names.isEmpty() ? "id, name__v" : String.join(", ", names);
    }

    /**
     * Constructs a SELECT query.
     *
     * @param columns the columns
     * @param object the object name
     * @return the query string
     */
    private String selectFrom(String columns, String object) {
        return "SELECT " + columns + "\nFROM " + object;
    }

    /**
     * Gets the currently selected node.
     *
     * @return the selected node
     */
    private ToolboxTreeNode selectedNode() {
        TreePath path = tree.getSelectionPath();
        return path != null && path.getLastPathComponent() instanceof ToolboxTreeNode node ? node : null;
    }

    /**
     * Returns the key of the selected node when it carries a usable API name, else {@code null}.
     *
     * @return the key or null
     */
    private NodeKey selectedActionableKey() {
        ToolboxTreeNode node = selectedNode();
        if (node != null && node.getUserObject() instanceof NodeKey key && key.isActionable()) {
            return key;
        }
        return null;
    }

    /**
     * Opens the given query in the VQL Console (or copies it as a fallback if the console is
     * unavailable). The query is prefilled but not auto-run.
     *
     * @param vql the query
     */
    private void openVql(String vql) {
        VqlConsoleService service = VqlConsoleService.getInstance(project);
        if (service != null && service.isAvailable()) {
            service.openWithQuery(vql);
        } else {
            copyToClipboard(vql);
        }
    }

    /**
     * Selects this tab and reveals the named object or picklist in the tree, expanding it (which
     * lazily loads its fields/values). Invoked from the VQL Console via {@link SchemaExplorerService}.
     *
     * @param name the name of the component
     */
    public void revealComponent(String name) {
        if (name == null || name.isBlank()) {
            return;
        }
        MetadataIndex index = metadataService != null ? metadataService.getIndex() : MetadataIndexImpl.EMPTY;
        String categoryId;
        NodeKey.Kind kind;
        if (index.objectExists(name) == MetadataIndex.Existence.EXISTS) {
            categoryId = "objects";
            kind = NodeKey.Kind.OBJECT;
        } else if (index.picklistExists(name) == MetadataIndex.Existence.EXISTS) {
            categoryId = "picklists";
            kind = NodeKey.Kind.PICKLIST;
        } else {
            Messages.showInfoMessage(project,
                    "'" + name + "' is not a known object or picklist in the connected vault.", "Reveal in Schema");
            return;
        }

        JTabbedPane tabs = (JTabbedPane) SwingUtilities.getAncestorOfClass(JTabbedPane.class, this);
        if (tabs != null) {
            tabs.setSelectedComponent(this);
        }
        if (toolboxProject.getToolWindow() != null) {
            toolboxProject.getToolWindow().activate(null);
        }

        if (!filterField.getText().isEmpty()) {
            filterField.setText("");
        }
        rebuild();
        ToolboxTreeNode categoryNode = findChildByKey(rootNode, NodeKey.Kind.CATEGORY, categoryId);
        if (categoryNode == null) {
            return;
        }
        tree.expandPath(new TreePath(categoryNode.getPath()));
        ToolboxTreeNode node = findChildByKey(categoryNode, kind, name);
        if (node == null) {
            return;
        }
        TreePath path = new TreePath(node.getPath());
        tree.expandPath(path);
        tree.setSelectionPath(path);
        tree.scrollPathToVisible(path);
    }

    /**
     * Finds a child node by its key.
     *
     * @param parent the parent node
     * @param kind the key kind
     * @param name the key name
     * @return the child node or null
     */
    private ToolboxTreeNode findChildByKey(ToolboxTreeNode parent, NodeKey.Kind kind, String name) {
        for (int i = 0; i < parent.getChildCount(); i++) {
            if (parent.getChildAt(i) instanceof ToolboxTreeNode child
                    && child.getUserObject() instanceof NodeKey key
                    && key.kind == kind && name.equals(key.name)) {
                return child;
            }
        }
        return null;
    }

    /**
     * Returns the extracted-MDL component-type folder for a node, or null if it has no own file.
     *
     * @param key the node key
     * @return the definition folder
     */
    private String definitionFolderFor(NodeKey key) {
        switch (key.kind) {
            case OBJECT:
                return "Object";
            case PICKLIST:
                return "Picklist";
            default:
                return null;
        }
    }

    /**
     * Opens the component's extracted {@code .mdl} file (the source-of-truth definition), bridging
     * the live schema view to the config-as-code files produced by Extract MDL.
     *
     * @param typeFolder the type folder
     * @param name the component name
     */
    private void openDefinition(String typeFolder, String name) {
        MetadataIndex index = metadataService != null ? metadataService.getIndex() : MetadataIndexImpl.EMPTY;
        File mdlDirectory = toolboxProject.getMdlDirectory();
        if (mdlDirectory == null || index.vaultId() == null) {
            return;
        }
        File file = new File(mdlDirectory,
                index.vaultId() + "/" + typeFolder + "/" + typeFolder + "." + name + ".mdl");
        VirtualFile virtualFile = file.exists() ? VfsUtil.findFileByIoFile(file, true) : null;
        if (virtualFile == null) {
            Messages.showInfoMessage(project,
                    "No extracted MDL found for '" + name + "'. Run Extract MDL to download component definitions.",
                    "Open Definition");
            return;
        }
        FileEditorManager.getInstance(project).openFile(virtualFile, true);
    }

    /**
     * Copies text to the clipboard.
     *
     * @param text the text to copy
     */
    private void copyToClipboard(String text) {
        CopyPasteManager.getInstance().setContents(new StringSelection(text));
    }

    private static final class NodeKey {
        enum Kind { INFO, CATEGORY, FOLDER, OBJECT, PICKLIST, TYPE, FIELD, RELATIONSHIP, VALUE, ATTRIBUTE, PLACEHOLDER }

        private final Kind kind;
        private final String name;

        private NodeKey(Kind kind, String name) {
            this.kind = kind;
            this.name = name;
        }

        static NodeKey info() { return new NodeKey(Kind.INFO, "info"); }
        static NodeKey category(String id) { return new NodeKey(Kind.CATEGORY, id); }
        static NodeKey folder(String id) { return new NodeKey(Kind.FOLDER, id); }
        static NodeKey object(String name) { return new NodeKey(Kind.OBJECT, name); }
        static NodeKey picklist(String name) { return new NodeKey(Kind.PICKLIST, name); }
        static NodeKey type(String name) { return new NodeKey(Kind.TYPE, name); }
        static NodeKey field(String name) { return new NodeKey(Kind.FIELD, name); }
        static NodeKey relationship(String name) { return new NodeKey(Kind.RELATIONSHIP, name); }
        static NodeKey value(String name) { return new NodeKey(Kind.VALUE, name); }
        static NodeKey attribute(String name) { return new NodeKey(Kind.ATTRIBUTE, name); }
        static NodeKey placeholder(String id) { return new NodeKey(Kind.PLACEHOLDER, id); }

        boolean isActionable() {
            switch (kind) {
                case OBJECT:
                case PICKLIST:
                case TYPE:
                case FIELD:
                case RELATIONSHIP:
                case VALUE:
                case ATTRIBUTE:
                    return true;
                default:
                    return false;
            }
        }

        String stableId() {
            return kind + ":" + name;
        }

        @Override
        public String toString() {
            return name;
        }
    }
}
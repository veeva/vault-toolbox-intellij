package com.veeva.vault.toolbox.intellij.ui;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.treeStructure.Tree;
import com.intellij.ui.TreeSpeedSearch;
import com.veeva.vault.toolbox.intellij.listeners.ToolboxTreeNodeListener;
import com.veeva.vault.toolbox.intellij.project.ToolboxProject;
import com.veeva.vault.toolbox.core.models.VpkBuildManifest;
import icons.ToolboxIcons;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Panel that displays and manages the hierarchy of components in a Vault VPK package.
 * Supports adding, removing, and reordering components (MDL, CSV, JSON).
 */
public class ComponentTreePanel extends JPanel {
	private static final Logger logger = LoggerFactory.getLogger(ComponentTreePanel.class);

	private final ToolboxProject toolboxProject;
	private final VpkBuildManifest buildManifest;
	private JTree tree;
	private DefaultTreeModel treeModel;
	private ToolboxTreeNode rootNode;

	/**
	 * Initializes the component tree panel.
	 *
	 * @param toolboxProject The toolbox project context.
	 * @param buildManifest  The package build manifest to manage.
	 */
	public ComponentTreePanel(ToolboxProject toolboxProject, VpkBuildManifest buildManifest) {
		super();
		this.toolboxProject = toolboxProject;
		this.buildManifest = buildManifest;
		init();
	}

	/**
	 * Configures the UI components, toolbar, and initial tree state.
	 */
	private void init() {
		this.setLayout(new BorderLayout());
		ToolboxTreeNodeRenderer renderer = new ToolboxTreeNodeRenderer();
		rootNode = new ToolboxTreeNode("Components", true, ToolboxIcons.Menu);
		treeModel = new DefaultTreeModel(rootNode);

		tree = new Tree(treeModel);
		tree.setOpaque(false);
		tree.setCellRenderer(renderer);
		tree.addMouseListener(new ToolboxTreeNodeMouseListener(tree));

		tree.getSelectionModel().setSelectionMode(TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION);

		TreeSpeedSearch.installOn(tree);

		tree.setDragEnabled(true);
		tree.setDropMode(DropMode.INSERT);
		tree.setTransferHandler(new ComponentTransferHandler());

		this.add(createToolbar(), BorderLayout.NORTH);
		this.add(new JScrollPane(tree), BorderLayout.CENTER);

		buildTree();
	}

	/**
	 * Creates the management toolbar with actions for modifying the component list.
	 *
	 * @return The toolbar component.
	 */
	private JComponent createToolbar() {
		DefaultActionGroup actionGroup = new DefaultActionGroup();

		actionGroup.add(new AnAction("Add Component", "Add a new component", AllIcons.General.Add) {
			@Override
			public void actionPerformed(@NotNull AnActionEvent e) {
				addComponent();
			}
		});

		actionGroup.add(new AnAction("Remove Component", "Remove selected component(s)", AllIcons.General.Remove) {			@Override
			public void actionPerformed(@NotNull AnActionEvent e) {
				removeComponent();
			}
			@Override
			public void update(@NotNull AnActionEvent e) {
				e.getPresentation().setEnabled(!getSelectedComponents().isEmpty());
			}
		});

		actionGroup.add(new AnAction("Move Up", "Move component up", AllIcons.Actions.MoveUp) {
			@Override
			public void actionPerformed(@NotNull AnActionEvent e) {
				moveComponentUp();
			}
			@Override
			public void update(@NotNull AnActionEvent e) {
				e.getPresentation().setEnabled(canMoveUp());
			}
		});

		actionGroup.add(new AnAction("Move Down", "Move component down", AllIcons.Actions.MoveDown) {
			@Override
			public void actionPerformed(@NotNull AnActionEvent e) {
				moveComponentDown();
			}
			@Override
			public void update(@NotNull AnActionEvent e) {
				e.getPresentation().setEnabled(canMoveDown());
			}
		});

		ActionToolbar actionToolbar = ActionManager.getInstance().createActionToolbar("ComponentTreeToolbar", actionGroup, true);
		actionToolbar.setTargetComponent(this);
		return actionToolbar.getComponent();
	}

	/**
	 * Opens a file chooser to select a new component to add to the package.
	 */
	private void addComponent() {
		FileChooserDescriptor fileChooserDescriptor = FileChooserDescriptorFactory.createSingleFileOrFolderDescriptor();
		fileChooserDescriptor.setForcedToUseIdeaFileChooser(true);

		VirtualFile projectFile = VfsUtil.findFileByIoFile(new File(toolboxProject.getProject().getBasePath()), true);
		fileChooserDescriptor.setRoots(projectFile);
		VirtualFile chosenFile = FileChooser.chooseFile(fileChooserDescriptor, toolboxProject.getProject(), null);
		if (chosenFile != null) {
			String path = chosenFile.getPath();
			if (path != null && !path.isEmpty()) {
				String newPath = path.replace(toolboxProject.getProject().getBasePath(), "");
				addComponent(newPath);
			}
		}
	}

	/**
	 * Adds a component at the specified relative path and refreshes the tree.
	 *
	 * @param path The relative project path of the component.
	 */
	private void addComponent(String path) {
		if (!isValidFileType(path)) {
			Messages.showErrorDialog(this, "Invalid file type. Only .mdl, .csv, and .json files are supported.", "Invalid File");
			return;
		}

		if (buildManifest.getComponents() != null) {
			for (VpkBuildManifest.Component c : buildManifest.getComponents()) {
				if (c.getPath().equals(path)) {
					Messages.showWarningDialog(this, "Component with path '" + path + "' already exists.", "Duplicate Component");
					return;
				}
			}
		}
		
		VpkBuildManifest.Component component = new VpkBuildManifest.Component("", path);
		buildManifest.addComponent(component);
		regenerateStepLabels();
		buildTree();
		
		int newIndex = buildManifest.getComponents().size() - 1;
		selectComponentAtIndex(newIndex);
	}
	
	/**
	 * Checks if the given path represents a valid file type.
	 *
	 * @param path the file path
	 * @return true if valid, false otherwise
	 */
	private boolean isValidFileType(String path) {
		if (path == null) return false;
		String lowerPath = path.toLowerCase();
		return lowerPath.endsWith(".mdl") || lowerPath.endsWith(".csv") || lowerPath.endsWith(".json");
	}

	private List<VpkBuildManifest.Component> getSelectedComponents() {
		List<VpkBuildManifest.Component> components = new ArrayList<>();
		TreePath[] selectionPaths = tree.getSelectionPaths();
		if (selectionPaths != null) {
			for (TreePath path : selectionPaths) {
				ToolboxTreeNode selectedNode = (ToolboxTreeNode) path.getLastPathComponent();
				Object userObject = selectedNode.getUserObject();
				if (userObject instanceof VpkBuildManifest.Component && !components.contains(userObject)) {
					components.add((VpkBuildManifest.Component) userObject);
				}
			}
		}
		return components;
	}

	private VpkBuildManifest.Component getSelectedComponent() {
		List<VpkBuildManifest.Component> selected = getSelectedComponents();
		return selected.isEmpty() ? null : selected.get(0);
	}

	/**
	 * Removes the selected component(s) from the manifest and refreshes the tree.
	 */
	private void removeComponent() {
		List<VpkBuildManifest.Component> components = getSelectedComponents();
		if (!components.isEmpty()) {
			int minIndex = Integer.MAX_VALUE;
			for (VpkBuildManifest.Component component : components) {
				int index = buildManifest.getComponents().indexOf(component);
				if (index != -1 && index < minIndex) {
					minIndex = index;
				}
				buildManifest.removeComponent(component);
			}
			regenerateStepLabels();
			buildTree();

			if (buildManifest.getComponents().size() > 0 && minIndex != Integer.MAX_VALUE) {
				int newIndex = Math.min(minIndex, buildManifest.getComponents().size() - 1);
				selectComponentAtIndex(newIndex);
			}
		}
	}

	/**
	 * Reorders the selected component one step up in the manifest.
	 */
	private void moveComponentUp() {
		int index = getSelectedComponentIndex();
		if (index > 0) {
			buildManifest.moveComponent(index, index - 1);
			regenerateStepLabels();
			buildTree();
			selectComponentAtIndex(index - 1);
		}
	}

	/**
	 * Reorders the selected component one step down in the manifest.
	 */
	private void moveComponentDown() {
		int index = getSelectedComponentIndex();
		if (index >= 0 && buildManifest.getComponents() != null && index < buildManifest.getComponents().size() - 1) {
			buildManifest.moveComponent(index, index + 1);
			regenerateStepLabels();
			buildTree();
			selectComponentAtIndex(index + 1);
		}
	}
	
	/**
	 * Recalculates the numerical step IDs for all components based on their current order.
	 */
	private void regenerateStepLabels() {
		if (buildManifest.getComponents() != null) {
			for (int i = 0; i < buildManifest.getComponents().size(); i++) {
				VpkBuildManifest.Component component = buildManifest.getComponents().get(i);
				String step = String.format("%05d", (i + 1) * 10);
				component.setStep(step);
			}
		}
	}
	
	private int getSelectedComponentIndex() {
		VpkBuildManifest.Component selectedComponent = getSelectedComponent();
		if (selectedComponent != null && buildManifest.getComponents() != null) {
			return buildManifest.getComponents().indexOf(selectedComponent);
		}
		return -1;
	}
	
	private void selectComponentAtIndex(int index) {
		if (index >= 0 && index < rootNode.getChildCount()) {
			TreeNode node = rootNode.getChildAt(index);
			if (node instanceof ToolboxTreeNode) {
				TreePath path = new TreePath(((ToolboxTreeNode) node).getPath());
				tree.setSelectionPath(path);
				tree.scrollPathToVisible(path);
			}
		}
	}

	/**
	 * Checks if the selected component can be moved up.
	 *
	 * @return true if it can be moved up, false otherwise
	 */
	private boolean canMoveUp() {
		if (getSelectedComponents().size() != 1) return false;
		int index = getSelectedComponentIndex();
		return index > 0;
	}

	/**
	 * Checks if the selected component can be moved down.
	 *
	 * @return true if it can be moved down, false otherwise
	 */
	private boolean canMoveDown() {
		if (getSelectedComponents().size() != 1) return false;
		int index = getSelectedComponentIndex();
		return index >= 0 && buildManifest.getComponents() != null && index < buildManifest.getComponents().size() - 1;
	}

	/**
	 * Determines the file type from the path.
	 *
	 * @param path the file path
	 * @return the file type string
	 */
	private String getFileType(String path) {
		if (path == null) return "Unknown";
		String lowerPath = path.toLowerCase();
		if (lowerPath.endsWith(".mdl")) return "MDL";
		if (lowerPath.endsWith(".csv")) return "CSV";
		if (lowerPath.endsWith(".json")) return "JSON";
		return "Unknown";
	}

	/**
	 * Rebuilds the visual tree structure based on the current state of the build manifest.
	 */
	void buildTree() {
		rootNode.removeAllChildren();
		List<VpkBuildManifest.Component> components = buildManifest.getComponents();
		if (components != null && !components.isEmpty()) {
			for (VpkBuildManifest.Component component : components) {
				ToolboxTreeNode stepNode = new ToolboxTreeNode(component, true, ToolboxIcons.ComponentFolder);
				String type = getFileType(component.getPath());
				stepNode.setText(component.getStep());

				if ("CSV".equals(type)) {
					ToolboxTreeNode csvNode = new ToolboxTreeNode(component, true, ToolboxIcons.Database);
					csvNode.setText(component.getPath());
					csvNode.toolboxTreeNodeListener = new ToolboxTreeNodeListener() {
						@Override
						public void singleClick(ToolboxTreeNode node) {}

						@Override
						public void doubleClick(ToolboxTreeNode node) {
							openCsvDataViewer(component);
						}
					};
					stepNode.add(csvNode);

					String xmlPath = component.getPath().substring(0, component.getPath().lastIndexOf(".")) + ".xml";
					File xmlFile = new File(toolboxProject.getProject().getBasePath(), xmlPath);
					
					ToolboxTreeNode xmlNode = new ToolboxTreeNode(component, true, ToolboxIcons.Xml);
					if (xmlFile.exists()) {
						xmlNode.setText(xmlPath);
					} else {
						xmlNode.setText("{missing data manifest}");
					}

					xmlNode.toolboxTreeNodeListener = new ToolboxTreeNodeListener() {
						@Override
						public void singleClick(ToolboxTreeNode node) {}

						@Override
						public void doubleClick(ToolboxTreeNode node) {
							openCsvDataEditor(component);
						}
					};
					stepNode.add(xmlNode);
				} else {
					Icon pathIcon = ToolboxIcons.Component;
					if ("JSON".equals(type)) {
						pathIcon = ToolboxIcons.Json;
					}
					
					ToolboxTreeNode pathNode = new ToolboxTreeNode(component, true, pathIcon);
					pathNode.setText(component.getPath());
					stepNode.add(pathNode);
				}
				
				rootNode.add(stepNode);
			}
		}
		treeModel.reload();
		for (int i = 0; i < tree.getRowCount(); i++) {
			tree.expandRow(i);
		}
		tree.setRootVisible(false);
	}

	/**
	 * Opens the CSV data editor for the specified component.
	 *
	 * @param component The component whose data manifest should be edited.
	 */
	private void openCsvDataEditor(VpkBuildManifest.Component component) {
		File csvFile = new File(toolboxProject.getProject().getBasePath(), component.getPath());
		if (csvFile.exists()) {
			CsvDataEditorDialog dialog = new CsvDataEditorDialog(toolboxProject, csvFile);
			if (dialog.showAndGet()) {
				buildTree();
			}
		} else {
			Messages.showErrorDialog(this, "CSV file not found: " + csvFile.getAbsolutePath(), "Error");
		}
	}

	/**
	 * Opens the CSV data viewer for the specified component.
	 *
	 * @param component The component whose CSV data should be viewed.
	 */
	private void openCsvDataViewer(VpkBuildManifest.Component component) {
		File csvFile = new File(toolboxProject.getProject().getBasePath(), component.getPath());
		if (csvFile.exists()) {
			CsvDataViewerDialog dialog = new CsvDataViewerDialog(toolboxProject.getProject(), csvFile);
			dialog.show();
		} else {
			Messages.showErrorDialog(this, "CSV file not found: " + csvFile.getAbsolutePath(), "Error");
		}
	}

	/**
	 * Handles drag and drop operations within the component tree.
	 */
	private class ComponentTransferHandler extends TransferHandler {
		private DataFlavor nodesFlavor;

		public ComponentTransferHandler() {
			try {
				String mimeType = DataFlavor.javaJVMLocalObjectMimeType + ";class=\"" +
						VpkBuildManifest.Component[].class.getName() + "\"";
				nodesFlavor = new DataFlavor(mimeType);
			} catch (ClassNotFoundException e) {
				logger.error("ClassNotFound: " + e.getMessage());
			}
		}

		@Override
		public boolean canImport(TransferSupport support) {
			if (!support.isDrop() || nodesFlavor == null) return false;
			support.setShowDropLocation(true);
			if (!support.isDataFlavorSupported(nodesFlavor)) return false;
			
			JTree.DropLocation dl = (JTree.DropLocation) support.getDropLocation();
			TreePath dest = dl.getPath();
			return dest != null;
		}

		@Override
		protected Transferable createTransferable(JComponent c) {
			List<VpkBuildManifest.Component> selected = getSelectedComponents();
			if (selected.isEmpty()) return null;
			return new Transferable() {
				@Override
				public DataFlavor[] getTransferDataFlavors() { return new DataFlavor[]{nodesFlavor}; }
				@Override
				public boolean isDataFlavorSupported(DataFlavor flavor) { return nodesFlavor.equals(flavor); }
				@Override
				public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException {
					if (!isDataFlavorSupported(flavor)) throw new UnsupportedFlavorException(flavor);
					return selected.toArray(new VpkBuildManifest.Component[0]);
				}
			};
		}

		@Override
		public int getSourceActions(JComponent c) {
			return MOVE;
		}

		@Override
		public boolean importData(TransferSupport support) {
			if (!canImport(support)) return false;
			
			try {
				VpkBuildManifest.Component[] nodes = (VpkBuildManifest.Component[]) support.getTransferable().getTransferData(nodesFlavor);
				JTree.DropLocation dl = (JTree.DropLocation) support.getDropLocation();
				TreePath dest = dl.getPath();
				
				ToolboxTreeNode parent = (ToolboxTreeNode) dest.getLastPathComponent();
				Object userObject = parent.getUserObject();
				int dropIndex = 0;
				if (userObject instanceof VpkBuildManifest.Component) {
					dropIndex = buildManifest.getComponents().indexOf((VpkBuildManifest.Component) userObject);
				} else if (parent == rootNode) {
					dropIndex = buildManifest.getComponents().size();
				} else {
					return false;
				}

				List<VpkBuildManifest.Component> componentsList = buildManifest.getComponents();
				for (VpkBuildManifest.Component comp : nodes) {
					componentsList.remove(comp);
				}
				
				dropIndex = Math.min(dropIndex, componentsList.size());
				for (int i = 0; i < nodes.length; i++) {
					componentsList.add(dropIndex + i, nodes[i]);
				}
				
				regenerateStepLabels();
				buildTree();
				
				tree.clearSelection();
				for (VpkBuildManifest.Component comp : nodes) {
					int newIdx = buildManifest.getComponents().indexOf(comp);
					if (newIdx != -1) {
						TreeNode node = rootNode.getChildAt(newIdx);
						TreePath path = new TreePath(((ToolboxTreeNode) node).getPath());
						tree.addSelectionPath(path);
						tree.scrollPathToVisible(path);
					}
				}
				return true;
			} catch (Exception e) {
				logger.error("Import data failed: " + e.getMessage(), e);
			}
			return false;
		}
	}
}
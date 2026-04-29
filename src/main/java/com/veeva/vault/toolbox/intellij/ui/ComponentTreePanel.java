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
import java.awt.*;
import java.io.File;

public class ComponentTreePanel extends JPanel {
	private static final Logger logger = LoggerFactory.getLogger(ComponentTreePanel.class);

	ToolboxProject toolboxProject;
	JTree tree;
	DefaultTreeModel treeModel;
	ToolboxTreeNode rootNode;
	final VpkBuildManifest buildManifest;

	public ComponentTreePanel(ToolboxProject toolboxProject, VpkBuildManifest buildManifest) {
		super();
		this.toolboxProject = toolboxProject;
		this.buildManifest = buildManifest;
		init();
	}

	private void init() {
		this.setLayout(new BorderLayout());
		ToolboxTreeNodeRenderer renderer = new ToolboxTreeNodeRenderer();
		rootNode = new ToolboxTreeNode("Components", true, ToolboxIcons.Menu);
		treeModel = new DefaultTreeModel(rootNode);

		tree = new Tree(treeModel);
		tree.setOpaque(false);
		tree.setCellRenderer(renderer);
		tree.addMouseListener(new ToolboxTreeNodeMouseListener(tree));

		this.add(createToolbar(), BorderLayout.NORTH);
		this.add(new JScrollPane(tree), BorderLayout.CENTER);

		buildTree();
	}

	private JComponent createToolbar() {
		DefaultActionGroup actionGroup = new DefaultActionGroup();
		
		actionGroup.add(new AnAction("Add Component", "Add a new component", AllIcons.General.Add) {
			@Override
			public void actionPerformed(@NotNull AnActionEvent e) {
				addComponent();
			}
		});
		
		actionGroup.add(new AnAction("Remove Component", "Remove selected component", AllIcons.General.Remove) {
			@Override
			public void actionPerformed(@NotNull AnActionEvent e) {
				removeComponent();
			}
			@Override
			public void update(@NotNull AnActionEvent e) {
				e.getPresentation().setEnabled(getSelectedComponent() != null);
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
		
		// Select the newly added component
		int newIndex = buildManifest.getComponents().size() - 1;
		selectComponentAtIndex(newIndex);
	}
	
	private boolean isValidFileType(String path) {
		if (path == null) return false;
		String lowerPath = path.toLowerCase();
		return lowerPath.endsWith(".mdl") || lowerPath.endsWith(".csv") || lowerPath.endsWith(".json");
	}

	private VpkBuildManifest.Component getSelectedComponent() {
		TreePath selectionPath = tree.getSelectionPath();
		if (selectionPath != null) {
			ToolboxTreeNode selectedNode = (ToolboxTreeNode) selectionPath.getLastPathComponent();
			Object userObject = selectedNode.getUserObject();
			if (userObject instanceof VpkBuildManifest.Component) {
				return (VpkBuildManifest.Component) userObject;
			}
		}
		return null;
	}

	private void removeComponent() {
		VpkBuildManifest.Component component = getSelectedComponent();
		if (component != null) {
			int index = buildManifest.getComponents().indexOf(component);
			buildManifest.removeComponent(component);
			regenerateStepLabels();
			buildTree();
			
			// Select the next component or previous if last was removed
			if (buildManifest.getComponents().size() > 0) {
				int newIndex = Math.min(index, buildManifest.getComponents().size() - 1);
				selectComponentAtIndex(newIndex);
			}
		}
	}

	private void moveComponentUp() {
		int index = getSelectedComponentIndex();
		if (index > 0) {
			buildManifest.moveComponent(index, index - 1);
			regenerateStepLabels();
			buildTree();
			selectComponentAtIndex(index - 1);
		}
	}

	private void moveComponentDown() {
		int index = getSelectedComponentIndex();
		if (index >= 0 && buildManifest.getComponents() != null && index < buildManifest.getComponents().size() - 1) {
			buildManifest.moveComponent(index, index + 1);
			regenerateStepLabels();
			buildTree();
			selectComponentAtIndex(index + 1);
		}
	}
	
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

	private boolean canMoveUp() {
		int index = getSelectedComponentIndex();
		return index > 0;
	}

	private boolean canMoveDown() {
		int index = getSelectedComponentIndex();
		return index >= 0 && buildManifest.getComponents() != null && index < buildManifest.getComponents().size() - 1;
	}

	private String getFileType(String path) {
		if (path == null) return "Unknown";
		String lowerPath = path.toLowerCase();
		if (lowerPath.endsWith(".mdl")) return "MDL";
		if (lowerPath.endsWith(".csv")) return "CSV";
		if (lowerPath.endsWith(".json")) return "JSON";
		return "Unknown";
	}

	void buildTree() {
		rootNode.removeAllChildren();
		java.util.List<VpkBuildManifest.Component> components = buildManifest.getComponents();
		if (components != null && components.size() > 0) {
			for (VpkBuildManifest.Component component : components) {
				// Store the component object in the node, but set the text explicitly
				ToolboxTreeNode stepNode = new ToolboxTreeNode(component, true, ToolboxIcons.ComponentFolder);
				String type = getFileType(component.getPath());
				stepNode.setText(component.getStep());

				if ("CSV".equals(type)) {
					// Add CSV file node
					ToolboxTreeNode csvNode = new ToolboxTreeNode(component, true, ToolboxIcons.Database);
					csvNode.setText(component.getPath());
					csvNode.toolboxTreeNodeListener = new com.veeva.vault.toolbox.intellij.listeners.ToolboxTreeNodeListener() {
						@Override
						public void singleClick(ToolboxTreeNode node) {}

						@Override
						public void doubleClick(ToolboxTreeNode node) {
							openCsvDataViewer(component);
						}
					};
					stepNode.add(csvNode);

					// Add XML manifest node
					String xmlPath = component.getPath().substring(0, component.getPath().lastIndexOf(".")) + ".xml";
					File xmlFile = new File(toolboxProject.getProject().getBasePath(), xmlPath);
					
					ToolboxTreeNode xmlNode = new ToolboxTreeNode(component, true, ToolboxIcons.Xml);
					if (xmlFile.exists()) {
						xmlNode.setText(xmlPath);
					} else {
						xmlNode.setText("{missing data manifest}");
					}

					xmlNode.toolboxTreeNodeListener = new com.veeva.vault.toolbox.intellij.listeners.ToolboxTreeNodeListener() {
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

	private void openCsvDataViewer(VpkBuildManifest.Component component) {
		File csvFile = new File(toolboxProject.getProject().getBasePath(), component.getPath());
		if (csvFile.exists()) {
			CsvDataViewerDialog dialog = new CsvDataViewerDialog(csvFile);
			dialog.show();
		} else {
			Messages.showErrorDialog(this, "CSV file not found: " + csvFile.getAbsolutePath(), "Error");
		}
	}
}

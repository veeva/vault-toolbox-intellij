package com.veeva.vault.toolbox.intellij.ui;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.treeStructure.Tree;
import com.veeva.vault.toolbox.intellij.listeners.ToolboxTreeNodeListener;
import com.veeva.vault.toolbox.intellij.project.ToolboxProject;
import com.veeva.vault.toolbox.core.models.VpkBuildManifest;
import icons.ToolboxIcons;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.io.File;

public class DistributionTreePanel extends JPanel {
	private static final Logger logger = LoggerFactory.getLogger(DistributionTreePanel.class);

	ToolboxProject toolboxProject;
	JTree tree;
	DefaultTreeModel treeModel;
	ToolboxTreeNode rootNode;
	final VpkBuildManifest buildManifest;

	public DistributionTreePanel(ToolboxProject toolboxProject, VpkBuildManifest buildManifest) {
		super();
		this.toolboxProject = toolboxProject;
		this.buildManifest = buildManifest;
		init();
	}

	private void init() {
		this.setLayout(new BorderLayout());
		ToolboxTreeNodeRenderer renderer = new ToolboxTreeNodeRenderer();
		rootNode = new ToolboxTreeNode("Distributions", true, ToolboxIcons.Menu);
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
		
		actionGroup.add(new AnAction("Add Distribution", "Add a new distribution", AllIcons.General.Add) {
			@Override
			public void actionPerformed(@NotNull AnActionEvent e) {
				addDistribution();
			}
		});
		
		actionGroup.add(new AnAction("Edit Distribution", "Edit selected distribution", AllIcons.Actions.Edit) {
			@Override
			public void actionPerformed(@NotNull AnActionEvent e) {
				editDistribution();
			}
			@Override
			public void update(@NotNull AnActionEvent e) {
				e.getPresentation().setEnabled(getSelectedDistribution() != null);
			}
		});
		
		actionGroup.add(new AnAction("Remove Distribution", "Remove selected distribution", AllIcons.General.Remove) {
			@Override
			public void actionPerformed(@NotNull AnActionEvent e) {
				removeDistribution();
			}
			@Override
			public void update(@NotNull AnActionEvent e) {
				e.getPresentation().setEnabled(getSelectedDistribution() != null);
			}
		});

		ActionToolbar actionToolbar = ActionManager.getInstance().createActionToolbar("DistributionTreeToolbar", actionGroup, true);
		actionToolbar.setTargetComponent(this);
		return actionToolbar.getComponent();
	}

	private void addDistribution() {
		DistributionDialog dialog = new DistributionDialog(null);
		if (dialog.showAndGet()) {
			VpkBuildManifest.WebSdk.Distribution distribution = dialog.getDistribution();
			if (buildManifest.getWebSdk() == null) {
				buildManifest.setWebSdk(new VpkBuildManifest.WebSdk());
			}
			buildManifest.getWebSdk().addDistribution(distribution);
			buildTree();
		}
	}

	private void editDistribution() {
		VpkBuildManifest.WebSdk.Distribution distribution = getSelectedDistribution();
		if (distribution != null) {
			DistributionDialog dialog = new DistributionDialog(distribution);
			if (dialog.showAndGet()) {
				// Update is handled by reference modification in dialog or we can replace it
				// Since we modify the object directly in the dialog (if we pass it), we just need to refresh tree
				buildTree();
			}
		}
	}

	private void removeDistribution() {
		VpkBuildManifest.WebSdk.Distribution distribution = getSelectedDistribution();
		if (distribution != null && buildManifest.getWebSdk() != null) {
			buildManifest.getWebSdk().removeDistribution(distribution);
			buildTree();
		}
	}

	private VpkBuildManifest.WebSdk.Distribution getSelectedDistribution() {
		TreePath selectionPath = tree.getSelectionPath();
		if (selectionPath != null) {
			ToolboxTreeNode selectedNode = (ToolboxTreeNode) selectionPath.getLastPathComponent();
			Object userObject = selectedNode.getUserObject();
			if (userObject instanceof VpkBuildManifest.WebSdk.Distribution) {
				return (VpkBuildManifest.WebSdk.Distribution) userObject;
			}
			// Handle child nodes (properties) - find parent
			if (selectedNode.getParent() instanceof ToolboxTreeNode) {
				Object parentObject = ((ToolboxTreeNode) selectedNode.getParent()).getUserObject();
				if (parentObject instanceof VpkBuildManifest.WebSdk.Distribution) {
					return (VpkBuildManifest.WebSdk.Distribution) parentObject;
				}
			}
		}
		return null;
	}

	void buildTree() {
		rootNode.removeAllChildren();
		if (buildManifest.getWebSdk() != null) {
			java.util.List<VpkBuildManifest.WebSdk.Distribution> distributions = buildManifest.getWebSdk().getDistributions();
			if (distributions != null && distributions.size() > 0) {
				for (VpkBuildManifest.WebSdk.Distribution distribution : distributions) {
					ToolboxTreeNode distributionNode = new ToolboxTreeNode(distribution, true, ToolboxIcons.ComponentFolder, new ToolboxTreeNodeListener() {
						@Override
						public void singleClick(ToolboxTreeNode node) {}

						@Override
						public void doubleClick(ToolboxTreeNode node) {
							editDistribution();
						}
					});
					distributionNode.setText(distribution.getName());
					
					ToolboxTreeNode manifestNode = new ToolboxTreeNode("Manifest: " + distribution.getManifest(), true, ToolboxIcons.Configured);
					ToolboxTreeNode distFolderNode = new ToolboxTreeNode("Path: " + distribution.getPath(), true, ToolboxIcons.Folder);
					ToolboxTreeNode shellNode = new ToolboxTreeNode("Shell: " + distribution.getShell(), true, ToolboxIcons.Code);
					
					distributionNode.add(manifestNode);
					distributionNode.add(distFolderNode);
					if (distribution.getShell() != null && !distribution.getShell().isEmpty()) {
						distributionNode.add(shellNode);
					}
					rootNode.add(distributionNode);
				}
			}
		}
		treeModel.reload();
		for (int i = 0; i < tree.getRowCount(); i++) {
			tree.expandRow(i);
		}
		tree.setRootVisible(false);
	}

	private class DistributionDialog extends DialogWrapper {
		private JTextField nameField;
		private TextFieldWithBrowseButton manifestField;
		private TextFieldWithBrowseButton pathField;
		private TextFieldWithBrowseButton shellField;
		private VpkBuildManifest.WebSdk.Distribution distribution;

		public DistributionDialog(VpkBuildManifest.WebSdk.Distribution distribution) {
			super(true);
			this.distribution = distribution;
			setTitle(distribution == null ? "Add Distribution" : "Edit Distribution");
			init();
		}

		@Override
		protected @Nullable JComponent createCenterPanel() {
			JPanel panel = new JPanel(new GridLayout(4, 2));
			
			nameField = new JTextField();
			manifestField = new TextFieldWithBrowseButton();
			pathField = new TextFieldWithBrowseButton();
			shellField = new TextFieldWithBrowseButton();

			VirtualFile projectFile = VfsUtil.findFileByIoFile(new File(toolboxProject.getProject().getBasePath()), true);

			// Configure file choosers
			FileChooserDescriptor manifestDescriptor = FileChooserDescriptorFactory.createSingleFileDescriptor("json");
			manifestDescriptor.setForcedToUseIdeaFileChooser(true);
			manifestDescriptor.setRoots(projectFile);
			manifestDescriptor.withTitle("Select Manifest");
			manifestField.addBrowseFolderListener(toolboxProject.getProject(), manifestDescriptor);

			FileChooserDescriptor pathDescriptor = FileChooserDescriptorFactory.createSingleFolderDescriptor();
			pathDescriptor.setForcedToUseIdeaFileChooser(true);
			pathDescriptor.setRoots(projectFile);
			pathDescriptor.withTitle("Select Distribution Path");
			pathField.addBrowseFolderListener(toolboxProject.getProject(), pathDescriptor);

			FileChooserDescriptor shellDescriptor = FileChooserDescriptorFactory.createSingleFileNoJarsDescriptor(); // .sh or .cmd
			shellDescriptor.setForcedToUseIdeaFileChooser(true);
			shellDescriptor.setRoots(projectFile);
			shellDescriptor.withTitle("Select Shell Script");
			shellField.addBrowseFolderListener(toolboxProject.getProject(), shellDescriptor);

			if (distribution != null) {
				nameField.setText(distribution.getName());
				manifestField.setText(distribution.getManifest());
				pathField.setText(distribution.getPath());
				shellField.setText(distribution.getShell());
			}

			panel.add(new JLabel("Name:"));
			panel.add(nameField);
			panel.add(new JLabel("Manifest:"));
			panel.add(manifestField);
			panel.add(new JLabel("Path:"));
			panel.add(pathField);
			panel.add(new JLabel("Shell:"));
			panel.add(shellField);

			return panel;
		}

		public VpkBuildManifest.WebSdk.Distribution getDistribution() {
			if (distribution == null) {
				distribution = new VpkBuildManifest.WebSdk.Distribution();
			}
			distribution.setName(nameField.getText().replace(toolboxProject.getProject().getBasePath(), ""));
			distribution.setManifest(manifestField.getText().replace(toolboxProject.getProject().getBasePath(), ""));
			distribution.setPath(pathField.getText().replace(toolboxProject.getProject().getBasePath(), ""));
			distribution.setShell(shellField.getText().replace(toolboxProject.getProject().getBasePath(), ""));
			return distribution;
		}
	}
}

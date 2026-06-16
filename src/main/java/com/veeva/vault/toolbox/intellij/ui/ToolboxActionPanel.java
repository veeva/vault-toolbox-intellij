package com.veeva.vault.toolbox.intellij.ui;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.ui.treeStructure.Tree;
import com.veeva.vault.toolbox.intellij.listeners.ToolboxTreeNodeListener;
import com.veeva.vault.toolbox.intellij.project.ToolboxProject;
import com.veeva.vault.toolbox.intellij.tasks.ConfigurationReportTask;
import com.veeva.vault.toolbox.intellij.tasks.ExtractMdlTask;
import com.veeva.vault.toolbox.intellij.tasks.ExtractSdkTask;
import icons.ToolboxIcons;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.io.File;

/**
 * Provides a tree-based navigation panel for executing various Vault developer actions,
 * including log retrieval, configuration management, and package deployment.
 */
public class ToolboxActionPanel extends JPanel {
	private static final Logger logger = LoggerFactory.getLogger(ToolboxActionPanel.class);

	/**
	 * The current project context.
	 */
	ToolboxProject toolboxProject;

	/**
	 * The tree component used to display the action nodes.
	 */
	JTree tree;

	/**
	 * The root node for developer logs.
	 */
	ToolboxTreeNode logsNode;

	/**
	 * The root node for configuration components.
	 */
	ToolboxTreeNode configurationNode;

	/**
	 * The root node for deployment actions.
	 */
	ToolboxTreeNode deploymentNode;

	/**
	 * The main root node of the action tree.
	 */
	ToolboxTreeNode rootNode;

	/**
	 * Creates a new action panel for the specified project.
	 *
	 * @param toolboxProject The toolbox project context.
	 */
	public ToolboxActionPanel(ToolboxProject toolboxProject) {
		super();
		this.toolboxProject = toolboxProject;
		init();
	}

	/**
	 * Initializes the tree component and its visual properties.
	 */
	private void init() {
		this.setLayout(new BorderLayout());
		ToolboxTreeNodeRenderer renderer = new ToolboxTreeNodeRenderer();
		rootNode = new ToolboxTreeNode("Veeva Vault", true, ToolboxIcons.Logs);

		tree = new Tree(rootNode);
		tree.setOpaque(false);
		tree.setCellRenderer(renderer);
		tree.addMouseListener(new ToolboxTreeNodeMouseListener(tree));
		this.add(tree, BorderLayout.CENTER);
		for (int i = 0; i < tree.getRowCount(); i++) {
			tree.expandRow(i);
		}

		ApplicationManager.getApplication().invokeLater(this::buildTree);
	}

	/**
	 * Constructs the action tree structure and defines the behavior for each action node.
	 */
	void buildTree() {
		logsNode = new ToolboxTreeNode("Developer Logs", true, ToolboxIcons.Code);
		configurationNode = new ToolboxTreeNode("Components", true, ToolboxIcons.Component);
		deploymentNode = new ToolboxTreeNode("Deployment", true, ToolboxIcons.Vpk);

		ToolboxTreeNode apiUsageNode = new ToolboxTreeNode(
				"API Usage",
				true,
				ToolboxIcons.Api,
				new ToolboxTreeNodeListener() {
					/**
					 * Handles single click.
					 *
					 * @param node the node
					 */
					@Override
					public void singleClick(ToolboxTreeNode node) {
					}

					/**
					 * Handles double click.
					 *
					 * @param node the node
					 */
					@Override
					public void doubleClick(ToolboxTreeNode node) {
						ApplicationManager.getApplication().invokeLater(() -> {
							if (toolboxProject.prepareRequest()) {
								DeveloperLogsDialog logsDialog = new DeveloperLogsDialog(toolboxProject, DeveloperLogsDialog.LogType.API_USAGE);
								logsDialog.show();
							}
						});
					}
				});

		ToolboxTreeNode debugNode = new ToolboxTreeNode(
				"SDK Debug",
				true,
				ToolboxIcons.Debug,
				new ToolboxTreeNodeListener() {
					/**
					 * Handles single click.
					 *
					 * @param node the node
					 */
					@Override
					public void singleClick(ToolboxTreeNode node) {
					}

					/**
					 * Handles double click.
					 *
					 * @param node the node
					 */
					@Override
					public void doubleClick(ToolboxTreeNode node) {
						ApplicationManager.getApplication().invokeLater(() -> {
							if (toolboxProject.prepareRequest()) {
								DeveloperLogsDialog logsDialog = new DeveloperLogsDialog(toolboxProject, DeveloperLogsDialog.LogType.SDK_DEBUG);
								logsDialog.show();
							}
						});
					}
				});

		ToolboxTreeNode runtimeNode = new ToolboxTreeNode(
				"SDK Runtime",
				true,
				ToolboxIcons.Runtime,
				new ToolboxTreeNodeListener() {
					/**
					 * Handles single click.
					 *
					 * @param node the node
					 */
					@Override
					public void singleClick(ToolboxTreeNode node) {
					}

					/**
					 * Handles double click.
					 *
					 * @param node the node
					 */
					@Override
					public void doubleClick(ToolboxTreeNode node) {
						ApplicationManager.getApplication().invokeLater(() -> {
							if (toolboxProject.prepareRequest()) {
								DeveloperLogsDialog logsDialog = new DeveloperLogsDialog(toolboxProject, DeveloperLogsDialog.LogType.SDK_RUNTIME);
								logsDialog.show();
							}
						});
					}
				});

		ToolboxTreeNode profilerNode = new ToolboxTreeNode(
				"SDK Profiler",
				true,
				ToolboxIcons.Atom,
				new ToolboxTreeNodeListener() {
					/**
					 * Handles single click.
					 *
					 * @param node the node
					 */
					@Override
					public void singleClick(ToolboxTreeNode node) {
					}

					/**
					 * Handles double click.
					 *
					 * @param node the node
					 */
					@Override
					public void doubleClick(ToolboxTreeNode node) {
						ApplicationManager.getApplication().invokeLater(() -> {
							if (toolboxProject.prepareRequest()) {
								DeveloperLogsDialog logsDialog = new DeveloperLogsDialog(toolboxProject, DeveloperLogsDialog.LogType.SDK_PROFILER);
								logsDialog.show();
							}
						});
					}
				});

		logsNode.add(apiUsageNode);
		logsNode.add(debugNode);
		logsNode.add(runtimeNode);
		logsNode.add(profilerNode);

		ToolboxTreeNode configReportNode = new ToolboxTreeNode(
				"Download Configuration Report",
				true,
				ToolboxIcons.Download,
				new ToolboxTreeNodeListener() {
					/**
					 * Handles single click.
					 *
					 * @param node the node
					 */
					@Override
					public void singleClick(ToolboxTreeNode node) {
					}

					/**
					 * Handles double click.
					 *
					 * @param node the node
					 */
					@Override
					public void doubleClick(ToolboxTreeNode node) {
						ApplicationManager.getApplication().invokeLater(() -> {
							if (ConfigurationReportTask.isDownloading.get()) {
								Message message = toolboxProject.newMessage();
								message.setTitle("Download in Progress");
								message.append("A Configuration Report is already downloading. Please wait for it to finish before starting another.");
								message.showWarning();
								return;
							}

							if (toolboxProject.prepareRequest()) {
								ConfigurationReportDialog dialog = new ConfigurationReportDialog(toolboxProject.getProject());
								if (dialog.showAndGet()) {
									ConfigurationReportTask.isDownloading.set(true);

									ConfigurationReportTask task = new ConfigurationReportTask(toolboxProject.getProject(), dialog.getOptions());
									task.queue();
								}
							}
						});

					}
				});

		ToolboxTreeNode mdlExtractNode = new ToolboxTreeNode(
				"Extract MDL from Vault",
				true,
				ToolboxIcons.Download,
				new ToolboxTreeNodeListener() {
					/**
					 * Handles single click.
					 *
					 * @param node the node
					 */
					@Override
					public void singleClick(ToolboxTreeNode node) {
					}

					/**
					 * Handles double click.
					 *
					 * @param node the node
					 */
					@Override
					public void doubleClick(ToolboxTreeNode node) {
						ApplicationManager.getApplication().invokeLater(() -> {
							if (toolboxProject.prepareRequest()) {
								File vaultMdlDir = new File(toolboxProject.getMdlDirectory(), toolboxProject.getVaultId().toString());
								boolean mdlAlreadyExtracted = vaultMdlDir.exists()
										&& !FileUtils.listFiles(vaultMdlDir, new String[]{"mdl"}, true).isEmpty();

								if (mdlAlreadyExtracted) {
									MdlDialog mdlDialog = new MdlDialog(toolboxProject, MdlDialog.ActionType.DOWNLOAD);
									if (!mdlDialog.showAndGet()) {
										return;
									}
								} else {
									ExtractMdlDialog mdlDialog = new ExtractMdlDialog(toolboxProject);
									if (!mdlDialog.showAndGet()) {
										return;
									}
								}

								ExtractMdlTask task = new ExtractMdlTask(toolboxProject.getProject());
								task.queue();
							}
						});
					}
				});

		ToolboxTreeNode sdkExtractNode = new ToolboxTreeNode(
				"Extract SDK from Vault",
				true,
				ToolboxIcons.Download,
				new ToolboxTreeNodeListener() {
					/**
					 * Handles single click.
					 *
					 * @param node the node
					 */
					@Override
					public void singleClick(ToolboxTreeNode node) {
					}

					/**
					 * Handles double click.
					 *
					 * @param node the node
					 */
					@Override
					public void doubleClick(ToolboxTreeNode node) {
						ApplicationManager.getApplication().invokeLater(() -> {
							if (toolboxProject.prepareRequest()) {
								File vaultSdkDir = new File(new File(toolboxProject.getToolboxDirectory(), "sdk"), toolboxProject.getVaultId().toString());
								boolean sdkAlreadyExtracted = vaultSdkDir.exists()
										&& !FileUtils.listFiles(vaultSdkDir, new String[]{"java"}, true).isEmpty();

								if (sdkAlreadyExtracted) {
									SdkDialog sdkDialog = new SdkDialog(toolboxProject, SdkDialog.ActionType.DOWNLOAD);
									if (!sdkDialog.showAndGet()) {
										return;
									}
								} else {
									ExtractSdkDialog sdkDialog = new ExtractSdkDialog(toolboxProject);
									if (!sdkDialog.showAndGet()) {
										return;
									}
								}
								
								ExtractSdkTask task = new ExtractSdkTask(toolboxProject.getProject());
								task.queue();
							}
						});
					}
				});

		ToolboxTreeNode compareEnvNode = new ToolboxTreeNode(
				"Compare Environments",
				true,
				ToolboxIcons.DoubleRight,
				new ToolboxTreeNodeListener() {
					/**
					 * Handles single click.
					 *
					 * @param node the node
					 */
					@Override
					public void singleClick(ToolboxTreeNode node) {
					}

					/**
					 * Handles double click.
					 *
					 * @param node the node
					 */
					@Override
					public void doubleClick(ToolboxTreeNode node) {
						ApplicationManager.getApplication().invokeLater(() -> {
							new CompareEnvironmentsResultDialog(toolboxProject).show();
						});
					}
				});

		configurationNode.add(configReportNode);
		configurationNode.add(mdlExtractNode);
		configurationNode.add(sdkExtractNode);
		configurationNode.add(compareEnvNode);

		ToolboxTreeNode localPackagesNode = new ToolboxTreeNode(
				"Local Packages",
				true,
				ToolboxIcons.Vpk,
				new ToolboxTreeNodeListener() {
					/**
					 * Handles single click.
					 *
					 * @param node the node
					 */
					@Override
					public void singleClick(ToolboxTreeNode node) {
					}

					/**
					 * Handles double click.
					 *
					 * @param node the node
					 */
					@Override
					public void doubleClick(ToolboxTreeNode node) {
						ApplicationManager.getApplication().invokeLater(() -> {
							DeploymentDialog dialog = new DeploymentDialog(toolboxProject, DeploymentDialog.PackageType.LOCAL);
							dialog.show();
						});
					}
				});

		ToolboxTreeNode inboundPackagesNode = new ToolboxTreeNode(
				"Inbound Packages",
				true,
				ToolboxIcons.Vpk,
				new ToolboxTreeNodeListener() {
					/**
					 * Handles single click.
					 *
					 * @param node the node
					 */
					@Override
					public void singleClick(ToolboxTreeNode node) {
					}

					/**
					 * Handles double click.
					 *
					 * @param node the node
					 */
					@Override
					public void doubleClick(ToolboxTreeNode node) {
						ApplicationManager.getApplication().invokeLater(() -> {
							if (toolboxProject.prepareRequest()) {
								DeploymentDialog dialog = new DeploymentDialog(toolboxProject, DeploymentDialog.PackageType.INBOUND);
								dialog.show();
							}
						});
					}
				});

		deploymentNode.add(localPackagesNode);
		deploymentNode.add(inboundPackagesNode);

		rootNode.add(logsNode);
		rootNode.add(configurationNode);
		rootNode.add(deploymentNode);

		for (int i = 0; i < tree.getRowCount(); i++) {
			tree.expandRow(i);
		}
		tree.setRootVisible(false);
	}

}

package com.veeva.vault.toolbox.intellij.ui;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.ui.treeStructure.Tree;
import com.veeva.vault.toolbox.intellij.listeners.ToolboxTreeNodeListener;
import com.veeva.vault.toolbox.intellij.project.ToolboxProject;
import com.veeva.vault.toolbox.intellij.tasks.AnalyzApiLogTask;
import com.veeva.vault.toolbox.intellij.tasks.AnalyzDebugLogTask;
import com.veeva.vault.toolbox.intellij.tasks.ConfiguratonReportTask;
import com.veeva.vault.toolbox.intellij.tasks.ExtractMdlTask;
import icons.ToolboxIcons;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;

public class ToolboxActionPanel extends JPanel {
	private static final Logger logger = LoggerFactory.getLogger(ToolboxActionPanel.class);

	ToolboxProject toolboxProject;
	JTree tree;
	ToolboxTreeNode logsNode;
	ToolboxTreeNode configurationNode;
	ToolboxTreeNode deploymentNode;
	ToolboxTreeNode rootNode;

	public ToolboxActionPanel(ToolboxProject toolboxProject) {
		super();
		this.toolboxProject = toolboxProject;
		init();
	}

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

		ApplicationManager.getApplication().invokeLater(()-> {
			buildTree();
		});
	}

	void buildTree() {
		logsNode = new ToolboxTreeNode("Developer Logs", true, ToolboxIcons.Code);
		configurationNode = new ToolboxTreeNode("Components", true, ToolboxIcons.Component);
		deploymentNode = new ToolboxTreeNode("Deployment", true, ToolboxIcons.Vpk);

		//-----------------------------------------------------------------------------------------------
		//LOGS
		//-----------------------------------------------------------------------------------------------
		ToolboxTreeNode apiUsageNode = new ToolboxTreeNode(
				"API Usage",
				true,
				ToolboxIcons.Api,
				new ToolboxTreeNodeListener() {
					@Override
					public void singleClick(ToolboxTreeNode node) {
					}

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
					@Override
					public void singleClick(ToolboxTreeNode node) {
					}

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
					@Override
					public void singleClick(ToolboxTreeNode node) {
					}

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
					@Override
					public void singleClick(ToolboxTreeNode node) {
					}

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

		//-----------------------------------------------------------------------------------------------


		//-----------------------------------------------------------------------------------------------
		//CONFIG
		//-----------------------------------------------------------------------------------------------
        ToolboxTreeNode configReportNode = new ToolboxTreeNode(
                "Download Configuration Report",
                true,
                ToolboxIcons.Download,
                new ToolboxTreeNodeListener() {
                    @Override
                    public void singleClick(ToolboxTreeNode node) {
                    }

                    @Override
                    public void doubleClick(ToolboxTreeNode node) {
                        ApplicationManager.getApplication().invokeLater(() -> {
                            if (ConfiguratonReportTask.isDownloading.get()) {
                                Message message = toolboxProject.newMessage();
                                message.setTitle("Download in Progress");
                                message.append("A Configuration Report is already downloading. Please wait for it to finish before starting another.");
                                message.showWarning();
                                return;
                            }

                            if (toolboxProject.prepareRequest()) {
                                ConfiguratonReportTask.isDownloading.set(true);

                                ConfiguratonReportTask task = new ConfiguratonReportTask(toolboxProject.getProject());
                                task.queue();
                            }
                        });

                    }
                });

		ToolboxTreeNode mdlExtractNode = new ToolboxTreeNode(
				"Extract MDL from Vault",
				true,
				ToolboxIcons.Download,
				new ToolboxTreeNodeListener() {
					@Override
					public void singleClick(ToolboxTreeNode node) {
					}

					@Override
					public void doubleClick(ToolboxTreeNode node) {
						ApplicationManager.getApplication().invokeLater(() -> {
							if (toolboxProject.prepareRequest()) {
								MdlDialog mdlDialog = new MdlDialog(toolboxProject, MdlDialog.ActionType.DOWNLOAD);
								if (mdlDialog.showAndGet()) {
									ExtractMdlTask task = new ExtractMdlTask(toolboxProject.getProject());
									task.queue();
								}
							}
						});
					}
				});

		ToolboxTreeNode sdkExtractNode = new ToolboxTreeNode(
				"Extract SDK from Vault",
				true,
				ToolboxIcons.Download,
				new ToolboxTreeNodeListener() {
					@Override
					public void singleClick(ToolboxTreeNode node) {
					}

					@Override
					public void doubleClick(ToolboxTreeNode node) {
						ApplicationManager.getApplication().invokeLater(() -> {
							if (toolboxProject.isToolboxEnabled()) {
								ExtractSdkDialog sdkDialog = new ExtractSdkDialog(toolboxProject);
								sdkDialog.show();
							}
						});
					}
				});

		configurationNode.add(configReportNode);
		configurationNode.add(mdlExtractNode);
		//configurationNode.add(sdkExtractNode);

		/*
		//-----------------------------------------------------------------------------------------------
		//Vault Packages
		//-----------------------------------------------------------------------------------------------
		ToolboxTreeNode designVpkNode = new ToolboxTreeNode(
				"Design",
				true,
				ToolboxIcons.Pencil,
				new ToolboxTreeNodeListener() {
					@Override
					public void singleClick(ToolboxTreeNode node) {
					}

					@Override
					public void doubleClick(ToolboxTreeNode node) {
						PackageDialog packageDialog = new PackageDialog(toolboxProject, PackageDialog.ActionType.DESIGN);
						packageDialog.show();
					}
				});

		ToolboxTreeNode buildVpkNode = new ToolboxTreeNode(
				"Build",
				true,
				ToolboxIcons.Box,
				new ToolboxTreeNodeListener() {
					@Override
					public void singleClick(ToolboxTreeNode node) {
					}

					@Override
					public void doubleClick(ToolboxTreeNode node) {
						PackageDialog packageDialog = new PackageDialog(toolboxProject, PackageDialog.ActionType.BUILD);
						packageDialog.show();

					}
				});

		ToolboxTreeNode deployVpkNode = new ToolboxTreeNode(
				"Deploy",
				true,
				ToolboxIcons.Upload,
				new ToolboxTreeNodeListener() {
					@Override
					public void singleClick(ToolboxTreeNode node) {
					}

					@Override
					public void doubleClick(ToolboxTreeNode node) {
						ApplicationManager.getApplication().invokeLater(() -> {
							if (toolboxProject.prepareRequest()) {
								PackageDialog packageDialog = new PackageDialog(toolboxProject, PackageDialog.ActionType.DEPLOY);
								packageDialog.show();
							}

						});

					}
				});

		ToolboxTreeNode buildAndDeployVpkNode = new ToolboxTreeNode(
				"Build and Deploy",
				true,
				ToolboxIcons.DoubleRight,
				new ToolboxTreeNodeListener() {
					@Override
					public void singleClick(ToolboxTreeNode node) {
					}

					@Override
					public void doubleClick(ToolboxTreeNode node) {
						ApplicationManager.getApplication().invokeLater(() -> {
							if (toolboxProject.prepareRequest()) {
								PackageDialog packageDialog = new PackageDialog(toolboxProject, PackageDialog.ActionType.BUILD_DEPLOY);
								packageDialog.show();
							}

						});

					}
				});

		deploymentNode.add(designVpkNode);
		deploymentNode.add(buildVpkNode);
		deploymentNode.add(deployVpkNode);
		deploymentNode.add(buildAndDeployVpkNode);
		//-----------------------------------------------------------------------------------------------
		*/

		//-----------------------------------------------------------------------------------------------
		//Deployment Packages
		//-----------------------------------------------------------------------------------------------
		ToolboxTreeNode localPackagesNode = new ToolboxTreeNode(
				"Local Packages",
				true,
				ToolboxIcons.Vpk,
				new ToolboxTreeNodeListener() {
					@Override
					public void singleClick(ToolboxTreeNode node) {
					}

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
					@Override
					public void singleClick(ToolboxTreeNode node) {
					}

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
		//-----------------------------------------------------------------------------------------------

		rootNode.add(logsNode);
		rootNode.add(configurationNode);
		rootNode.add(deploymentNode);

		for (int i = 0; i < tree.getRowCount(); i++) {
			tree.expandRow(i);
		}
		tree.setRootVisible(false);
	}
}

package com.veeva.vault.toolbox.intellij.ui;

import com.intellij.openapi.ui.DialogWrapper;
import com.veeva.vault.toolbox.intellij.project.ToolboxProject;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

/**
 * A dialog wrapper that hosts the deployment management interface.
 * Supports switching between local and inbound package views.
 */
public class DeploymentDialog extends DialogWrapper {

    /**
     * Defines the source types for Vault packages.
     */
    public enum PackageType {
        LOCAL("Local Packages"),
        INBOUND("Inbound Packages");

        private final String displayName;

        PackageType(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
        
        @Override
        public String toString() {
            return displayName;
        }
    }

    private final ToolboxProject toolboxProject;
    private final PackageType initialType;
    private DeploymentPanel deploymentPanel;

    /**
     * Initializes the deployment dialog.
     *
     * @param toolboxProject The toolbox project context.
     * @param initialType    The package type to display upon opening.
     */
    public DeploymentDialog(ToolboxProject toolboxProject, PackageType initialType) {
        super(toolboxProject.getProject(), true);
        this.toolboxProject = toolboxProject;
        this.initialType = initialType;
        init();
        setTitle("Deployment Packages");
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        deploymentPanel = new DeploymentPanel(toolboxProject, initialType);
        return deploymentPanel;
    }
}

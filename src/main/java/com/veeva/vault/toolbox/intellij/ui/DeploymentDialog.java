package com.veeva.vault.toolbox.intellij.ui;

import com.intellij.openapi.ui.DialogWrapper;
import com.veeva.vault.toolbox.intellij.project.ToolboxProject;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class DeploymentDialog extends DialogWrapper {
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

    public DeploymentDialog(ToolboxProject toolboxProject, PackageType initialType) {
        super(true); // use current window as parent
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

package com.veeva.vault.toolbox.intellij.ui;

import com.intellij.openapi.project.Project;

/**
 * Project-level bridge that lets other surfaces (e.g. the Schema Explorer) hand a query to the
 * VQL Console without holding a direct reference to it. The console registers itself when built;
 * callers ask the service to open a query in it.
 */
public final class VqlConsoleService {

    private VqlConsolePanel panel;

    public static VqlConsoleService getInstance(Project project) {
        return project.getService(VqlConsoleService.class);
    }

    /** Called by the VQL Console panel when it is constructed. */
    void register(VqlConsolePanel panel) {
        this.panel = panel;
    }

    /** @return whether a VQL Console is available to receive queries. */
    public boolean isAvailable() {
        return panel != null;
    }

    /** Prefills the given VQL into the console, selects its tab, and reveals it. */
    public void openWithQuery(String vql) {
        if (panel != null) {
            panel.openWithQuery(vql);
        }
    }
}

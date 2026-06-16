package com.veeva.vault.toolbox.intellij.ui;

import com.intellij.openapi.project.Project;

/**
 * Project-level bridge that lets other surfaces (e.g. the VQL Console) reveal a component in the
 * Schema Explorer without holding a direct reference to it. The Schema Explorer registers itself
 * when built; callers ask the service to reveal a named object/picklist.
 */
public final class SchemaExplorerService {

    private SchemaExplorerPanel panel;

    public static SchemaExplorerService getInstance(Project project) {
        return project.getService(SchemaExplorerService.class);
    }

    /** Called by the Schema Explorer panel when it is constructed. */
    void register(SchemaExplorerPanel panel) {
        this.panel = panel;
    }

    /** @return whether a Schema Explorer is available to receive reveal requests. */
    public boolean isAvailable() {
        return panel != null;
    }

    /** Selects the Schema Explorer tab and reveals the named object or picklist. */
    public void reveal(String name) {
        if (panel != null) {
            panel.revealComponent(name);
        }
    }
}

package com.veeva.vault.toolbox.intellij.ui;

import com.veeva.vault.vapil.api.request.LogRequest;
import java.lang.reflect.Method;

/**
 * Diagnostic utility that uses reflection to inspect the {@link LogRequest} class.
 * It identifies and prints methods related to "debug" functionality to help
 * developers verify available SDK logging endpoints.
 */
public class VapilInspector {

    /**
     * Main entry point that reflects on {@code LogRequest} and prints all methods
     * containing "debug" in their name along with their return types.
     *
     * @param args command line arguments (not used)
     */
    public static void main(String[] args) {
        for (Method m : LogRequest.class.getMethods()) {
            if (m.getName().toLowerCase().contains("debug")) {
                System.out.println(m.getName() + " -> " + m.getReturnType().getName());
            }
        }
    }
}

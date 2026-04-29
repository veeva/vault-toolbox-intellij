// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package com.veeva.vault.toolbox.intellij.settings;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

/*
 * Supports storing the application settings in a persistent way.
 * The {@link com.intellij.openapi.components.State State} and {@link Storage}
 * annotations define the name of the data and the filename where these persistent
 * application settings are stored.
 */

@State(
    name = "com.veeva.vault.toolbox.intellij.settings.AppSettings",
    storages = @Storage("VaultToolbox.xml")
)
public final class AppSettings implements PersistentStateComponent<AppSettings.AppState> {

  public static boolean requireRestart = false;

  public static class AppState {
    @NonNls
    public boolean autoConnect = false;
    public String vaultDNS = "";
    public String username = "";
    public boolean saveSecret = false;
    public boolean allowAllCertificates = false;
    public int csvMaxRows = 100;
    public Vault.AuthenticationType authenticationType = Vault.AuthenticationType.BASIC;
    public int connectionTimeout = 15;
  }

  private AppState state = new AppState();

  public static AppSettings getInstance() {
    return ApplicationManager.getApplication().getService(AppSettings.class);
  }

  @Override
  public AppState getState() {
    return state;
  }

  @Override
  public void loadState(@NotNull AppState state) {
    this.state = state;
  }

}

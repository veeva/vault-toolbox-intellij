// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package com.veeva.vault.toolbox.intellij.settings;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.project.Project;
import com.intellij.util.xmlb.XmlSerializerUtil;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/*
 * Supports storing the application settings in a persistent way.
 * The {@link com.intellij.openapi.components.State State} and {@link Storage}
 * annotations define the name of the data and the filename where these persistent
 * application settings are stored.
 */

@State(
		name = "com.veeva.vault.toolbox.intellij.settings.FileSettings",
		storages = @Storage("toolbox-files.xml")
)
public final class FileSettings implements PersistentStateComponent<FileSettings> {

	Map<String, ToolboxFile> linkedFiles = new HashMap<>();
	public Map<String, ToolboxFile> getLinkedFiles() { return linkedFiles; }
	public void setLinkedFiles(Map<String, ToolboxFile> linkedFiles) { this.linkedFiles = linkedFiles; }

	public void addLinkedFile(String localPath, String remoteMd5) {
		ToolboxFile toolboxFile = linkedFiles.get(localPath);
		if (toolboxFile == null) {
			toolboxFile = new ToolboxFile();
			toolboxFile.setLocalPath(localPath);
			linkedFiles.put(localPath, toolboxFile);
		}
		toolboxFile.setRemoteMd5(remoteMd5);
	}

	public ToolboxFile getLinkedFile(String localPath) {
		return linkedFiles.get(localPath);
	}

	public void removeLinkedFile(String localPath) {
		linkedFiles.remove(localPath);
	}

	@Nullable
	@Override
	public FileSettings getState() {
		return this;
	}
	@Override
	public void loadState(FileSettings projectSettings) {
		XmlSerializerUtil.copyBean(projectSettings, this);
	}
	@Nullable
	public static FileSettings getInstance(Project project) {
		return project.getService(FileSettings.class);
	}
}

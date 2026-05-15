package com.veeva.vault.toolbox.intellij.settings;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.project.Project;
import com.intellij.util.xmlb.XmlSerializerUtil;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * Project-level persistent state component that tracks files linked between
 * the local workspace and the remote Vault, keyed by their local path.
 */
@State(
		name = "com.veeva.vault.toolbox.intellij.settings.FileSettings",
		storages = @Storage("toolbox-files.xml")
)
public final class FileSettings implements PersistentStateComponent<FileSettings> {

	private Map<String, ToolboxFile> linkedFiles = new HashMap<>();

	/**
	 * Returns the {@link FileSettings} service instance for the given project.
	 *
	 * @param project the project whose settings are requested
	 * @return the project-scoped {@link FileSettings} service
	 */
	@Nullable
	public static FileSettings getInstance(Project project) {
		return project.getService(FileSettings.class);
	}

	/**
	 * @return the map of linked files keyed by local path
	 */
	public Map<String, ToolboxFile> getLinkedFiles() {
		return linkedFiles;
	}

	/**
	 * Replaces the map of linked files.
	 *
	 * @param linkedFiles the new map of linked files keyed by local path
	 */
	public void setLinkedFiles(Map<String, ToolboxFile> linkedFiles) {
		this.linkedFiles = linkedFiles;
	}

	/**
	 * Records or updates a linked file with the latest remote MD5 checksum.
	 * Creates a new {@link ToolboxFile} entry if one does not yet exist for the path.
	 *
	 * @param localPath the local path of the linked file
	 * @param remoteMd5 the MD5 checksum of the remote file
	 */
	public void addLinkedFile(String localPath, String remoteMd5) {
		ToolboxFile toolboxFile = linkedFiles.get(localPath);
		if (toolboxFile == null) {
			toolboxFile = new ToolboxFile();
			toolboxFile.setLocalPath(localPath);
			linkedFiles.put(localPath, toolboxFile);
		}
		toolboxFile.setRemoteMd5(remoteMd5);
	}

	/**
	 * @param localPath the local path to look up
	 * @return the linked file entry for the given path, or {@code null} if not found
	 */
	public ToolboxFile getLinkedFile(String localPath) {
		return linkedFiles.get(localPath);
	}

	/**
	 * Removes the linked file entry for the given local path.
	 *
	 * @param localPath the local path of the entry to remove
	 */
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
}

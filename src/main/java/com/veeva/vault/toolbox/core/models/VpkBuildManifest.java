/*---------------------------------------------------------------------
 *	Copyright (c) 2020 Veeva Systems Inc.  All Rights Reserved.
 *	This code is based on pre-existing content developed and
 *	owned by Veeva Systems Inc. and may only be used in connection
 *	with the deliverable with which it was provided to Customer.
 *---------------------------------------------------------------------
 */
package com.veeva.vault.toolbox.core.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.veeva.vault.toolbox.core.config.VaultPackage;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Represents the build manifest for a Vault package, defining its components,
 * metadata, and SDK configurations used to assemble the package.
 */
public class VpkBuildManifest {

	private String name;
	private String author;
	private String description;
	private String summary;
	private Integer vault;
	private List<Component> components;
	private JavaSdk javaSdk;
	private WebSdk webSdk;
	private VaultPackage.PackageType packageType;

	/**
	 * Returns the name of this Vault package.
	 *
	 * @return the package name
	 */
	public String getName() {
		return name;
	}

	/**
	 * Sets the name of this Vault package.
	 *
	 * @param name the package name
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * Returns the author of this Vault package.
	 *
	 * @return the author
	 */
	public String getAuthor() {
		return author;
	}

	/**
	 * Sets the author of this Vault package.
	 *
	 * @param author the author
	 */
	public void setAuthor(String author) {
		this.author = author;
	}

	/**
	 * Returns the summary of this Vault package.
	 *
	 * @return the summary
	 */
	public String getSummary() {
		return summary;
	}

	/**
	 * Sets the summary of this Vault package.
	 *
	 * @param summary the summary
	 */
	public void setSummary(String summary) {
		this.summary = summary;
	}

	/**
	 * Returns the description of this Vault package.
	 *
	 * @return the description
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * Sets the description of this Vault package.
	 *
	 * @param description the description
	 */
	public void setDescription(String description) {
		this.description = description;
	}

	/**
	 * Returns the package type.
	 *
	 * @return the package type
	 */
	public VaultPackage.PackageType getPackageType() {
		return packageType;
	}

	/**
	 * Sets the package type.
	 *
	 * @param packageType the package type
	 */
	public void setPackageType(VaultPackage.PackageType packageType) {
		this.packageType = packageType;
	}

	/**
	 * Returns the Vault ID associated with this package.
	 *
	 * @return the Vault ID
	 */
	public Integer getVault() {
		return vault;
	}

	/**
	 * Sets the Vault ID associated with this package.
	 *
	 * @param vault the Vault ID
	 */
	public void setVault(Integer vault) {
		this.vault = vault;
	}

	/**
	 * Returns the list of components included in this package.
	 *
	 * @return the component list
	 */
	public List<Component> getComponents() {
		return components;
	}

	/**
	 * Sets the list of components included in this package.
	 *
	 * @param components the component list
	 */
	public void setComponents(List<Component> components) {
		this.components = components;
	}

	/**
	 * Adds a component to this package. Initializes the component list if necessary.
	 *
	 * @param component the component to add
	 */
	public void addComponent(Component component) {
		if (components == null) {
			components = new ArrayList<>();
		}
		components.add(component);
	}

	/**
	 * Removes a component from this package.
	 *
	 * @param component the component to remove
	 */
	public void removeComponent(Component component) {
		if (components != null) {
			components.remove(component);
		}
	}

	/**
	 * Swaps two components in the component list by their indices.
	 *
	 * @param fromIndex the index of the first component
	 * @param toIndex   the index of the second component
	 */
	public void moveComponent(int fromIndex, int toIndex) {
		if (components != null
				&& fromIndex >= 0 && fromIndex < components.size()
				&& toIndex >= 0 && toIndex < components.size()) {
			Collections.swap(components, fromIndex, toIndex);
		}
	}

	/**
	 * Returns the Java SDK configuration for this package.
	 *
	 * @return the Java SDK configuration
	 */
	public JavaSdk getJavaSdk() {
		return javaSdk;
	}

	/**
	 * Sets the Java SDK configuration for this package.
	 *
	 * @param javaSdk the Java SDK configuration
	 */
	public void setJavaSdk(JavaSdk javaSdk) {
		this.javaSdk = javaSdk;
	}

	/**
	 * Returns the Web SDK configuration for this package.
	 *
	 * @return the Web SDK configuration
	 */
	public WebSdk getWebSdk() {
		return webSdk;
	}

	/**
	 * Sets the Web SDK configuration for this package.
	 *
	 * @param webSdk the Web SDK configuration
	 */
	public void setWebSdk(WebSdk webSdk) {
		this.webSdk = webSdk;
	}

	/**
	 * Represents a single component (step) entry within a Vault package build manifest.
	 */
	public static class Component {

		private String path;
		private String step;

		/** Creates an empty component. */
		public Component() {}

		/**
		 * Creates a component with the given step identifier and file path.
		 *
		 * @param step the step identifier
		 * @param path the file path of the component
		 */
		public Component(String step, String path) {
			this.step = step;
			this.path = path;
		}

		/**
		 * Returns the file path of this component.
		 *
		 * @return the file path
		 */
		public String getPath() {
			return path;
		}

		/**
		 * Sets the file path of this component.
		 *
		 * @param path the file path
		 */
		public void setPath(String path) {
			this.path = path;
		}

		/**
		 * Returns the step identifier of this component.
		 *
		 * @return the step identifier
		 */
		public String getStep() {
			return step;
		}

		/**
		 * Sets the step identifier of this component.
		 *
		 * @param step the step identifier
		 */
		public void setStep(String step) {
			this.step = step;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			Component component = (Component) o;
			return Objects.equals(path, component.path) && Objects.equals(step, component.step);
		}

		@Override
		public int hashCode() {
			return Objects.hash(path, step);
		}
	}

	/**
	 * Represents the Java SDK configuration section of a Vault package build manifest.
	 */
	public static class JavaSdk {

		private String path;
		private VaultPackage.JavaSdk.DeploymentOption deploymentOption;

		/**
		 * Returns the path to the Java SDK build artifact.
		 *
		 * @return the path
		 */
		public String getPath() {
			return path;
		}

		/**
		 * Sets the path to the Java SDK build artifact.
		 *
		 * @param path the path
		 */
		public void setPath(String path) {
			this.path = path;
		}

		/**
		 * Returns the deployment option for this Java SDK configuration.
		 *
		 * @return the deployment option
		 */
		public VaultPackage.JavaSdk.DeploymentOption getDeploymentOption() {
			return deploymentOption;
		}

		/**
		 * Sets the deployment option for this Java SDK configuration.
		 *
		 * @param deploymentOption the deployment option
		 */
		public void setDeploymentOption(VaultPackage.JavaSdk.DeploymentOption deploymentOption) {
			this.deploymentOption = deploymentOption;
		}
	}

	/**
	 * Represents the Web SDK configuration section of a Vault package build manifest.
	 */
	public static class WebSdk {

		private List<Distribution> distributions;

		/**
		 * Returns the list of Web SDK distributions in this configuration.
		 *
		 * @return the distribution list
		 */
		public List<Distribution> getDistributions() {
			return distributions;
		}

		/**
		 * Sets the list of Web SDK distributions in this configuration.
		 *
		 * @param distributions the distribution list
		 */
		public void setDistributions(List<Distribution> distributions) {
			this.distributions = distributions;
		}

		/**
		 * Adds a distribution to this Web SDK configuration. Initializes the list if necessary.
		 *
		 * @param distribution the distribution to add
		 */
		public void addDistribution(Distribution distribution) {
			if (distributions == null) {
				distributions = new ArrayList<>();
			}
			distributions.add(distribution);
		}

		/**
		 * Removes a distribution from this Web SDK configuration.
		 *
		 * @param distribution the distribution to remove
		 */
		public void removeDistribution(Distribution distribution) {
			if (distributions != null) {
				distributions.remove(distribution);
			}
		}

		/**
		 * Represents a single Web SDK distribution within a Vault package build manifest.
		 */
		public static class Distribution {

			private String name;
			private String manifest;
			private String path;
			private String shell;

			/**
			 * Returns the shell entry point for this distribution.
			 *
			 * @return the shell entry point
			 */
			public String getShell() {
				return shell;
			}

			/**
			 * Sets the shell entry point for this distribution.
			 *
			 * @param shell the shell entry point
			 */
			public void setShell(String shell) {
				this.shell = shell;
			}

			/**
			 * Returns the name of this distribution.
			 *
			 * @return the distribution name
			 */
			public String getName() {
				return name;
			}

			/**
			 * Sets the name of this distribution.
			 *
			 * @param name the distribution name
			 */
			public void setName(String name) {
				this.name = name;
			}

			/**
			 * Returns the manifest file path for this distribution.
			 *
			 * @return the manifest file path
			 */
			public String getManifest() {
				return manifest;
			}

			/**
			 * Sets the manifest file path for this distribution.
			 *
			 * @param manifest the manifest file path
			 */
			public void setManifest(String manifest) {
				this.manifest = manifest;
			}

			/**
			 * Returns the output path for this distribution.
			 *
			 * @return the output path
			 */
			public String getPath() {
				return path;
			}

			/**
			 * Sets the output path for this distribution.
			 *
			 * @param path the output path
			 */
			public void setPath(String path) {
				this.path = path;
			}

			@Override
			public boolean equals(Object o) {
				if (this == o) return true;
				if (o == null || getClass() != o.getClass()) return false;
				Distribution that = (Distribution) o;
				return Objects.equals(name, that.name)
						&& Objects.equals(manifest, that.manifest)
						&& Objects.equals(path, that.path)
						&& Objects.equals(shell, that.shell);
			}

			@Override
			public int hashCode() {
				return Objects.hash(name, manifest, path, shell);
			}
		}
	}

	/**
	 * Serializes this build manifest to the given file as indented JSON.
	 *
	 * @param settingsFile the target file
	 */
	@JsonIgnore
	public void save(File settingsFile) {
		try {
			ObjectMapper mapper = new ObjectMapper();
			mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
			mapper.configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);
			String json = mapper.writeValueAsString(this);
			FileUtils.writeStringToFile(settingsFile, json, "UTF-8");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Deserializes a {@link VpkBuildManifest} from the given JSON file.
	 *
	 * @param manifestFile the source file
	 * @return the loaded manifest, or {@code null} if the file does not exist or cannot be parsed
	 */
	@JsonIgnore
	public static VpkBuildManifest load(File manifestFile) {
		try {
			if (!manifestFile.exists()) {
				return null;
			}
			ObjectMapper mapper = new ObjectMapper();
			mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
			return mapper.readValue(new String(Files.readAllBytes(Paths.get(manifestFile.getPath()))), VpkBuildManifest.class);
		} catch (Exception e) {
			return null;
		}
	}
}

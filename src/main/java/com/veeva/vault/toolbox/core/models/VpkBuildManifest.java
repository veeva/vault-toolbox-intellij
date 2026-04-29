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

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getAuthor() {
		return author;
	}

	public void setAuthor(String author) {
		this.author = author;
	}

	public String getSummary() {
		return summary;
	}

	public void setSummary(String summary) {
		this.summary = summary;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public VaultPackage.PackageType getPackageType() {
		return packageType;
	}

	public void setPackageType(VaultPackage.PackageType packageType) {
		this.packageType = packageType;
	}

	public Integer getVault() {
		return vault;
	}

	public void setVault(Integer vault) {
		this.vault = vault;
	}

	public List<Component> getComponents() {
		return components;
	}

	public void setComponents(List<Component> components) {
		this.components = components;
	}

	public void addComponent(Component component) {
		if (components == null) {
			components = new ArrayList<>();
		}
		components.add(component);
	}

	public void removeComponent(Component component) {
		if (components != null) {
			components.remove(component);
		}
	}

	public void moveComponent(int fromIndex, int toIndex) {
		if (components != null && fromIndex >= 0 && fromIndex < components.size() && toIndex >= 0 && toIndex < components.size()) {
			Collections.swap(components, fromIndex, toIndex);
		}
	}

	public JavaSdk getJavaSdk() {
		return javaSdk;
	}

	public void setJavaSdk(JavaSdk javaSdk) {
		this.javaSdk = javaSdk;
	}

	public WebSdk getWebSdk() {
		return webSdk;
	}

	public void setWebSdk(WebSdk webSdk) {
		this.webSdk = webSdk;
	}

	public static class Component {
		String path;
		String step;

		public Component() {}

		public Component(String step, String path) {
			this.step = step;
			this.path = path;
		}

		public String getPath() {
			return path;
		}

		public void setPath(String path) {
			this.path = path;
		}

		public String getStep() {
			return step;
		}

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

	public static class JavaSdk {
		private String path;
		private VaultPackage.JavaSdk.DeploymentOption deploymentOption;

		public String getPath() {
			return path;
		}

		public void setPath(String path) {
			this.path = path;
		}

		public VaultPackage.JavaSdk.DeploymentOption getDeploymentOption() {
			return deploymentOption;
		}

		public void setDeploymentOption(VaultPackage.JavaSdk.DeploymentOption deploymentOption) {
			this.deploymentOption = deploymentOption;
		}
	}

	public static class WebSdk {
		private List<Distribution> distributions;

		public List<Distribution> getDistributions() {
			return distributions;
		}

		public void setDistributions(List<Distribution> distributions) {
			this.distributions = distributions;
		}
		
		public void addDistribution(Distribution distribution) {
			if (distributions == null) {
				distributions = new ArrayList<>();
			}
			distributions.add(distribution);
		}
		
		public void removeDistribution(Distribution distribution) {
			if (distributions != null) {
				distributions.remove(distribution);
			}
		}

		public static class Distribution {
			private String name;
			private String manifest;
			private String path;
			private String shell;

			public String getShell() {
				return shell;
			}

			public void setShell(String shell) {
				this.shell = shell;
			}

			public String getName() {
				return name;
			}

			public void setName(String name) {
				this.name = name;
			}

			public String getManifest() {
				return manifest;
			}

			public void setManifest(String manifest) {
				this.manifest = manifest;
			}

			public String getPath() {
				return path;
			}

			public void setPath(String path) {
				this.path = path;
			}
			
			@Override
			public boolean equals(Object o) {
				if (this == o) return true;
				if (o == null || getClass() != o.getClass()) return false;
				Distribution that = (Distribution) o;
				return Objects.equals(name, that.name) && Objects.equals(manifest, that.manifest) && Objects.equals(path, that.path) && Objects.equals(shell, that.shell);
			}

			@Override
			public int hashCode() {
				return Objects.hash(name, manifest, path, shell);
			}
		}
	}

	@JsonIgnore
	public void save(File settingsFile) {
		try	 {
			ObjectMapper mapper = new ObjectMapper();
			mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
			mapper.configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);
			String json = mapper.writeValueAsString(this);
			FileUtils.writeStringToFile(new File(settingsFile.getAbsolutePath()), json,"UTF-8");
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	@JsonIgnore
	public static VpkBuildManifest load(File manifestFile) {
		try	 {
			if (manifestFile.exists()) {
				ObjectMapper mapper = new ObjectMapper();
				mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
				VpkBuildManifest buildManifest = mapper.readValue(new String(Files.readAllBytes(Paths.get(manifestFile.getPath()))), VpkBuildManifest.class);
				return buildManifest;
			}
			return null;
		}
		catch (Exception e) {
			return null;
		}
	}
}

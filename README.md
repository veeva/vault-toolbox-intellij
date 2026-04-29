# Vault Toolbox IntelliJ Plugin

<!-- Plugin description -->
The Vault Toolbox IntelliJ Plugin is a dedicated IDE plugin designed to make the everyday life of Vault Admins and Developers easier. It leverages the power of the Vault REST API and the VAPIL library to integrate Vault configuration, deployment, and debugging workflows directly into your local development environment.

<p>Integrates Veeva Vault development and administration workflows directly into IntelliJ IDEA.</p>
<br/>
<b>Features:</b>
<ul>
    <li>Manage and switch between multiple Vault environments.</li>
    <li>Download Configuration Reports directly into your workspace.</li>
    <li>Extract MDL and Vault Packages (VPK), automatically organized by Vault ID.</li>
    <li>Download and analyze Developer Logs (API Usage, SDK Debug, SDK Runtime, and SDK Profiler).</li>
    <li>Language support for MDL and VPK files, including syntax highlighting, formatting, and auto-completion.</li>
    <li>Project templates for Vault API Integration, Java SDK, and Custom Pages.</li>
</ul>
<!-- Plugin description end -->

## Getting Started

The Vault Toolbox IntelliJ Plugin is available for installation directly via the JetBrains Marketplace.

- **Using the IDE built-in plugin system:**
  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>Marketplace</kbd> > <kbd>Search for "Vault Toolbox"</kbd> > <kbd>Install</kbd>

- **Using JetBrains Marketplace:**
  Go to the [JetBrains Marketplace](https://plugins.jetbrains.com/plugin/MARKETPLACE_ID) and install it by clicking the <kbd>Install to ...</kbd> button while your IDE is running.

## Features

### Environment Management
The Vault Toolbox IntelliJ Plugin requires you to authenticate into a Vault to extract data or deploy code. The plugin allows you to easily connect, manage, and seamlessly switch between multiple Veeva Vault environments directly from the IDE tool window.

### Configuration Reports
Easily download Vault Configuration Reports straight into your IntelliJ workspace. Reports are safely fetched using background tasks to respect API limits and are automatically organized into a clean `VaultName/YYYY-MM-DD` folder structure. Downloaded `.xlsm` files can be opened instantly with your native OS spreadsheet viewer.

### Component Extraction
Extract MDL and Vault Packages (VPK) directly from your connected Vault environments. To keep your workspace clean and prevent overwriting, all extracted components are cleanly separated into subdirectories based on your specific Vault ID.

### Developer Logs
Streamline your debugging process. The Toolbox allows you to download and deeply analyze Vault Developer Logs directly within the IDE, including:
* API Usage Logs
* SDK Debug Logs
* SDK Runtime Logs
* SDK Profiler Logs

### Native Language Support
The Vault Toolbox IntelliJ Plugin uses IntelliJ's powerful open-source parsing engine to provide a first-class coding experience for Vault-specific files. Working with `.mdl` and `.vpk` files now includes:
* Native Syntax Highlighting
* Code Formatting
* Intelligent Auto-Completion

### Project Templates
Kickstart new Veeva Vault projects instantly. The plugin includes pre-configured project templates to set up the correct folder structures and dependencies for:
* Vault API Integrations
* Vault Java SDK (Hello World)
* Custom Pages
* Vault Research

## Required Permissions
The Vault Toolbox IntelliJ Plugin uses published and validated API endpoints via the VAPIL library. It does not provide any overrides to documented functionalities.

To use the plugin, users must have standard Developer/Admin API access permissions within their Vault environments. Access to specific configuration data or logs is subject to the relevant permissions and Lifecycle Role restrictions of the authenticated user.

## Support
Support for the Vault Toolbox IntelliJ Plugin is handled exclusively through the [Vault for Developers community](https://veevaconnect.com/communities/ATeJ3k8lgAA/about) on Veeva Connect.

---
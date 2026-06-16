# Vault Toolbox IntelliJ Plugin

<!-- Plugin description -->
The Vault Toolbox IntelliJ Plugin is a dedicated IDE plugin designed to make the everyday life of Vault Admins and
Developers easier. It leverages the power of the Vault REST API and the VAPIL library to integrate Vault configuration,
deployment, and debugging workflows directly into your local development environment.

<p>Integrates Veeva Vault development and administration workflows directly into IntelliJ IDEA.</p>
<br/>
<b>Features:</b>
<ul>
    <li>Manage and switch between multiple Vault environments (Dev, QA, Prod Vaults and PVMs), including securely saved credentials.</li>
    <li>Compare Vaults to identify MDL and SDK differences between environments.</li>
    <li>Extract MDL, Vault Packages (VPK), and Vault SDKs, automatically organized by Vault ID.</li>
    <li>Deploy and drop MDL and Vault SDKs directly from the IDE, with built-in safety checks for Production Vaults.</li>
    <li>Download and analyze Developer Logs (API Usage, SDK Debug, SDK Runtime, and SDK Profiler).</li>
    <li>Download Configuration Reports directly into your workspace.</li>
    <li>Vault Java SDK code inspections to catch sandbox violations at development time.</li>
    <li>Language support for MDL and VPK files, including syntax highlighting, formatting, and auto-completion.</li>
    <li>Project templates for Vault API Integration, Java SDK, and Custom Pages.</li>
</ul>
<!-- Plugin description end -->

## Getting Started

The Vault Toolbox IntelliJ Plugin is available for installation directly via the JetBrains Marketplace.

- **Using the IDE built-in plugin system:**
  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>Marketplace</kbd> > <kbd>Search for "Vault
  Toolbox"</kbd> > <kbd>Install</kbd>

- **Using JetBrains Marketplace:**
  Go to the [JetBrains Marketplace](https://plugins.jetbrains.com/plugin/31516-vault-toolbox) and install it by clicking
  the <kbd>Install to ...</kbd> button while your IDE is running.

## Features

### Environment Management

The Vault Toolbox IntelliJ Plugin requires you to authenticate into a Vault to extract data or deploy code. The plugin
allows you to easily connect, manage, and seamlessly switch between multiple Veeva Vault environments directly from the
IDE tool window, including support for securely saved credentials via your OS password manager.

### VQL Console

Run read-only Vault queries directly in the IDE with the VQL Console. It features VQL syntax highlighting, object and field auto-completion, paginated results, CSV export, inline editing with Save Changes, and persistent query history. Use the visual Query Builder to construct queries easily by picking objects, selecting fields, adding filter conditions, and ordering with a live preview.

### Compare Vaults

Easily compare MDL and Vault SDK components between two different Vault environments. Quickly identify configuration and code differences across environments using raw file comparison or the Semantic JSON View, then apply changes directly from source to target or target to source without leaving the IDE.

### Configuration Reports

Easily download Vault Configuration Reports straight into your IntelliJ workspace. Reports are safely fetched using
background tasks to respect API limits and are automatically organized into a clean `VaultName/YYYY-MM-DD` folder
structure. Downloaded `.xlsm` files can be opened instantly with your native OS spreadsheet viewer.

### Component Extraction

Extract MDL, Vault Packages (VPK), and Vault SDKs directly from your connected Vault environments. To keep your
workspace clean and prevent overwriting, all extracted components are cleanly separated into subdirectories based on
your specific Vault ID.

### Deployment & Dropping

Streamline your deployment workflow by deploying or dropping MDL and Vault SDKs directly from the IDE. The plugin
includes built-in safety checks to prevent accidental modifications to Production Vaults, ensuring you always know where
your code is going.

### Developer Logs

Streamline your debugging process. The Toolbox allows you to download and deeply analyze Vault Developer Logs directly
within the IDE, including:

* API Usage Logs
* SDK Debug Logs
* SDK Runtime Logs
* SDK Profiler Logs

### Vault Java SDK Code Inspections

Write safer Vault Java SDK code without leaving the IDE. The plugin provides a set of code inspections that flag APIs
and patterns not permitted inside the Vault sandbox, including:

* Java Reflection API (`java.lang.reflect.*`, `Class.forName()`, etc.)
* Multi-threading constructs (`Thread`, `ThreadLocal`, `java.util.concurrent.*`, `synchronized`)
* File I/O and NIO operations (`java.io.File*`, `java.nio.file.*`)
* Console output and system calls (`System.out`, `System.exit()`, `Runtime.exec()`)
* Direct network access (`Socket`, `HttpURLConnection`, `ProcessBuilder`)
* Static mutable and `volatile` fields
* Standard Java collections (`ArrayList`, `HashMap`, etc.) — use `VaultCollections` and `VaultCollectors` instead

Inspections are disabled by default and can be enabled in **Settings → Inspections → Vault Java SDK** for projects that
include the Vault Java SDK on their classpath.

### Native Language Support

The Vault Toolbox IntelliJ Plugin uses IntelliJ's powerful open-source parsing engine to provide a first-class coding
experience for Vault-specific files. Working with `.mdl` and `.vpk` files now includes:

* Native Syntax Highlighting
* Code Formatting
* Intelligent Auto-Completion

### Project Templates

Kickstart new Veeva Vault projects instantly. The plugin includes pre-configured project templates to set up the correct
folder structures and dependencies for:

* Vault API Integrations
* Vault Java SDK (Hello World)
* Custom Pages
* Vault Research

## Required Permissions

The Vault Toolbox IntelliJ Plugin uses published and validated API endpoints via the VAPIL library. It does not provide
any overrides to documented functionalities.

To use the plugin, users must have standard Developer/Admin API access permissions within their Vault environments.
Access to specific configuration data or logs is subject to the relevant permissions and Lifecycle Role restrictions of
the authenticated user.

Destructive actions such as deploying or dropping MDL and SDKs are blocked on Production Vaults to prevent accidental modifications.

## Support

Support for the Vault Toolbox IntelliJ Plugin is handled exclusively through
the [Vault for Developers community](https://veevaconnect.com/communities/ATeJ3k8lgAA/about) on Veeva Connect.

---
# Vault Toolbox IntelliJ Plugin Changelog

## [Unreleased]

## [26.1.1] - 2026-05-08

- Added support for saving multiple credentials.
- Upgraded `VAPIL` dependency to version `26.1.0`.
- Upgraded underlying dependencies (`Jackson`, `JSON-Java`, `OpenCSV`, `SQLite JDBC`, `JFreeChart`) to their latest stable patches.
- Added support for extracting SDKs and dropping SDK and MDL from Vault.
- Added safety checks for Production Vaults during deploy and drop operations.
- Added the ability to cancel Configuration Report downloads.
- Improved Developer Logs with fixes for duplicate entries, concurrency issues, and handling of inactive SDK debug logs.
- Enhanced UI with updated icons, improved spacing, and automatic highlighting of downloaded log folders.
- General bug fixes, performance improvements, and resolution of deprecated methods.

## [26.1.0] - 2026-04-27

### Added
- **Initial Release:** Official launch of the Vault Toolbox IntelliJ Plugin!
- **Environment Management:** Easily connect, manage, and seamlessly switch between multiple Veeva Vault environments directly from the IDE tool window.
- **Secure Authentication:** Support for Basic Authentication and Session ID login, utilizing IntelliJ's native `PasswordSafe` for secure credential storage.
- **Configuration Reports:** Safely fetch and download Vault Configuration Reports directly into your IntelliJ workspace, automatically organized into a `VaultID/YYYY-MM-DD` folder structure.
- **Component Extraction:** Extract MDL and Vault Packages (VPK) from connected environments, automatically partitioned into subdirectories based on Vault ID.
- **Developer Logs:** Download and deeply analyze Vault Developer Logs directly within the IDE (API Usage Logs, SDK Debug Logs, SDK Runtime Logs, and SDK Profiler Logs).
- **Native Language Support:** First-class IDE support for `.mdl` and `.vpk` files, including native syntax highlighting, code formatting, and intelligent auto-completion.
- **Project Templates:** Pre-configured project templates to instantly scaffold new Vault API Integrations, Vault Java SDK (Hello World), Custom Pages, and Vault Research projects.



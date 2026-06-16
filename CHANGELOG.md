# Vault Toolbox IntelliJ Plugin Changelog

## [26.1.3] - 2026-05-26

- Added the ability to manually import external `.zip` log archives into Developer Logs, and a generic File Viewer to natively preview downloaded files, archives, and logs directly within the IDE.

- Added a VQL Console and visual Query Builder for running read-only Vault queries directly in the IDE, featuring VQL syntax highlighting, object/field auto-completion, paginated results, CSV export, inline editing with Save Changes, and persistent query history.
- Added a dialog for Download Configuration report, supporting optional parameters.
- Enhanced Developer Logs and Deployment Packages tables with drag-to-select multiple rows and right-click context menu for quick actions.
- Added Apply Changes to Compare Vaults, allowing MDL and SDK differences to be pushed from Source to Target or Target to Source. Added an optional Semantic JSON View to easily inspect field-level differences.
- Added Vault Java SDK code inspections to flag reflection, multi-threading, file I/O, network access, static mutable fields, and standard Java collections that are not permitted in the Vault sandbox.

## [26.1.2] - 2026-05-22

- Added new Compare Vaults feature for MDL and SDK.
- Added syntax highlighting for Vault CSV files.
- Enhanced UI with native IntelliJ components, consistent table resizing, and improved progress tracking and pagination for extraction tasks.
- Fixed VPK path issues on Windows.

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



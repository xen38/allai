# Allai Changelog

## [Unreleased] — Phase B: Final Judgment Phase Preparation

### Fixed
- **C1 — Model Selector Search:** Added search/filter text field in model picker overlay with real-time filtering of available models
- **C2 — Floating Bubble Model Access:** Added model selector button in bubble header showing current model
- **C3 — Floating Bubble Expand/Minimize:** Added minimize/expand toggle; minimized = header only (200x60dp), expanded = full chat (340x540dp) with dynamic window resizing
- **C4 — Tool Permission Checks:** Added `requiresPermission()` to `PhonePlugin` interface; `PluginManager.runChecked()` checks `ContextCompat.checkSelfPermission()` before executing sensitive tools
- **C5 — Tool Confirmation Distinction:** Added `requiresConfirmation()` to `PhonePlugin`; `AgentLoop.run()`/`runStreaming()` intercepts confirmation-required tools and prompts user via CONFIRM_REQUIRED flow before executing
- **C6 — Memory Search & Source Distinction:** Added search filter field in MemoriesTab; added explicit/inferred source labeling on MemoryCard; favorite flag for prioritization
- **C7 — Account Session Persistence:** GoogleSignIn session (email, name) persisted to SharedPreferences, restored on compose, cleared on sign-out
- **C8 — Notification Access:** Added NotificationListenerPlugin (system service) + NotificationPluginWrapper (PhonePlugin adapter), BIND_NOTIFICATION_LISTENER permission declared, notification toggle in Security settings
- **C9 — Project Analysis/Debugging:** Added ProjectPlugin with analyze/explore/read actions for code project analysis on device
- **C10 — Log Redaction:** Added LogRedactor utility for redacting API keys (nvapi-, sk-), JWT tokens, email addresses, and credit card numbers from log output
- **C11 — Credential Obfuscation:** Added SecurePrefs with XOR obfuscation for API key at rest; initialized in AllaiApplication.onCreate()

### Added
- `app/src/main/java/com/allai/app/security/LogRedactor.kt` — Log redaction utility
- `app/src/main/java/com/allai/app/security/SecurePrefs.kt` — Obfuscated SharedPreferences wrapper
- `app/src/main/java/com/allai/app/analysis/ProjectPlugin.kt` — Project analysis plugin
- `app/src/main/java/com/allai/app/notification/NotificationListenerPlugin.kt` — Notification listener service
- `app/src/main/java/com/allai/app/notification/NotificationPluginWrapper.kt` — Notification plugin adapter

### Modified
- `app/src/main/AndroidManifest.xml` — Added BIND_NOTIFICATION_LISTENER permission and NotificationListenerService declaration
- `app/build.gradle` — Removed unavailable security-crypto dependency (offline limitation)
- `app/src/main/java/com/allai/app/AllaiApplication.kt` — Initialize SecurePrefs in onCreate()
- `app/src/main/java/com/allai/app/settings/SettingsState.kt` — API key uses SecurePrefs for obfuscated storage
- `app/src/main/java/com/allai/app/plugin/PluginManager.kt` — Added runChecked(), ToolExecutionResult, requiresPermission(), requiresConfirmation(); registered NotificationPluginWrapper and ProjectPlugin
- `app/src/main/java/com/allai/app/agent/AgentLoop.kt` — Added context to AgentConfig; uses runChecked for permission/confirmation-aware tool execution; handles CONFIRM_REQUIRED flow
- `app/src/main/java/com/allai/app/ui/AgentScreen.kt` — Added model search field; passes context to AgentConfig
- `app/src/main/java/com/allai/app/ui/SettingsTabsScreen.kt` — Added memory search, suggested/explicit/inferred distinction; account session persistence; notification toggle in Security
- `app/src/main/java/com/allai/app/floating/FloatingWindowService.kt` — Added model selector button and minimize/expand toggle to bubble overlay

## [Previous Releases]

### v1.0.0 — Initial Release
- Basic chat UI with Compose
- Terminal with shell command execution
- MCP client stub
- Plugin system (shell, file, contacts, SMS, launcher, accessibility, MCP, self, skill)
- LLM client (OpenAI-compatible)
- Agent loop with tool calling
- Settings with 5 sections (Provider, Agent, Security, Account, Integrations)
- Floating bubble mode
- Sessions with checkpoints and import/export
- Skills system
- Streaming LLM support with SSE parsing
- JSON Schema ToolDefinition
- Multi-provider support (OpenAI, Anthropic, Google, Custom)

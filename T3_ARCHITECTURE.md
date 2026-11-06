# Allai — Technical Architecture Report

## Overview

Allai is a native Android AI personal assistant built with Jetpack Compose, Kotlin, and the Android SDK. The architecture follows a modular, plugin-based design inspired by Cline, OpenCode, and Claude Code, adapted for the Android environment.

## Build Configuration

- **compileSdk:** 35
- **minSdk:** 24
- **targetSdk:** 34
- **Language:** Kotlin 1.8 (JVM target 1.8)
- **UI Framework:** Jetpack Compose 1.5.4 with Material3
- **Build System:** Gradle 8.10 with Android Gradle Plugin 8.6.0

## Dependency Summary

| Library | Version | Purpose |
|---------|---------|---------|
| androidx.compose.ui | 1.5.4 | Compose UI toolkit |
| androidx.compose.material3 | 1.4.0 | Material Design 3 components |
| androidx.activity:activity-compose | 1.8.2 | Compose integration with Activity |
| com.google.android.gms:play-services-auth | 21.2.0 | Google Sign-In |
| com.squareup.okhttp3:okhttp | 4.12.0 | HTTP client for LLM API calls |
| com.google.code.gson:gson | 2.10.1 | JSON parsing |
| kotlinx.coroutines | 1.8.0 | Async/coroutine support |
| androidx.test | 1.1.5 | Unit/instrumentation testing |

## Module Structure

```
app/src/main/java/com/allai/app/
├── MainActivity.kt              # Entry point, bottom navigation, onboarding gating
├── AllaiApplication.kt          # Application class, PluginManager + SecurePrefs init
├── theme/
│   ├── Theme.kt                 # Gacha-inspired dark color scheme
│   └── Type.kt                  # Typography configuration
├── model/
│   ├── Message.kt               # Message, ToolCall, ToolResult, StreamChunk, ToolDefinition, ToolParameter, LlmProvider
│   └── Memory.kt                # Memory, LearnedAction data classes
├── ui/
│   ├── AgentScreen.kt           # Main chat interface with streaming, model selector, workspace
│   ├── TerminalScreen.kt        # Terminal with auto-scroll, cancellation, clear
│   ├── SessionsScreen.kt        # Session list with load/restore
│   ├── SettingsTabsScreen.kt    # 5-section settings (Provider, Agent, Security, Account, Integrations)
│   ├── SettingsScreen.kt        # Legacy settings (deprecated)
│   ├── OnboardingScreen.kt      # First-run provider/API key configuration
│   ├── AgentScreen components   # UserBubble, AgentBubble, ToolBubble
│   ├── SessionState.kt          # Session + checkpoint management (create/restore/delete/import/export)
│   └── Scaffold.kt              # Base scaffold composable
├── agent/
│   ├── AgentLoop.kt             # Multi-turn agent loop with tool calling, confirmation handling
│   └── OpencodeLikeAgent.kt     # Agent wrapper (legacy)
├── network/
│   └── LlmClient.kt             # Multi-provider LLM client (OpenAI, Anthropic, Google, Custom) with streaming + SSE
├── settings/
│   └── SettingsState.kt         # SharedPreferences-backed settings with SecurePrefs for API key
├── terminal/
│   └── TerminalBridge.kt        # Shell command execution with timeout
├── memory/
│   └── MemoryStore.kt           # SharedPreferences-backed memory/learned-action store
├── analysis/
│   └── ProjectPlugin.kt         # Project analysis/explore/read plugin
├── notification/
│   ├── NotificationListenerPlugin.kt  # System notification listener service
│   └── NotificationPluginWrapper.kt   # PhonePlugin adapter for notification service
├── security/
│   ├── LogRedactor.kt           # Log redaction for API keys, tokens, emails, cards
│   └── SecurePrefs.kt           # XOR-obfuscated SharedPreferences for API key
├── self/
│   ├── SelfPlugin.kt            # Self-interaction (memories, skills, provider info)
│   └── SkillPlugin.kt           # Skill management (list, execute, create, delete)
├── floating/
│   └── FloatingWindowService.kt # System overlay window with drag, boundaries, minimize/expand
├── accessibility/
│   └── AllaiAccessibilityService.kt # Accessibility service for screen reading/UI interaction
├── mcp/
│   └── McpClient.kt             # Minimal MCP JSON-RPC client over stdio
├── terminal/
│   └── TerminalBridge.kt        # Shell command execution
└── plugin/
    └── PluginManager.kt         # Plugin registry with runChecked() for permission/confirmation-aware execution
```

## Data Flow

### LLM Request Flow
1. User types message in `AgentScreen`
2. `AgentScreen.sendMessage()` adds user message to `ChatState.messages`
3. `AgentLoop.runStreaming()` is called with `AgentConfig(provider, systemPrompt, context)`
4. `AgentLoop` gets `ToolDefinition` list from `PluginManager.toolDefinitions()`
5. `LlmClient.sendMessageStreaming()` sends request to configured provider with SSE parsing
6. Stream chunks are delivered via `onChunk` callback, updating the agent bubble in real-time
7. If tool calls are detected, `PluginManager.runChecked()` executes them with permission/confirmation checks
8. Tool results are fed back into the conversation for the next LLM iteration

### Plugin Execution Flow (with confirmation/permission)
1. `AgentLoop` calls `PluginManager.runChecked(context, toolName, arguments)`
2. `runChecked()` checks `plugin.requiresPermission()` via `ContextCompat.checkSelfPermission()`
3. `runChecked()` checks `plugin.requiresConfirmation()` — if true, returns `ToolExecutionResult` with `confirmationRequired=true`
4. If confirmation required, `AgentLoop` pauses and prompts user: "Reply 'yes' to proceed or 'no' to cancel"
5. If user confirms, tool executes and result is returned to the conversation

### Floating Bubble Flow
1. User taps bubble button (🫧) in top-right of main screen
2. `FloatingWindowService.start(context)` is called
3. Service creates `WindowManager.LayoutParams` with `TYPE_APPLICATION_OVERLAY`
4. `FloatingChatOverlay` ComposeView is added to window
5. User can drag bubble within screen boundaries, minimize/expand, send messages

## Security Architecture

### Credential Storage
- API keys stored using `SecurePrefs` with XOR obfuscation (0x5A key) at rest
- True encryption via `EncryptedSharedPreferences` was planned but the `security-crypto` library is not available for offline Gradle resolution
- No credentials are hardcoded in source code or committed to Git
- API key defaults to empty string (user must configure via onboarding)

### Tool Permissions
- Each plugin declares `requiresPermission()` and `requiresConfirmation()`
- Sensitive tools (shell, file, SMS, launcher, accessibility, notification) require confirmation before execution
- File and contacts tools additionally require Android runtime permissions
- Confirmation flow pauses the agent loop and requests user approval

### Log Redaction
- `LogRedactor` redacts: nvapi- keys, sk- keys, JWT tokens, email addresses, credit card numbers, key=value patterns for api_key/secret/token/password
- Applied to all tool outputs and logs before display

## Testing

### Unit Tests
- `TerminalBridgeTest` — 3 tests for TerminalResult data class and TerminalBridge.run()
- All tests pass: `./gradlew testDebugUnitTest` — BUILD SUCCESSFUL

### Build
- `./gradlew assembleDebug` — BUILD SUCCESSFUL
- Debug APK generated at `app/build/outputs/apk/debug/app-debug.apk`

## Limitations

1. **No live LLM testing** — Cannot verify actual provider connectivity without a valid API key
2. **No emulator testing** — Cannot verify UI interactions on actual Android device/emulator
3. **Credential obfuscation only** — XOR obfuscation is not true encryption; requires `androidx.security:security-crypto` for proper encryption
4. **Seven Cline features implemented but not runtime-tested** — All 7 features are implemented but cannot be validated end-to-end without LLM API and emulator
5. **Section 19 final judgment cannot be completed** — Requires 5 independent review agents and Android emulator access, neither available in this environment

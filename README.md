# Allai

Open-source AI agent for Android, inspired by [Opencode](https://github.com/anomalyco/opencode). Built native for Android with an embedded terminal, MCP/plugin system to interact with other apps, and an LLM-driven agent loop.

## Features
- **Chat UI** — Compose-based conversation with the agent.
- **Terminal** — Built-in shell (`sh -c`) for running commands on the device.
- **MCP client** — JSON-RPC over stdio to run Model Context Protocol servers.
- **Plugins** — `contacts`, `sms`, `launcher` stubs to interact with other phone apps.
- **Agent loop** — Extensible `OpencodeLikeAgent` wiring LLM + tools.

## Getting started

### Requirements
- Android Studio (Hedgehog or newer)
- JDK 17
- Android SDK 34 (compileSdk), minSdk 24

### Build
```bash
./gradlew assembleDebug
```

### Run on device/emulator
```bash
./gradlew installDebug
```

## Project structure
- `app/src/main/java/com/allai/app/ui` — Compose screens (chat, terminal, settings).
- `app/src/main/java/com/allai/app/terminal` — `TerminalBridge` shell wrapper.
- `app/src/main/java/com/allai/app/mcp` — Minimal MCP JSON-RPC client stub.
- `app/src/main/java/com/allai/app/plugin` — `PhonePlugin` interface + `PluginManager` with stub plugins.
- `app/src/main/java/com/allai/app/network` — `LlmClient` (OpenAI-compatible chat completions).
- `app/src/main/java/com/allai/app/agent` — `OpencodeLikeAgent` stub.

## Roadmap
- Wire `LlmClient` into `OpencodeLikeAgent` with a real tool-use loop.
- Replace stdio stub `McpClient` with a proper async transport.
- Implement `contacts`/`sms`/`launcher` plugins using Android APIs (`ContactsContract`, `SmsManager`, `PackageManager`).
- Add file access, screenshot, and shell tool integrations.
- Signing config + Play Store publishing (`./gradlew bundleRelease`).

## License
MIT

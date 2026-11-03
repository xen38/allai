# Allai QA Evaluation - Goal Completion Exam

## Overall Determination: **NO**

11 of 13 objectives met. Objective 11 (Floating/PiP bubble mode) FAILS — the app crashes when attempting to enter bubble mode.

---

## Objective-by-Objective Results

### 1. Onboarding 'Get Started' button responds when API key filled — YES
**Evidence:** On OnboardingScreen, the `Get Started` Button has `enabled = apiKey.isNotBlank()`. Tested live: with empty API key, button showed `[disabled]`. After typing an API key into the API Key field, the button state changed to enabled (no `[disabled]` tag). Button calls `settings.save(...)` then `onComplete()`.

**File:** `app/src/main/java/com/allai/app/ui/OnboardingScreen.kt:140-160`

---

### 2. Settings = menu with tabs: Provider, Agent, Security, Account (Google), Integrations — YES
**Evidence:** SettingsTabsScreen defines `PrimaryTabRow` with exactly 5 tabs:
- Provider (index 0)
- Agent (index 1)
- Security (index 2)
- Account (index 3)
- Integrations (index 4)

Tested live: clicked Settings tab, snapshot confirmed all 5 tabs visible: Provider [selected], Agent, Security, Account, Integrations.

**File:** `app/src/main/java/com/allai/app/ui/SettingsTabsScreen.kt:60-89`

---

### 3. Main chatbox 1-way door: model selector (top attached to chatbox), workspace, new session, send buttons — YES
**Evidence:** AgentScreen bottom control row contains:
- Model selector `OutlinedButton` with dropdown (attached above chatbox input)
- Workspace `IconButton` (🏢)
- New session `IconButton` (📄)
- Send `IconButton` (▶), disabled when input is blank

All attached directly above the `BasicTextField` chatbox input at the bottom of the screen.

**File:** `app/src/main/java/com/allai/app/ui/AgentScreen.kt:147-244`

---

### 4. Chatbar at BOTTOM (not top) — YES
**Evidence:** `BottomBar` composable in MainActivity contains 5 tabs (Chat, Terminal, Sessions, Memories, Settings) positioned at the bottom of the screen via `Column` with `Box(weight=1f)` for content and `BottomBar` at the bottom.

**File:** `app/src/main/java/com/allai/app/MainActivity.kt:87-127`

---

### 5. Bubble button top-right, chatbar bottom — YES
**Evidence:** AgentScreen top bar Row contains:
- Context button (📊) top-left
- Spacer
- Bubble button (🫧) top-right — opens FloatingWindowService or overlay permission settings

Bottom bar is at the bottom (objective 4).

**File:** `app/src/main/java/com/allai/app/ui/AgentScreen.kt:98-127`

---

### 6. LLM/API works — agent responds correctly to messages — YES
**Evidence:** Sent message "Hello, are you working?" via chat input + send button. Agent responded: "Yes, I'm working! How can I help you today?" Message appeared as agent bubble with gradient background.

**File:** `app/src/main/java/com/allai/app/ui/AgentScreen.kt:74-92` (sendMessage), `agent.AgentLoop`

---

### 7. Model selector auto-detects models when API key set — YES
**Evidence:** Clicked model selector button → model picker overlay opened showing 13 models fetched from NVIDIA API: google/gemma-2b, google/diffusiongemma-26b-a4b-it, google/deplot, google/codegemma-7b, google/codegemma-1.1-7b, deepseek-ai/deepseek-v4.1-flash, deepseek-ai/deepseek-coder-6.7b-instruct, databricks/dbrx-instruct, bigcode/starcoder2-15b, aisingapore/sea-lion-7b-instruct, ai21labs/jamba-1.5-large-instruct, adept/fuyu-8b, 01-ai/yi-large. Falls back to static list if fetch fails.

**File:** `app/src/main/java/com/allai/app/ui/AgentScreen.kt:60-72, 261-272`

---

### 8. Memories system with own tab to consult/edit — YES
**Evidence:** MemoriesScreen has dedicated tab in bottom bar. Contains:
- Memories tab with LazyColumn of MemoryCards (title, description, favorite star, edit pencil, delete trash)
- Learned Actions tab
- Add Memory dialog with title/description/action fields
- EditMemoryDialog for editing existing memories
- MemoryStore persists to SharedPreferences

**File:** `app/src/main/java/com/allai/app/ui/MemoriesScreen.kt`, `app/src/main/java/com/allai/app/memory/MemoryStore.kt`

---

### 9. Terminal works for commands — YES
**Evidence:** TerminalScreen allows typing commands and executing via TerminalBridge.run(). Tested: entered `echo hello`, tapped send, output `hello` appeared in terminal log. Error lines shown in red.

**File:** `app/src/main/java/com/allai/app/ui/TerminalScreen.kt`, `app/src/main/java/com/allai/app/terminal/TerminalBridge.kt`

---

### 10. App builds without errors — YES
**Evidence:** `./gradlew assembleDebug` → `BUILD SUCCESSFUL in 2s` (37 actionable tasks, 1 executed, 36 up-to-date). No compilation errors.

---

### 11. Floating/PiP bubble mode works — NO
**Evidence:** Clicked bubble button (🫧) at top-right of chat screen. App crashed with system dialog: **"Allai keeps stopping"** with options "App info" / "Close app". The FloatingWindowService attempts to create an overlay window using `TYPE_APPLICATION_OVERLAY` which requires `SYSTEM_ALERT_WINDOW` permission. When the permission is not granted, the code attempts to open `Settings.ACTION_MANAGE_OVERLAY_PERMISSION`, but the app crashes instead of gracefully handling the permission flow. Bubble mode does not function.

**File:** `app/src/main/java/com/allai/app/floating/FloatingWindowService.kt`, `AgentScreen.kt:113-122`

---

### 12. Gacha-inspired visual aesthetic — YES
**Evidence:** Theme.kt defines `GachaDarkColorScheme` with gacha-game colors:
- Primary: `#7B2CBF` (purple)
- Secondary: `#00D4FF` (cyan)
- Tertiary: `#E879F9` (pink)
- Background: `#05050A` (near-black)
- Surface: `#0F0A1E` (dark purple)

UI uses glass cards with gradient borders (purple-to-cyan), gradient chat bubbles (user: purple gradient, agent: blue gradient), bubble (🫧) motif throughout. This is clearly gacha-inspired.

**File:** `app/src/main/java/com/allai/app/theme/Theme.kt`

---

### 13. Self-plugin for agent to interact with Allai's own systems — YES
**Evidence:** SelfPlugin.kt registered in PluginManager.init() (line 34). Implements `PhonePlugin` with name "self" and actions:
- `read_memories` — read all stored memories
- `add_memory` — add a new memory
- `learn_action` — record a learned action
- `get_learned_actions` — list learned actions
- `get_provider` — get current LLM provider/model/base URL

**File:** `app/src/main/java/com/allai/app/self/SelfPlugin.kt`, `PluginManager.kt:34`

---

## Summary Table

| # | Objective | Result | Evidence |
|---|-----------|--------|----------|
| 1 | Onboarding Get Started responds when API key filled | ✅ YES | Button enabled when apiKey non-blank |
| 2 | Settings tabs: Provider, Agent, Security, Account, Integrations | ✅ YES | All 5 tabs present in SettingsTabsScreen |
| 3 | Main chatbox: model selector, workspace, new session, send | ✅ YES | All buttons attached above chatbox input |
| 4 | Chatbar at BOTTOM | ✅ YES | BottomBar at bottom of screen |
| 5 | Bubble button top-right, chatbar bottom | ✅ YES | 🫧 at top-right, bottom bar at bottom |
| 6 | LLM/API works | ✅ YES | Agent responded "Yes, I'm working!" to test message |
| 7 | Model selector auto-detects models | ✅ YES | 13 models fetched from NVIDIA API |
| 8 | Memories system with own tab | ✅ YES | MemoriesScreen with add/edit/delete |
| 9 | Terminal works | ✅ YES | `echo hello` → output `hello` |
| 10 | App builds without errors | ✅ YES | `BUILD SUCCESSFUL` |
| 11 | Floating/PiP bubble mode works | ❌ NO | App crashes: "Allai keeps stopping" |
| 12 | Gacha-inspired visual aesthetic | ✅ YES | Purple/cyan/pink color scheme, glass cards, gradients |
| 13 | Self-plugin for agent | ✅ YES | SelfPlugin registered with 5 actions |

## Failure Details: Objective 11

**Symptom:** App crashes with system dialog "Allai keeps stopping" when clicking the bubble button.

**Root Cause:** `FloatingWindowService` uses `WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY` which requires the `SYSTEM_ALERT_WINDOW` permission. The `AgentScreen` bubble button onClick checks `Settings.canDrawOverlays(context)` and either opens the permission settings intent or starts the service. However, the app crashes instead of gracefully handling the permission state. The overlay permission must be manually granted via Android Settings > Special app access > Display over other apps > Allai before bubble mode can function.

**Fix Required:** Wrap `windowManager.addView()` in try/catch, or ensure the permission-check flow works correctly without crashing, or guide the user to grant the overlay permission before allowing bubble mode activation.

## Final Determination

**NO** — Not all 13 objectives are met. Objective 11 (Floating/PiP bubble mode) fails due to app crash. 11/13 objectives pass.
# ALLAI — TEMP DOC: FINAL JUDGMENT PHASE FINDINGS & FIXES
## Audit Date: Section 19-20 Execution
## Repository: https://github.com/xen38/allai.git
## Branch: main (all changes committed & pushed)

---

## PART 1: ERRORS & GAPS FOUND BY AUDIT

### CRITICAL (must fix before judgment)

| ID | Section | Feature | Gap | Severity |
|----|---------|---------|-----|----------|
| C1 | 7 | Model Selector | No search/filter for models | High |
| C2 | 8 | Floating Bubble | No model selector inside bubble chat | High |
| C3 | 8 | Floating Bubble | Fixed size only — no expand/minimize | High |
| C4 | 10 | SelfPlugin | No permission checks on tool actions | Critical |
| C5 | 10 | SelfPlugin | No immediate-vs-confirmation distinction | Critical |
| C6 | 13 | Memory | No search, suggested, or user-approved flows | Medium |
| C7 | 5.4 | Account | GoogleSignIn initiated but no token/session persistence | Medium |
| C8 | 9 | Notification access | Not implemented at all | Medium |
| C9 | 11 | Project analysis/debugging | Not implemented | Medium |
| C10 | 17 | Log redaction | No redacted logging | Medium |
| C11 | 17 | Credential storage | SharedPreferences (plaintext), not encrypted | High |

### MODERATE (should fix)

| ID | Section | Feature | Gap | Severity |
|----|---------|---------|-----|----------|
| M1 | 4.2 | Keyboard insets | adjustResize set but no inset animation handling | Low |
| M2 | 12.4 | NVIDIA/OpenRouter | Not explicit providers (covered by Custom) | Low |
| M3 | 14 | Animations | Minimal — no entrance/exit transitions | Low |
| M4 | 16 | Performance | No startup/memory optimization | Low |

### MINOR (document as limitations)

| ID | Section | Feature | Gap |
|----|---------|---------|-----|
| m1 | 5.2 | Inferred vs explicit memory | No distinction stored |
| m2 | 13 | Duplicate detection | Not implemented |
| m3 | 13 | Memory prioritization | Favorite flag only |
| m4 | 11 | Workspace management | Basic "Default" only |

---

## PART 2: FIXES TO APPLY

### Fix C1 — Model Selector Search
Add a search text field above the model list in the model picker overlay in `AgentScreen.kt`. Filter the displayed models in real time.

### Fix C2 — Floating Bubble Model Access
Add a model selector button inside `FloatingChatOverlay` in `FloatingWindowService.kt` so users can change models while in bubble mode.

### Fix C3 — Floating Bubble Expand/Minimize
Add minimize/expand toggle in `FloatingChatOverlay`. Minimized = compact header only. Expanded = full chat.

### Fix C4 — SelfPlugin Permission Checks
Add a permission-check wrapper in `SelfPlugin.kt` and `PluginManager.run()` that checks a permissions map before executing sensitive actions (shell, file write, SMS, launcher close).

### Fix C5 — Immediate vs Confirmation Distinction
Add a `confirmationRequired: Boolean` field to `ToolDefinition` and `PhonePlugin.toolDefinition()`. `AgentLoop` checks this flag and, if true, returns a "CONFIRM_REQUIRED" marker instead of executing.

### Fix C6 — Memory Search & Suggested
Add search filter to `MemoriesTab` in `SettingsTabsScreen.kt`. Add "Suggested" tab placeholder with auto-suggest stub.

### Fix C7 — Account Session Persistence
Complete GoogleSignIn flow: store account email/name in SharedPreferences on successful sign-in, show signed-in state on reload.

### Fix C8 — Notification Access
Add notification listener stub in plugin system and Security section toggle.

### Fix C9 — Project Analysis/Debugging
Add `ProjectPlugin` with `analyze` action that reads project files and returns architecture summary.

### Fix C10 — Log Redaction
Add `LogRedactor` utility that strips API keys, tokens, emails from log output.

### Fix C11 — Encrypted Credential Storage
Wrap `SettingsState` API key get/set with `EncryptedSharedPreferences` (AndroidX Security) fallback to plain SharedPreferences if unavailable.

---

## PART 3: IMPLEMENTATION PLAN

Phase A: Fix C1-C5 (Critical) — Model search, bubble model, bubble expand, permission checks, confirmation distinction
Phase B: Fix C6-C11 (Moderate/High) — Memory search, account persistence, notifications, project analysis, log redaction, encrypted storage
Phase C: Re-run Final Judgment Phase validation
Phase D: Commit all fixes and push to repository

---

## PART 4: VALIDATION CRITERIA (Section 19.6)

- 5 independent reviewers must each vote YES
- ≥10 real-world tasks executed (≥50% via emulator UI)
- ≥3 complex coding-agent tasks
- All reports evidence-based with screenshots and logs
- Unanimous 5/5 approval required

---

## PART 5: REMAINING LIMITATIONS (to document honestly)

- No actual LLM API key available for live testing (requires user-supplied key)
- No Android emulator access for UI screenshots in this environment
- EncryptedSharedPreferences requires minSdk 23+ (current minSdk 24 — OK)
- Google OAuth requires real google-services.json configuration
- Notification listener requires BIND_NOTIFICATION_LISTENER permission (declared but needs runtime grant)

# OpenText Agent Guide

This document helps AI coding agents work effectively with the OpenText codebase.

## Project Overview

OpenText is an Android code editor written in Kotlin + Jetpack Compose. It edits folders on the device (via Storage Access Framework) or remote folders via the OpenCode server. Inspired by Zed editor with OpenCode's color tokens.

**Package:** `com.tom7.opentext`

## Ecosystem & Relationships

OpenText is part of a three-project ecosystem:

### OpenText (this repo)
- **What:** Android code editor
- **Purpose:** Edit code on device or remote folders via OpenCode server
- **Location:** `../OpenText` (current directory)

### OpenRemote (`../openremote`)
- **What:** Android companion app for OpenCode
- **Purpose:** Mobile client for AI coding sessions - browse sessions, chat, approve permissions, steer prompts
- **Package:** `com.tom7.openremote`
- **Integration with OpenText:** OpenRemote has a code icon in session headers that launches OpenText with the session's project directory, passing the server URL and password
- **Handoff contract:** Intent action `com.tom7.opentext.OPEN_REMOTE` with extras: `url`, `password`, `directory`

### OpenCode (`../opencode`)
- **What:** The open source AI coding agent (TypeScript monorepo)
- **Purpose:** Provides the server that both OpenRemote and OpenText connect to
- **Package:** Monorepo with CLI, TUI, desktop app, server, SDK
- **API endpoints used by OpenText:**
  - `/api/fs/list` - List directory contents
  - `/api/fs/read/*` - Read file contents
  - `/api/experimental/fs/write` - Write files (experimental)
- **Authentication:** Basic auth with username `opencode` and server password

## ⚠️ CRITICAL: Modification Rules

**ALLOWED:**
- ✅ Modify **OpenText** codebase (this repo)
- ✅ Open PRs to **OpenText**
- ✅ Modify **OpenRemote** codebase (`../openremote`)
- ✅ Open PRs to **OpenRemote**

**STRICTLY BANNED:**
- ❌ **NEVER modify OpenCode codebase** (`../opencode`)
- ❌ **NEVER open PRs to OpenCode**
- ❌ Do not suggest changes to OpenCode, even if you see bugs or improvements
- ❌ If OpenCode has a bug or missing feature, work around it in OpenText or OpenRemote instead

This is a hard rule. Even if you notice issues in OpenCode, do not modify it. Work around them in the client apps.

## Tech Stack

- **Language:** Kotlin
- **UI:** Jetpack Compose
- **Build:** Gradle with Kotlin DSL
- **Min SDK:** 24 | **Target SDK:** 37
- **JDK:** 17+
- **Serialization:** kotlinx.serialization

## Architecture

```
app/src/main/java/com/tom7/opentext/
├── MainActivity.kt              Root navigation, intent handoff, SAF launcher
├── data/
│   ├── Workspace.kt             Workspace interface + LocalWorkspace/RemoteWorkspace implementations
│   ├── WorkspaceStore.kt        Persists last folder and connection
│   ├── SettingsStore.kt         App settings persistence
│   └── AppJson.kt               JSON serialization config
└── ui/
    ├── home/HomeScreen.kt       Open folder, connect, recent projects
    ├── connect/ConnectScreen.kt Server connection UI
    ├── editor/
    │   ├── EditorScreen.kt      Top bar, tabs, drawer, status bar
    │   ├── EditorViewModel.kt   Open files, tree management, save logic
    │   ├── CodeEditor.kt        Gutter + syntax highlighting surface
    │   ├── Syntax.kt            Language detection + tokenizer
    │   ├── FileTree.kt          Lazy directory tree
    │   └── QuickOpen.kt         Fuzzy file search
    ├── settings/SettingsScreen.kt
    ├── qrscanner/QRScannerScreen.kt
    └── theme/                   OpenCode tokens, typography
```

## Key Concepts

### Workspaces

Two implementations of the `Workspace` interface:
- **LocalWorkspace:** Uses Android SAF (Storage Access Framework) for on-device folders
- **RemoteWorkspace:** Talks to OpenCode server via `/api/fs/list`, `/api/fs/read/*`, `/api/experimental/fs/write`

### Editor Architecture

- **Tabs:** One file per tab with dirty state tracking
- **Syntax Highlighting:** Runs through `VisualTransformation` without modifying text
- **Line Numbers:** Gutter stays aligned via shared scroll containers
- **Performance:** Highlighting capped at 120k characters to prevent keystroke stalls

### Remote Integration

OpenText can be launched from OpenRemote with an intent:
```kotlin
Intent("com.tom7.opentext.OPEN_REMOTE")
    .setPackage("com.tom7.opentext")
    .putExtra("url", serverUrl)
    .putExtra("password", password)
    .putExtra("directory", directory)
```

## Build Commands

```bash
./gradlew assembleDebug          # Debug APK
./gradlew assembleRelease        # Signed release (needs keystore.properties)
./gradlew test                   # Unit tests
```

## Code Conventions

- **Package structure:** Feature-first (ui/home, ui/editor, etc.)
- **State management:** ViewModels for screen state
- **Naming:** PascalCase for classes, camelCase for functions/properties
- **Compose:** Stateless composables where possible, state hoisted to ViewModels
- **Error handling:** Result types for operations that can fail

## Design System

- **Colors:** OpenCode tokens from `packages/ui/src/styles/tokens/colors.css`
- **Syntax:** One Dark spectrum (Zed-style)
- **Typography:** System sans for UI (11-15sp), monospace for code (13sp, 19sp line height)
- **Density:** Minimal chrome - 52dp top bar, 34dp tabs, 26dp status bar

## Testing

Tests are in `app/src/test/java/com/tom7/opentext/`. Key test files:
- `ServerUrlTest.kt` - Server URL parsing
- `LanguageTest.kt` - Language detection

## Important Notes

- No language server or git integration (yet)
- Remote write endpoint is experimental in OpenCode server
- `local.properties` must point to Android SDK
- Release signing requires `keystore.properties` (not in git)
- Editor uses unbounded constraints to prevent soft wrapping (logical line = visual line)

## Common Tasks

### Add a new language for syntax highlighting

1. Open `ui/editor/Syntax.kt`
2. Add language detection logic in `detectLanguage()`
3. Add tokenizer rules following the existing pattern

### Add a new screen

1. Create screen composable in appropriate `ui/` subpackage
2. Add navigation route in `MainActivity.kt`
3. Create ViewModel if needed for state management

### Modify the editor

- `CodeEditor.kt` - Editor surface and scrolling
- `Syntax.kt` - Tokenization and highlighting
- `EditorViewModel.kt` - File management and save logic

## Limitations

- No split panes
- Remote folders need server reachable from phone
- Experimental remote write API may change

# OpenText

A quiet, fast code editor for Android, inspired by [Zed](https://zed.dev) and built on OpenCode's
colour tokens. It edits folders **on the device** and **on your machine** through the OpenCode
server, so it works with or without [OpenRemote](https://github.com/tomlin7/openremote).

```
┌───────────────────────────┐
│ ☰  project              ⌕│   Zed-ish chrome: one accent, no decoration
├───────────────────────────┤
│ main.kt ×                 │   tabs only when you need them
├───────────────────────────┤
│ 1  package demo           │
│ 2                         │   line numbers track the vertical scroll
│ 3  fun main() {           │
│ 4      println("hi")      │   One Dark syntax over OpenCode grey
│ 5  }                      │
├───────────────────────────┤
│ Kotlin      Ln 4, Col 18  │   quiet status bar
└───────────────────────────┘
```

## What it does

- **Local folders** through the Storage Access Framework. Open any folder, keep the permission
  across launches, and return to it from the home screen.
- **Remote folders** through the OpenCode server: the same address and password OpenRemote uses.
  OpenText asks the server for the project directory, then lists, reads and writes through
  `/api/fs/list`, `/api/fs/read/*` and `/api/experimental/fs/write`.
- **Tabs with dirty state**, one file per tab, with a discard confirmation instead of silent loss.
- **File tree drawer** with lazily expanded directories and common build folders folded away.
- **Quick open** (`⌕`) with fuzzy matching over up to 4000 files.
- **Syntax highlighting** for Kotlin, Java, JavaScript/TypeScript, Python, Rust, Go, C family,
  JSON, HTML/XML, CSS, Markdown, Shell, YAML, TOML and SQL.
- **Line-number gutter**, caret line/column readout, and a save/revert-aware status bar.
- **Save all** when a workspace is closing with unsaved edits.

## Two ways in

### On its own

Launch OpenText, tap **Open folder**, pick a directory, edit. No server, no account, no network.

### From OpenRemote

Open a session in OpenRemote, then tap the **code icon** in the session header. OpenRemote hands
OpenText the server URL, the connection password and the session's project directory:

```kotlin
Intent("com.tom7.opentext.OPEN_REMOTE")
    .setPackage("com.tom7.opentext")
    .putExtra("url", serverUrl)
    .putExtra("password", password)
    .putExtra("directory", directory)
```

OpenText opens that folder straight away, so an edit on the phone lands in the same checkout the
agent is working in. If OpenText is not installed, OpenRemote explains where to get it.

## Build

```bash
./gradlew assembleDebug          # debug APK
./gradlew assembleRelease        # signed release, when keystore.properties exists
```

`local.properties` must point at your Android SDK. Release signing uses an optional
`keystore.properties` (kept out of git):

```properties
storeFile=keystore/opentext.jks
storePassword=…
keyAlias=opentext
keyPassword=…
```

Requires JDK 17+ and Android SDK 37. `minSdk` is 24.

## Design notes

- **Palette.** Chrome, borders, error and success colours are the OpenCode tokens from
  `packages/ui/src/styles/tokens/colors.css`; syntax is the One Dark spectrum Zed ships with.
  Dark by default, light scheme available in code.
- **Type.** The UI uses a system sans at 11–15sp; the editor uses a monospace at 13sp with a 19sp
  line height so a line of code and a gutter number share a baseline.
- **Editing.** The editor is a `BasicTextField` inside shared vertical/horizontal scroll
  containers: unbounded constraints stop soft wrapping, so a logical line stays one visual line and
  the gutter stays aligned. Highlighting runs through a `VisualTransformation` that recolours tokens
  without touching the text, and is capped at 120k characters so a giant file never stalls a
  keystroke.
- **Density.** One 52dp bar, a 34dp tab strip, a 26dp status bar, and 6dp tree rows. Nothing is
  centred that can be left aligned.

## Layout

```
app/src/main/java/com/tom7/opentext/
├── MainActivity.kt          root navigation, intent handoff, SAF launcher
├── data/
│   ├── Workspace.kt         Workspace interface, LocalWorkspace (SAF), RemoteWorkspace (server)
│   ├── WorkspaceStore.kt    last folder, last connection
│   └── AppJson.kt
└── ui/
    ├── home/HomeScreen.kt   open a folder, connect, recent projects
    ├── connect/ConnectScreen.kt
    ├── editor/
    │   ├── EditorScreen.kt      top bar, tabs, drawer, status bar
    │   ├── EditorViewModel.kt   open files, tree, save
    │   ├── CodeEditor.kt        gutter + highlighting surface
    │   ├── Syntax.kt            language detection + tokenizer
    │   ├── FileTree.kt
    │   └── QuickOpen.kt
    └── theme/               OpenCode tokens, typography
```

## Limits

- The remote write endpoint is experimental in the OpenCode server and may change.
- Remote folders need the server reachable from the phone (LAN address or OpenRemote's tunnel).
- No language server, no git integration, no split panes. It edits text, quickly.

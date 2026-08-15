# Modern Mobile Text Editor with Incremental Version Control 📱💻

[![Kotlin](https://img.shields.io/badge/Kotlin-1.9.24-7F52FF.svg?style=flat&logo=kotlin)](https://kotlinlang.org/)
[![Android](https://img.shields.io/badge/Platform-Android%2015%20%28API%2035%29-3DDC84.svg?style=flat&logo=android)](https://developer.android.com/)
[![Compose](https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4.svg?style=flat&logo=jetpackcompose)](https://developer.android.com/jetpack/compose)
[![Room](https://img.shields.io/badge/Database-Room%20Persistence-0052CC.svg?style=flat&logo=sqlite)](https://developer.android.com/training/data-storage/room)

> **IS2205: Mobile Application Design & Development Mini-Project**  
> A lightweight, feature-rich Android text editor built for mobile developers and technical writers with **Delta-Based Incremental Version Control**, syntax highlighting, crash recovery, and real-time code compilation.

---

## 👥 Team Work Distribution & Roles

### 👑 1. Bomal (Lead Developer & System Architect)
- **Branch**: `bomal_d1`
- **Core Responsibilities**:
  - Entire Application Architecture Design & Modular Folder Structure
  - **Database Layer**: `VersionDatabase.kt`, `AppDao.kt`, `Entities.kt` (File, Version, Recovery, Recent tables)
  - **Incremental Version Control Engine**: `EditorRepository.kt`, `DiffManager.kt` (`java-diff-utils` unified diff patch chain)
  - **Version Control UI**: `VersionHistoryDialog.kt`, `DiffViewerDialog.kt` (Arbitrary V_A vs V_B diff view & rollback mechanics)
  - **MVVM Integration**: `EditorViewModel.kt` single-source-of-truth state management

### 🎨 2. Sanduni (UI & Theme Engineer)
- **Branch**: `sanduni_d2`
- **Core Responsibilities**:
  - **Editor & UI Components**: `CodeEditor.kt`, `ModernBottomBar.kt`, `DrawerContent.kt`, `ToolBar.kt`
  - **Resource Configuration**: `themes.xml`, `colors.xml`, `strings.xml`, `network_security_config.xml`

### ⚡ 3. Miheesha (Syntax & Language Specialist)
- **Branch**: `miheesha_d3`
- **Core Responsibilities**:
  - **Syntax Rule Configurations**: `kotlin.json`, `java.json`, `python.json`, `markdown.json`
  - **Syntax Parsing & Rendering**: `highlightSyntax.kt`, `SyntaxRules.kt`, Markdown preview engine

---

## 🌟 Key Features

### 1. 📝 Mobile Text Editor Engine
- **Single Active File View**: Streamlined interface focusing on the active document with a slide-out drawer navigation bar.
- **File Lifecycle Operations**: New File, Open File, Save, Save As, and Recent Files history.
- **UTF-8 Encoding**: Complete support for standard UTF-8 characters and multi-language scripts.
- **Read-Only Lock Mode**: Prevents accidental modifications with a quick top-bar lock toggle.
- **Word Wrapping Toggle**: Switch dynamically between horizontal scroll code view and auto-wrapping text.
- **Undo / Redo System**: Session memory stack to step backward and forward through text edits.
- **Text Search & Replace**: Find and replace first or all occurrences across the active buffer.
- **Code Formatter**: Built-in Kotlin raw code auto-formatter.

### 2. 🎨 Syntax Highlighting & Rendering
- **Kotlin & Java & Python Highlighting**: Real-time keyword, string, comment (`//`, `/* */`), annotation (`@Composable`, `@Dao`, etc.), and function styling using custom JSON rules.
- **Markdown Highlighting & Live Preview**: Inline styled markdown with an optional toggleable **Markdown Preview Panel** rendering styled headers, bold, italic, blockquotes, lists, and code blocks.
- **Lag-Free Rendering**: Single-pass `AnnotatedString` direct rendering engine.

### 3. 🛡️ Fault Tolerance & Crash Recovery
- **10-Second Periodic Autosave**: Background timer periodically caches the active buffer to local SQLite database storage.
- **Crash Protection**: Automatic prompt upon launch to restore unsaved drafts if the application was unexpectedly terminated.

### 4. 🗂️ Incremental Delta-Based Version Control
- **Non-Duplicating Storage**: Stores **V1 as a full base snapshot**, while **V2..VN store Unified Diff text patches** generated via `java-diff-utils`.
- **Arbitrary Version Diff Comparison**: Select any pair of historical versions ($V_A$ vs $V_B$) and view line-by-line colored diffs (Green = Added, Red = Removed).
- **Rollback / Restore**: Reconstruct historical text by stepping through delta patch chains and restoring to the active editor workspace.

---

## 📐 Architecture & Incremental Versioning

```
[ Active File (Disk) ] <---> [ EditorViewModel ] <---> [ Room Database (v3) ]
                                    |
                    +---------------+---------------+
                    |                               |
          [ FileEntity (Meta) ]           [ VersionEntity (Deltas) ]
                                          |-- V1 (isBase = true, Full Text)
                                          |-- V2 (isBase = false, Patch Delta)
                                          |-- V3 (isBase = false, Patch Delta)
```

### Delta Versioning Concept

| Version | Storage Type | Content |
|:---:|:---:|:---|
| **Version 1** | **Base Snapshot** | `fun main() { println("Hello World") }` |
| **Version 2** | **Unified Diff Patch** | `@@ -1 +1,2 @@\n+ // Added comment` |
| **Version 3** | **Unified Diff Patch** | `@@ -2 +2 @@\n- println("Hello World")\n+ println("Hello Kotlin")` |

---

## 🛠️ Technology Stack & Libraries

- **Language**: Kotlin 1.9
- **UI Framework**: Jetpack Compose + Material 3 (Dark Theme Palette)
- **Architecture**: MVVM (Model-View-ViewModel) + Coroutines Flow
- **Local Persistence**: Room SQLite DB v3 (`fallbackToDestructiveMigration`)
- **Diff Engine**: `io.github.java-diff-utils:java-diff-utils:4.12`
- **Compiler API**: Remote Code Execution Engine (Judge0 API Integration)

---

## 🚀 Getting Started & Installation

### Prerequisites
- Android Studio Ladybug (2024.2+) or newer
- JDK 11 or JDK 17
- Android SDK 35
- Physical Device or Emulator (Android 11.0 / API 30+)

### Setup Instructions

1. **Clone the Repository**
   ```bash
   git clone https://github.com/bomaldesilva/MobileCodeEditor.git
   cd MobileCodeEditor
   ```

2. **Open in Android Studio**
   - Open Android Studio -> **Open Project** -> Select `MobileCodeEditor` folder.
   - Allow Gradle Sync to finish downloading dependencies.

3. **Build & Run via Terminal (Alternative)**
   ```bash
   # Set JAVA_HOME (macOS example)
   export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
   
   # Build Debug APK
   ./gradlew assembleDebug
   
   # Install to connected device/emulator
   adb install -r app/build/outputs/apk/debug/app-debug.apk
   ```

---

## 📂 Project Directory Structure

```
CodeEditor/
├── app/src/main/
│   ├── assets/                 # Language syntax rules (kotlin.json, java.json, python.json, markdown.json)
│   ├── kotlin/com/example/codeeditor/
│   │   ├── MainActivity.kt      # Main Entry Point & Compose UI Host
│   │   ├── repository/         # EditorRepository (Disk I/O & Room abstraction)
│   │   ├── storage/            # Room Entities, DAOs, VersionDatabase, DiffManager
│   │   ├── ui/                 # CodeEditor, ModernBottomBar, DrawerContent, DiffViewerDialog
│   │   └── viewmodel/          # EditorViewModel (UI State Management)
│   └── res/                    # App icons, xml configs & theme resources
└── build.gradle.kts
```

---

## 📜 License & Academic Disclaimer
Developed for the **IS2205: Mobile Application Design and Development** University Module. All rights reserved.

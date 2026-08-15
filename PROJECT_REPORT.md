# IS2205: Mobile Application Design & Development
## Final Project Report
### Modern Mobile Text Editor with Incremental Version Control

---

**Course**: IS2205 - Mobile Application Design and Development  
**Project Title**: Modern Mobile Text Editor with Incremental Version Control  
**Platform**: Android Native (Kotlin, Jetpack Compose, Room SQLite)  
**Submission Date**: August 15, 2026  

---

### Team Members & Contributions

| Member Name | Student Role | Primary Responsibilities & Module Ownership | Branch |
|:---|:---|:---|:---:|
| **Bomal** | **Lead Architect & Database Engineer** | Overall System Architecture, Modular Project Structure, Room Database Schema (`VersionDatabase.kt`, `Entities.kt`, `AppDao.kt`), `java-diff-utils` Delta Engine, Version History & Rollback (`EditorRepository.kt`, `VersionHistoryDialog.kt`, `DiffViewerDialog.kt`), ViewModel (`EditorViewModel.kt`). | `bomal_d1` |
| **Sanduni** | **UI/UX & Theme Specialist** | Jetpack Compose Editor Components (`CodeEditor.kt`, `ModernBottomBar.kt`, `DrawerContent.kt`), M3 Dark Theme System (`Theme.kt`, `Color.kt`), Resource Configuration (`themes.xml`, `colors.xml`, `strings.xml`, `network_security_config.xml`). | `sanduni_d2` |
| **Miheesha** | **Syntax & Parser Specialist** | Syntax Highlighting Engine (`highlightSyntax.kt`, `SyntaxRules.kt`), Rule Configuration JSONs (`kotlin.json`, `java.json`, `python.json`, `markdown.json`), Markdown Live Preview Engine (`MarkdownPreview.kt`). | `miheesha_d3` |

---

## 1. Executive Summary & Problem Overview

Mobile devices are increasingly used by technical writers and software developers for quick code reviews, edits, and script writing. Traditional mobile text editors suffer from three major shortcomings:
1. **Inefficient Versioning**: Saving multiple file revisions duplicates full file content, leading to storage bloat on mobile hardware.
2. **Data Loss from Crashes**: Sudden Android OS lifecycle interruptions or app crashes discard active unsaved editor buffers.
3. **Laggy Syntax Highlighting**: Inefficient regex styling algorithms cause typing lag during active editing.

To resolve these problems, **Modern Mobile Text Editor** was developed. It introduces a **Delta-Based Incremental Version Control System** utilizing `java-diff-utils` (storing $V_1$ as a full base snapshot and $V_2..V_N$ as lightweight unified diff patches), a **10-Second Crash Recovery Daemon**, and a lag-free **Jetpack Compose Single-Pass Syntax Rendering Engine**.

---

## 2. System Architecture & Component Design

The application adheres strictly to the **MVVM (Model-View-ViewModel)** architectural pattern to decouple business logic from Jetpack Compose UI rendering.

```
 +-----------------------------------------------------------------------+
 |                             PRESENTATION                              |
 |   [ MainActivity ] <---> [ CodeEditor ] <---> [ DrawerContent ]       |
 +----------------------------------+------------------------------------+
                                    | StateFlow / UI Events
 +----------------------------------v------------------------------------+
 |                              VIEWMODEL                                |
 |                         [ EditorViewModel ]                           |
 |              (Manages Single Active File State & Buffers)             |
 +----------------------------------+------------------------------------+
                                    | IO Dispatcher / Repository
 +----------------------------------v------------------------------------+
 |                            DATA LAYER                                 |
 |                       [ EditorRepository ]                            |
 |        +-------------------------+-------------------------+          |
 |        | Disk Storage (UTF-8)    | Room Database (v3)      |          |
 |        +-------------------------+-------------------------+          |
 |                                  | AppDao & DiffManager               |
 +----------------------------------+------------------------------------+
```

`[DIAGRAM / SCREENSHOT: Overall System Architecture & Data Flow Diagram]`

---

## 3. Database Architecture & Room Schema

The persistence layer is implemented using Android **Room Persistence Library (SQLite)**. To prevent storage duplication, the database segregates file metadata, delta patch histories, crash recovery drafts, and recent file records.

### 3.1 Database Schemas

#### 1. `files` Table (File Metadata)
Stores high-level file information without duplicating file content.
- `fileId` (Long, Primary Key, Auto-Increment)
- `fileName` (String)
- `filePath` (String, Absolute Local Path)
- `encoding` (String, Default: "UTF-8")
- `language` (String: "kotlin", "java", "python", "markdown")
- `isReadOnly` (Boolean)
- `createdAt`, `updatedAt` (Long, Unix Timestamps)

#### 2. `file_versions` Table (Incremental Delta Storage)
Linked via Foreign Key (`fileId`) with `CASCADE` deletion.
- `versionId` (Long, Primary Key)
- `fileId` (Long, Foreign Key -> `files.fileId`)
- `versionNumber` (Int)
- `versionName` (String)
- `createdAt` (Long)
- `parentVersionId` (Long)
- `isBase` (Boolean): `true` for Base Version $V_1$; `false` for Delta Versions $V_2..V_N$.
- `patchContent` (String): Contains full initial text if `isBase = true`; contains Unified Diff patch string if `isBase = false`.
- `checksum` (String)

#### 3. `recovery_drafts` Table (Crash Recovery)
- `fileId` (Long, Primary Key)
- `content` (String, Auto-Saved Active Buffer)
- `updatedAt` (Long)

#### 4. `recent_files` Table
- `filePath` (String, Primary Key)
- `fileName` (String)
- `lastOpenedAt` (Long)

`[DIAGRAM / SCREENSHOT: Room Database ER Diagram & Table Schemas]`

---

## 4. Key Functional Modules & Features

### 4.1 Mobile Text Editor Engine
- **Single Active File Workaround**: Maintains one active editing buffer at a time while providing rapid switching via the **Recent Files Drawer**.
- **File Operations**: Full support for New File, Open File (via System Storage Picker), Save, Save As, and Recent Files.
- **UTF-8 Encoding**: Explicitly enforced across file I/O operations (`Charsets.UTF_8`).
- **Read-Only Mode**: Locks editor input via Compose `readOnly = true` state.
- **Word Wrapping Toggle**: Dynamically switches the layout between horizontal scroll code view and auto-wrapping text.
- **Undo / Redo System**: Implemented using `ArrayDeque<TextFieldValue>` in `TextEditorState` to track granular editing history.
- **Search & Replace**: Modal dialog allowing single replacement (`replace`) or global replacement (`replaceAll`).

`[SCREENSHOT: Main Code Editor Workspace with Line Numbers & Top Action Bar]`

`[SCREENSHOT: Sidebar Drawer Menu showing File Actions & Recent Files]`

---

### 4.2 Syntax Highlighting & Markdown Live Preview
- **Multi-Language Support**: Real-time highlighting for **Kotlin, Java, Python, and Markdown**.
- **Lag-Free Rendering**: Uses a single-pass `AnnotatedString` builder directly bound to `BasicTextField`, avoiding double-pass UI overlays and typing lag.
- **JSON Rule Parser**: Keywords, comments, and annotations are dynamically loaded from `assets/` (`kotlin.json`, `java.json`, `python.json`, `markdown.json`).
- **Markdown Live Preview**: An integrated preview panel parsing headers (`#`, `##`, `###`), blockquotes (`>`), lists (`- `, `* `), bold (`**text**`), italic (`*text*`), and monospace code blocks (` ``` `).

`[SCREENSHOT: Kotlin Syntax Highlighting in Editor (Red Keywords, Blue Strings, Grey Comments, Purple Annotations)]`

`[SCREENSHOT: Java / Python Code Syntax Highlighting]`

`[SCREENSHOT: Markdown Split View / Live Preview Panel]`

---

### 4.3 10-Second Crash Recovery Daemon
To safeguard against app crashes or unexpected OS activity termination:
- A background coroutine loop runs every 10 seconds inside `viewModelScope`:
  ```kotlin
  LaunchedEffect(Unit) {
      while (true) {
          delay(10000)
          viewModel.autoSaveRecoveryDraft()
      }
  }
  ```
- If an unsaved draft is detected upon reopening a file, an `AlertDialog` prompts the user to restore or discard the recovery buffer.
- Recovery records are automatically purged when explicit manual saves occur.

`[SCREENSHOT: Crash Recovery Alert Prompt upon App Relaunch]`

---

### 4.4 Incremental Version Control & Delta Storage
To fulfill the requirement of **non-duplicating version control**:
1. **First Save / Initial Version ($V_1$)**: Stored as a complete base snapshot (`isBase = true`).
2. **Subsequent Snapshots ($V_2..V_N$)**: Stored strictly as **Unified Diff text patches** generated via `java-diff-utils`:
   ```kotlin
   val patchString = DiffManager.createPatch(previousText, currentContent)
   ```
3. **Reconstruction Algorithm**: Any historical version $V_K$ is reconstructed by walking the linear delta chain:
   $$\text{Text}_{V_K} = \text{ApplyPatch}(\dots \text{ApplyPatch}(\text{BaseText}_{V_1}, \text{Patch}_{V_2}) \dots, \text{Patch}_{V_K})$$

`[SCREENSHOT: Version History & Snapshot Creation Dialog]`

---

### 4.5 Line-by-Line Diff Viewer & Rollback Engine
- **Arbitrary Version Comparison**: Users can select any two versions ($V_A$ vs $V_B$) from dropdown selectors to inspect differences.
- **Visual Color Coding**:
  - 🟢 **Green Highlight (`+`)**: Newly added lines
  - 🔴 **Red Highlight (`-`)**: Deleted lines
- **Rollback Mechanism**: Reconstructs the target historical version, loads it into the active workspace, and automatically creates a new tracking snapshot (`Restored from VX`).

`[SCREENSHOT: Version Line-by-Line Diff Viewer Dialog (Green Added / Red Removed)]`

---

### 4.6 Online Remote Compiler API
Integrates a remote code execution client (Judge0 REST API) allowing users to compile and run active Kotlin, Java, or Python code directly within the app.

`[SCREENSHOT: Remote Compiler Output Dialog showing Execution Log, Output & CPU Time]`

---

## 5. Verification & Test Execution Results

All 17 required test scenarios specified in the assignment mandate were executed and verified clean.

| Test ID | Test Scenario | Steps Executed | Result |
|:---:|:---|:---|:---:|
| **TEST 1** | Create file & save | Click New File → type Kotlin code → Save. | **PASS** |
| **TEST 2** | Create Version 2 | Edit text → click "+ Snapshot" → Version 2 created as Delta Patch. | **PASS** |
| **TEST 3** | Create Version 3 | Further edits → Save Snapshot → Version 3 created as Delta Patch. | **PASS** |
| **TEST 4** | Open Version 1 | Open Version History → Restore V1 → Editor displays original V1 text. | **PASS** |
| **TEST 5** | Open Version 2 | Restore V2 → Delta chain reconstructed accurately. | **PASS** |
| **TEST 6** | Compare V1 & V3 | Select V1 in Dropdown A and V3 in Dropdown B → Line diff shown cleanly. | **PASS** |
| **TEST 7** | Rollback V1 | Click Restore V1 → Editor updates → New snapshot recorded. | **PASS** |
| **TEST 8** | Edit post rollback | Type new code after rollback → Save → New delta branch created cleanly. | **PASS** |
| **TEST 9** | Read-Only Mode | Toggle Lock icon in top bar → Typing disabled; cursor read-only. | **PASS** |
| **TEST 10** | App restart persistence | Close app completely → Reopen → Recent files and version DB intact. | **PASS** |
| **TEST 11** | Crash Recovery | Type text → wait 10s → kill process → reopen → Recovery dialog prompts. | **PASS** |
| **TEST 12** | Kotlin Highlighting | Verify keywords (`fun`, `val`, `sealed`), strings, comments in red/blue/grey. | **PASS** |
| **TEST 13** | Markdown Highlighting | Switch to `.md` → verify headers, bold, lists, and live preview rendering. | **PASS** |
| **TEST 14** | Search and Replace | Open Find bar → enter target/replace strings → execute Replace All. | **PASS** |
| **TEST 15** | Undo / Redo | Perform typing → tap Undo button → text reverts; tap Redo → restored. | **PASS** |
| **TEST 16** | Save As | Tap Save As → export to external URI via Android Storage Access Framework. | **PASS** |
| **TEST 17** | Recent Files | Open drawer → select previous file from list → file loads from local path. | **PASS** |

---

## 6. Conclusion & Future Enhancements

The **Modern Mobile Text Editor** successfully meets all core functional, architectural, and non-duplicating version control requirements specified in the **IS2205 Mini-Project Specification**.

### Key Achievements:
- Non-duplicating incremental delta versioning engine powered by Room DB & `java-diff-utils`.
- 10-second crash recovery daemon protecting against data loss.
- Lag-free multi-language syntax highlighting & live Markdown preview.
- 100% test pass rate across all 17 mandatory university test scenarios.

### Future Roadmap:
- Syntax highlighting tree-sitter integration for multi-threaded large file parsing.
- Git remote sync integration for pushing local version snapshots directly to GitHub repositories.
- Multi-tab file editing interface.

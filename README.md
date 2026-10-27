# FileMan: Java Swing File Manager

[![Maven Central](https://img.shields.io/maven-central/v/com.github.javadev/filemanager?style=flat-square)](https://central.sonatype.com/artifact/com.github.javadev/filemanager/1.1)
[![Java CI](https://github.com/javadev/file-manager/actions/workflows/maven.yml/badge.svg)](https://github.com/javadev/file-manager/actions/workflows/maven.yml)
[![CodeQL](https://github.com/javadev/file-manager/actions/workflows/codeql-analysis.yml/badge.svg)](https://github.com/javadev/file-manager/actions/workflows/codeql-analysis.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)
[![Java](https://img.shields.io/badge/Java-8%2B-blue.svg)](https://www.java.com)
[![GitHub stars](https://img.shields.io/github/stars/javadev/file-manager.svg?style=social)](https://github.com/javadev/file-manager)

**FileMan** is a lightweight, cross-platform desktop file manager written in pure Java Swing. It lets you browse your file system and view file details. It can also open, edit, print, create, copy, rename and delete files and directories.

---

## Table of Contents

- [Features](#features)
- [Requirements](#requirements)
- [Getting Started](#getting-started)
  - [Clone the Repository](#clone-the-repository)
  - [Build with Maven](#build-with-maven)
  - [Run the Application](#run-the-application)
- [Usage](#usage)
  - [User Interface Overview](#user-interface-overview)
  - [Keyboard Shortcuts](#keyboard-shortcuts)
  - [Menus](#menus)
- [Embedding FileMan in Your Application](#embedding-fileman-in-your-application)
- [Project Structure](#project-structure)
- [Known Limitations](#known-limitations)
- [Contributing](#contributing)
- [License](#license)

---

## Features

- 🌳 **File system tree**: lazily loaded directory tree that shows the file system roots, with native system icons and display names.
- 📋 **Directory listing table**: shows the contents of the selected directory in these columns:
  - Icon, File name, Path, Size, Last Modified
  - Readable (R), Writable (W), Executable (E), Directory (D) and File (F) flags
- 🔍 **Live filtering**: case-insensitive filtering of the directory listing by file name as you type.
- ↕️ **Sortable columns**: click any column header to sort.
- 🖱️ **Double-click navigation**: double-click a directory to open it, or a file to open it with its default system application.
- 📄 **File details panel**: shows the file name, full path, last modified date, human-readable size (B, KB, MB, GB…) and type.
- 🛠️ **File operations**:
  - **Open**, **Edit** and **Print** using the platform's default applications (via `java.awt.Desktop`)
  - **New** file or directory
  - **Copy** a file or directory to a chosen destination
  - **Copy / Paste** using an internal clipboard
  - **Rename** files and directories
  - **Delete** files and directories, with recursive delete and a confirmation prompt
  - **Refresh** the current directory
- 🔐 **Permission editing**: toggle Read / Write / Execute permissions with checkboxes.
- ⏳ **Responsive UI**: directory loading runs in a background `SwingWorker` and shows a progress indicator.
- 📊 **Status bar**: shows item counts and the results of operations.
- ⌨️ **Keyboard shortcuts** and **mnemonics** for common actions.
- 🎨 **Native look & feel**: uses the system Look and Feel of your operating system.
- ⚠️ **Overwrite protection**: asks for confirmation before overwriting existing files during copy and paste.

---

## Requirements

- **Java 8** or newer (JDK to build, JRE to run)
- **Apache Maven 3.x** to build the project
- **[Apache Commons IO](https://commons.apache.org/proper/commons-io/)**, used for recursive copy and delete. Maven resolves it automatically.
- A desktop environment. FileMan is a GUI application and cannot run in a headless environment.

---

## Getting Started

### Clone the Repository

```bash
git clone https://github.com/javadev/file-manager.git
cd file-manager
```

### Build with Maven

```bash
mvn clean package
```

The compiled artifacts are placed in the `target/` directory.

### Run the Application

**Option 1: Using Maven**

```bash
mvn compile exec:java -Dexec.mainClass="com.github.filemanager.FileManager"
```

**Option 2: Using the built JAR**

```bash
java -jar target/file-manager-<version>.jar
```

> **Note:** If the produced JAR does not bundle its dependencies, add Commons IO to the classpath:
>
> ```bash
> java -cp "target/classes:path/to/commons-io.jar" com.github.filemanager.FileManager
> ```
>
> On Windows, use `;` instead of `:` as the classpath separator.

**Option 3: From your IDE**

Open the project as a Maven project in IntelliJ IDEA, Eclipse or NetBeans. Then run the `main` method of `com.github.filemanager.FileManager`.

---

## Usage

### User Interface Overview

```
+---------------------------------------------------------------------+
| File  Edit  Help                                                    |
+------------------+--------------------------------------------------+
|                  | Filter: [______________________________________] |
|  File System     | +----------------------------------------------+ |
|  Tree            | | Icon | File | Path/name | Size | Modified |…  | |
|                  | |------+------+-----------+------+----------+-- | |
|  > Desktop       | |  …   |  …   |    …      |  …   |    …     |   | |
|    > Documents   | +----------------------------------------------+ |
|    > Downloads   | [Open][Edit][Print] | [New][Copy][Rename]        |
|    > …           | [Delete][Refresh]   | [ ]Read [ ]Write [ ]Execute|
|                  |                                                  |
|                  |  File:          example.txt                      |
|                  |  Path/name:     /home/user/example.txt           |
|                  |  Last Modified: 2025-01-01 12:00:00              |
|                  |  File size:     1.23 KB (1260 bytes)             |
|                  |  Type:          ( ) Directory  (•) File          |
+------------------+--------------------------------------------------+
| Ready                                               [progress bar]  |
+---------------------------------------------------------------------+
```

1. **Left pane (tree):** Select a directory to load its contents. Subdirectories are added to the tree as you explore.
2. **Right pane (table):** Shows the contents of the selected directory. Select a row to see its details. Double-click a row to navigate into it or open it.
3. **Filter field:** Type to filter the listing by file name.
4. **Toolbar:** Runs actions on the currently selected file or directory.
5. **Details panel:** Shows information about the current selection.
6. **Status bar:** Shows feedback, such as item count or "Copied to…", plus a progress indicator while directories load.

### Keyboard Shortcuts

| Shortcut                  | Action                                       |
|---------------------------|----------------------------------------------|
| `F2`                      | Rename selected file/directory               |
| `Delete`                  | Delete selected file/directory               |
| `F5`                      | Refresh current directory                    |
| `Ctrl+C` / `⌘+C` (macOS)  | Copy selected item to the internal clipboard |
| `Ctrl+V` / `⌘+V` (macOS)  | Paste clipboard item into current directory  |
| `Ctrl+N` / `⌘+N` (macOS)  | Create a new file or directory               |

Toolbar buttons also have mnemonics, such as `Alt+O` for Open, `Alt+E` for Edit, `Alt+P` for Print, `Alt+N` for New, `Alt+C` for Copy, `Alt+R` for Rename, `Alt+D` for Delete and `Alt+F` for Refresh.

### Menus

- **File**: New…, Refresh, Exit
- **Edit**: Copy, Paste, Rename…, Delete
- **Help**: About (shows a summary of shortcuts)

---

## Embedding FileMan in Your Application

`FileManager` exposes its UI as a reusable Swing `Container`, so you can embed it in your own windows:

```java
import com.github.filemanager.FileManager;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

public class MyApp {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("My App with FileMan");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

            FileManager fileManager = new FileManager();
            frame.setContentPane(fileManager.getGui());
            frame.setJMenuBar(fileManager.createMenuBar()); // optional

            frame.pack();
            frame.setLocationByPlatform(true);
            frame.setVisible(true);

            fileManager.showRootFile(); // select the first root node
        });
    }
}
```

**Public API:**

| Method                                     | Description                                                         |
|--------------------------------------------|---------------------------------------------------------------------|
| `Container getGui()`                       | Builds (once) and returns the main file manager panel.              |
| `JMenuBar createMenuBar()`                 | Creates the File / Edit / Help menu bar.                            |
| `void showRootFile()`                      | Selects the first node in the file system tree.                     |
| `static boolean copyFile(File, File)`      | Utility that copies a single file using NIO channels and keeps its permissions. |

> **Note:** The **File → Exit** menu item calls `System.exit(0)`. If you embed FileMan in a larger application, you may want to provide your own menu bar.

---

## Project Structure

```
file-manager/
├── pom.xml
├── LICENSE
├── README.md
└── src/
    └── main/
        ├── java/
        │   └── com/github/filemanager/
        │       └── FileManager.java        # Main class, FileTableModel, FileTreeCellRenderer
        └── resources/
            └── com/github/filemanager/
                ├── fm-icon-16x16.png       # Window icon (small)
                └── fm-icon-32x32.png       # Window icon (large)
```

**Main classes:**

- **`FileManager`**: the application entry point. It builds the GUI and implements the file operations.
- **`FileTableModel`**: an `AbstractTableModel` that wraps a `File[]` for the directory listing table.
- **`FileTreeCellRenderer`**: a `DefaultTreeCellRenderer` that shows native file icons and display names in the tree.

---

## Known Limitations

- **Permanent deletion:** Delete removes files and directories permanently, including all directory contents. Items do **not** go to the system Recycle Bin / Trash.
- **Internal clipboard only:** Copy/Paste uses an in-app clipboard. It does not connect to the operating system clipboard.
- **Single selection:** Only one file or directory can be selected at a time.
- **Platform-dependent actions:** Open, Edit and Print depend on `java.awt.Desktop` support. They are disabled on platforms or file types that don't support them.
- **Permission changes:** Read/Write/Execute changes use `java.io.File` permission methods. Their behavior depends on the OS and file system. For example, Windows has limited support for removing read or execute permissions.
- **Headless environments:** FileMan requires a graphical display.

---

## Contributing

Contributions are welcome! To contribute:

1. Fork the repository: <https://github.com/javadev/file-manager>
2. Create a feature branch: `git checkout -b feature/my-feature`
3. Commit your changes: `git commit -am 'Add my feature'`
4. Push the branch: `git push origin feature/my-feature`
5. Open a Pull Request.

Please report bugs and request features through [GitHub Issues](https://github.com/javadev/file-manager/issues).

---

## License

This project is licensed under the **MIT License**. See the [LICENSE](LICENSE) file for details.

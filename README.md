# Task Assignment Manager

A Java Swing desktop application for managing task assignments, built with Java 1.8 and H2 embedded database.

---

## Project Structure

```
src/main/java/com/taskmanager/
├── Task.java              # Domain model (POJO)
├── TaskDAO.java           # H2 database access layer
├── CsvUtil.java           # CSV import / export
├── TaskTableModel.java    # Swing table model + colour renderer
├── TaskDialog.java        # Add / edit task modal dialog
├── MainFrame.java         # Main application window
└── TaskManagerApp.java    # Entry point + system tray setup

src/main/resources/
└── icon.png               # (Optional) tray icon — 16×16 or 32×32 PNG
```

---

## Build

Requires **Java 8+** and **Maven 3.6+**.

```bash
mvn clean package
```

This produces:
```
target/task-manager-1.0.0-all.jar   ← fat JAR with H2 bundled
```

---

## Run

```bash
java -jar target/task-manager-1.0.0-all.jar
```

The H2 database file (`taskmanager.mv.db`) is created in the **same directory as the JAR**.

---

## Features

### Task Fields
| Field       | Required | Notes                          |
|-------------|----------|--------------------------------|
| Assigned To | ✅       | Team member name               |
| Summary     | ✅       | Short description              |
| Due Date    | ✅       | Format: YYYY-MM-DD             |
| Status      |          | NEW / ASSIGNED / COMPLETE      |
| Details     |          | Multi-line free text           |
| Created     |          | Auto-set to today on creation  |

### Colour Coding
| Colour | Meaning              |
|--------|----------------------|
| 🔴 Red    | Overdue (past due date, not complete) |
| 🟠 Orange | Due today                             |
| 🟢 Green  | Active, on track                      |
| ⬜ Grey   | Completed (visible only when toggled) |

### Completed Tasks
- Hidden by default — toggle **"Show / Hide"** in the toolbar to reveal them.

### Editing
- Click a row and press **✏ Edit** in the toolbar, or simply **double-click** any row.
- All fields including full Details text are editable.

### Colour Refresh
- A background timer fires every **60 seconds** to recalculate overdue/due-today colours.  
  Colours update automatically at midnight without restarting.

### CSV Export (File menu)
- **Export All Tasks to CSV…** — exports every task including completed ones.
- **Export Active Tasks to CSV…** — exports only NEW and ASSIGNED tasks.  
  Ideal for handing over open tasks when going on leave.

### CSV Import (File menu)
- **Import Tasks from CSV…** — parses a CSV exported by this app.
- Duplicate detection: rows with the same `assigned_to` + `summary` + `created_date` are skipped.
- Any malformed lines are reported in a post-import summary dialog.

### CSV Format
```
id,assigned_to,created_date,due_date,summary,details,status
1,"Alice","2026-03-01","2026-03-10","Fix login bug","Details here...","ASSIGNED"
```
Fields are double-quote wrapped; embedded quotes are escaped as `""`.

### System Tray
- Closing the window **minimises to tray** — the app keeps running.
- Right-click the tray icon → **Open** or **Exit**.
- Double-click the tray icon to restore the window.
- Place an `icon.png` (16×16 or 32×32) in the same directory as the JAR or in  
  `src/main/resources/` to use a custom tray icon; otherwise a built-in fallback is used.

---

## Running on Linux (tray support)

Most modern Linux desktop environments support `java.awt.SystemTray`. If your environment does not, the app falls back to a standard close-to-exit behaviour and logs a warning.

For GNOME users, install the **KStatusNotifierItem/AppIndicator Support** GNOME extension for best tray icon compatibility.
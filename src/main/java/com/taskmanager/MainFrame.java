package com.taskmanager;

import javax.swing.*;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class MainFrame extends JFrame {

    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private final TaskDAO         dao;
    private final TaskTableModel  model  = new TaskTableModel();
    private final JTable          table  = new JTable(model);
    private final JLabel          lblStatus = new JLabel(" ");
    private final JToggleButton   toggleComplete = new JToggleButton("Show");

    private static final DateTimeFormatter DT_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public MainFrame(TaskDAO dao) {
        super("Task Assignment Manager");
        this.dao = dao;
        buildUI();
        loadTasks();
        startRefreshTimer();
    }

    // ── UI construction ───────────────────────────────────────────────────────

    private void buildUI() {
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setSize(1000, 600);
        setLocationRelativeTo(null);

        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) { minimizeToTray(); }
        });

        setJMenuBar(buildMenuBar());
        add(buildToolbar(), BorderLayout.NORTH);
        add(buildTablePanel(), BorderLayout.CENTER);
        add(buildStatusBar(), BorderLayout.SOUTH);
    }

    // ── Menu bar ──────────────────────────────────────────────────────────────

    private JMenuBar buildMenuBar() {
        JMenuBar mb = new JMenuBar();

        // File menu
        JMenu file = new JMenu("File");

        JMenuItem exportAll    = new JMenuItem("Export All Tasks to CSV…");
        JMenuItem exportActive = new JMenuItem("Export Active Tasks to CSV…");
        JMenuItem importCsv    = new JMenuItem("Import Tasks from CSV…");
        JMenuItem exitItem     = new JMenuItem("Exit");

        exportAll.addActionListener(e    -> exportTasks(false));
        exportActive.addActionListener(e -> exportTasks(true));
        importCsv.addActionListener(e    -> importTasks());
        exitItem.addActionListener(e     -> exitApp());

        file.add(exportAll);
        file.add(exportActive);
        file.addSeparator();
        file.add(importCsv);
        file.addSeparator();
        file.add(exitItem);
        mb.add(file);

        // View menu
        JMenu view = new JMenu("View");
        JMenuItem refresh = new JMenuItem("Refresh");
        refresh.addActionListener(e -> loadTasks());
        view.add(refresh);
        mb.add(view);

        // Help menu
        JMenu help = new JMenu("Help");
        JMenuItem about = new JMenuItem("About");
        about.addActionListener(e -> {
            ImageIcon icon = null;
            try {
                java.io.InputStream is = getClass().getResourceAsStream("/icon.png");
                if (is == null) is = getClass().getResourceAsStream("icon.png");
                if (is != null) {
                    java.awt.image.BufferedImage src = javax.imageio.ImageIO.read(is);
                    is.close();
                    if (src != null) {
                        java.awt.image.BufferedImage composite =
                            new java.awt.image.BufferedImage(128, 128,
                                java.awt.image.BufferedImage.TYPE_INT_ARGB);
                        Graphics2D g2 = composite.createGraphics();
                        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                RenderingHints.VALUE_ANTIALIAS_ON);
                        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                                RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                        g2.drawImage(src, 0, 0, 128, 128, null);
                        g2.dispose();
                        icon = new ImageIcon(composite);
                    }
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            JOptionPane.showMessageDialog(this,
                    "<html><center><b>Task Assignment Manager</b><br/>" +
                    "Java Swing + H2 Database<br/>v1.0 03/06/2026<br/>Sonal Brito</center></html>",
                    "About", JOptionPane.PLAIN_MESSAGE, icon);
        });
        help.add(about);
        mb.add(help);

        return mb;
    }

    // ── Toolbar ───────────────────────────────────────────────────────────────

    private JPanel buildToolbar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 6));
        bar.setBackground(new Color(217, 217, 217));

        JButton btnAdd    = makeButton("＋ Add Task",  new Color(74, 144, 217));
        JButton btnEdit   = makeButton("✏  Edit",      new Color(108, 117, 125));
        JButton btnDelete = makeButton("🗑  Delete",    new Color(192, 57, 43));
        JButton btnRefresh= makeButton("↻ Refresh",    new Color(108, 117, 125));

        btnAdd.addActionListener(e    -> addTask());
        btnEdit.addActionListener(e   -> editSelected());
        btnDelete.addActionListener(e -> deleteSelected());
        btnRefresh.addActionListener(e-> loadTasks());

        bar.add(btnAdd);
        bar.add(btnEdit);
        bar.add(btnDelete);
        bar.add(btnRefresh);
        bar.add(new JSeparator(SwingConstants.VERTICAL));

        bar.add(new JLabel("  Completed Tasks:"));
        toggleComplete.setSelected(false);
        toggleComplete.setText("Hidden");
        toggleComplete.addActionListener(e -> {
            boolean show = toggleComplete.isSelected();
            toggleComplete.setText(show ? "Shown" : "Hidden");
            model.setShowComplete(show);
            updateStatusBar();
        });
        bar.add(toggleComplete);

        bar.add(Box.createHorizontalStrut(20));
        bar.add(new JLabel("Filter:"));
        JTextField tfFilter = new JTextField(14);
        tfFilter.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e)  { filter(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e)  { filter(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { filter(); }
            private void filter() { model.setFilterText(tfFilter.getText()); }
        });
        bar.add(tfFilter);

        return bar;
    }

    private JButton makeButton(String text, Color bg) {
        JButton b = new JButton(text);
        b.setBackground(bg);
        b.setForeground(Color.WHITE);
        b.setFocusPainted(false);
        b.setBorderPainted(false);
        b.setOpaque(true);
        return b;
    }

    // ── Table ─────────────────────────────────────────────────────────────────

    private JScrollPane buildTablePanel() {
        table.setDefaultRenderer(Object.class, new TaskTableModel.ColorRenderer());
        table.setRowHeight(28);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setAutoCreateRowSorter(true);
        table.getTableHeader().setReorderingAllowed(false);

        // Column widths
        int[] widths = { 80, 130, 90, 90, 200, 250 };
        for (int i = 0; i < widths.length; i++) {
            TableColumn col = table.getColumnModel().getColumn(i);
            col.setPreferredWidth(widths[i]);
        }

        // Double-click to edit
        table.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) editSelected();
            }
        });

        return new JScrollPane(table);
    }

    // ── Status bar ────────────────────────────────────────────────────────────

    private JPanel buildStatusBar() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(new Color(212, 208, 200));
        p.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Color.GRAY));
        lblStatus.setBorder(BorderFactory.createEmptyBorder(2, 8, 2, 8));
        lblStatus.setFont(lblStatus.getFont().deriveFont(11f));
        p.add(lblStatus, BorderLayout.WEST);

        JLabel trayLbl = new JLabel("🖥 Running in tray  ");
        trayLbl.setFont(trayLbl.getFont().deriveFont(11f));
        p.add(trayLbl, BorderLayout.EAST);
        return p;
    }

    private void updateStatusBar() {
        long total    = model.getRowCount();
        long overdue  = countVisible(t -> t.isOverdue());
        long today    = countVisible(t -> t.isDueToday());
        lblStatus.setText(String.format(
            "Showing %d tasks  |  %d overdue  |  %d due today  |  Last refreshed: %s",
            total, overdue, today, LocalDateTime.now().format(DT_FMT)));
    }

    private long countVisible(java.util.function.Predicate<Task> pred) {
        long count = 0;
        for (int r = 0; r < model.getRowCount(); r++)
            if (pred.test(model.getTaskAt(r))) count++;
        return count;
    }

    // ── Task operations ───────────────────────────────────────────────────────

    private void addTask() {
        try {
            TaskDialog dlg = new TaskDialog(this, null, dao.findAssignedNames());
            dlg.setVisible(true);
            if (dlg.isSaved()) {
                dao.insert(dlg.getTask());
                loadTasks();
            }
        } catch (SQLException ex) {
            showError("Failed to save task: " + ex.getMessage());
        }
    }

    private void editSelected() {
        int row = table.getSelectedRow();
        if (row < 0) { JOptionPane.showMessageDialog(this, "Please select a task to edit."); return; }
        Task t = model.getTaskAt(table.convertRowIndexToModel(row));
        try {
            TaskDialog dlg = new TaskDialog(this, t, dao.findAssignedNames());
            dlg.setVisible(true);
            if (dlg.isSaved()) {
                dao.update(dlg.getTask());
                loadTasks();
            }
        } catch (SQLException ex) {
            showError("Failed to update task: " + ex.getMessage());
        }
    }

    private void deleteSelected() {
        int row = table.getSelectedRow();
        if (row < 0) { JOptionPane.showMessageDialog(this, "Please select a task to delete."); return; }
        Task t = model.getTaskAt(table.convertRowIndexToModel(row));
        int confirm = JOptionPane.showConfirmDialog(this,
            "Delete task: \"" + t.getSummary() + "\"?", "Confirm Delete",
            JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (confirm == JOptionPane.YES_OPTION) {
            try {
                dao.delete(t.getId());
                loadTasks();
            } catch (SQLException ex) {
                showError("Failed to delete task: " + ex.getMessage());
            }
        }
    }

    private void loadTasks() {
        try {
            List<Task> tasks = dao.findAll();
            model.setTasks(tasks);
            updateStatusBar();
        } catch (SQLException ex) {
            showError("Failed to load tasks: " + ex.getMessage());
        }
    }

    // ── CSV import / export ───────────────────────────────────────────────────

    private void exportTasks(boolean activeOnly) {
        JFileChooser fc = new JFileChooser();
        fc.setSelectedFile(new File(activeOnly ? "active_tasks.csv" : "all_tasks.csv"));
        fc.setDialogTitle(activeOnly ? "Export Active Tasks" : "Export All Tasks");
        if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        try {
            List<Task> tasks = activeOnly ? dao.findActive() : dao.findAll();
            CsvUtil.export(tasks, fc.getSelectedFile());
            JOptionPane.showMessageDialog(this,
                tasks.size() + " task(s) exported successfully.", "Export Complete",
                JOptionPane.INFORMATION_MESSAGE);
        } catch (SQLException | IOException ex) {
            showError("Export failed: " + ex.getMessage());
        }
    }

    private void importTasks() {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Import Tasks from CSV");
        if (fc.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
        try {
            CsvUtil.ImportResult result = CsvUtil.importFrom(fc.getSelectedFile());
            int imported = 0, skipped = 0;
            for (Task t : result.tasks) {
                if (dao.exists(t)) { skipped++; }
                else               { dao.insert(t); imported++; }
            }
            loadTasks();
            StringBuilder msg = new StringBuilder();
            msg.append(imported).append(" task(s) imported, ")
               .append(skipped).append(" duplicate(s) skipped.");
            if (!result.errors.isEmpty()) {
                msg.append("\n\nWarnings:\n");
                result.errors.forEach(e -> msg.append("  • ").append(e).append("\n"));
            }
            JOptionPane.showMessageDialog(this, msg.toString(), "Import Complete",
                JOptionPane.INFORMATION_MESSAGE);
        } catch (SQLException | IOException ex) {
            showError("Import failed: " + ex.getMessage());
        }
    }

    // ── Tray support ──────────────────────────────────────────────────────────

    public void minimizeToTray() { setVisible(false); }

    private void exitApp() {
        dao.close();
        System.exit(0);
    }

    private void showError(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Error", JOptionPane.ERROR_MESSAGE);
    }

    // ── Background refresh timer ──────────────────────────────────────────────

    private void startRefreshTimer() {
        // Re-evaluate colours every 60 seconds in case date rolls over midnight
        Timer t = new Timer(60_000, e -> {
            model.fireTableDataChanged(); // re-triggers colour rendering
            updateStatusBar();
        });
        t.setRepeats(true);
        t.start();
    }
}
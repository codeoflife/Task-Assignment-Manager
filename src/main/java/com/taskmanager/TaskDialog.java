package com.taskmanager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Calendar;
import java.util.List;

/**
 * Modal dialog for adding or editing a Task.
 * Call getTask() after isSaved() == true to retrieve the result.
 */
public class TaskDialog extends JDialog {

    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("MM/dd/yyyy");

    private final JComboBox<String> cbAssignedTo = new JComboBox<>();
    private final JTextField        tfSummary    = new JTextField(30);
    private final JTextField        tfDueDate    = new JTextField(12);
    private final JComboBox<Task.Status> cbStatus =
            new JComboBox<>(Task.Status.values());
    private final JTextArea         taDetails    = new JTextArea(6, 30);
    private final JLabel            lblError     = new JLabel(" ");

    private Task    task;
    private boolean saved = false;

    // ── Constructor ───────────────────────────────────────────────────────────

    public TaskDialog(Frame owner, Task existing, List<String> knownNames) {
        super(owner, existing == null ? "Add New Task" : "Edit Task", true);
        this.task = existing == null ? new Task() : existing;
        knownNames.forEach(cbAssignedTo::addItem);
        cbAssignedTo.setEditable(true);
        cbAssignedTo.setPreferredSize(new Dimension(280, 26));
        buildUI();
        if (existing != null) populate();
        pack();
        setResizable(false);
        setLocationRelativeTo(owner);
    }

    // ── UI construction ───────────────────────────────────────────────────────

    private void buildUI() {
        JPanel root = new JPanel(new BorderLayout(8, 8));
        root.setBorder(new EmptyBorder(12, 16, 12, 16));
        root.add(buildForm(),    BorderLayout.CENTER);
        root.add(buildButtons(), BorderLayout.SOUTH);

        lblError.setForeground(Color.RED);
        lblError.setFont(lblError.getFont().deriveFont(11f));
        root.add(lblError, BorderLayout.NORTH);
        setContentPane(root);
    }

    private JPanel buildForm() {
        JPanel p = new JPanel(new GridBagLayout());
        GridBagConstraints lc = new GridBagConstraints();
        lc.anchor = GridBagConstraints.EAST;
        lc.insets = new Insets(4, 4, 4, 6);

        GridBagConstraints fc = new GridBagConstraints();
        fc.fill    = GridBagConstraints.HORIZONTAL;
        fc.weightx = 1.0;
        fc.insets  = new Insets(4, 0, 4, 4);

        int row = 0;
        addRow(p, lc, fc, row++, "Assigned To *", cbAssignedTo);
        addRow(p, lc, fc, row++, "Summary *",     tfSummary);
        addRow(p, lc, fc, row++, "Due Date *",     buildDateRow());
        addRow(p, lc, fc, row++, "Status",         cbStatus);

        // Details — label anchored to top
        lc.gridy = row; lc.gridx = 0; lc.anchor = GridBagConstraints.NORTHEAST;
        p.add(new JLabel("Details"), lc);
        fc.gridy = row; fc.gridx = 1;
        taDetails.setLineWrap(true);
        taDetails.setWrapStyleWord(true);
        p.add(new JScrollPane(taDetails), fc);

        return p;
    }

    /** Builds the due-date row: text field + calendar picker button. */
    private JPanel buildDateRow() {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));

        tfDueDate.setPreferredSize(new Dimension(100, 26));
        tfDueDate.setToolTipText("MM/dd/yyyy");
        row.add(tfDueDate);
        row.add(Box.createHorizontalStrut(6));

        JButton btnPick = new JButton("📅");
        btnPick.setToolTipText("Pick a date");
        btnPick.setFocusPainted(false);
        btnPick.setPreferredSize(new Dimension(36, 26));
        btnPick.addActionListener(e -> showDatePicker());
        row.add(btnPick);
        row.add(Box.createHorizontalStrut(8));

        JLabel hint = new JLabel("MM/dd/yyyy");
        hint.setForeground(Color.GRAY);
        hint.setFont(hint.getFont().deriveFont(11f));
        row.add(hint);
        return row;
    }

    private void addRow(JPanel p, GridBagConstraints lc, GridBagConstraints fc,
                        int row, String label, Component field) {
        lc.gridy = row; lc.gridx = 0; lc.anchor = GridBagConstraints.EAST;
        p.add(new JLabel(label), lc);
        fc.gridy = row; fc.gridx = 1;
        p.add(field, fc);
    }

    private JPanel buildButtons() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton save   = new JButton("Save");
        JButton cancel = new JButton("Cancel");

        save.setBackground(new Color(74, 144, 217));
        save.setForeground(Color.WHITE);
        save.setFocusPainted(false);
        save.addActionListener(e -> onSave());
        cancel.addActionListener(e -> dispose());

        p.add(save);
        p.add(cancel);
        return p;
    }

    // ── Date picker popup ─────────────────────────────────────────────────────

    private void showDatePicker() {
        // Parse currently typed date so picker opens on the right month
        LocalDate initial = parseField();
        if (initial == null) initial = LocalDate.now();

        JDialog picker = new JDialog(this, "Pick a Date", true);
        picker.setLayout(new BorderLayout(4, 4));
        picker.getRootPane().setBorder(new EmptyBorder(8, 8, 8, 8));

        // Header: < Month Year >
        final Calendar cal = Calendar.getInstance();
        cal.set(initial.getYear(), initial.getMonthValue() - 1, initial.getDayOfMonth());

        JLabel lblMonth = new JLabel("", SwingConstants.CENTER);
        lblMonth.setFont(lblMonth.getFont().deriveFont(Font.BOLD, 13f));

        JPanel calGrid = new JPanel(new GridLayout(0, 7, 2, 2));
        final int[] selectedDay = { initial.getDayOfMonth() };

        // Rebuild the calendar grid whenever month changes
        Runnable buildGrid = () -> {
            calGrid.removeAll();
            String[] days = {"Su","Mo","Tu","We","Th","Fr","Sa"};
            for (String d : days) {
                JLabel lbl = new JLabel(d, SwingConstants.CENTER);
                lbl.setFont(lbl.getFont().deriveFont(Font.BOLD, 11f));
                lbl.setForeground(new Color(80, 80, 80));
                calGrid.add(lbl);
            }
            Calendar tmp = (Calendar) cal.clone();
            tmp.set(Calendar.DAY_OF_MONTH, 1);
            int startDow = tmp.get(Calendar.DAY_OF_WEEK) - 1; // 0=Sun
            int daysInMonth = tmp.getActualMaximum(Calendar.DAY_OF_MONTH);
            int dispYear  = tmp.get(Calendar.YEAR);
            int dispMonth = tmp.get(Calendar.MONTH); // 0-based

            for (int blank = 0; blank < startDow; blank++) calGrid.add(new JLabel(""));

            LocalDate today = LocalDate.now();
            for (int d = 1; d <= daysInMonth; d++) {
                final int day = d;
                JButton btn = new JButton(String.valueOf(d));
                btn.setFocusPainted(false);
                btn.setMargin(new Insets(1, 1, 1, 1));
                btn.setFont(btn.getFont().deriveFont(11f));

                boolean isToday = today.getYear() == dispYear
                        && today.getMonthValue() - 1 == dispMonth
                        && today.getDayOfMonth() == d;
                boolean isSel   = selectedDay[0] == d
                        && cal.get(Calendar.YEAR) == dispYear
                        && cal.get(Calendar.MONTH) == dispMonth;

                if (isSel)        btn.setBackground(new Color(74, 144, 217));
                else if (isToday) btn.setBackground(new Color(255, 224, 178));
                btn.setOpaque(true);

                btn.addActionListener(ev -> {
                    selectedDay[0] = day;
                    LocalDate chosen = LocalDate.of(
                        cal.get(Calendar.YEAR),
                        cal.get(Calendar.MONTH) + 1,
                        day);
                    tfDueDate.setText(chosen.format(DATE_FMT));
                    picker.dispose();
                });
                calGrid.add(btn);
            }
            String[] months = {"January","February","March","April","May","June",
                               "July","August","September","October","November","December"};
            lblMonth.setText(months[tmp.get(Calendar.MONTH)] + " " + tmp.get(Calendar.YEAR));
            calGrid.revalidate();
            calGrid.repaint();
        };

        // Nav buttons
        JButton prev = new JButton("‹");
        JButton next = new JButton("›");
        prev.setFocusPainted(false); next.setFocusPainted(false);
        prev.addActionListener(e -> { cal.add(Calendar.MONTH, -1); buildGrid.run(); });
        next.addActionListener(e -> { cal.add(Calendar.MONTH,  1); buildGrid.run(); });

        JPanel header = new JPanel(new BorderLayout());
        header.add(prev, BorderLayout.WEST);
        header.add(lblMonth, BorderLayout.CENTER);
        header.add(next, BorderLayout.EAST);

        picker.add(header,  BorderLayout.NORTH);
        picker.add(calGrid, BorderLayout.CENTER);

        // "Today" shortcut
        JButton todayBtn = new JButton("Today");
        todayBtn.addActionListener(e -> {
            tfDueDate.setText(LocalDate.now().format(DATE_FMT));
            picker.dispose();
        });
        JPanel south = new JPanel(new FlowLayout(FlowLayout.CENTER));
        south.add(todayBtn);
        picker.add(south, BorderLayout.SOUTH);

        buildGrid.run();
        picker.pack();
        picker.setLocationRelativeTo(this);
        picker.setVisible(true);
    }

    /** Try to parse whatever is in tfDueDate; returns null if blank/invalid. */
    private LocalDate parseField() {
        String s = tfDueDate.getText().trim();
        if (s.isEmpty()) return null;
        try { return LocalDate.parse(s, DATE_FMT); } catch (DateTimeParseException e) { return null; }
    }

    // ── Pre-populate when editing ─────────────────────────────────────────────

    private void populate() {
        cbAssignedTo.setSelectedItem(task.getAssignedTo());
        tfSummary.setText(task.getSummary());
        tfDueDate.setText(task.getDueDate() != null ? task.getDueDate().format(DATE_FMT) : "");
        cbStatus.setSelectedItem(task.getStatus());
        taDetails.setText(task.getDetails() == null ? "" : task.getDetails());
    }

    // ── Validation & save ─────────────────────────────────────────────────────

    private void onSave() {
        String assignedTo = cbAssignedTo.getEditor().getItem().toString().trim();
        String summary    = tfSummary.getText().trim();
        String dueDateStr = tfDueDate.getText().trim();
        String details    = taDetails.getText().trim();

        if (assignedTo.isEmpty()) { lblError.setText("⚠  'Assigned To' is required.");  return; }
        if (summary.isEmpty())    { lblError.setText("⚠  'Summary' is required.");       return; }
        if (dueDateStr.isEmpty()) { lblError.setText("⚠  'Due Date' is required.");      return; }

        LocalDate dueDate;
        try {
            dueDate = LocalDate.parse(dueDateStr, DATE_FMT);
        } catch (DateTimeParseException ex) {
            lblError.setText("⚠  Due Date must be MM/dd/yyyy  (e.g. 03/15/2026)");
            return;
        }

        task.setAssignedTo(assignedTo);
        task.setSummary(summary);
        task.setDueDate(dueDate);
        task.setStatus((Task.Status) cbStatus.getSelectedItem());
        task.setDetails(details.isEmpty() ? null : details);

        saved = true;
        dispose();
    }

    // ── Public API ────────────────────────────────────────────────────────────

    public boolean isSaved() { return saved; }
    public Task    getTask() { return task; }
}
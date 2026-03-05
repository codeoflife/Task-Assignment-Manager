package com.taskmanager;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class TaskTableModel extends AbstractTableModel {

    /**
	 * 
	 */
	private static final long serialVersionUID = -9096044441188700213L;

	private static final String[] COLS =
        { "Status", "Assigned To", "Created", "Due Date", "Summary", "Details" };

    private static final Color RED    = new Color(255, 204, 204);
    private static final Color ORANGE = new Color(255, 224, 178);
    private static final Color GREEN  = new Color(212, 237, 218);
    private static final Color GREY   = new Color(220, 220, 220);

    private List<Task> all      = new ArrayList<>(); // full list from DB
    private List<Task> visible  = new ArrayList<>(); // filtered view
    private boolean showComplete = false;
    private String  filterText   = "";

    // ── Data loading ──────────────────────────────────────────────────────────

    public void setTasks(List<Task> tasks) {
        this.all = new ArrayList<>(tasks);
        applyFilter();
    }

    public void setShowComplete(boolean show) {
        this.showComplete = show;
        applyFilter();
    }

    public void setFilterText(String text) {
        this.filterText = text == null ? "" : text.trim().toLowerCase();
        applyFilter();
    }

    private void applyFilter() {
        visible = all.stream()
            .filter(t -> showComplete || t.getStatus() != Task.Status.COMPLETE)
            .filter(t -> {
                if (filterText.isEmpty()) return true;
                return t.getAssignedTo().toLowerCase().contains(filterText)
                    || t.getSummary().toLowerCase().contains(filterText)
                    || (t.getDetails() != null && t.getDetails().toLowerCase().contains(filterText));
            })
            .collect(Collectors.toList());
        fireTableDataChanged();
    }

    // ── TableModel ────────────────────────────────────────────────────────────

    @Override public int getRowCount()    { return visible.size(); }
    @Override public int getColumnCount() { return COLS.length; }
    @Override public String getColumnName(int col) { return COLS[col]; }

    @Override
    public Object getValueAt(int row, int col) {
        Task t = visible.get(row);
        switch (col) {
            case 0: return t.getStatus().name();
            case 1: return t.getAssignedTo();
            case 2: return t.getCreatedDate().toString();
            case 3: return t.getDueDate().toString();
            case 4: return t.getSummary();
            case 5: return t.getDetails() == null ? "" : t.getDetails();
            default: return "";
        }
    }

    /** Retrieve the Task object for a given view row. */
    public Task getTaskAt(int row) {
        return visible.get(row);
    }

    // ── Cell renderer ─────────────────────────────────────────────────────────

    public static class ColorRenderer extends DefaultTableCellRenderer {
        /**
		 * 
		 */
		private static final long serialVersionUID = -3115871085241473600L;

		@Override
        public Component getTableCellRendererComponent(
                JTable table, Object value, boolean isSelected,
                boolean hasFocus, int row, int col) {

            Component c = super.getTableCellRendererComponent(
                    table, value, isSelected, hasFocus, row, col);

            if (!isSelected) {
                TaskTableModel mdl = (TaskTableModel) table.getModel();
                Task t = mdl.getTaskAt(row);
                if (t.getStatus() == Task.Status.COMPLETE) {
                    c.setBackground(GREY);
                } else if (t.isOverdue()) {
                    c.setBackground(RED);
                } else if (t.isDueToday()) {
                    c.setBackground(ORANGE);
                } else {
                    c.setBackground(GREEN);
                }
                c.setForeground(Color.BLACK);
            } else {
                c.setBackground(table.getSelectionBackground());
                c.setForeground(table.getSelectionForeground());
            }

            // Truncate details column with tooltip
            if (col == 5) {
                String full = value == null ? "" : value.toString();
                setText(full.length() > 60 ? full.substring(0, 57) + "..." : full);
                setToolTipText(full.isEmpty() ? null : "<html><p style='width:300px'>" +
                        full.replace("\n", "<br>") + "</p></html>");
            }
            return c;
        }
    }
}
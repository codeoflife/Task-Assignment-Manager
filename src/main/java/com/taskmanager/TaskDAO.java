package com.taskmanager;

import java.io.File;
import java.sql.Connection;
import java.sql.Date;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class TaskDAO {

    private static final String DB_NAME = "taskmanager";
    private Connection conn;

    public TaskDAO() throws SQLException {
        String dbPath = resolveDbPath();
        // FILE_LOCK=FS avoids stale lock errors on unclean shutdown
        // IGNORE_UNKNOWN_SETTINGS=TRUE for forward compatibility
        String url = "jdbc:h2:file:" + dbPath
                + ";FILE_LOCK=FS"
                + ";IGNORE_UNKNOWN_SETTINGS=TRUE"
                + ";AUTO_SERVER=FALSE";
        conn = DriverManager.getConnection(url, "sa", "");
        initSchema();
    }

    private String resolveDbPath() {
        // Always use the current working directory (launch folder).
        // In Eclipse this is the project root; for a deployed JAR it is
        // wherever the user runs the JAR from — consistent in both cases.
        File cwd = new File(System.getProperty("user.dir"));
        return new File(cwd, DB_NAME).getAbsolutePath();
    }

    private void initSchema() throws SQLException {
        try (Statement st = conn.createStatement()) {
            st.execute(
                "CREATE TABLE IF NOT EXISTS tasks (" +
                "  id           INT AUTO_INCREMENT PRIMARY KEY," +
                "  assigned_to  VARCHAR(200) NOT NULL," +
                "  created_date DATE         NOT NULL," +
                "  due_date     DATE         NOT NULL," +
                "  summary      VARCHAR(500) NOT NULL," +
                "  details      CLOB," +
                "  status       VARCHAR(20)  NOT NULL DEFAULT 'NEW'" +
                ")"
            );
        }
    }

    // ── CRUD ─────────────────────────────────────────────────────────────────

    public List<Task> findAll() throws SQLException {
        return query("SELECT * FROM tasks ORDER BY due_date ASC");
    }

    public List<Task> findActive() throws SQLException {
        return query("SELECT * FROM tasks WHERE status <> 'COMPLETE' ORDER BY due_date ASC");
    }

    public void insert(Task t) throws SQLException {
        String sql = "INSERT INTO tasks (assigned_to, created_date, due_date, summary, details, status) " +
                     "VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, t.getAssignedTo());
            ps.setDate(2, Date.valueOf(t.getCreatedDate()));
            ps.setDate(3, Date.valueOf(t.getDueDate()));
            ps.setString(4, t.getSummary());
            ps.setString(5, t.getDetails());
            ps.setString(6, t.getStatus().name());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) t.setId(rs.getInt(1));
            }
        }
    }

    public void update(Task t) throws SQLException {
        String sql = "UPDATE tasks SET assigned_to=?, created_date=?, due_date=?, " +
                     "summary=?, details=?, status=? WHERE id=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, t.getAssignedTo());
            ps.setDate(2, Date.valueOf(t.getCreatedDate()));
            ps.setDate(3, Date.valueOf(t.getDueDate()));
            ps.setString(4, t.getSummary());
            ps.setString(5, t.getDetails());
            ps.setString(6, t.getStatus().name());
            ps.setInt(7, t.getId());
            ps.executeUpdate();
        }
    }

    public void delete(int id) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("DELETE FROM tasks WHERE id=?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    /**
     * Duplicate check: same assigned_to + summary + created_date.
     */
    public boolean exists(Task t) throws SQLException {
        String sql = "SELECT COUNT(*) FROM tasks WHERE assigned_to=? AND summary=? AND created_date=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, t.getAssignedTo());
            ps.setString(2, t.getSummary());
            ps.setDate(3, Date.valueOf(t.getCreatedDate()));
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    /** Returns all distinct assigned-to names, alphabetically sorted. */
    public List<String> findAssignedNames() throws SQLException {
        List<String> names = new ArrayList<>();
        String sql = "SELECT DISTINCT assigned_to FROM tasks ORDER BY assigned_to ASC";
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) names.add(rs.getString(1));
        }
        return names;
    }

    public void close() {
        try { if (conn != null && !conn.isClosed()) conn.close(); } catch (SQLException ignored) {}
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private List<Task> query(String sql) throws SQLException {
        List<Task> list = new ArrayList<>();
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(map(rs));
        }
        return list;
    }

    private Task map(ResultSet rs) throws SQLException {
        return new Task(
            rs.getInt("id"),
            rs.getString("assigned_to"),
            rs.getDate("created_date").toLocalDate(),
            rs.getDate("due_date").toLocalDate(),
            rs.getString("summary"),
            rs.getString("details"),
            Task.Status.valueOf(rs.getString("status"))
        );
    }
}
package com.taskmanager;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.sql.SQLException;

/**
 * Application entry point.
 * Sets up the system tray and launches MainFrame.
 */
public class TaskManagerApp {

    public static void main(String[] args) {
        // Use system look-and-feel
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); }
        catch (Exception ignored) {}

        SwingUtilities.invokeLater(() -> {
            TaskDAO dao;
            try {
                dao = new TaskDAO();
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(null,
                    "Cannot open database:\n" + ex.getMessage(),
                    "Startup Error", JOptionPane.ERROR_MESSAGE);
                System.exit(1);
                return;
            }

            MainFrame frame = new MainFrame(dao);

            // Apply icon to the JFrame window / taskbar
            Image appIcon = loadTrayIcon(new Dimension(256, 256));
            frame.setIconImage(appIcon);

            if (SystemTray.isSupported()) {
                setupTray(frame, dao);
            } else {
                frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            }

            frame.setVisible(true);
        });
    }

    // ── System tray setup ─────────────────────────────────────────────────────

    private static void setupTray(MainFrame frame, TaskDAO dao) {
        SystemTray tray = SystemTray.getSystemTray();
        Image icon = loadTrayIcon(tray.getTrayIconSize());

        PopupMenu popup = new PopupMenu();

        MenuItem openItem = new MenuItem("Open Task Manager");
        openItem.addActionListener(e -> showFrame(frame));

        MenuItem exitItem = new MenuItem("Exit");
        exitItem.addActionListener(e -> {
            dao.close();
            System.exit(0);
        });

        popup.add(openItem);
        popup.addSeparator();
        popup.add(exitItem);

        TrayIcon trayIcon = new TrayIcon(icon, "Task Manager", popup);
        trayIcon.setImageAutoSize(true);
        trayIcon.addActionListener(e -> showFrame(frame)); // double-click

        try {
            tray.add(trayIcon);
        } catch (AWTException ex) {
            System.err.println("Could not add tray icon: " + ex.getMessage());
        }
    }

    private static void showFrame(MainFrame frame) {
        SwingUtilities.invokeLater(() -> {
            frame.setVisible(true);
            frame.setExtendedState(Frame.NORMAL);
            frame.toFront();
            frame.requestFocus();
        });
    }

    // ── Tray icon: PNG from classpath, fall back to drawn icon ────────────────

    private static Image loadTrayIcon(Dimension size) {
        int w = size.width  > 0 ? size.width  : 16;
        int h = size.height > 0 ? size.height : 16;
        try {
            java.io.InputStream is = TaskManagerApp.class.getResourceAsStream("/icon.png");
            if (is == null) {
                // Some JARs need the path without leading slash
                is = TaskManagerApp.class.getResourceAsStream("icon.png");
            }
            if (is != null) {
                java.awt.image.BufferedImage src = javax.imageio.ImageIO.read(is);
                is.close();
                if (src != null) {
                    java.awt.image.BufferedImage out =
                        new java.awt.image.BufferedImage(w, h,
                            java.awt.image.BufferedImage.TYPE_INT_ARGB);
                    Graphics2D g2 = out.createGraphics();
                    g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                        RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                    g2.drawImage(src, 0, 0, w, h, null);
                    g2.dispose();
                    return out;
                }
            }
        } catch (Exception ignored) {}
        return buildFallbackIcon(w, h);
    }

    /** Draws a simple clipboard icon programmatically as a fallback. */
    private static Image buildFallbackIcon(int w, int h) {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Background
        g.setColor(new Color(74, 144, 217));
        g.fillRoundRect(0, 0, w, h, 4, 4);

        // Simple "T" letter
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, (int)(h * 0.7)));
        FontMetrics fm = g.getFontMetrics();
        String txt = "T";
        int tx = (w - fm.stringWidth(txt)) / 2;
        int ty = (h - fm.getHeight()) / 2 + fm.getAscent();
        g.drawString(txt, tx, ty);

        g.dispose();
        return img;
    }
}
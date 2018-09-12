package org.dreipic.gui.exp;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

final class LogPanel {
    private static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss,SSS");

    private final JTextArea txtLog;
    private final JComponent component;
    private String logText;

    LogPanel(int rows) {
        logText = "";
        txtLog = new JTextArea(rows, 0);
        txtLog.setEditable(false);
        component = new JScrollPane(txtLog);
    }

    JComponent getComponent() {
        return component;
    }

    void clear() {
        SwingUtilities.invokeLater(() -> {
            logText = "";
            txtLog.setText("");
        });
    }

    void log(String pattern, Object... args) {
        SwingUtilities.invokeLater(() -> {
            String timeStr = DATE_FORMAT.format(new Date());
            String msg = String.format(pattern, args);
            logText += timeStr + " " + msg + "\n";
            txtLog.setText(logText);
        });
    }
}

package org.dreipic.gui.exp;

import javax.swing.SwingUtilities;

public final class DreipicExplorer {
    private DreipicExplorer() {
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            ConnectDetails connect = ConnectWindow.show();
            if (connect != null) {
                FilesWindow.show(connect.credentials, connect.dataKey, connect.metas);
            }
        });
    }
}

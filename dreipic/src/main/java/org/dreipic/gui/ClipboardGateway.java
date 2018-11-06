package org.dreipic.gui;

import javax.swing.Timer;

final class ClipboardGateway {
    private String clipboardStr;
    private final Timer clipboardTimer;

    ClipboardGateway() {
        clipboardTimer = new Timer(30_000, __ -> clearClipboardIfNeeded());
        clipboardTimer.stop();
        clipboardTimer.setRepeats(false);
    }

    void copyToClipboard(String str) {
        SwingUtils.copyToClipboard(str);
        clipboardStr = str;
        clipboardTimer.restart();
    }

    void clearClipboard() {
        SwingUtils.copyToClipboard("");
        clipboardStr = null;
    }

    private void clearClipboardIfNeeded() {
        if (clipboardStr != null) {
            String str = SwingUtils.copyFromClipboard();
            if (clipboardStr.equals(str)) {
                SwingUtils.copyToClipboard("");
                clipboardStr = null;
            }
        }
    }
}

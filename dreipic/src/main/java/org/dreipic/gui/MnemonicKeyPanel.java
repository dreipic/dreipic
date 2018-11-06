package org.dreipic.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridLayout;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.UIManager;

import org.dreipic.util.DigestUtils;
import org.dreipic.util.MnemonicEncoder;

import com.google.common.base.Charsets;
import com.google.common.primitives.Bytes;

public final class MnemonicKeyPanel {
    private final RawKeyPanel rawKeyPanel;

    private final JTextField statusField;
    private final KeyHashField hashField;
    private final JPanel panel;

    private byte[] lastKey;
    private byte[] lastValidationHash;

    private final Runnable listener;

    public MnemonicKeyPanel(Runnable listener) {
        this.listener = listener;

        rawKeyPanel = new RawKeyPanel(this::updateStatus);

        statusField = new JTextField();
        statusField.setFocusable(false);
        statusField.setEditable(false);

        hashField = new KeyHashField();

        JComponent fieldPanel = new JPanel(new GridLayout(1, 2));
        fieldPanel.add(statusField);
        fieldPanel.add(hashField.component());

        panel = new JPanel(new BorderLayout());
        panel.add(rawKeyPanel.getComponent(), BorderLayout.CENTER);
        panel.add(fieldPanel, BorderLayout.SOUTH);

        updateStatus0();
    }

    private void updateStatus() {
        updateStatus0();
        listener.run();
    }

    private void updateStatus0() {
        lastKey = null;
        lastValidationHash = null;

        final Color textFieldFore = UIManager.getColor("TextField.foreground");
        final Color textFieldInactiveBack = UIManager.getColor("TextField.inactiveBackground");

        String sts = "Keep typing";
        Color fore = textFieldFore;
        Color back = textFieldInactiveBack;

        int[] rawKey = rawKeyPanel.getKey();
        if (rawKey != null) {
            byte[] key = MnemonicEncoder.decode32BytesEx(rawKey);
            if (key == null) {
                sts = "Checksum missmatch";
                fore = Color.BLACK;
                back = Color.YELLOW;
            } else {
                sts = "OK";
                fore = Color.WHITE;
                back = Color.GREEN.darker();
                lastKey = key;
                lastValidationHash = validationDigest(key);
            }
        }

        statusField.setText(sts);
        hashField.setValue(lastValidationHash);
        statusField.setForeground(fore);
        statusField.setBackground(back);
    }

    public JComponent getComponent() {
        return panel;
    }

    public byte[] getKey() {
        return lastKey == null ? null : lastKey.clone();
    }

    public byte[] getValidationHash() {
        return lastValidationHash == null ? null : lastValidationHash.clone();
    }

    public byte[] getRawKey() {
        List<String> words = getWords();
        String[] array = words.toArray(new String[0]);
        byte[] key = MnemonicEncoder.decode32Bytes(array);
        return key;
    }

    public void setWords(List<String> words) {
        rawKeyPanel.setWords(words);
    }

    public List<String> getWords() {
        return rawKeyPanel.getWords();
    }

    public void setShowText(boolean b) {
        rawKeyPanel.setShowText(b);
    }

    private static byte[] validationDigest(byte[] key) {
        byte[] bs = Bytes.concat("validation-333:".getBytes(Charsets.US_ASCII), key);
        for (int i = 0; i < 333; ++i) {
            bs = DigestUtils.sha256(bs);
        }
        return bs;
    }
}

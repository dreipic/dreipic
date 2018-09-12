package org.dreipic.gui;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.xml.bind.DatatypeConverter;

import org.dreipic.util.MnemonicEncoder;
import org.dreipic.util.PasswordEncoder;

import com.google.common.base.Charsets;
import com.google.common.primitives.Bytes;

public final class DreipicKeyTool {
    private final JComponent mainPanel;

    private final MnemonicKeyPanel keyPanel;
    private final JButton copyRawKeyHexButton;
    private final JButton copyKeyHexButton;
    private final JButton copyPasswordButton;
    private final JTextField extraField;
    private final JTextField portField;

    private String clipboardStr;
    private final Timer clipboardTimer;

    private DreipicKeyTool() {
        keyPanel = new MnemonicKeyPanel(this::updateStatus);

        extraField = new JTextField();

        portField = new JTextField(6);
        portField.setEditable(false);

        JPanel extraPanel = createExtraPanel();

        JPanel editPanel = new JPanel(new BorderLayout());
        editPanel.add(keyPanel.getComponent(), BorderLayout.CENTER);
        editPanel.add(extraPanel, BorderLayout.SOUTH);

        SwingUtils.onTextChanged(extraField, this::onExtraFieldChange);

        JToggleButton showTextButton = SwingUtils.newToggleButton("Show hidden text", false, this::onShowTextClick);
        JButton copyWordsButton = SwingUtils.newButton("Copy words", true, false, this::onCopyWordsClick);
        copyRawKeyHexButton = SwingUtils.newButton("Copy raw key hex", true, false, this::onCopyRawKeyHexClick);
        copyKeyHexButton = SwingUtils.newButton("Copy key hex", true, false, this::onCopyKeyHexClick);
        copyPasswordButton = SwingUtils.newButton("Copy password", true, false, this::onCopyPasswordClick);
        JButton clearClipboardButton = SwingUtils.newButton("Clear clipboard", true, false, this::onClearClipboardClick);
        JButton setRandomButton = SwingUtils.newButton("Set random", true, false, this::onSetRandomClick);

        JPanel buttonsPanel = new JPanel(new GridLayout(7, 1));
        buttonsPanel.add(showTextButton);
        buttonsPanel.add(copyWordsButton);
        buttonsPanel.add(copyRawKeyHexButton);
        buttonsPanel.add(copyKeyHexButton);
        buttonsPanel.add(copyPasswordButton);
        buttonsPanel.add(clearClipboardButton);
        buttonsPanel.add(setRandomButton);

        updateStatus();

        clipboardTimer = new Timer(30_000, __ -> clearClipboard());
        clipboardTimer.stop();
        clipboardTimer.setRepeats(false);

        mainPanel = new JPanel(new BorderLayout());
        mainPanel.add(editPanel, BorderLayout.CENTER);
        mainPanel.add(buttonsPanel, BorderLayout.EAST);
    }

    private JPanel createExtraPanel() {
        JPanel extraPanel = new JPanel(new GridBagLayout());

        GridBagConstraints c;

        c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 1;
        c.fill = GridBagConstraints.BOTH;
        extraPanel.add(extraField, c);

        c = new GridBagConstraints();
        c.gridx = 1;
        c.gridy = 0;
        c.fill = GridBagConstraints.HORIZONTAL;
        extraPanel.add(new JLabel("Port:"), c);

        c = new GridBagConstraints();
        c.gridx = 2;
        c.gridy = 0;
        c.fill = GridBagConstraints.HORIZONTAL;
        extraPanel.add(portField, c);
        return extraPanel;
    }

    private void onExtraFieldChange() {
        updateStatus();

        String portStr = "";

        byte[] fullBytes = getFullBytes();
        byte[] extraBytes = getExtraBytes();
        if (fullBytes != null && extraBytes != null && extraBytes.length > 0) {
            int port = PasswordEncoder.encodePort(fullBytes);
            portStr = "" + port;
        }

        portField.setText(portStr);
    }

    private void onShowTextClick(boolean b) {
        keyPanel.setShowText(b);
    }

    private void onCopyWordsClick() {
        StringBuilder buf = new StringBuilder();
        String sep = "";

        List<String> words = keyPanel.getWords();

        for (int i = 0; i < words.size(); ++i) {
            String s = words.get(i);
            if (!s.isEmpty()) {
                if (i % 4 == 0 && buf.length() > 0) {
                    sep = "";
                    buf.append("\n");
                }

                buf.append(sep);
                buf.append(s);
                sep = " ";
            }
        }

        String str = buf.toString();
        copyToClipboard(str);
    }

    private byte[] getExtraBytes() {
        String s = extraField.getText();
        return s.getBytes(Charsets.US_ASCII);
    }

    private void onCopyRawKeyHexClick() {
        byte[] bytes = keyPanel.getRawKey();
        if (bytes != null) {
            String s = DatatypeConverter.printHexBinary(bytes).toLowerCase();
            copyToClipboard(s);
        }
    }

    private void onCopyKeyHexClick() {
        byte[] bytes = keyPanel.getKey();
        if (bytes != null) {
            String s = DatatypeConverter.printHexBinary(bytes).toLowerCase();
            copyToClipboard(s);
        }
    }

    private void onCopyPasswordClick() {
        byte[] fullBytes = getFullBytes();
        if (fullBytes != null) {
            char[] cs = PasswordEncoder.encodePassword(fullBytes);
            String s = new String(cs);
            copyToClipboard(s);
        }
    }

    private byte[] getFullBytes() {
        byte[] bytes = keyPanel.getKey();
        byte[] extraBytes = getExtraBytes();
        if (bytes != null && extraBytes != null) {
            byte[] fullBytes = Bytes.concat(bytes, extraBytes);
            return fullBytes;
        } else {
            return null;
        }
    }

    private void onClearClipboardClick() {
        SwingUtils.copyToClipboard("");
        clipboardStr = null;
    }

    private void onSetRandomClick() {
        SecureRandom rnd = new SecureRandom();
        byte[] bytes = new byte[32];
        rnd.nextBytes(bytes);
        String[] words = MnemonicEncoder.encode32Bytes(bytes);
        keyPanel.setWords(Arrays.asList(words));
    }

    private void updateStatus() {
        boolean ok = keyPanel.getKey() != null;
        copyKeyHexButton.setEnabled(ok);
        copyPasswordButton.setEnabled(ok);
    }

    private void copyToClipboard(String str) {
        SwingUtils.copyToClipboard(str);
        clipboardStr = str;
        clipboardTimer.restart();
    }

    private void clearClipboard() {
        if (clipboardStr != null) {
            String str = SwingUtils.copyFromClipboard();
            if (clipboardStr.equals(str)) {
                SwingUtils.copyToClipboard("");
                clipboardStr = null;
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            DreipicKeyTool tool = new DreipicKeyTool();
            JFrame frame = SwingUtils.createFrame("KeyTool", true, tool.mainPanel);
            SwingUtils.onWindowClosed(frame, tool::clearClipboard);
            frame.setVisible(true);
        });
    }
}

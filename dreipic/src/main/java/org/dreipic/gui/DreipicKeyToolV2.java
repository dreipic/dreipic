package org.dreipic.gui;

import java.awt.BorderLayout;
import java.awt.Color;
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
import javax.swing.UIManager;
import javax.xml.bind.DatatypeConverter;

import org.dreipic.util.ByteUtils;
import org.dreipic.util.DigestUtils;
import org.dreipic.util.MnemonicEncoder;

import com.google.common.base.Charsets;

public class DreipicKeyToolV2 {
    private final JComponent mainPanel;
    private final RawKeyPanel keyPanel;
    private final JTextField txtV1Checksum;
    private final JTextField txtV2Checksum;
    private final JTextField txtExtra;

    private final JButton copyWordsButton;
    private final JButton copyRawKeyHexButton;
    private final JButton copyFullKeyHexButton;

    private final ClipboardGateway clipboard;

    private DreipicKeyToolV2() {
        keyPanel = new RawKeyPanel(this::updateStatus);

        txtV1Checksum = newTextField(false);
        txtV2Checksum = newTextField(false);
        txtExtra = newTextField(true);

        JPanel fieldsPanel = new JPanel(new GridLayout(3, 2));
        fieldsPanel.add(new JLabel("V1 Checksum"));
        fieldsPanel.add(txtV1Checksum);
        fieldsPanel.add(new JLabel("V2 Checksum"));
        fieldsPanel.add(txtV2Checksum);
        fieldsPanel.add(new JLabel("Extra"));
        fieldsPanel.add(txtExtra);

        JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.add(keyPanel.getComponent(), BorderLayout.CENTER);
        leftPanel.add(fieldsPanel, BorderLayout.SOUTH);

        JToggleButton showTextButton = SwingUtils.newToggleButton("Show hidden text", false, this::onShowTextClick);
        copyWordsButton = SwingUtils.newButton("Copy words", true, false, this::onCopyWordsClick);
        copyRawKeyHexButton = SwingUtils.newButton("Copy raw key hex", true, false, this::onCopyRawKeyHexClick);
        copyFullKeyHexButton = SwingUtils.newButton("Copy full key hex", true, false, this::onCopyFullKeyHexClick);
        JButton clearClipboardButton = SwingUtils.newButton("Clear clipboard", true, false, this::onClearClipboardClick);
        JButton setRandomButton = SwingUtils.newButton("Set random", true, false, this::onSetRandomClick);

        JComponent[] buttons = {
                showTextButton,
                copyWordsButton,
                copyRawKeyHexButton,
                copyFullKeyHexButton,
                clearClipboardButton,
                setRandomButton,
        };

        JPanel buttonPanel = new JPanel(new GridLayout(buttons.length, 1));
        for (JComponent c : buttons) {
            buttonPanel.add(c);
        }

        clipboard = new ClipboardGateway();

        mainPanel = new JPanel(new BorderLayout());
        mainPanel.add(leftPanel, BorderLayout.CENTER);
        mainPanel.add(buttonPanel, BorderLayout.EAST);

        updateStatus();
    }

    private static JTextField newTextField(boolean editable) {
        JTextField f = new JTextField();
        f.setEditable(editable);
        return f;
    }

    private void updateStatus() {
        String v1Checksum = "";
        Color v1ColorF = UIManager.getColor("TextField.foreground");
        Color v1ColorB = UIManager.getColor("TextField.inactiveBackground");

        String v2Checksum = "";
        Color v2ColorF = UIManager.getColor("TextField.foreground");
        Color v2ColorB = UIManager.getColor("TextField.inactiveBackground");

        int[] keyWords = keyPanel.getKey();
        if (keyWords != null) {
            byte[] v1Key = MnemonicEncoder.decode32Bytes(keyWords);
            if (v1Key != null) {
                v1Checksum = "OK";
                v1ColorF = Color.WHITE;
                v1ColorB = Color.GREEN.darker();
            } else {
                v1Checksum = "MISSMATCH";
                v1ColorF = Color.BLACK;
                v1ColorB = Color.YELLOW;
            }

            byte[] v2Key = MnemonicEncoder.encodeRaw(keyWords);
            byte[] v2Chksum = MnemonicEncoder.v2Checksum(v2Key);
            v2Checksum = DatatypeConverter.printHexBinary(Arrays.copyOf(v2Chksum, 4));
        }

        txtV1Checksum.setText(v1Checksum);
        txtV1Checksum.setForeground(v1ColorF);
        txtV1Checksum.setBackground(v1ColorB);

        txtV2Checksum.setText(v2Checksum);
        txtV2Checksum.setForeground(v2ColorF);
        txtV2Checksum.setBackground(v2ColorB);
    }

    private void onShowTextClick(boolean b) {
        keyPanel.setShowText(b);
    }

    private void onCopyWordsClick() {
        List<String> words = keyPanel.getWords();
        String str = DreipicKeyTool.wordsToString(words);
        clipboard.copyToClipboard(str);
    }

    private void onCopyRawKeyHexClick() {
        int[] words = keyPanel.getKey();
        if (words != null) {
            byte[] bytes = MnemonicEncoder.encodeRaw(words);
            String s = DatatypeConverter.printHexBinary(bytes).toLowerCase();
            clipboard.copyToClipboard(s);
        }
    }

    private void onCopyFullKeyHexClick() {
        int[] words = keyPanel.getKey();
        if (words != null) {
            byte[] bytes = MnemonicEncoder.encodeRaw(words);
            byte[] extraBytes = txtExtra.getText().getBytes(Charsets.US_ASCII);
            byte[] fullBytes = ByteUtils.concat(bytes, extraBytes);
            byte[] key = DigestUtils.sha256(fullBytes);
            String s = DatatypeConverter.printHexBinary(key).toLowerCase();
            clipboard.copyToClipboard(s);
        }
    }

    private void onClearClipboardClick() {
        clipboard.clearClipboard();
    }

    private void onSetRandomClick() {
        SecureRandom rnd = new SecureRandom();
        int[] nums = new int[24];
        for (int i = 0; i < nums.length; ++i) nums[i] = rnd.nextInt(2048);
        String[] words = MnemonicEncoder.intsToWords(nums);
        keyPanel.setWords(Arrays.asList(words));
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            DreipicKeyToolV2 tool = new DreipicKeyToolV2();
            JFrame frame = SwingUtils.createFrame("KeyTool V2", true, tool.mainPanel);
            SwingUtils.onWindowClosed(frame, tool.clipboard::clearClipboard);
            frame.setVisible(true);
        });
    }
}

package org.dreipic.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridLayout;
import java.util.Arrays;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JPopupMenu;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.xml.bind.DatatypeConverter;

import org.dreipic.util.DigestUtils;
import org.dreipic.util.MnemonicEncoder;

import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.primitives.Bytes;

public final class MnemonicKeyPanel {
    private final JPasswordField[] passwordFields;
    private final JTextField statusField;
    private final KeyHashField hashField;
    private final JPanel panel;

    private boolean showText;

    private byte[] lastKey;
    private byte[] lastValidationHash;

    private final Runnable listener;

    public MnemonicKeyPanel(Runnable listener) {
        this.listener = listener;

        passwordFields = new JPasswordField[24];
        JPanel keyPanel = new JPanel(new GridLayout(6, 4));

        JPopupMenu popupMenu = new JPopupMenu();
        SwingUtils.addMenuItem(popupMenu, "Paste", this::onPaste);

        for (int i = 0; i < passwordFields.length; ++i) {
            JPasswordField f = new JPasswordField(12);
            f.setHorizontalAlignment(JTextField.CENTER);
            passwordFields[i] = f;
            keyPanel.add(f);
            f.setComponentPopupMenu(popupMenu);

            SwingUtils.onTextChanged(f, this::updateStatus);
            SwingUtils.onFocusGained(f, __ -> f.selectAll());
            SwingUtils.onFocusLost(f, __ -> updateStatus());
            SwingUtils.onMouseEnter(f, __ -> onMouseEnter(f));
            SwingUtils.onMouseExit(f, __ -> onMouseExit(f));
        }

        statusField = new JTextField();
        statusField.setFocusable(false);
        statusField.setEditable(false);

        hashField = new KeyHashField();

        JComponent fieldPanel = new JPanel(new GridLayout(1, 2));
        fieldPanel.add(statusField);
        fieldPanel.add(hashField.component());

        panel = new JPanel(new BorderLayout());
        panel.add(keyPanel, BorderLayout.CENTER);
        panel.add(fieldPanel, BorderLayout.SOUTH);

        updateStatus0();
    }

    private void onPaste() {
        String s = SwingUtils.copyFromClipboard();
        String[] words = parseKey(s);
        if (words != null) {
            for (int i = 0; i < words.length; ++i) {
                passwordFields[i].setText(words[i]);
            }
        }
    }

    private void onMouseEnter(JPasswordField f) {
        if (showText) {
            SwingUtils.setShowCharacters(f, true);
        }
    }

    private void onMouseExit(JPasswordField f) {
        if (showText) {
            SwingUtils.setShowCharacters(f, false);
        }
    }

    private static String[] parseKey(String str) {
        str = str.trim();
        String[] words = null;
        if (str.matches("[0-9A-Fa-f]{64}")) {
            byte[] key = DatatypeConverter.parseHexBinary(str);
            words = MnemonicEncoder.encode32Bytes(key);
        } else if (str.matches("[a-z]+(\\s+[a-z]+){23}")) {
            String[] parts = str.split("\\s+");
            if (parts.length == 24) {
                for (int i = 0; i < parts.length; ++i) {
                    parts[i] = parts[i].trim();
                }
                words = parts;
            }
        }
        return words;
    }

    private void updateStatus() {
        updateStatus0();
        listener.run();
    }

    private void updateStatus0() {
        lastKey = null;
        lastValidationHash = null;

        final Color textFieldFore = UIManager.getColor("TextField.foreground");
        final Color textFieldBack = UIManager.getColor("TextField.background");
        final Color textFieldInactiveBack = UIManager.getColor("TextField.inactiveBackground");

        String sts = "Keep typing";
        Color fore = textFieldFore;
        Color back = textFieldInactiveBack;

        int[] nums = new int[passwordFields.length];
        int numCount = 0;

        for (JPasswordField f : passwordFields) {
            char[] cs = f.getPassword();
            if (cs.length == 0) {
                f.setForeground(textFieldFore);
                f.setBackground(textFieldBack);
                continue;
            }

            if (!checkCharacters(cs)) {
                sts = "Invalid character";
                fore = Color.WHITE;
                back = Color.RED;
                f.setForeground(fore);
                f.setBackground(back);
            }

            String s = new String(cs);
            Integer index = MnemonicEncoder.wordToIntOpt(s);
            if (index != null) {
                f.setForeground(Color.WHITE);
                f.setBackground(Color.BLUE);
            } else if (!f.hasFocus()) {
                sts = "Wrong word";
                fore = Color.WHITE;
                back = Color.RED;
                f.setForeground(fore);
                f.setBackground(back);
            } else {
                f.setForeground(textFieldFore);
                f.setBackground(textFieldBack);
            }

            if (index != null) {
                nums[numCount++] = index;
            }
        }

        if (numCount == nums.length) {
            byte[] key = MnemonicEncoder.decode32BytesEx(nums);
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

    private static boolean checkCharacters(char[] cs) {
        for (char c : cs) {
            if (!(c >= 'a' && c <= 'z')) {
                return false;
            }
        }
        return true;
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
        Preconditions.checkNotNull(words);
        Preconditions.checkArgument(words.size() == passwordFields.length);
        for (int i = 0; i < passwordFields.length; ++i) {
            String word = words.get(i);
            passwordFields[i].setText(word);
        }
    }

    public List<String> getWords() {
        List<String> words = Lists.transform(Arrays.asList(passwordFields), f -> new String(f.getPassword()).trim());
        return ImmutableList.copyOf(words);
    }

    public void setShowText(boolean b) {
        showText = b;
        if (!b) {
            for (JPasswordField f : passwordFields) {
                SwingUtils.setShowCharacters(f, false);
            }
        }
    }

    private static byte[] validationDigest(byte[] key) {
        byte[] bs = Bytes.concat("validation-333:".getBytes(Charsets.US_ASCII), key);
        for (int i = 0; i < 333; ++i) {
            bs = DigestUtils.sha256(bs);
        }
        return bs;
    }
}

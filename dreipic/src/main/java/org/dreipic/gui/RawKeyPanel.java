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

import org.dreipic.util.MnemonicEncoder;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

public final class RawKeyPanel {
    private final JPasswordField[] passwordFields;
    private final JPanel panel;

    private boolean showText;
    private int[] lastKey;

    private final Runnable listener;

    private boolean updating;

    public RawKeyPanel(Runnable listener) {
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

        panel = new JPanel(new BorderLayout());
        panel.add(keyPanel, BorderLayout.CENTER);

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
        if (updating) {
            return;
        }

        lastKey = null;

        final Color textFieldFore = UIManager.getColor("TextField.foreground");
        final Color textFieldBack = UIManager.getColor("TextField.background");

        Integer[] nums = new Integer[passwordFields.length];

        for (int i = 0; i < nums.length; ++i) {
            JPasswordField f = passwordFields[i];

            char[] cs = f.getPassword();
            if (cs.length == 0) {
                f.setForeground(textFieldFore);
                f.setBackground(textFieldBack);
                continue;
            }

            if (!checkCharacters(cs)) {
                f.setForeground(Color.WHITE);
                f.setBackground(Color.RED);
                continue;
            }

            String s = new String(cs);
            Integer index = MnemonicEncoder.wordToIntOpt(s);
            if (index != null) {
                f.setForeground(Color.WHITE);
                f.setBackground(Color.BLUE);
            } else if (!f.hasFocus()) {
                f.setForeground(Color.WHITE);
                f.setBackground(Color.RED);
            } else {
                f.setForeground(textFieldFore);
                f.setBackground(textFieldBack);
            }

            nums[i] = index;
        }

        updateKey(nums);
    }

    private static boolean checkCharacters(char[] cs) {
        for (char c : cs) {
            if (!(c >= 'a' && c <= 'z')) {
                return false;
            }
        }
        return true;
    }

    private void updateKey(Integer[] nums) {
        int[] key = new int[nums.length];
        boolean valid = true;

        for (int i = 0; i < key.length; ++i) {
            Integer num = nums[i];
            if (num == null) {
                valid = false;
                break;
            }
            key[i] = num;
        }

        if (valid) {
            lastKey = key;
        }
    }

    public JComponent getComponent() {
        return panel;
    }

    public void setShowText(boolean b) {
        showText = b;
        if (!b) {
            for (JPasswordField f : passwordFields) {
                SwingUtils.setShowCharacters(f, false);
            }
        }
    }

    public List<String> getWords() {
        List<String> words = Lists.transform(Arrays.asList(passwordFields), f -> new String(f.getPassword()).trim());
        return ImmutableList.copyOf(words);
    }

    public void setWords(List<String> words) {
        Preconditions.checkNotNull(words);
        Preconditions.checkArgument(words.size() == passwordFields.length);

        Preconditions.checkState(!updating);
        updating = true;
        try {
            for (int i = 0; i < passwordFields.length; ++i) {
                String word = words.get(i);
                passwordFields[i].setText(word);
            }
        } finally {
            updating = false;
        }

        updateStatus();
    }


    public int[] getKey() {
        return lastKey == null ? null : lastKey.clone();
    }
}

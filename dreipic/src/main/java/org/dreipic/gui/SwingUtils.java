package org.dreipic.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.util.function.Consumer;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JPopupMenu;
import javax.swing.JToggleButton;
import javax.swing.UIManager;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.JTextComponent;

public final class SwingUtils {
    private SwingUtils() {
    }

    public static void setShowCharacters(JPasswordField field, boolean show) {
        char c = show ? 0 : (Character)UIManager.getDefaults().get("PasswordField.echoChar");
        field.setEchoChar(c);
    }

    public static void copyToClipboard(String str) {
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        clipboard.setContents(new StringSelection(str), null);
    }

    public static String copyFromClipboard() {
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        String data;
        try {
            data = (String) clipboard.getData(DataFlavor.stringFlavor);
        } catch (UnsupportedFlavorException | IOException e) {
            return null;
        }
        return data;
    }

    public static JMenuItem addMenuItem(JPopupMenu menu, String title, Runnable listener) {
        JMenuItem item = new JMenuItem(title);
        item.addActionListener(__ -> listener.run());
        menu.add(item);
        return item;
    }

    public static JButton newButton(String title, boolean enabled, boolean focusable, Runnable listener) {
        JButton button = new JButton(title);
        button.setEnabled(enabled);
        button.setFocusable(focusable);
        button.addActionListener(e -> listener.run());
        return button;
    }

    public static JToggleButton newToggleButton(String title, boolean focusable, Consumer<Boolean> listener) {
        JToggleButton button = new JToggleButton(title);
        button.setFocusable(focusable);
        button.addActionListener(__ -> listener.accept(button.isSelected()));
        return button;
    }

    public static void onWindowClosed(Window window, Runnable listener) {
        window.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                listener.run();
            }
        });
    }

    public static void onMouseClicked(Component c, Consumer<MouseEvent> listener) {
        c.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                listener.accept(e);
            }
        });
    }

    public static void onMouseEnter(Component c, Consumer<MouseEvent> listener) {
        c.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                listener.accept(e);
            }
        });
    }

    public static void onMouseExit(Component c, Consumer<MouseEvent> listener) {
        c.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseExited(MouseEvent e) {
                listener.accept(e);
            }
        });
    }

    public static void onFocusGained(Component c, Consumer<FocusEvent> listener) {
        c.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                listener.accept(e);
            }
        });
    }

    public static void onFocusLost(Component c, Consumer<FocusEvent> listener) {
        c.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                listener.accept(e);
            }
        });
    }

    public static void onTextChanged(JTextComponent c, Runnable listener) {
        c.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void removeUpdate(DocumentEvent e) {
                listener.run();
            }

            @Override
            public void insertUpdate(DocumentEvent e) {
                listener.run();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                listener.run();
            }
        });
    }

    public static JFrame createFrame(String title, boolean pack, JComponent component) {
        JFrame frame = newFrame0(title, component);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        if (pack) {
            frame.pack();
            frame.setLocationRelativeTo(null);
        }

        return frame;
    }

    public static JFrame newFrame0(String title, JComponent body) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(body, BorderLayout.CENTER);
        JFrame frame = new JFrame(title);
        frame.setContentPane(panel);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setSize(800, 600);
        frame.setLocationRelativeTo(null);
        return frame;
    }
}

package org.dreipic.gui.exp;

import java.awt.BorderLayout;
import java.awt.GridLayout;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.dreipic.gui.MnemonicKeyPanel;
import org.dreipic.util.PasswordEncoder;

import com.google.common.base.Charsets;
import com.google.common.primitives.Bytes;

final class CredentialsPanel {
    private final JTextField txtHost;
    private final JTextField txtLogin;
    private final JTextField txtPassword;
    private final JTextField txtPort;
    private final MnemonicKeyPanel keyPanel;
    private final JPanel panel;

    CredentialsPanel(Runnable listener) {
        txtHost = new JTextField();
        txtLogin = new JTextField();
        txtPassword = new JTextField();
        txtPort = new JTextField();

        panel = new JPanel(new BorderLayout());

        keyPanel = new MnemonicKeyPanel(listener);
        panel.add(keyPanel.getComponent(), BorderLayout.NORTH);

        JPanel extraPanel = new JPanel(new GridLayout(8, 1));
        extraPanel.add(new JLabel("Host:"));
        extraPanel.add(txtHost);
        extraPanel.add(new JLabel("Port:"));
        extraPanel.add(txtPort);
        extraPanel.add(new JLabel("Login:"));
        extraPanel.add(txtLogin);
        extraPanel.add(new JLabel("Password:"));
        extraPanel.add(txtPassword);
        panel.add(extraPanel, BorderLayout.CENTER);

    }

    JComponent getComponent() {
        return panel;
    }

    FtpCredentials getCredentials() {
        byte[] key = keyPanel.getKey();
        if (key == null) {
            return null;
        }

        String host = txtHost.getText();
        String login = txtLogin.getText();
        byte[] passwordKey = getFullKey(key, txtPassword);
        byte[] portKey = getFullKey(key, txtPort);
        char[] passwordChars = PasswordEncoder.encodePassword(passwordKey);
        String password = new String(passwordChars);
        int port = PasswordEncoder.encodePort(portKey);

        return new FtpCredentials(host, port, login, password);
    }

    private static byte[] getFullKey(byte[] key, JTextField txt) {
        String str = txt.getText();
        byte[] extra = str.getBytes(Charsets.US_ASCII);
        return Bytes.concat(key, extra);
    }
}

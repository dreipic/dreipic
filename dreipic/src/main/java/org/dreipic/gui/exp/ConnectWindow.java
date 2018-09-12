package org.dreipic.gui.exp;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.dreipic.gui.MnemonicKeyPanel;
import org.dreipic.gui.SwingUtils;
import org.dreipic.struct.StructMeta;
import org.dreipic.util.DecryptUtils;

import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;

final class ConnectWindow {
    private final JDialog dialog;
    private final CredentialsPanel credPanel;
    private final MnemonicKeyPanel dataKeyPanel;
    private final JButton btnOK;
    private final LogPanel logPanel;

    private boolean connecting;

    private ConnectDetails result;

    private ConnectWindow() {
        credPanel = new CredentialsPanel(this::onStateChange);
        dataKeyPanel = new MnemonicKeyPanel(this::onStateChange);
        btnOK = SwingUtils.newButton("OK", false, true, this::onConnectClick);
        logPanel = new LogPanel(0);

        JPanel credPanelEx = new JPanel(new BorderLayout());
        credPanelEx.add(credPanel.getComponent(), BorderLayout.CENTER);
        credPanelEx.setBorder(BorderFactory.createTitledBorder("FTP Credentials"));

        JPanel dataKeyPanelEx = new JPanel(new BorderLayout());
        dataKeyPanelEx.add(dataKeyPanel.getComponent(), BorderLayout.CENTER);
        dataKeyPanelEx.setBorder(BorderFactory.createTitledBorder("Data Key"));

        JPanel centerPanel = new JPanel(new GridLayout(1, 2));
        centerPanel.add(credPanelEx);

        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.add(dataKeyPanelEx, BorderLayout.NORTH);
        rightPanel.add(logPanel.getComponent(), BorderLayout.CENTER);
        centerPanel.add(rightPanel);

        JPanel panel = new JPanel(new BorderLayout());
        panel.add(centerPanel, BorderLayout.CENTER);
        panel.add(btnOK, BorderLayout.SOUTH);

        dialog = new JDialog();
        dialog.setTitle("Connect");
        dialog.setModal(true);
        dialog.setContentPane(panel);
        dialog.pack();
        dialog.setLocationRelativeTo(null);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialog.setVisible(true);
    }

    private void onStateChange() {
        FtpCredentials creds = credPanel.getCredentials();
        byte[] dataKey = dataKeyPanel.getKey();
        btnOK.setEnabled(!connecting && creds != null && dataKey != null);
    }

    private void onConnectClick() {
        FtpCredentials creds = credPanel.getCredentials();
        byte[] dataKey = dataKeyPanel.getKey();
        if (creds == null || dataKey == null) {
            return;
        }

        Thread thread = new Thread(() -> connect(creds, dataKey));
        thread.setName("FTP");
        thread.setDaemon(true);
        thread.start();

        connecting = true;
        btnOK.setEnabled(false);
    }

    private void connect(FtpCredentials creds, byte[] dataKey) {
        List<StructMeta> metas = new ArrayList<>();

        try {
            logPanel.clear();

            FtpConnector.connect(creds, logPanel, ftp -> {
                List<Long> txs = listTransactions(ftp);
                logPanel.log("Transactions: %d", txs.size());

                for (long tx : txs) {
                    StructMeta meta = downloadMeta(ftp, dataKey, tx);
                    metas.add(meta);
                    logPanel.log("Decrypted tx: %d", tx);
                }
            });
        } catch (Throwable e) {
            String s = Throwables.getStackTraceAsString(e);
            logPanel.log("ERROR: %s", s);
        } finally {
            SwingUtilities.invokeLater(() -> {
                connecting = false;
                onStateChange();
            });
        }

        SwingUtilities.invokeLater(() -> {
            result = new ConnectDetails(creds, dataKey, metas);
            dialog.dispose();
        });
    }

    private StructMeta downloadMeta(FTPClient ftp, byte[] dataKey, long tx) {
        String path = "/meta/" + tx;
        byte[] rawData = FtpConnector.download(ftp, path);
        logPanel.log("Loaded tx: %d (%,d bytes)", tx, rawData.length);

        byte[] data = DecryptUtils.decryptData(dataKey, rawData);
        InputStream in = new ByteArrayInputStream(data);
        DataInputStream din = new DataInputStream(in);
        StructMeta meta = new StructMeta(din);
        return meta;
    }

    private List<Long> listTransactions(FTPClient ftp) {
        FTPFile[] ftpFiles = FtpConnector.listFiles(ftp, "/meta");
        List<Long> txs = new ArrayList<>();
        for (FTPFile f : ftpFiles) {
            boolean isDir = f.isDirectory();
            boolean isFile = f.isFile();
            Preconditions.checkState(isFile, "%s", f);
            Preconditions.checkState(!isDir, "%s", f);
            String name = f.getName();
            long tx = Long.parseLong(name);
            txs.add(tx);
        }
        Collections.sort(txs);
        return txs;
    }

    static ConnectDetails show() {
        ConnectWindow window = new ConnectWindow();
        return window.result;
    }
}

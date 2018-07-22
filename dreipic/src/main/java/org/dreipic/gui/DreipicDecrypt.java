package org.dreipic.gui;

import java.awt.BorderLayout;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import org.dreipic.util.DecryptUtils;

import com.google.common.base.Verify;

public final class DreipicDecrypt {
    private final MnemonicKeyPanel keyPanel;
    private final JTextField txtPath;
    private final JButton btnDecrypt;
    private final JTextField txtProgress;

    private final JPanel mainPanel;

    private boolean decrypting;

    private DreipicDecrypt() {
        keyPanel = new MnemonicKeyPanel(this::updateState);

        txtPath = new JTextField();
        SwingUtils.onTextChanged(txtPath, this::updateState);

        btnDecrypt = SwingUtils.newButton("Decrypt", true, true, this::onDecryptClick);
        txtProgress = new JTextField("Nothing done");
        txtProgress.setEditable(false);

        JPanel panPath = new JPanel(new BorderLayout());
        panPath.add(txtPath, BorderLayout.CENTER);
        panPath.add(btnDecrypt, BorderLayout.EAST);

        JPanel panDecrypt = new JPanel(new BorderLayout());
        panDecrypt.add(panPath, BorderLayout.NORTH);
        panDecrypt.add(txtProgress, BorderLayout.SOUTH);

        mainPanel = new JPanel(new BorderLayout());
        mainPanel.add(keyPanel.getComponent(), BorderLayout.CENTER);
        mainPanel.add(panDecrypt, BorderLayout.SOUTH);

        updateState();
    }

    private void updateState() {
        byte[] key = keyPanel.getKey();
        String path = txtPath.getText();
        File file = new File(path);
        btnDecrypt.setEnabled(!decrypting && key != null && file.isDirectory());
    }

    private void onDecryptClick() {
        if (decrypting) {
            return;
        }

        decrypting = true;
        updateState();

        byte[] key = keyPanel.getKey();
        String path = txtPath.getText();
        File dir = new File(path);
        List<File> files = getTargetFiles(dir);

        updateProgress(0, files.size());

        Thread thread = new Thread(() -> decryptFiles(key, files));
        thread.start();
    }

    private void decryptFiles(byte[] key, List<File> files) {
        try {
            int done = 0;
            for (File file : files) {
                try {
                    decryptFile(file, key);
                } catch (Throwable e) {
                    e.printStackTrace();
                    SwingUtilities.invokeLater(() -> txtProgress.setText("Failed: " + file));
                    break;
                }

                ++done;
                int doneF = done;
                SwingUtilities.invokeLater(() -> updateProgress(doneF, files.size()));
            }
        } finally {
            SwingUtilities.invokeLater(() -> {
                decrypting = false;
                updateState();
            });
        }
    }

    private void decryptFile(File file, byte[] key) {
        File dir = file.getParentFile();
        File tempFile = new File(dir, ".tempfile");

        boolean dec;
        try (InputStream in = new FileInputStream(file)) {
            try (OutputStream out = new FileOutputStream(tempFile)) {
                dec = DecryptUtils.decryptFile(key, in, out);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        if (dec) {
            Verify.verify(file.delete());
            Verify.verify(tempFile.renameTo(file));
        } else {
            Verify.verify(tempFile.delete());
        }
    }

    private void updateProgress(int done, int total) {
        txtProgress.setText("Processed: " + done + " / " + total);
    }

    private static List<File> getTargetFiles(File dir) {
        return Stream.of(dir.listFiles()).filter(f -> f.isFile() && !f.getName().equals(".tempfile")).collect(Collectors.toList());
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            DreipicDecrypt dec = new DreipicDecrypt();
            JFrame frame = SwingUtils.createFrame("Decrypt", true, dec.mainPanel);
            frame.setVisible(true);
        });
    }
}

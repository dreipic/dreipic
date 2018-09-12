package org.dreipic.gui.exp;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.function.Consumer;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPSClient;

import com.google.common.base.Preconditions;

final class FtpConnector {
    private FtpConnector() {
    }

    static void connect(FtpCredentials creds, LogPanel log, Consumer<FTPClient> consumer) {
        log.log("Connecting...");

        try {
            FTPSClient ftp = new FTPSClient();
            ftp.connect(creds.host, creds.port);

            try {
                ftp.enterLocalPassiveMode();
                ftp.execPBSZ(0);
                ftp.execPROT("P");

                Preconditions.checkState(ftp.login(creds.login, creds.password), "Failed to login");

                ftp.setFileType(FTP.BINARY_FILE_TYPE);

                log.log("Connected.");

                consumer.accept(ftp);
            } finally {
                ftp.disconnect();
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    static FTPFile[] listFiles(FTPClient ftp, String path) {
        try {
            return ftp.listFiles(path);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    static byte[] download(FTPClient ftp, String path) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            boolean b = ftp.retrieveFile(path, out);
            Preconditions.checkState(b, "Failed to retrieve: [%s]", path);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        byte[] data = out.toByteArray();
        return data;
    }
}

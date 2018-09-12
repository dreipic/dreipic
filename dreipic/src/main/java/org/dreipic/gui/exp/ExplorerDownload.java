package org.dreipic.gui.exp;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.security.DigestOutputStream;
import java.security.MessageDigest;

import javax.xml.bind.DatatypeConverter;

import org.apache.commons.net.ftp.FTPClient;
import org.dreipic.DreipicExtract;
import org.dreipic.struct.StructBlockFile;
import org.dreipic.struct.StructMeta;
import org.dreipic.struct.StructMetaData;
import org.dreipic.struct.StructMetaPath;
import org.dreipic.struct.StructPathType;
import org.dreipic.util.DecryptUtils;
import org.dreipic.util.DigestUtils;

import com.google.common.base.Preconditions;
import com.google.common.base.Verify;

final class ExplorerDownload {
    private static final String DOWNLOAD_TEMP_FILE_NAME = ".dreipic_download.temp";
    private static final String ARCHIVE_TEMP_FILE_NAME = ".dreipic_archive.temp";

    private ExplorerDownload() {
    }

    static void download(
            FtpCredentials creds,
            byte[] dataKey,
            StructMeta meta,
            StructMetaPath path,
            StructMetaData data,
            File dstDir,
            LogPanel log)
    {
        String fileName = pathName(path.path);
        File dstFile = new File(dstDir, fileName);
        if (dstFile.exists()) {
            log.log("Destination file/dir already exists, doing nothing: [%s]", dstFile);
            return;
        }

        File downloadTempFile = prepareTempFile(dstDir, DOWNLOAD_TEMP_FILE_NAME, log);
        File archiveTempFile = prepareTempFile(dstDir, ARCHIVE_TEMP_FILE_NAME, log);

        try {
            FtpConnector.connect(creds, log, ftp -> {
                downloadFile(dataKey, meta, data, downloadTempFile, ftp, log);
            });

            log.log("File downloaded");

            if (path.type == StructPathType.ARCHIVE) {
                Preconditions.checkState(dstFile.mkdir(), "Failed to create directory: [%s]", dstFile);
                DreipicExtract.extractFile(downloadTempFile, dstFile, archiveTempFile);
                log.log("Archive extracted");
            } else {
                Verify.verify(downloadTempFile.renameTo(dstFile));
                log.log("File renamed");
            }
        } finally {
            downloadTempFile.delete();
            archiveTempFile.delete();
        }
    }

    private static File prepareTempFile(File dir, String name, LogPanel log) {
        File tempFile = new File(dir, name);
        if (tempFile.exists()) {
            Verify.verify(tempFile.isFile(), "%s", tempFile);
            log.log("Found old temp file [%s], deleting (%,d bytes)", tempFile, tempFile.length());
            Verify.verify(tempFile.delete(), "%s", tempFile);
        }
        return tempFile;
    }

    private static void downloadFile(
            byte[] dataKey,
            StructMeta meta,
            StructMetaData data,
            File tempFile,
            FTPClient ftp,
            LogPanel log)
    {
        MessageDigest digest = DigestUtils.newSha256();
        long ofs = 0;

        try (OutputStream out = new FileOutputStream(tempFile)) {
            DigestOutputStream digOut = new DigestOutputStream(out, digest);

            log.log("Downloading %,d blocks, %,d bytes", data.blocks.size(), data.size);

            for (int blockIdx = 0; blockIdx < data.blocks.size(); ++blockIdx) {
                byte[] blockId = DecryptUtils.getBlockId(meta.storageId, data.hash, blockIdx);
                String blockIdStr = DatatypeConverter.printHexBinary(blockId).toLowerCase();
                String blockDir = blockIdStr.substring(0, 2);
                String blockFile = blockIdStr.substring(2);
                String blockPath = "/data/" + blockDir + "/" + blockFile;

                byte[] rawData = FtpConnector.download(ftp, blockPath);
                byte[] decData = DecryptUtils.decryptData(dataKey, rawData);

                InputStream in = new ByteArrayInputStream(decData);
                DataInputStream din = new DataInputStream(in);
                StructBlockFile struct = new StructBlockFile(din);

                Verify.verify(struct.blockIdx == blockIdx);
                Verify.verify(struct.blockOffset == ofs);
                digOut.write(struct.data);
                ofs += struct.data.length;

                log.log("Downloaded block %,d / %,d (%,d bytes)", blockIdx, data.blocks.size(), ofs);
            }
            digOut.flush();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        byte[] actHashBytes = digest.digest();
        String actHashStr = DatatypeConverter.printHexBinary(actHashBytes);
        String hashStr = DatatypeConverter.printHexBinary(data.hash);
        Verify.verify(actHashStr.equals(hashStr), "Expected %s was %s", hashStr, actHashStr);
    }

    private static String pathName(String path) {
        int idx = path.lastIndexOf('/');
        return idx == -1 ? path : path.substring(idx + 1);
    }
}

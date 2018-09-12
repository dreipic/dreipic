package org.dreipic;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;

import org.dreipic.util.ByteUtils;

import com.google.common.base.Preconditions;
import com.google.common.base.Verify;
import com.google.common.io.ByteStreams;

public final class DreipicExtract {
    public static void main(String[] args) {
        if (args.length != 2) {
            System.err.println("Usage: IN_FILE OUT_DIR");
            System.exit(1);
        }

        File inFile = new File(args[0]);
        File outDir = new File(args[1]);
        File tempFile = new File(outDir, ".tempfile");

        extractFile(inFile, outDir, tempFile);
    }

    public static void extractFile(File inFile, File outDir, File tempFile) {
        try (InputStream in = new FileInputStream(inFile)) {
            DataInputStream dataIn = new DataInputStream(in);
            extract(dataIn, outDir, tempFile);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static void extract(DataInputStream in, File dir, File tempFile) throws IOException {
        long nSubs = in.readLong();
        for (long i = 0; i < nSubs; ++i) {
            extractSub(in, dir, tempFile);
        }
    }

    private static void extractSub(DataInputStream in, File dir, File tempFile) throws IOException {
        boolean directory = in.readBoolean();
        String name = ByteUtils.readString(in);

        File sub = new File(dir, name);

        if (directory) {
            Preconditions.checkState(sub.isDirectory() || sub.mkdirs(), "%s", sub);
           long nSubs = in.readLong();
            for (long i = 0; i < nSubs; ++i) {
                extractSub(in, sub, tempFile);
            }
        } else {
            long size = in.readLong();
            long time = in.readLong();

            if (sub.exists()) {
                Preconditions.checkState(sub.isFile(), "%s", sub);
                ByteStreams.skipFully(in, size);
            } else {
                try (OutputStream out = new FileOutputStream(tempFile)) {
                    InputStream limIn = ByteStreams.limit(in, size);
                    long copy = ByteStreams.copy(limIn, out);
                    Verify.verify(copy == size);
                }
                tempFile.setLastModified(time);
                Preconditions.checkState(tempFile.renameTo(sub), "%s %s", tempFile, sub);
            }
        }
    }
}

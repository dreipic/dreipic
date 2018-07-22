package org.dreipic;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.xml.bind.DatatypeConverter;

import org.dreipic.struct.StructMeta;
import org.dreipic.struct.StructMetaPath;
import org.dreipic.util.DecryptUtils;

import com.google.common.base.Verify;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Ordering;

public final class DreipicListFiles {
    public static void main(String[] args) {
        if (args.length != 1) {
            System.err.println("Usage: META_DIR");
            System.exit(1);
        }

        File metaDir = new File(args[0]);

        Map<Long, File> metaFiles = getMetaFiles(metaDir);
        for (long id : metaFiles.keySet()) {
            File file = metaFiles.get(id);
            processMetaFile(id, file);
        }
    }

    private static void processMetaFile(long id, File file) {
        System.out.printf("META: %s\n", id);
        StructMeta meta = readMeta(file);

        for (StructMetaPath path : meta.paths) {
            String hashStr = path.hash == null ? "-" : DatatypeConverter.printHexBinary(path.hash);
            System.out.printf("  %s %s %s\n", path.type, path.path, hashStr);
        }
    }

    static Map<Long, File> getMetaFiles(File metaDir) {
        Map<Long, File> map = new HashMap<>();
        for (File file : metaDir.listFiles()) {
            Verify.verify(file.isFile(), "%s", file);
            String name = file.getName();
            Verify.verify(name.matches("\\d+"), "%s", file);
            long id = Long.parseLong(name);
            Verify.verify(!map.containsKey(id), "%s", id);
            map.put(id, file);
        }

        Map<Long, File> res = new LinkedHashMap<>();
        for (long id : Ordering.natural().sortedCopy(map.keySet())) {
            res.put(id, map.get(id));
        }

        return ImmutableMap.copyOf(res);
    }

    static StructMeta readMeta(File file) {
        try (InputStream in = new FileInputStream(file)) {
            DataInputStream dataIn = new DataInputStream(in);
            Verify.verify(dataIn.readInt() == DecryptUtils.DECRYPT_SIGN);
            return new StructMeta(dataIn);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}

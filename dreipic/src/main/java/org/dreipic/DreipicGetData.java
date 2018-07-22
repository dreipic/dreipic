package org.dreipic;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.bind.DatatypeConverter;

import org.dreipic.struct.StructBlockFile;
import org.dreipic.struct.StructMeta;
import org.dreipic.struct.StructMetaData;
import org.dreipic.util.DecryptUtils;
import org.dreipic.util.DigestUtils;

import com.google.common.base.Verify;
import com.google.common.collect.Ordering;

public final class DreipicGetData {
    public static void main(String[] args) throws IOException {
        if (args.length != 3) {
            System.err.println("Usage: META_DIR BLOCK_DIR DST_DIR");
            System.exit(1);
        }

        File metaDir = new File(args[0]);
        File blockDir = new File(args[1]);
        File dstDir = new File(args[2]);

        Map<String, List<String>> dataMap = new HashMap<>();
        Map<String, String> blockMap = new HashMap<>();
        loadMetaData(metaDir, dataMap, blockMap);

        Map<String, File> blockFiles = getBlockFiles(blockDir);

        Set<String> blockDatas = new HashSet<>();
        for (String blockId : blockFiles.keySet()) {
            String hash = blockMap.get(blockId);
            Verify.verifyNotNull(hash, "Unknown block: %s", blockId);
            blockDatas.add(hash);
        }

        System.out.printf("Found %,d blocks for %,d datas\n", blockFiles.size(), blockDatas.size());

        for (String hash : Ordering.natural().sortedCopy(blockDatas)) {
            mergeBlocks(dstDir, dataMap, blockFiles, hash);
        }
    }

    private static void mergeBlocks(
            File dstDir,
            Map<String, List<String>> dataMap,
            Map<String, File> blockFiles,
            String hash) throws IOException
    {
        List<String> blockIds = dataMap.get(hash);
        Verify.verifyNotNull(blockIds);

        int k = 0;
        for (String blockId : blockIds) {
            if (blockFiles.containsKey(blockId)) {
                ++k;
            }
        }

        if (k < blockIds.size()) {
            System.out.printf("Data %s: only %d of %d blocks found\n", k, blockIds.size());
            return;
        }

        File dstFile = new File(dstDir, hash);
        if (dstFile.exists()) {
            System.out.printf("Data %s: file already exists\n", hash);
            return;
        }

        MessageDigest digest = DigestUtils.newSha256();
        int idx = 0;
        long ofs = 0;

        File tempFile = new File(dstDir, ".tempfile");
        try (OutputStream out = new FileOutputStream(tempFile)) {
            DigestOutputStream digOut = new DigestOutputStream(out, digest);

            for (String blockId : blockIds) {
                File file = blockFiles.get(blockId);
                try (InputStream in = new FileInputStream(file)) {
                    DataInputStream dataIn = new DataInputStream(in);
                    Verify.verify(dataIn.readInt() == DecryptUtils.DECRYPT_SIGN);
                    StructBlockFile struct = new StructBlockFile(dataIn);
                    Verify.verify(struct.blockIdx == idx);
                    Verify.verify(struct.blockOffset == ofs);
                    digOut.write(struct.data);
                    ++idx;
                    ofs += struct.data.length;
                }
            }
            digOut.flush();
        }

        byte[] actHashBytes = digest.digest();
        String actHash = DatatypeConverter.printHexBinary(actHashBytes);
        Verify.verify(actHash.equals(hash), "Expected %s was %s", hash, actHash);

        Verify.verify(tempFile.renameTo(dstFile));

        System.out.printf("Data %s: done (%,d blocks, %,d bytes)\n", hash, blockIds.size(), ofs);
    }

    private static void loadMetaData(File metaDir, Map<String, List<String>> dataMap, Map<String, String> blockMap) {
        Map<Long, File> metaFiles = DreipicListFiles.getMetaFiles(metaDir);
        for (File file : metaFiles.values()) {
            StructMeta meta = DreipicListFiles.readMeta(file);

            for (StructMetaData data : meta.datas) {
                String hash = DatatypeConverter.printHexBinary(data.hash);

                List<String> blockIds = new ArrayList<>();

                for (int i = 0; i < data.blocks.size(); ++i) {
                    byte[] blockId = DreipicListDatas.getBlockId(meta.storageId, data.hash, i);
                    String blockIdStr = DatatypeConverter.printHexBinary(blockId);
                    blockIdStr = blockIdStr.substring(2);
                    String oldHash = blockMap.put(blockIdStr, hash);
                    Verify.verify(oldHash == null || oldHash.equals(hash));
                    blockIds.add(blockIdStr);
                }

                dataMap.put(hash, blockIds);
            }
        }
    }

    private static final Map<String, File> getBlockFiles(File blockDir) {
        Map<String, File> map = new HashMap<>();
        for (File file : blockDir.listFiles()) {
            Verify.verify(file.isFile());
            String name = file.getName();
            Verify.verify(name.matches("[0-9a-f]{38}"), "%s", name);
            map.put(name.toUpperCase(), file);
        }
        return map;
    }
}

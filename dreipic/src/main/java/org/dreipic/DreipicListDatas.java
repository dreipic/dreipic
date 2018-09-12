package org.dreipic;

import java.io.File;
import java.util.Map;

import javax.xml.bind.DatatypeConverter;

import org.dreipic.struct.StructBlockInfo;
import org.dreipic.struct.StructMeta;
import org.dreipic.struct.StructMetaData;
import org.dreipic.util.DecryptUtils;

public final class DreipicListDatas {
    public static void main(String[] args) {
        if (args.length != 1) {
            System.err.println("Usage: META_DIR");
            System.exit(1);
        }

        File metaDir = new File(args[0]);

        Map<Long, File> metaFiles = DreipicListFiles.getMetaFiles(metaDir);
        for (long id : metaFiles.keySet()) {
            File file = metaFiles.get(id);
            processMetaFile(id, file);
        }
    }

    private static void processMetaFile(long id, File file) {
        System.out.printf("META: %s\n", id);
        StructMeta meta = DreipicListFiles.readMeta(file);

        for (StructMetaData data : meta.datas) {
            System.out.printf("  %s %,d %,d\n", DatatypeConverter.printHexBinary(data.hash), data.size, data.blocks.size());
            for (int i = 0; i < data.blocks.size(); ++i) {
                StructBlockInfo block = data.blocks.get(i);
                byte[] blockId = DecryptUtils.getBlockId(meta.storageId, data.hash, i);
                System.out.printf("    %s %,d\n", DatatypeConverter.printHexBinary(blockId), block.outerSize);
            }
        }
    }
}

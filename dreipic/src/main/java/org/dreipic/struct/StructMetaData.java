package org.dreipic.struct;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public final class StructMetaData {
    public final long size;
    public final byte[] hash;
    public final List<StructBlockInfo> blocks;

    StructMetaData(DataInputStream in) throws IOException {
        size = in.readLong();
        hash = new byte[32];
        in.readFully(hash);

        int n = in.readInt();
        blocks = new ArrayList<>();
        for (int i = 0; i < n; ++i) {
            blocks.add(new StructBlockInfo(in));
        }
    }
}

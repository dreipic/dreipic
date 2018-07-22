package org.dreipic.struct;

import java.io.DataInputStream;
import java.io.IOException;

public final class StructBlockInfo {
    public final long innerSize;
    public final byte[] innerHash;
    public final long outerSize;
    public final byte[] outerHash;

    StructBlockInfo(DataInputStream in) throws IOException {
        innerSize = in.readLong();
        innerHash = new byte[32];
        in.readFully(innerHash);
        outerSize = in.readLong();
        outerHash = new byte[32];
        in.readFully(outerHash);
    }
}

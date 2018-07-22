package org.dreipic.struct;

import java.io.DataInputStream;
import java.io.IOException;

import com.google.common.base.Verify;

public final class StructBlockFile {
    private static final int SIGN = 0xF8D57165;

    public final byte[] storageId;
    public final long transaction;
    public final int blockIdx;
    public final long blockOffset;
    public final byte[] data;

    public StructBlockFile(DataInputStream in) throws IOException {
        Verify.verify(in.readInt() == SIGN);

        storageId = new byte[32];
        in.readFully(storageId);

        transaction = in.readLong();
        blockIdx = in.readInt();
        blockOffset = in.readLong();
        int dataLen = in.readInt();

        data = new byte[dataLen];
        in.readFully(data);
    }
}

package org.dreipic.struct;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;

import com.google.common.base.Verify;

public final class StructMeta {
    private static final int SIGN = 0xB5624A23;

    public final byte[] storageId;
    public final long timestamp;
    public final long prevTimestamp;
    public final List<StructMetaData> datas;
    public final List<StructMetaPath> paths;

    public StructMeta(DataInputStream in) {
        try {
            int sign = in.readInt();
            Verify.verify(sign == SIGN);

            storageId = new byte[32];
            in.readFully(storageId);

            timestamp = in.readLong();
            prevTimestamp = in.readLong();

            datas = new ArrayList<>();
            paths = new ArrayList<>();

            int n = in.readInt();
            for (int i = 0; i < n; ++i) {
                int k = in.readByte();
                switch (k) {
                case 0:
                    datas.add(new StructMetaData(in));
                    break;
                case 1:
                    paths.add(new StructMetaPath(in, true));
                    break;
                case 2:
                    paths.add(new StructMetaPath(in, false));
                    break;
                default:
                    throw new IllegalStateException("" + k);
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}

package org.dreipic.struct;

import java.io.DataInputStream;
import java.io.IOException;

import org.dreipic.util.ByteUtils;

public final class StructMetaPath {
    public final String path;
    public final StructPathType type;
    public final byte[] hash;
    public final Long time;

    StructMetaPath(DataInputStream in, boolean file) throws IOException {
        path = ByteUtils.readString(in);

        int typeVal = in.readInt();

        if (file) {
            switch (typeVal) {
            case 0:
                type = StructPathType.FILE;
                break;
            case 1:
                type = StructPathType.ARCHIVE;
                break;
            default:
                throw new IllegalStateException("" + typeVal);
            }

            hash = new byte[32];
            in.readFully(hash);

            long timeVal = in.readLong();
            time = timeVal == 0 ? null : timeVal;
        } else {
            switch (typeVal) {
            case 0:
                type = StructPathType.DELETED;
                break;
            case 1:
                type = StructPathType.DIRECTORY;
                break;
            default:
                throw new IllegalStateException("" + typeVal);
            }

            hash = null;
            time = null;
        }
    }
}

package org.dreipic.util;

import java.io.DataInputStream;
import java.io.IOException;

import com.google.common.base.Charsets;

public final class ByteUtils {
    private ByteUtils() {
    }

    public static long getLong(byte[] bytes, int ofs) {
        int ch1 = bytes[ofs + 0] & 0xFF;
        int ch2 = bytes[ofs + 1] & 0xFF;
        int ch3 = bytes[ofs + 2] & 0xFF;
        int ch4 = bytes[ofs + 3] & 0xFF;
        int ch5 = bytes[ofs + 4] & 0xFF;
        int ch6 = bytes[ofs + 5] & 0xFF;
        int ch7 = bytes[ofs + 6] & 0xFF;
        int ch8 = bytes[ofs + 7] & 0xFF;
        return ((long) ch1 << 56)
                | ((long) ch2 << 48)
                | ((long) ch3 << 40)
                | ((long) ch4 << 32)
                | ((long) ch5 << 24)
                | ((long) ch6 << 16)
                | ((long) ch7 << 8)
                | ((long) ch8 << 0);
    }

    public static byte[] longToBytes(long v) {
        byte[] bs = new byte[8];
        bs[0] = (byte) (v >>> 56);
        bs[1] = (byte) (v >>> 48);
        bs[2] = (byte) (v >>> 40);
        bs[3] = (byte) (v >>> 32);
        bs[4] = (byte) (v >>> 24);
        bs[5] = (byte) (v >>> 16);
        bs[6] = (byte) (v >>>  8);
        bs[7] = (byte) (v >>>  0);
        return bs;
    }

    public static String readString(DataInputStream in) throws IOException {
        int len = in.readInt();

        if (len < 0) {
            throw new IOException();
        }
        if (len == 0) {
            return "";
        }

        byte[] bs = new byte[len];
        in.readFully(bs);
        String s = new String(bs, Charsets.UTF_8);
        return s;
    }
}

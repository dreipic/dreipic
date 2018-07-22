package org.dreipic.util;

import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
import com.google.common.primitives.Bytes;

public final class PasswordEncoder {
    private static final String UP_LETTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String LO_LETTERS = UP_LETTERS.toLowerCase();
    private static final String DIGITS = "0123456789";
    private static final String SPECIALS = "!@#$%^&*()";

    private static final String[] GROUPS = {
            UP_LETTERS,
            UP_LETTERS,
            LO_LETTERS,
            LO_LETTERS,
            LO_LETTERS,
            LO_LETTERS,
            DIGITS,
            SPECIALS,
    };

    private static final byte[] PREFIX = "password:".getBytes(Charsets.US_ASCII);

    private PasswordEncoder() {
    }

    public static void main(String[] args) {
        for (int i = 0; i < 250; ++i) {
            byte[] bytes = { (byte) i };
            char[] ps = encodePassword(bytes, 32);
            System.out.println(i + " " + new String(ps));
        }
    }

    public static char[] encodePassword(byte[] bytes, int len) {
        Preconditions.checkNotNull(bytes);
        Preconditions.checkArgument(bytes.length > 0);

        byte[] hash = passwordToHash(bytes);

        char[] res = new char[len];

        List<String> groups = new ArrayList<>();

        for (int i = 0; i < len; ++i) {
            String g = nextGroup(hash, groups);
            int x = nextInt(hash, g.length());
            char c = g.charAt(x);
            res[i] = c;
        }

        return res;
    }

    private static String nextGroup(byte[] hash, List<String> groups) {
        if (groups.isEmpty()) {
            groups.addAll(Arrays.asList(GROUPS));
        }

        int n = groups.size();
        int i = nextInt(hash, n);
        String group = groups.remove(i);
        return group;
    }

    private static byte[] passwordToHash(byte[] bytes) {
        MessageDigest md = DigestUtils.newSha256();

        byte[] digest = Bytes.concat(PREFIX, bytes);
        for (int i = 0; i < 55555; ++i) {
            md.update(digest);
            digest = md.digest();
        }
        return digest;
    }

    private static int nextInt(byte[] hash, int range) {
        long v = ByteUtils.getLong(hash, 0);
        int x = (int) Long.remainderUnsigned(v, range);
        byte[] hash2 = DigestUtils.sha256(hash);
        System.arraycopy(hash2, 0, hash, 0, hash2.length);
        return x;
    }

    public static int encodePort(byte[] bytes) {
        Preconditions.checkNotNull(bytes);
        Preconditions.checkArgument(bytes.length > 0);
        byte[] hash = passwordToHash(bytes);
        int port = 10_000 + nextInt(hash, 10_000);
        return port;
    }
}

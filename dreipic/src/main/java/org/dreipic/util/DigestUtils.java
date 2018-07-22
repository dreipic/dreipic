package org.dreipic.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import com.google.common.base.Charsets;

public final class DigestUtils {
    private DigestUtils() {
    }

    public static MessageDigest newSha256() {
        return newDigest("SHA-256");
    }

    public static byte[] sha256(String str) {
        byte[] bytes = str.getBytes(Charsets.UTF_8);
        return sha256(bytes);
    }

    public static byte[] sha256(byte[] data) {
        MessageDigest digest = newSha256();
        digest.update(data);
        byte[] result = digest.digest();
        return result;
    }

    private static MessageDigest newDigest(String algorithm) {
        try {
            return MessageDigest.getInstance(algorithm);
        } catch (final NoSuchAlgorithmException e) {
            throw new IllegalArgumentException(e);
        }
    }
}

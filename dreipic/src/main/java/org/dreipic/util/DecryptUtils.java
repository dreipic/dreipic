package org.dreipic.util;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import com.google.common.base.Preconditions;
import com.google.common.base.Verify;
import com.google.common.io.ByteStreams;
import com.google.common.primitives.Bytes;

public final class DecryptUtils {
    public static final int DECRYPT_SIGN = 0x1AFC72E7;

    private static final byte[] ENCRYPTION_PREFIX = { 51, 53, 57 };
    private static final byte[] ENCRYPTION_SUFFIX = { 97, 101, 113 };

    private DecryptUtils() {
    }

    public static boolean decryptFile(byte[] key, InputStream in, OutputStream out) {
        Preconditions.checkNotNull(key);
        Preconditions.checkNotNull(in);
        Preconditions.checkNotNull(out);

        try {
            return decryptFile0(key, in, out);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static boolean decryptFile0(byte[] key, InputStream in, OutputStream out) throws IOException {
        DataInputStream dataIn = new DataInputStream(in);
        int sign = dataIn.readInt();
        if (sign == DECRYPT_SIGN) {
            return false;
        }

        byte[] data = decryptDataSub(key, dataIn);

        DataOutputStream dataOut = new DataOutputStream(out);
        dataOut.writeInt(DECRYPT_SIGN);
        dataOut.writeInt(sign);
        dataOut.write(data);

        return true;
    }

    public static byte[] decryptData(byte[] key, byte[] inData) {
        Preconditions.checkNotNull(key);
        Preconditions.checkNotNull(inData);

        InputStream in = new ByteArrayInputStream(inData);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            decryptData0(key, in, out);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        byte[] outData = out.toByteArray();
        return outData;
    }

    private static void decryptData0(byte[] key, InputStream in, OutputStream out) throws IOException {
        DataInputStream dataIn = new DataInputStream(in);
        int sign = dataIn.readInt();

        byte[] data = decryptDataSub(key, dataIn);

        DataOutputStream dataOut = new DataOutputStream(out);
        dataOut.writeInt(sign);
        dataOut.write(data);
    }

    private static byte[] decryptDataSub(byte[] key, InputStream in) throws IOException {
        byte[] salt = new byte[32];
        ByteStreams.readFully(in, salt);

        byte[][] pair = calcEncryptionKeyAndInitVector(key, salt);

        InputStream bufIn = new BufferedInputStream(in, 1024 * 1024);
        InputStream cipIn = createDecryptingInputStream(bufIn, pair[0], pair[1]);

        byte[] test = new byte[16];
        ByteStreams.readFully(cipIn, test);

        byte[] actCheckHash = new byte[4];
        ByteStreams.readFully(cipIn, actCheckHash);
        byte[] expCheckHash = DigestUtils.sha256(test);
        expCheckHash = Arrays.copyOf(expCheckHash, 4);
        Verify.verify(Arrays.equals(actCheckHash, expCheckHash), "Wrong key");

        Inflater inflater = new Inflater(true);
        InputStream infIn = new InflaterInputStream(cipIn, inflater, 256 * 1024);

        byte[] dataPlusHash = readAll(infIn);
        byte[] data = Arrays.copyOfRange(dataPlusHash, 0, dataPlusHash.length - 8);
        byte[] expHash = Arrays.copyOfRange(dataPlusHash, dataPlusHash.length - 8, dataPlusHash.length);

        byte[] actHash = DigestUtils.sha256(data);
        actHash = Arrays.copyOf(actHash, 8);

        Verify.verify(Arrays.equals(actHash, expHash));
        return data;
    }

    private static byte[][] calcEncryptionKeyAndInitVector(byte[] key, byte[] salt) {
        byte[] encKey = secretKeyToEncryptionKey(key, salt);

        MessageDigest md = DigestUtils.newSha256();
        md.update(encKey);
        md.update(salt);
        byte[] digest = md.digest();
        md.update(digest);
        digest = md.digest();
        md.update(digest);
        digest = md.digest();
        byte[] initVector = Arrays.copyOf(digest, 16);

        return new byte[][]{ encKey, initVector };
    }

    private static InputStream createDecryptingInputStream(InputStream in, byte[] key, byte[] initVector) {
        Cipher cipher = createCipher(key, initVector, Cipher.DECRYPT_MODE);
        return new javax.crypto.CipherInputStream(in, cipher);
    }

    private static Cipher createCipher(byte[] key, byte[] initVector, int mode) {
        IvParameterSpec ivSpec = new IvParameterSpec(initVector);
        SecretKeySpec keySpec = new SecretKeySpec(key, "AES");

        Cipher cipher;
        try {
            cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");
            cipher.init(mode, keySpec, ivSpec);
        } catch (GeneralSecurityException e) {
            throw new RuntimeException(e);
        }

        Verify.verify(cipher.getBlockSize() == 16);
        return cipher;
    }

    private static byte[] secretKeyToEncryptionKey(byte[] key, byte[] salt) {
        byte[] hash = passwordKeyToHash(key, salt, ENCRYPTION_PREFIX, ENCRYPTION_SUFFIX);
        return hash;
    }

    private static byte[] passwordKeyToHash(byte[] key, byte[] salt, byte[] prefix, byte[] suffix) {
        MessageDigest digest = DigestUtils.newSha256();
        digest.update(prefix, 0, prefix.length);
        digest.update(key, 0, key.length);
        digest.update(suffix, 0, suffix.length);
        digest.update(salt, 0, salt.length);
        byte[] hash = digest.digest();
        Verify.verify(hash.length == 32);
        return hash;
    }

    private static byte[] readAll(InputStream in) throws IOException {
        byte[] res = ByteStreams.toByteArray(in);
        return res;
    }

    public static byte[] getBlockId(byte[] storageId, byte[] dataHash, int blockIdx) {
        byte[] baseHash = DigestUtils.sha256(Bytes.concat(storageId, dataHash));

        byte[] idxBytes = ByteUtils.longToBytes(blockIdx);
        byte[] hash = DigestUtils.sha256(Bytes.concat(baseHash, idxBytes));

        byte[] blockId = Arrays.copyOf(hash, 20);
        return blockId;
    }
}

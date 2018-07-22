package org.dreipic.util;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
import com.google.common.base.Verify;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.Resources;

public final class MnemonicEncoder {
    private MnemonicEncoder() {
    }

    static List<String> getWordList() {
        return Initializer.WORD_LIST;
    }

    static Map<String, Integer> getWordMap() {
        return Initializer.WORD_MAP;
    }

    public static Integer wordToIntOpt(String word) {
        Preconditions.checkNotNull(word);
        Integer index = Initializer.WORD_MAP.get(word);
        return index;
    }

    public static int[] wordsToInts(String[] words) {
        Preconditions.checkNotNull(words);

        int[] result = new int[words.length];
        for (int i = 0; i < words.length; ++i) {
            int idx = wordToIntOpt(words[i]);
            result[i] = idx;
        }

        return result;
    }

    public static String[] encode32Bytes(byte[] bytes) {
        Preconditions.checkNotNull(bytes);
        Preconditions.checkArgument(bytes.length == 32);

        byte[] hashBytes = DigestUtils.sha256(bytes);
        byte[] chkBits = new byte[8];
        intToBits(hashBytes[0] & 0xFF, chkBits, 0, 8);

        byte[] fullBits = new byte[264];
        byte[] blockBits = new byte[33];
        int[] checkPoses = Initializer.CHECKBIT_POSES;

        for (int i = 0; i < 8; ++i) {
            bytesToBits(bytes, i * 4, 4, blockBits, 0);
            insert(blockBits, checkPoses[i], chkBits[i]);
            copy(blockBits, 0, 33, fullBits, i * 33);
        }

        List<String> wordList = Initializer.WORD_LIST;

        String[] result = new String[24];
        for (int i = 0; i < 24; ++i) {
            int idx = bitsToInt(fullBits, i * 11, 11);
            result[i] = wordList.get(idx);
        }

        return result;
    }

    public static byte[] decode32Bytes(int[] words) {
        Preconditions.checkNotNull(words);
        Preconditions.checkArgument(words.length == 24);

        byte[] fullBits = new byte[11 * 24];
        for (int i = 0; i < 24; ++i) {
            int word = words[i];
            Preconditions.checkArgument(word >= 0);
            Preconditions.checkArgument(word < 2048);
            intToBits(word, fullBits, i * 11, 11);
        }

        byte[] blockBits = new byte[33];
        byte[] bytes = new byte[32];
        byte[] chkBits = new byte[8];
        int[] checkPoses = Initializer.CHECKBIT_POSES;

        for (int i = 0; i < 8; ++i) {
            copy(fullBits, i * 33, 33, blockBits, 0);
            int chkPos = checkPoses[i];
            chkBits[i] = blockBits[chkPos];
            delete(blockBits, chkPos, 1);
            bitsToBytes(blockBits, 0, bytes, i * 4, 4);
        }

        int chk = bitsToInt(chkBits, 0, 8);
        byte[] hash = DigestUtils.sha256(bytes);
        int hashChk = hash[0] & 0xFF;
        if (hashChk != chk) {
            return null;
        }

        return bytes;
    }

    public static byte[] decode32Bytes(String[] words) {
        Preconditions.checkNotNull(words);
        Preconditions.checkArgument(words.length == 24);

        int[] nums = new int[words.length];
        for (int i = 0; i < nums.length; ++i) {
            Integer index = Initializer.WORD_MAP.get(words[i]);
            if (index == null) {
                return null;
            }
            nums[i] = index;
        }

        byte[] key = decode32Bytes(nums);
        return key;
    }

    public static byte[] decode32BytesEx(int[] words) {
        Preconditions.checkNotNull(words);
        Preconditions.checkArgument(words.length == 24);

        byte[] key = decode32Bytes(words);
        if (key == null) {
            return null;
        }

        for (int i = 0; i < 33; ++i) {
            key = DigestUtils.sha256(key);
        }
        return key;
    }

    private static void bytesToBits(byte[] bytes, int srcOfs, int srcLen, byte[] bits, int dstOfs) {
        Preconditions.checkArgument(srcOfs >= 0);
        Preconditions.checkArgument(srcLen >= 0);
        Preconditions.checkArgument(dstOfs >= 0);
        Preconditions.checkArgument(srcOfs + srcLen <= bytes.length);
        Preconditions.checkArgument(dstOfs + srcLen * 8 <= bits.length);

        for (int i = 0; i < srcLen; ++i) {
            intToBits(bytes[srcOfs + i] & 0xFF, bits, dstOfs + i * 8, 8);
        }
    }

    private static void bitsToBytes(byte[] bits, int srcOfs, byte[] bytes, int dstOfs, int dstLen) {
        Preconditions.checkArgument(srcOfs >= 0);
        Preconditions.checkArgument(dstOfs >= 0);
        Preconditions.checkArgument(dstLen >= 0);
        Preconditions.checkArgument(srcOfs + dstLen * 8 <= bits.length);
        Preconditions.checkArgument(dstOfs + dstLen <= bytes.length);

        for (int i = 0; i < dstLen; ++i) {
            int v = bitsToInt(bits, srcOfs + i * 8, 8);
            bytes[dstOfs + i] = (byte) v;
        }
    }

    private static void intToBits(int v, byte[] bits, int ofs, int len) {
        Preconditions.checkArgument(ofs >= 0);
        Preconditions.checkArgument(len >= 0);
        Preconditions.checkArgument(ofs + len <= bits.length);

        int t = v;
        for (int i = 0; i < len; ++i) {
            bits[ofs + len - 1 - i] = (byte) (t & 1);
            t >>>= 1;
        }
    }

    private static int bitsToInt(byte[] bits, int ofs, int len) {
        Preconditions.checkArgument(ofs >= 0);
        Preconditions.checkArgument(len >= 0);
        Preconditions.checkArgument(ofs + len <= bits.length);
        Preconditions.checkArgument(len <= 32);

        int v = 0;
        for (int i = 0; i < len; ++i) {
            int b = bits[ofs + i];
            Verify.verify(b == 0 || b == 1);
            v = (v << 1) | b;
        }

        return v;
    }

    private static void insert(byte[] array, int ofs, byte value) {
        Preconditions.checkArgument(ofs >= 0);
        Preconditions.checkArgument(ofs < array.length);

        for (int i = array.length - 1; i > ofs; --i) {
            array[i] = array[i - 1];
        }

        array[ofs] = value;
    }

    private static void delete(byte[] array, int ofs, int len) {
        Preconditions.checkArgument(ofs >= 0);
        Preconditions.checkArgument(len >= 0);
        Preconditions.checkArgument(ofs + len <= array.length);

        int n = array.length - (ofs + len);
        for (int i = 0; i < n; ++i) {
            array[ofs + i] = array[ofs + len + i];
        }
    }

    private static void copy(byte[] src, int srcOfs, int len, byte[] dst, int dstOfs) {
        Preconditions.checkArgument(srcOfs >= 0);
        Preconditions.checkArgument(len >= 0);
        Preconditions.checkArgument(dstOfs >= 0);
        Preconditions.checkArgument(srcOfs + len <= src.length);
        Preconditions.checkArgument(dstOfs + len <= dst.length);
        Preconditions.checkArgument(src != dst);

        for (int i = 0; i < len; ++i) {
            dst[dstOfs + i] = src[srcOfs + i];
        }
    }

    private static final class Initializer {
        static final List<String> WORD_LIST = loadBipWords();

        static final Map<String, Integer> WORD_MAP;

        static {
            Map<String, Integer> map = new LinkedHashMap<>();
            for (int i = 0; i < WORD_LIST.size(); ++i) {
                String word = WORD_LIST.get(i);
                map.put(word, i);
            }
            WORD_MAP = ImmutableMap.copyOf(map);
        }

        static final int[] CHECKBIT_POSES = calcCheckbitPoses();

        private Initializer() {
        }

        private static List<String> loadBipWords() {
            URL url = Resources.getResource(MnemonicEncoder.class, "/bip0039-english.txt");
            List<String> words;
            try {
                words = Resources.readLines(url, Charsets.US_ASCII);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }

            for (String word : words) {
                Verify.verify(word.matches("[a-z]+"), "[%s]", word);
            }

            List<String> sorted = new ArrayList<>(words);
            Collections.sort(sorted);
            Verify.verify(sorted.equals(words), "Invalid word order");

            return ImmutableList.copyOf(words);
        }

        private static int[] calcCheckbitPoses() {
            int[] poses = new int[8];

            for (int i = 0; i < 8; ++i) {
                byte[] bytes = ("checkbit_" + i).getBytes(Charsets.US_ASCII);
                byte[] digest = DigestUtils.sha256(bytes);
                poses[i] = digest[0] & 0x1F;
            }

            return poses;
        }
    }
}

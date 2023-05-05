package com.github.jesse.l2cache.util;


public class HexCode {
    private static final char[] UPPER_HEX_CHARS =
            {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};

    private static final char[] LOWER_HEX_CHARS =
            {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};

    public static char[] encode2char(byte[] bytes, boolean lowercase) {
        char[] chars = new char[bytes.length * 2];
        for (int i = 0; i < chars.length; i = i + 2) {
            byte b = bytes[i / 2];

            char[] HEX_CHARS = LOWER_HEX_CHARS;
            if (!lowercase) {
                HEX_CHARS = UPPER_HEX_CHARS;
            }

            chars[i] = HEX_CHARS[(b >>> 0x4) & 0xf];
            chars[i + 1] = HEX_CHARS[b & 0xf];
        }

        return chars;
    }

    public static String encode(byte[] bytes, boolean lowercase) {
        char[] endata = encode2char(bytes, lowercase);
        return new String(endata);
    }

    public static byte[] decode(String data) {
        int len = (data.length() + 1) / 2;
        byte[] bytes = new byte[len];
        int index = 0;
        for (int i = 1; i < data.length(); i += 2) {
            char h = data.charAt(i - 1);
            char l = data.charAt(i);

            bytes[index] = decodeByte(h, l);
            index++;
        }

        return bytes;
    }

    public static byte decodeByte(char hight, char low) {
        byte data = 0;
        if (hight >= 'A' && hight <= 'F') {
            int value = hight - 'A' + 10;
            data = (byte) (value << 4);
        } else if (hight >= 'a' && hight <= 'f') {
            int value = hight - 'a' + 10;
            data = (byte) (value << 4);
        } else if (hight >= '0' && hight <= '9') {
            int value = hight - '0';
            data = (byte) (value << 4);
        } else {
            throw new IllegalArgumentException("非法参数");
        }

        if (low >= 'A' && low <= 'F') {
            int value = low - 'A' + 10;
            data |= (byte) value;
        } else if (low >= 'a' && low <= 'f') {
            int value = low - 'a' + 10;
            data |= (byte) value;
        } else if (low >= '0' && low <= '9') {
            int value = low - '0';
            data |= (byte) value;
        } else {
            throw new IllegalArgumentException("非法参数");
        }

        return data;
    }
}

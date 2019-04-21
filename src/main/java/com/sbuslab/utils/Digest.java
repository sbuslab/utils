package com.sbuslab.utils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.Hex;


public class Digest {

    public static String md5(String message) {
        return md5(message.getBytes());
    }

    public static String md5(byte[] message) {
        return digest("MD5", message);
    }

    public static String sha128(String message) {
        return sha256(message.getBytes());
    }

    public static String sha128(byte[] message) {
        return digest("SHA-128", message);
    }

    public static String sha256(String message) {
        return sha256(message.getBytes());
    }

    public static String sha256(byte[] message) {
        return digest("SHA-256", message);
    }

    public static String hMacSHA256(final String message, final String key) throws InvalidKeyException, NoSuchAlgorithmException {
        return hex(hmac("HmacSHA256", message, key));
    }

    public static byte[] hmac(final String algorithm, final String message, final String key) throws InvalidKeyException, NoSuchAlgorithmException {
        final Mac hmac = Mac.getInstance(algorithm);
        hmac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), algorithm));

        return hmac.doFinal(message.getBytes(StandardCharsets.UTF_8));
    }

    public static String hex(final byte[] data) {
        return Hex.encodeHexString(data);
    }

    public static String encodeBase64(final byte[] data) {
        return Base64.encodeBase64String(data);
    }

    public static byte[] decodeBase64(final String data) {
        return Base64.decodeBase64(data);
    }

    private static String digest(String digestName, byte[] message) {
        try {
            return toHexString(MessageDigest.getInstance(digestName).digest(message));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalArgumentException(e);
        }
    }

    private static String toHexString(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();

        for (int i = 0; i < bytes.length; i++) {
            String hex = Integer.toHexString(0xFF & bytes[i]);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }

        return hexString.toString();
    }
}

package com.sbuslab.utils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

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
        final Mac hmac = Mac.getInstance("HmacSHA256");
        hmac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));

        return new String(Hex.encodeHex(hmac.doFinal(message.getBytes(StandardCharsets.UTF_8))));
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

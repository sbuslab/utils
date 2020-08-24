package com.sbuslab.utils;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;
import java.util.UUID;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.Hex;
import org.bouncycastle.jcajce.provider.util.BadBlockException;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import com.sbuslab.utils.crypto.AesPbkdf2;


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

    public static String hMacSHA384(final String message, final String key) throws InvalidKeyException, NoSuchAlgorithmException {
        return hex(hmac("HmacSHA384", message, key));
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

    public static String decryptAes(String secret, String data) {
        byte[] cipherData = java.util.Base64.getDecoder().decode(data);
        byte[] saltData = Arrays.copyOfRange(cipherData, 8, 16);

        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            final byte[][] keyAndIV = generateKeyAndIV(32, 16, 1, saltData, secret.getBytes(StandardCharsets.UTF_8), md5);
            SecretKeySpec key = new SecretKeySpec(keyAndIV[0], "AES");
            IvParameterSpec iv = new IvParameterSpec(keyAndIV[1]);

            byte[] encrypted = Arrays.copyOfRange(cipherData, 16, cipherData.length);
            Cipher aesCBC = Cipher.getInstance("AES/CBC/PKCS5Padding");
            aesCBC.init(Cipher.DECRYPT_MODE, key, iv);
            byte[] decryptedData = aesCBC.doFinal(encrypted);

            return new String(decryptedData, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static String encryptAes(String secret, String text) {
        try {
            final String Transformation = "AES/CBC/PKCS5Padding";
            final byte[] SaltedPrefix = "Salted__".getBytes(StandardCharsets.ISO_8859_1);
            final int SaltSize = 8;

            MessageDigest md5 = MessageDigest.getInstance("MD5");
            final byte[] salt = Arrays.copyOf(UUID.randomUUID().toString().getBytes(StandardCharsets.UTF_8), SaltSize);
            byte[][] keyAndIv = generateKeyAndIV(32, 16, 1, salt, secret.getBytes(StandardCharsets.UTF_8), md5);
            SecretKeySpec key = new SecretKeySpec(keyAndIv[0], "AES");
            IvParameterSpec iv = new IvParameterSpec(keyAndIv[1]);

            Cipher cipher = Cipher.getInstance(Transformation);
            cipher.init(Cipher.ENCRYPT_MODE, key, iv);

            byte[] cipherData = cipher.doFinal(text.getBytes(StandardCharsets.UTF_8));

            byte[] saltedCipherData = new byte[SaltedPrefix.length + SaltSize + cipherData.length];
            ByteBuffer buf = ByteBuffer.wrap(saltedCipherData);
            buf.put(SaltedPrefix).put(salt).put(cipherData);

            return java.util.Base64.getEncoder().encodeToString(saltedCipherData);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static String encryptAesGcm(String secret, String text) {
        return AesPbkdf2.encrypt(secret, text);
    }

    public static String decryptAesGcm(String secret, String text) {
        return AesPbkdf2.decrypt(secret, text);
    }

    public static GeneratedKeys generateRsaKeyPair() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048, new SecureRandom());
            KeyPair pair = generator.generateKeyPair();

            return new GeneratedKeys(encodeBase64(pair.getPublic().getEncoded()), encodeBase64(pair.getPrivate().getEncoded()));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static String encryptRsa(String publicKeyString, String text) {
        try {
            final byte[] aesKeyBytes = new byte[32];
            SecureRandom random = SecureRandom.getInstanceStrong();
            random.nextBytes(aesKeyBytes);

            String aesEncryptedText = encryptAesGcm(new String(aesKeyBytes, StandardCharsets.UTF_8), text);

            PublicKey publicKey = KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(decodeBase64(publicKeyString)));

            Security.addProvider(new BouncyCastleProvider());
            Cipher rsaCipher = Cipher.getInstance("RSA/NONE/OAEPWithSHA256AndMGF1Padding", "BC");
            rsaCipher.init(Cipher.ENCRYPT_MODE, publicKey);
            byte[] cipheredAesKey = rsaCipher.doFinal(Base64.encodeBase64(aesKeyBytes));

            return encodeBase64(cipheredAesKey) + "\n" + aesEncryptedText;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static String decryptRsa(String privateKeyString, String text) throws Exception {
        PrivateKey privateKey = KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(decodeBase64(privateKeyString)));

        try {
            String[] parts = text.split("\n");

            Security.addProvider(new BouncyCastleProvider());
            Cipher rsaCipher = Cipher.getInstance("RSA/NONE/OAEPWithSHA256AndMGF1Padding", "BC");
            rsaCipher.init(Cipher.DECRYPT_MODE, privateKey);

            byte[] aesKey = rsaCipher.doFinal(Base64.decodeBase64(parts[0]));

            if (parts.length != 2) {
                throw new IllegalArgumentException("Illegal encrypted text");
            }

            return decryptAesGcm(new String(Base64.decodeBase64(aesKey), StandardCharsets.UTF_8), parts[1]);

        } catch (BadBlockException error) {
            try {
                Cipher oldRsaCipher = Cipher.getInstance("RSA");
                oldRsaCipher.init(Cipher.DECRYPT_MODE, privateKey);

                return new String(oldRsaCipher.doFinal(decodeBase64(text)), StandardCharsets.UTF_8);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static String crc16(byte[] bytes) {
        int crc = 0xFFFF;
        int polynomial = 0x1021;

        for (byte b : bytes) {
            for (int i = 0; i < 8; i++) {
                boolean bit = ((b >> (7 - i) & 1) == 1);
                boolean c15 = ((crc >> 15 & 1) == 1);
                crc <<= 1;
                if (c15 ^ bit) crc ^= polynomial;
            }
        }

        crc &= 0xffff;
        return Integer.toHexString(crc);
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

    private static byte[][] generateKeyAndIV(int keyLength, int ivLength, int iterations, byte[] salt, byte[] password, MessageDigest md) {

        int digestLength = md.getDigestLength();
        int requiredLength = (keyLength + ivLength + digestLength - 1) / digestLength * digestLength;
        byte[] generatedData = new byte[requiredLength];
        int generatedLength = 0;

        try {
            md.reset();

            // Repeat process until sufficient data has been generated
            while (generatedLength < keyLength + ivLength) {

                // Digest data (last digest if available, password data, salt if available)
                if (generatedLength > 0)
                    md.update(generatedData, generatedLength - digestLength, digestLength);
                md.update(password);
                if (salt != null)
                    md.update(salt, 0, 8);
                md.digest(generatedData, generatedLength, digestLength);

                // additional rounds
                for (int i = 1; i < iterations; i++) {
                    md.update(generatedData, generatedLength, digestLength);
                    md.digest(generatedData, generatedLength, digestLength);
                }

                generatedLength += digestLength;
            }

            // Copy key and IV into separate byte arrays
            byte[][] result = new byte[2][];
            result[0] = Arrays.copyOfRange(generatedData, 0, keyLength);
            if (ivLength > 0)
                result[1] = Arrays.copyOfRange(generatedData, keyLength, keyLength + ivLength);

            return result;

        } catch (DigestException e) {
            throw new RuntimeException(e);

        } finally {
            // Clean out temporary data
            Arrays.fill(generatedData, (byte) 0);
        }
    }

    public static class GeneratedKeys {
        private String publicKey;
        private String privateKey;

        public GeneratedKeys(String pbKey, String pvKey) {
            publicKey = pbKey;
            privateKey = pvKey;
        }

        public String getPublicKey() {
            return publicKey;
        }

        public String getPrivateKey() {
            return privateKey;
        }
    }

}

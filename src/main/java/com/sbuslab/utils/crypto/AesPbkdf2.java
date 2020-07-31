package com.sbuslab.utils.crypto;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Arrays;


public class AesPbkdf2 {

    private static final int SALT_SIZE  = 16;
    private static final int IV_SIZE    = 12;
    private static final int ITERATIONS = 100000;

    public static String encrypt(String secret, String text) {
        try {
            final byte[] salt = new byte[SALT_SIZE];
            SecureRandom random = SecureRandom.getInstanceStrong();
            random.nextBytes(salt);

            SecretKey key = new SecretKeySpec(
                SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                    .generateSecret(new PBEKeySpec(secret.toCharArray(), salt, ITERATIONS, 256)).getEncoded(), "AES");

            final byte[] iv = new byte[IV_SIZE];
            random.nextBytes(iv);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(128, iv));

            byte[] encrypted = cipher.doFinal(text.getBytes(StandardCharsets.UTF_8));

            ByteBuffer buf = ByteBuffer.allocate(SALT_SIZE + IV_SIZE + encrypted.length);
            buf.put(salt).put(iv).put(encrypted);

            return java.util.Base64.getEncoder().encodeToString(buf.array());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static String decrypt(String secret, String data) {
        try {
            byte[] encrypted = java.util.Base64.getDecoder().decode(data);
            byte[] salt = Arrays.copyOfRange(encrypted, 0, SALT_SIZE);
            byte[] iv = Arrays.copyOfRange(encrypted, SALT_SIZE, SALT_SIZE + IV_SIZE);

            SecretKey key = new SecretKeySpec(
                SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                    .generateSecret(new PBEKeySpec(secret.toCharArray(), salt, ITERATIONS, 256)).getEncoded(), "AES");

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(128, iv));

            byte[] decrypted = cipher.doFinal(Arrays.copyOfRange(encrypted, SALT_SIZE + IV_SIZE, encrypted.length));
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (javax.crypto.AEADBadTagException e) {
            throw new RuntimeException("AES: incorrect password", e);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}

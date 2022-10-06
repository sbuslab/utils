package com.sbuslab.utils;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;
import java.util.UUID;

import lombok.val;
import net.i2p.crypto.eddsa.EdDSAPrivateKey;
import net.i2p.crypto.eddsa.EdDSAPublicKey;
import net.i2p.crypto.eddsa.EdDSASecurityProvider;
import net.i2p.crypto.eddsa.spec.EdDSANamedCurveTable;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.Hex;
import org.bouncycastle.asn1.x9.X9ECParameters;
import org.bouncycastle.crypto.ec.CustomNamedCurves;
import org.bouncycastle.crypto.params.ECDomainParameters;
import org.bouncycastle.jcajce.provider.asymmetric.ec.BCECPrivateKey;
import org.bouncycastle.jcajce.provider.asymmetric.ec.BCECPublicKey;
import org.bouncycastle.jcajce.provider.util.BadBlockException;
import org.bouncycastle.jce.ECNamedCurveTable;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.math.ec.ECPoint;
import org.bouncycastle.math.ec.FixedPointCombMultiplier;

import com.sbuslab.utils.crypto.AesPbkdf2;


public class Digest {

    static {
        Security.addProvider(new BouncyCastleProvider());
        Security.addProvider(new EdDSASecurityProvider());
    }

    public static String md5(String message) {
        return md5(message.getBytes());
    }

    public static String md5(byte[] message) {
        return digest("MD5", message);
    }

    public static String sha128(String message) {
        return sha128(message.getBytes());
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

    public static String hMacSHA512(final String message, final String key) throws InvalidKeyException, NoSuchAlgorithmException {
        return hex(hmac("HmacSHA512", message, key));
    }

    public static byte[] hmac(final String algorithm, final String message, final String key) throws InvalidKeyException, NoSuchAlgorithmException {
        final Mac hmac = Mac.getInstance(algorithm);
        hmac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), algorithm));

        return hmac.doFinal(message.getBytes(StandardCharsets.UTF_8));
    }

    public static String hex(final byte[] data) {
        return Hex.encodeHexString(data);
    }

    public static byte[] unhex(final String hex) throws DecoderException {
        return Hex.decodeHex(hex);
    }

    public static String encodeBase64(final byte[] data) {
        return Base64.encodeBase64String(data);
    }

    public static String encodeBase64UrlSafe(final byte[] data) {
        return Base64.encodeBase64URLSafeString(data);
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

    @Deprecated
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

    private static byte[] digestBytes(String digestName, byte[] message) {
        try {
            return MessageDigest.getInstance(digestName).digest(message);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalArgumentException(e);
        }
    }

    private static String digest(String digestName, byte[] message) {
        return toHexString(digestBytes(digestName, message));
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

    public static GeneratedKeys generateEdDSAKeyPair() {
        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("EdDSA", "EdDSA");
            keyPairGenerator.initialize(EdDSANamedCurveTable.getByName("Ed25519"), SecureRandom.getInstanceStrong());

            KeyPair pair = keyPairGenerator.generateKeyPair();

            EdDSAPublicKey publicKey = (EdDSAPublicKey) pair.getPublic();
            EdDSAPrivateKey privateKey = (EdDSAPrivateKey) pair.getPrivate();
            return new GeneratedKeys(hex(publicKey.getAbyte()), hex(privateKey.getSeed()));
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
    }

    public static GeneratedKeys generateECDSAKeyPair() {
        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("ECDSA", "BC");
            keyPairGenerator.initialize(ECNamedCurveTable.getParameterSpec("secp256k1"), SecureRandom.getInstanceStrong());

            KeyPair pair = keyPairGenerator.generateKeyPair();

            BCECPublicKey publicKey = (BCECPublicKey) pair.getPublic();
            BCECPrivateKey privateKey = (BCECPrivateKey) pair.getPrivate();

            return new GeneratedKeys(hex(publicKey.getQ().getEncoded(true)), hex(bigIntegerToBytes(privateKey.getD(), 32)));
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
    }

    public static GeneratedKeys getECDSAKeyPairFromPrivate(String privateKey) {
        try {
            BigInteger privKey = new BigInteger(1, Hex.decodeHex(privateKey));

            /*
                * TODO: FixedPointCombMultiplier currently doesn't support scalars longer than the group
                * order, but that could change in future versions.
            */
            if (privKey.bitLength() > ECDSA_CURVE_PARAMS.getN().bitLength()) {
                privKey = privKey.mod(ECDSA_CURVE_PARAMS.getN());
            }

            ECPoint point = new FixedPointCombMultiplier().multiply(ECDSA_CURVE_PARAMS.getG(), privKey);

            return new GeneratedKeys(hex(point.getEncoded(true)), privateKey);
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
    }

    private static byte[] bigIntegerToBytes(BigInteger b, int numBytes) {
        byte[] src = b.toByteArray();
        byte[] dest = new byte[numBytes];
        boolean isFirstByteOnlyForSign = src[0] == 0;
        int length = isFirstByteOnlyForSign ? src.length - 1 : src.length;
        int srcPos = isFirstByteOnlyForSign ? 1 : 0;
        int destPos = numBytes - length;
        System.arraycopy(src, srcPos, dest, destPos, length);
        return dest;
    }

    /**
        * 160 bits bitcoin hash, used mostly for address encoding
        * hash160(input) = RIPEMD160(SHA256(input))
        *
        * @param input array of byte
        * @return the 160 bits BTC hash of input
    */
    public static byte[] hash160(byte[] input) {
        return digestBytes("RIPEMD160", digestBytes("SHA256", input));
    }

    private static final X9ECParameters ECDSA_CURVE_PARAMS = CustomNamedCurves.getByName("secp256k1");

    private static final ECDomainParameters ECDSA_CURVE =
            new ECDomainParameters(
                    ECDSA_CURVE_PARAMS.getCurve(),
                    ECDSA_CURVE_PARAMS.getG(),
                    ECDSA_CURVE_PARAMS.getN(),
                    ECDSA_CURVE_PARAMS.getH());

}

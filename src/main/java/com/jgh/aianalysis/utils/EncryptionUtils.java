package com.jgh.aianalysis.utils;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

public class EncryptionUtils {
    // 添加BouncyCastle作为安全提供者，提供更强大的加密算法支持
    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    // AES加密配置
    private static final String AES_ALGORITHM = "AES/CBC/PKCS7Padding";  // 使用CBC模式和PKCS7填充
    private static final int AES_KEY_SIZE = 256;  // 使用256位密钥提供更强的安全性
    
    // RSA加密配置
    private static final String RSA_ALGORITHM = "RSA/ECB/PKCS1Padding";  // 使用PKCS1填充
    private static final int RSA_KEY_SIZE = 2048;  // 使用2048位密钥长度，提供足够的安全强度

    /**
     * 生成RSA密钥对
     * @return 包含公钥和私钥的密钥对
     * @throws Exception 生成过程中可能出现的异常
     */
    public static KeyPair generateRSAKeyPair() throws Exception {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(RSA_KEY_SIZE);
        return keyPairGenerator.generateKeyPair();
    }

    /**
     * 将密钥转换为Base64编码字符串，便于存储和传输
     * @param key 密钥
     * @return Base64编码的密钥字符串
     */
    public static String keyToString(Key key) {
        return Base64.getEncoder().encodeToString(key.getEncoded());
    }

    /**
     * 从Base64编码的字符串恢复RSA公钥
     * @param keyStr Base64编码的公钥字符串
     * @return RSA公钥对象
     * @throws Exception 转换过程中可能出现的异常
     */
    public static PublicKey stringToRSAPublicKey(String keyStr) throws Exception {
        byte[] keyBytes = Base64.getDecoder().decode(keyStr);
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        return keyFactory.generatePublic(keySpec);
    }

    /**
     * 从Base64编码的字符串恢复RSA私钥
     * @param keyStr Base64编码的私钥字符串
     * @return RSA私钥对象
     * @throws Exception 转换过程中可能出现的异常
     */
    public static PrivateKey stringToRSAPrivateKey(String keyStr) throws Exception {
        byte[] keyBytes = Base64.getDecoder().decode(keyStr);
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        return keyFactory.generatePrivate(keySpec);
    }

    /**
     * 生成随机AES密钥
     * @return AES密钥
     * @throws Exception 生成过程中可能出现的异常
     */
    public static SecretKey generateAESKey() throws Exception {
        KeyGenerator keyGen = KeyGenerator.getInstance("AES");
        keyGen.init(AES_KEY_SIZE);
        return keyGen.generateKey();
    }

    /**
     * 从Base64编码的字符串恢复AES密钥
     * @param keyStr Base64编码的AES密钥字符串
     * @return AES密钥对象
     */
    public static SecretKey stringToAESKey(String keyStr) {
        byte[] keyBytes = Base64.getDecoder().decode(keyStr);
        return new SecretKeySpec(keyBytes, "AES");
    }

    /**
     * 使用RSA公钥加密数据
     * @param data 待加密数据
     * @param publicKey RSA公钥
     * @return Base64编码的加密数据
     * @throws Exception 加密过程中可能出现的异常
     */
    public static String encryptWithRSA(String data, PublicKey publicKey) throws Exception {
        Cipher cipher = Cipher.getInstance(RSA_ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, publicKey);
        byte[] encryptedBytes = cipher.doFinal(data.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(encryptedBytes);
    }

    /**
     * 使用RSA私钥解密数据
     * @param encryptedData Base64编码的加密数据
     * @param privateKey RSA私钥
     * @return 解密后的原始数据
     * @throws Exception 解密过程中可能出现的异常
     */
    public static String decryptWithRSA(String encryptedData, PrivateKey privateKey) throws Exception {
        byte[] encryptedBytes = Base64.getDecoder().decode(encryptedData);
        Cipher cipher = Cipher.getInstance(RSA_ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, privateKey);
        byte[] decryptedBytes = cipher.doFinal(encryptedBytes);
        return new String(decryptedBytes, StandardCharsets.UTF_8);
    }

    /**
     * 使用AES密钥加密数据
     * @param data 待加密数据
     * @param secretKey AES密钥
     * @param iv 初始化向量
     * @return Base64编码的加密数据
     * @throws Exception 加密过程中可能出现的异常
     */
    public static String encryptWithAES(String data, SecretKey secretKey, byte[] iv) throws Exception {
        Cipher cipher = Cipher.getInstance(AES_ALGORITHM, "BC");
        IvParameterSpec ivSpec = new IvParameterSpec(iv);
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivSpec);
        byte[] encryptedBytes = cipher.doFinal(data.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(encryptedBytes);
    }

    /**
     * 使用AES密钥解密数据
     * @param encryptedData Base64编码的加密数据
     * @param secretKey AES密钥
     * @param iv 初始化向量
     * @return 解密后的原始数据
     * @throws Exception 解密过程中可能出现的异常
     */
    public static String decryptWithAES(String encryptedData, SecretKey secretKey, byte[] iv) throws Exception {
        byte[] encryptedBytes = Base64.getDecoder().decode(encryptedData);
        Cipher cipher = Cipher.getInstance(AES_ALGORITHM, "BC");
        IvParameterSpec ivSpec = new IvParameterSpec(iv);
        cipher.init(Cipher.DECRYPT_MODE, secretKey, ivSpec);
        byte[] decryptedBytes = cipher.doFinal(encryptedBytes);
        return new String(decryptedBytes, StandardCharsets.UTF_8);
    }

    /**
     * 生成随机初始化向量
     * @return 16字节的随机初始化向量
     */
    public static byte[] generateIV() {
        SecureRandom random = new SecureRandom();
        byte[] iv = new byte[16];  // AES使用16字节的初始化向量
        random.nextBytes(iv);
        return iv;
    }
}

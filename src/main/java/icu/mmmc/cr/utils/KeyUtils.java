package icu.mmmc.cr.utils;

import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

/**
 * 密钥工具类
 *
 * @author shouchen
 */
@SuppressWarnings("unused")
public final class KeyUtils {
    private static final String RSA = "RSA";
    private static final String SIGNATURE_ALGORITHM = "SHA256withRSA";

    /**
     * 生成RSA密钥对
     *
     * @param keySize 密钥长度
     * @return 密钥对
     * @throws Exception 如果发生错误
     */
    public static KeyPair genRSAKeyPair(int keySize) throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance(RSA);
        generator.initialize(keySize, new SecureRandom());
        return generator.generateKeyPair();
    }

    /**
     * 解析RSA公钥
     *
     * @param code X509公钥数据
     * @return 公钥
     */
    public static PublicKey getPubKeyByCode(byte[] code) throws Exception {
        KeyFactory keyFactory = KeyFactory.getInstance(RSA);
        X509EncodedKeySpec x509EncodedKeySpec = new X509EncodedKeySpec(code);
        return keyFactory.generatePublic(x509EncodedKeySpec);
    }

    /**
     * 解析RSA私钥
     *
     * @param code PKCS8私钥数据
     * @return 私钥
     */
    public static PrivateKey getPriKeyByCode(byte[] code) throws Exception {
        KeyFactory keyFactory = KeyFactory.getInstance(RSA);
        PKCS8EncodedKeySpec pkcs8EncodedKeySpec = new PKCS8EncodedKeySpec(code);
        return keyFactory.generatePrivate(pkcs8EncodedKeySpec);
    }

    /**
     * 验证RSA密钥对是否匹配
     *
     * @param publicKey  RSA公钥
     * @param privateKey RSA私钥
     * @return 结果
     */
    public static boolean checkKeyPair(PublicKey publicKey, PrivateKey privateKey) {
        try {
            Signature signature = Signature.getInstance(SIGNATURE_ALGORITHM);
            signature.initSign(privateKey);
            signature.update("hello".getBytes(StandardCharsets.UTF_8));
            byte[] sign = signature.sign();
            signature.initVerify(publicKey);
            signature.update("hello".getBytes(StandardCharsets.UTF_8));
            return signature.verify(sign);
        } catch (Exception e) {
            Logger.warn(e);
            return false;
        }
    }
}

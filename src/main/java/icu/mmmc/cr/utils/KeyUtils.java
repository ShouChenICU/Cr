package icu.mmmc.cr.utils;

import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;

/**
 * 密钥工具类
 *
 * @author shouchen
 */
public final class KeyUtils {
    private static final String RSA = "RSA";

    /**
     * 解析RSA公钥
     *
     * @param code 原始数据
     * @return 公钥
     */
    public static PublicKey getPubKeyByCode(byte[] code) {
        try {
            KeyFactory keyFactory = KeyFactory.getInstance(RSA);
            X509EncodedKeySpec x509EncodedKeySpec = new X509EncodedKeySpec(code);
            return keyFactory.generatePublic(x509EncodedKeySpec);
        } catch (Exception e) {
            Logger.warn(e);
            return null;
        }
    }
}

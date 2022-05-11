package icu.mmmc.cr.utils;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;

/**
 * 签名工具
 *
 * @author shouchen
 */
@SuppressWarnings("unused")
public final class SignUtils {
    private static final String SIGN_ALGORITHM = "SHA256withRSA";

    /**
     * 签名
     *
     * @param privateKey 私钥
     * @param dat        待签名数据
     * @return 数字签名
     * @throws Exception 签名失败
     */
    public static byte[] sign(PrivateKey privateKey, byte[] dat) throws Exception {
        Signature signature = Signature.getInstance(SIGN_ALGORITHM);
        signature.initSign(privateKey);
        signature.update(dat);
        return signature.sign();
    }

    /**
     * 验签
     *
     * @param publicKey 公钥
     * @param dat       数据
     * @param signature 数字签名
     * @return 验证通过返回true, 否则返回false
     * @throws Exception 签名异常
     */
    public static boolean verify(PublicKey publicKey, byte[] dat, byte[] signature) throws Exception {
        Signature sign = Signature.getInstance(SIGN_ALGORITHM);
        sign.initVerify(publicKey);
        sign.update(dat);
        return sign.verify(signature);
    }
}

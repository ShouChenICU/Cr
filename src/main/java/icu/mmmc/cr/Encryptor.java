package icu.mmmc.cr;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Random;

/**
 * 加解密器
 *
 * @author shouchen
 */
public class Encryptor {
    private static final String ENCRYPT_ALGORITHM = "AES";
    private static final String CIPHER_ALGORITHM = "AES/GCM/NoPadding";
    private static final int T_LEN = 128;
    private final Random random;
    private SecretKey aesKey;
    private Cipher encryptCipher;
    private Cipher decryptCipher;
    private byte[] encryptIV;
    private byte[] decryptIV;

    public Encryptor() throws Exception {
        SecureRandom secureRandom = new SecureRandom();
        random = new Random(secureRandom.nextLong());
        byte[] key = new byte[32];
        byte[] iv = new byte[16];
        Arrays.fill(key, (byte) 0);
        Arrays.fill(iv, (byte) 0);
        init(new SecretKeySpec(key, ENCRYPT_ALGORITHM), iv, Arrays.copyOf(iv, iv.length));
    }

    /**
     * 初始化
     *
     * @param aesKey    AES密钥
     * @param encryptIV 加密初始向量
     * @param decryptIV 解密初始向量
     */
    public void init(SecretKey aesKey, byte[] encryptIV, byte[] decryptIV) throws Exception {
        this.aesKey = aesKey;
        this.encryptIV = encryptIV;
        this.decryptIV = decryptIV;
        encryptCipher = Cipher.getInstance(CIPHER_ALGORITHM);
        decryptCipher = Cipher.getInstance(CIPHER_ALGORITHM);
    }

    /**
     * 更新密钥
     *
     * @param aesKey AES密钥
     */
    public void updateKey(SecretKey aesKey) {
        this.aesKey = aesKey;
    }

    /**
     * 加密数据
     *
     * @param dat 待加密的数据
     * @return 加密后的数据
     */
    public byte[] encrypt(byte[] dat) throws Exception {
        encryptCipher.init(Cipher.ENCRYPT_MODE, aesKey, new GCMParameterSpec(T_LEN, encryptIV));
        byte[] tmpIV = new byte[16];
        random.nextBytes(tmpIV);
        byte[] data = new byte[dat.length + 16];
        System.arraycopy(dat, 0, data, 0, dat.length);
        System.arraycopy(tmpIV, 0, data, data.length - 16, 16);
        try {
            return encryptCipher.doFinal(data);
        } finally {
            encryptIV = tmpIV;
        }
    }

    /**
     * 解密数据
     *
     * @param dat 待解密的数据
     * @return 解密后的数据
     */
    public byte[] decrypt(byte[] dat) throws Exception {
        decryptCipher.init(Cipher.DECRYPT_MODE, aesKey, new GCMParameterSpec(T_LEN, decryptIV));
        byte[] data = decryptCipher.doFinal(dat);
        System.arraycopy(data, data.length - 16, decryptIV, 0, 16);
        return Arrays.copyOf(data, data.length - 16);
    }
}

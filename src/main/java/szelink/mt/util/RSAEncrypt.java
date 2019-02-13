package szelink.mt.util;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * @author mt
 * 使用RSA非对称加密算法来进行密码加密
 * 无论是加密还是解密获得的byte[]都是原始的byte[]
 * 1.加密后的byte[](无论是使用公钥加密还是私钥加密),直接展示成字符串是乱码的,
 *      所以加密后的byte[]在传输或展示过程中建议使用base64编码后在进行
 * 2.解密后的byte[](无论是使用公钥解密还是私钥解密),不需要使用base64进行编解码
 *      直接new String(byte[]) 即可获取加密前的原始字符串
 * 3.私钥签名按照 情况 1 规则
 *
 */
public class RSAEncrypt {

    private static RSAEncrypt rsa = null;

    /**
     * 加密算法使用 RSA
     */
    private static final String KEY_ALGORITHM = "RSA";

    /**
     * 签名算法
     */
    private static final String SIGNATURE_ALGORITHM = "MD5withRSA";

    /**
     * 公钥键
     */
    private static final String PUBLIC_KEY = "publicKey";

    /**
     * 私钥键
     */
    private static final String PRIVATE_KEY = "privateKey";

    /**
     *
     */
    private static final int KEY_SIZE = 1 << 10;

    /**
     * RSA最大加密明文大小
     */
    private static final int MAX_ENCRYPT = 117;

    /**
     * RSA最大解密密文大小
     */
    private static final int MAX_DECRYPT = 1 << 7;

    /**
     * 存放公钥和私钥键值对
     * 公钥私钥先以utf-8编码为字符串,然后再以base64编码
     */
    private final Map<String,String> keys = new HashMap<>(2);

    /**
     * base64解密对象
     */
    private final Base64.Decoder decoder = Base64.getDecoder();

    /**
     * base64加密对象
     */
    private final Base64.Encoder encoder = Base64.getEncoder();

    private KeyFactory keyFactory;

    private RSAEncrypt() {
        init();
    }

    public static RSAEncrypt getInstance() {
        if (rsa == null) {
            synchronized (RSAEncrypt.class) {
                if (rsa == null) {
                    rsa = new RSAEncrypt();
                }
            }
        }
        return rsa;
    }

    private void init() {
        keyPair();
    }

    private void keyPair() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance(KEY_ALGORITHM);
            // 以当前时间戳作为种子
            String seed = Long.toString(System.currentTimeMillis());
            generator.initialize(KEY_SIZE, new SecureRandom(seed.getBytes()));
            KeyPair keyPair = generator.generateKeyPair();
            keys.put(PUBLIC_KEY, encoder.encodeToString(keyPair.getPublic().getEncoded()));
            keys.put(PRIVATE_KEY, encoder.encodeToString(keyPair.getPrivate().getEncoded()));
            // 初始化key工厂
            keyFactory = KeyFactory.getInstance(KEY_ALGORITHM);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }

    public void changeKeyPairs() {
        keyPair();
    }

    /**
     * 获取公钥(base64编码)
     * @return
     */
    public String publicKey() {
        return keys.get(PUBLIC_KEY);
    }

    /**
     * 获取私钥(base64编码)
     * @return
     */
    public String privateKey() {
        return keys.get(PRIVATE_KEY);
    }

    /**
     * 秘钥签名
     * @param source 待签名内容
     * @param pk base64编码的私钥
     * @return 数字签名字节数组
     */
    public byte[] sign(byte[] source, String pk) {
        if (source == null) {
            throw new NullPointerException("source to be signed can not be null");
        }
        return signByPrivateKey(source, pk);
    }


    /**
     * 公钥验证数字签名
     * @param source 数据
     * @param sign 数字签名
     * @param pk base64编码的公钥
     * @return 验证正确返回 true 否则返回false
     */
    public boolean verify(byte[] source, byte[] sign, String pk) {
        if (source == null) {
            throw new NullPointerException("source to be signed can not be null");
        }
        if (sign == null) {
            throw new NullPointerException("digital signature can not be null");
        }
        return verifyByPublicKey(source, pk, sign);
    }

    private boolean verifyByPublicKey(byte[] source, String publicKey, byte[] sign) {
        boolean verify = false;
        byte[] decodeKey = decoder.decode(publicKey);
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(decodeKey);
        PublicKey pk;
        Signature signature;
        try {
            pk = keyFactory.generatePublic(keySpec);
            signature = Signature.getInstance(SIGNATURE_ALGORITHM);
            signature.initVerify(pk);
            signature.update(source);
            verify = signature.verify(sign);
        } catch (GeneralSecurityException e) {
            e.printStackTrace();
        }
        return verify;
    }

    private byte[] signByPrivateKey(byte[] source, String privateKey) {
        byte[] afterSign = null;
        byte[] decodeKey = decoder.decode(privateKey);
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(decodeKey);
        PrivateKey pk;
        Signature signature;
        try {
            pk = keyFactory.generatePrivate(keySpec);
            signature = Signature.getInstance(SIGNATURE_ALGORITHM);
            signature.initSign(pk);
            signature.update(source);
            afterSign = signature.sign();
        } catch (GeneralSecurityException e) {
            e.printStackTrace();
        }
        return afterSign;
    }

    /**
     * 私钥加密数据
     * @param source 待加密的数据
     * @param pk base64编码的私钥
     * @return 私钥加密后的字节数组
     */
    public byte[] privateKeyEncrypt(byte[] source, String pk) {

        checkSource(source, Mode.ENCRYPT);
        return encryptByPrivateKey(source, pk);
    }

    /**
     * 公钥解密
     * @param source 带解密数据(非base64)
     * @param pk base64编码的公钥
     * @return 解密后的字节数组
     */
    public byte[] publicKeyDecrypt(byte[] source, String pk) {
        checkSource(source, Mode.DECRYPT);
        return decryptByPublicKey(source, pk);
    }

    /**
     *
     * @param source 经过base64编码的待解密内容
     * @param pk base64编码的公钥
     * @return 解密后的字节数组
     */
    public byte[] publicKeyDecrypt(String source, String pk) {
        byte[] buf = decoder.decode(source);
        return publicKeyDecrypt(buf, pk);
    }


    /**
     * 公钥加密
     * @param source 待加密数据
     * @param pk base64编码的公钥
     * @return 经过加密后的字节数组
     */
    public byte[] publicKeyEncrypt(byte[] source, String pk) {
        checkSource(source, Mode.DECRYPT);
        return encryptByPublicKey(source, pk);
    }


    /**
     * 私钥解密
     * @param source 待解密数据
     * @param pk base64编码的私钥
     * @return 经过解密后的字节数组
     */
    public byte[] privateKeyDecrypt(byte[] source, String pk) {
        checkSource(source, Mode.DECRYPT);
        return decryptByPrivateKey(source, pk);
    }

    /**
     * 私钥解密
     * @param source 经过base64编码的待解密内容
     * @param pk base64编码的私钥
     * @return 经过解密后的字节数组
     */
    public byte[] privateKeyDecrypt(String source, String pk) {
        byte[] buf = decoder.decode(source);
        return privateKeyDecrypt(buf, pk);
    }

    private byte[] encryptByPrivateKey(byte[] source, String privateKey) {
        byte[] afterEncrypt = null;
        byte[] keyBytes = decoder.decode(privateKey);
        PKCS8EncodedKeySpec KeySpec = new PKCS8EncodedKeySpec(keyBytes);
        PrivateKey pk;
        Cipher cipher;
        try {
            pk = keyFactory.generatePrivate(KeySpec);
            cipher = Cipher.getInstance(keyFactory.getAlgorithm());
            cipher.init(Cipher.ENCRYPT_MODE, pk);
            afterEncrypt = cipherData(source, cipher, Mode.ENCRYPT);
        } catch (GeneralSecurityException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return afterEncrypt;
    }

    private byte[] encryptByPublicKey(byte[] source, String publicKey) {
        byte[] afterEncrypt = null;
        byte[] decede = decoder.decode(publicKey);
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(decede);
        PublicKey pk;
        Cipher cipher;
        try {
            pk = keyFactory.generatePublic(keySpec);
            cipher = Cipher.getInstance(pk.getAlgorithm());
            cipher.init(Cipher.ENCRYPT_MODE, pk);
            afterEncrypt = cipherData(source, cipher, Mode.ENCRYPT);
        }  catch (GeneralSecurityException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return afterEncrypt;
    }

    private byte[] decryptByPublicKey(byte[] source, String publicKey) {
        byte[] afterDecrypt = null;
        byte[] decodeKey = decoder.decode(publicKey);
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(decodeKey);
        PublicKey pk;
        Cipher cipher;
        try {
            pk = keyFactory.generatePublic(keySpec);
            cipher = Cipher.getInstance(keyFactory.getAlgorithm());
            cipher.init(Cipher.DECRYPT_MODE, pk);
            afterDecrypt = cipherData(source, cipher, Mode.DECRYPT);
        } catch (GeneralSecurityException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return afterDecrypt;
    }

    private byte[] decryptByPrivateKey(byte[] source, String privateKey) {
        byte[] afterDecrypt = null;
        byte[] decode = decoder.decode(privateKey);
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(decode);
        PrivateKey pk;
        Cipher cipher;
        try {
            pk = keyFactory.generatePrivate(keySpec);
            cipher = Cipher.getInstance(keyFactory.getAlgorithm());
            cipher.init(Cipher.DECRYPT_MODE, pk);
            afterDecrypt = cipherData(source, cipher, Mode.DECRYPT);
        } catch (GeneralSecurityException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return afterDecrypt;
    }

    private byte[] cipherData(byte[] source, Cipher cipher, Mode mode) throws BadPaddingException, IllegalBlockSizeException, IOException {
        int max = mode.equals(Mode.ENCRYPT) ? MAX_ENCRYPT : MAX_DECRYPT;
        int length = source.length;
        int offset = 0;
        byte[] buf;
        int i = 0;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        while (length - offset > 0) {
            if (length - offset > max) {
                buf = cipher.doFinal(source, offset, max);
            } else {
                buf = cipher.doFinal(source, offset, length - offset);
            }
            out.write(buf, 0, buf.length);
            i++;
            offset = i * max;
        }
        byte[] data = out.toByteArray();
        out.close();
        return data;
    }

    private void checkSource(byte[] source, Mode mode) {

        String content = mode.equals(Mode.ENCRYPT) ? "encrypted" : "decrypted";
        if (source == null || source.length == 0) {
            throw new NullPointerException("source to be " + content + " can not be null");
        }
    }

    private enum Mode {
        /**
         * 加密模式
         */
        ENCRYPT,

        /**
         * 解密模式
         */
        DECRYPT,

        /**
         * 签名模式
         */
        SIGN


    }

    public static void main(String[] args) {
        RSAEncrypt rsa = RSAEncrypt.getInstance();
    }
}

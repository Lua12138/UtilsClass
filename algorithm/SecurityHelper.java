import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.zip.CRC32;

/**
 * 提供常用的信息摘要算法<br/>
 * 每次需要重新计算时，务必调用<code>reset()</code>方法，否则将在原有基础上计算。
 */
public class SecurityHelper {
    public enum Algorithms {
        MD2("MD2"), // messageDigest <- 0
        MD5("MD5"),
        SHA1("SHA-1"),
        SHA224("SHA-224"),
        SHA256("SHA-256"),
        SHA384("SHA-384"),
        SHA512("SHA-512"), // <- 6
        AES128("AES/CBC/PKCS5Padding"), // AlgorithmParameters <- 7
        Blowfish("Blowfish");

        private String msg;

        private Algorithms(String text) {
            this.msg = text;
        }
    }

    private MessageDigest messageDigest;
    private CRC32 crc32;

    /**
     * byte[]转换成16进制字符串
     *
     * @param bytes
     * @return 转换结果
     */
    public static String toHexStr(byte[] bytes) {
        final byte[] TABLE = {8, 4, 2, 1, 8, 4, 2, 1};
        final char[] TABLE2 = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};
        final byte TAG = (byte) 0x80;
        StringBuilder builder = new StringBuilder();
        for (byte b : bytes) {
            for (int j = 0; j < 2; j++) {
                byte re = 0;
                for (int i = 0; i < 4; i++) {
                    if ((byte) (b & TAG) == TAG) {
                        re += TABLE[i];
                    }
                    b <<= 1;
                }
                builder.append(TABLE2[re]);
            }
        }

        return builder.toString();
    }

    /**
     * long转换16进制字符串
     *
     * @param lng
     * @return 转换结果
     */
    public static String toHexStr(long lng) {
        byte[] bytes = new byte[4];
        bytes[3] = (byte) (lng & 0xff);
        bytes[2] = (byte) ((lng >> 8) & 0xff);
        bytes[1] = (byte) ((lng >> 16) & 0xff);
        bytes[0] = (byte) ((lng >> 24) & 0xff);
        return toHexStr(bytes);
    }

    /**
     * 重置状态
     */
    public void reset() {
        if (messageDigest != null)
            messageDigest.reset();
    }

    /**
     * @return 填充并返回散列结果
     */
    public byte[] digest() {
        return this.messageDigest.digest();
    }

    /**
     * 初始化散列对象
     *
     * @param digest
     */
    private void init(Algorithms digest) {
        try {
            if (this.messageDigest != null) {
                if (!this.messageDigest.getAlgorithm().equals(digest.msg))
                    this.messageDigest = MessageDigest.getInstance(digest.msg);
            } else
                this.messageDigest = MessageDigest.getInstance(digest.msg);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }

    /**
     * 计算散列值
     *
     * @param algorithms 散列算法
     * @param input
     * @param offset
     * @param len
     * @return 指定算法的散列值
     */
    public byte[] hash(Algorithms algorithms, byte[] input, int offset, int len) {
        return this.hash(algorithms, input, offset, len, true);
    }

    /**
     * 计算散列值
     *
     * @param algorithms 散列算法
     * @param input
     * @param offset
     * @param len
     * @param digest     是否立即填充，若不填充，则不返回散列结果
     * @return 若digest为true，则返回散列结果，否则为null
     */
    public byte[] hash(Algorithms algorithms, byte[] input, int offset, int len, boolean digest) {
        this.init(algorithms);
        this.messageDigest.update(input, offset, len);
        if (digest)
            return this.messageDigest.digest();
        return null;
    }

    /**
     * 计算散列值
     *
     * @param algorithms  散列算法
     * @param inputStream 输入值
     * @return 散列值
     * @throws IOException
     */
    public byte[] hash(Algorithms algorithms, InputStream inputStream) throws IOException {
        return this.hash(algorithms, inputStream, true);
    }

    /**
     * 计算散列值
     *
     * @param algorithms  散列算法
     * @param inputStream 输入值
     * @param digest      是否填充
     * @return 填充则返回散列值，否则null
     * @throws IOException
     */
    public byte[] hash(Algorithms algorithms, InputStream inputStream, boolean digest) throws IOException {
        this.init(algorithms);
        byte[] buffer = new byte[4096];
        int len;
        while (-1 != (len = inputStream.read(buffer, 0, buffer.length))) {
            this.messageDigest.update(buffer, 0, len);
        }
        if (digest)
            return this.messageDigest.digest();
        return null;
    }

    /**
     * 计算CRC32校验值
     *
     * @param input  输入值
     * @param offset
     * @param len
     * @param reset  是否重置，true则丢弃之前的计算结果
     * @return 计算结果
     */
    public long crc32(byte[] input, int offset, int len, boolean reset) {
        if (crc32 == null)
            crc32 = new CRC32();
        if (reset) crc32.reset();
        crc32.update(input, offset, len);
        return crc32.getValue();
    }

    /**
     * base64编码
     *
     * @param input 输入值
     * @return 结果
     */
    public byte[] base64encode(byte[] input) {
        return Base64.getEncoder().encode(input);
    }

    /**
     * base64解码
     *
     * @param input 输入值
     * @return 结果
     */
    public byte[] base64decode(byte[] input) {
        return Base64.getDecoder().decode(input);
    }

    /**
     * AES相关操作类，有状态，且非线程安全类。<br>
     * 使用方法：<br>
     * <pre>
     * SecurityHelper.AES aes = new SecurityHelper.AES();
     * aes.setIV(IV);
     * aes.setKey(KEY);
     * aes.setCurrentMode(SecurityHelper.AES.AES128_CBC_PCKS5);
     * aes.prepareEncrypt();  // decode to invoke <i>aes.prepareDecrypt();</i>
     * aes.update(BLOCK_1);
     * aes.update(BLOCK_2);
     * aes.update(BLOCK_3);
     * // ...ect.
     * aes.doFinal(LAST_BLOCK);
     *
     * </pre>
     */
    public static class AES {
        public final static String AES128_CBC_PCKS5 = "AES/CBC/PKCS5Padding";
        public final static String AES128_CBC_NOPADDING = "AES/CBC/NoPadding";
        public final static String AES128_ECB_NOPADDING = "AES/ECB/NoPadding";
        public final static String AES128_ECB_PCKS5 = "AES/ECB/PKCS5Padding";


        private Cipher cipher;
        private IvParameterSpec ivParameterSpec;
        private SecretKey secretKey;
        private String currentMode; // 当前模式与填充方式
        private int keySize; // 加密长度 默认128bit
        private KeyGenerator keyGenerator;

        public AES() {
            this.keySize = 128;
        }

        /**
         * 设置初始化向量
         *
         * @param iv
         */
        public void setIV(byte[] iv) {
            this.ivParameterSpec = new IvParameterSpec(iv);
        }

        /**
         * 设置密钥
         *
         * @param key
         */
        public void setKey(byte[] key) {
            this.secretKey = new SecretKeySpec(key, "AES");
        }

        /**
         * 设置加密长度，默认128位
         *
         * @param keySize
         */
        @Deprecated
        public void setKeySize(int keySize) {
            this.keySize = keySize;
        }

        /**
         * 设置当前的加密模式与填充方式
         *
         * @param currentMode
         */
        public void setCurrentMode(String currentMode) throws NoSuchPaddingException, NoSuchAlgorithmException {
            this.currentMode = currentMode;
            this.cipher = Cipher.getInstance(this.currentMode);
        }

        /**
         * 准备编码
         *
         * @throws InvalidAlgorithmParameterException
         * @throws InvalidKeyException
         */
        public void prepareEncrypt() throws InvalidAlgorithmParameterException, InvalidKeyException {
            this.cipher.init(Cipher.ENCRYPT_MODE, this.secretKey, this.ivParameterSpec);
        }

        /**
         * 准备解码
         *
         * @throws InvalidAlgorithmParameterException
         * @throws InvalidKeyException
         */
        public void prepareDecrypt() throws InvalidAlgorithmParameterException, InvalidKeyException {
            this.cipher.init(Cipher.DECRYPT_MODE, this.secretKey, this.ivParameterSpec);
        }

        public byte[] doFinal() throws BadPaddingException, IllegalBlockSizeException {
            return this.cipher.doFinal();
        }

        /**
         * 单块编码/解码，或结束多块编码/解码
         *
         * @param input
         * @param offset
         * @param len
         * @return
         * @throws BadPaddingException
         * @throws IllegalBlockSizeException
         */
        public byte[] doFinal(byte[] input, int offset, int len) throws BadPaddingException, IllegalBlockSizeException {
            return this.cipher.doFinal(input, offset, len);
        }

        /**
         * 多块编码解码
         *
         * @param input
         * @param offset
         * @param len
         * @return
         */
        public byte[] update(byte[] input, int offset, int len) {
            return this.cipher.update(input, offset, len);
        }
    }
}
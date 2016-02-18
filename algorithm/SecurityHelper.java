import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.io.InputStream;
import java.security.*;
import java.security.spec.InvalidParameterSpecException;
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
     * @param input 输入值
     * @return 结果
     */
    public byte[] base64encode(byte[] input) {
        return Base64.getEncoder().encode(input);
    }

    /**
     * base64解码
     * @param input 输入值
     * @return 结果
     */
    public byte[] base64decode(byte[] input) {
        return Base64.getDecoder().decode(input);
    }
}
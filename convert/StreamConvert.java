import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * 关于输入/输出流与文件/字符串之间的转换
 */
interface InputStreamCallback {
    void read(byte[] buffer, int len);
}

public class StreamConvert {
    protected static void inputStreamReader(InputStream inputStream, InputStreamCallback callback) throws IOException {
        byte[] buffer = new byte[4096];
        int len;
        while (-1 != (len = inputStream.read(buffer, 0, buffer.length))) {
            callback.read(buffer, len);
        }
    }

    public static String inputStreamToString(InputStream inputStream) throws IOException {
        StringBuilder builder = new StringBuilder();

        inputStreamReader(inputStream, (byte[] buffer, int len) -> builder.append(new String(buffer, 0, len)));

        return builder.toString();
    }

    public static byte[] inputStreamToBytes(InputStream inputStream) throws IOException {
        List<Byte> bytes = new ArrayList<>(1024 * 1024); // 1M
        inputStreamReader(inputStream, (byte[] buffer, int len) -> {
            for (int i = 0; i < len; i++)
                bytes.add(buffer[i]);
        });
        int size = bytes.size();
        byte[] result = new byte[size];
        for (int i = 0; i < size; i++)
            result[i] = bytes.get(i);

        return result;
    }

    public static String inputStreamToString(InputStream inputStream, String charset) throws IOException {
        InputStreamReader inputStreamReader = new InputStreamReader(inputStream, charset);
        char[] buffer = new char[4096];
        int len;
        StringBuilder builder = new StringBuilder();
        while (-1 != (len = inputStreamReader.read(buffer, 0, buffer.length))) {
            builder.append(buffer, 0, len);
        }
        return builder.toString();
    }

    @Deprecated
    public static OutputStream inputstreamToOutputStream(InputStream inputStream) throws IOException {
        OutputStream outputStream = new BufferedOutputStream(new ByteArrayOutputStream());
        inputStreamReader(inputStream, (byte[] buffer, int len) -> {
            try {
                outputStream.write(buffer, 0, len);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        return outputStream;
    }

    public static InputStream fileToInputStream(File file) throws FileNotFoundException {
        return new BufferedInputStream(new FileInputStream(file));
    }

    public static InputStream fileToInputStream(String file) throws FileNotFoundException {
        return fileToInputStream(new File(file));
    }
}
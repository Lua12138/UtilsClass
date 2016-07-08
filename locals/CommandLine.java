import java.io.IOException;
import java.io.InputStream;

/**
 * 提供允许系统命令行并提供返回值
 */
public class CommandLine {
    private static void readStream(InputStream inputStream, StringBuilder builder) {
        byte[] buff = new byte[4096];
        int len;
        try {
            while (-1 != (len = inputStream.read(buff, 0, buff.length))) {
                builder.append(new String(buff, 0, len));
            }
            inputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 同步执行指定的命令，并在标准输出流中输出日志信息
     *
     * @param command 命令
     * @return 执行结果
     */
    public static String run(String command) {
        return run(command, true);
    }

    /**
     * 同步执行指定的命令
     *
     * @param command 命令
     * @param log     是否输入日志
     * @return 执行结果
     */
    public static String run(String command, boolean log) {
        // StringBuilder 非线程安全，因此一旦传递给线程中，就不要再次使用
        // 若子线程与父线程需要同时使用，需使用StringBuffer，为线程安全
        StringBuilder builderInfo = new StringBuilder(); // 正常信息
        StringBuilder builderErr = new StringBuilder(); // 错误信息
        try {
            if (log) {
                System.out.println("Invoked -> " + command);
            }
            Process process = Runtime.getRuntime().exec(command);
            // 启动两个线程读取输入流与错误输入流，防止程序死锁
            Thread threadInfo = new Thread(() -> readStream(process.getInputStream(), builderInfo));
            // 读取错误信息
            Thread threadErr = new Thread(() -> readStream(process.getErrorStream(), builderErr));
            threadInfo.start();
            threadErr.start();
            int exitValue = process.waitFor();
            if (exitValue != 0) {
                System.out.println("Invoked Err ReturnValue -> " + exitValue);

                while (threadErr.getState() != Thread.State.TERMINATED)
                    Thread.sleep(50); //等待读取线程完毕
                System.out.println(builderErr.toString());
            }
            // 等待线程读取完毕
            while (threadInfo.getState() != Thread.State.TERMINATED)
                Thread.sleep(50);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return builderInfo.toString();
    }
}

import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 命令行参数解析
 * 要求JDK 1.8
 * 将代码中的注解和lambda表达式去掉，并且改写Stream操作则需要JDK 1.5即可
 */
//   TestCase / 测试用例:
///////////////////////////////
//    @Test
//    public void CommandLineParserTest() {
//        CommandLineParser parser = new CommandLineParser(new String[]{"-h", "needHelp", "-x", "Data1", "    Data2        ", "Data3", "-s", "NeedStop", "dsfsd", "xxxxx", "-a"});
//        parser.addHandler("h", "There is no helpful.", true) // 一般测试
//                .addHandler("x", templateParameters -> { //处理器测试
//                    Arrays.stream(templateParameters).forEach(System.err::println);
//                    return true;
//                })
//                .addHandler("s", "Yes, I'm trying stop", false) // 终止判断测试
//                .addHandler("a", "Do you see me", true, -1) // 优先级测试，这应该是第一条
//                .addHandler("-n", "Canot see me", true); // 应该看不到这条消息
//
//        Assert.assertFalse(parser.parse());
//    }
public class CommandLineParser {
    @FunctionalInterface // 该注解需要Java 8，以下版本去掉该注解
    public interface ParameterHandler {
        /**
         * 处理收到的参数
         *
         * @param parameter 接收到的命令行参数
         * @return 是否继续处理其他参数
         */
        boolean doHandler(String[] parameter);
    }

    private String[] args;
    private PriorityQueue<Map<String, Object>> templateParameters;
    private final String priorityIndexKey = "priorityIndexKey";
    private final String priorityParameterKey = "priorityParameterKey";
    private final String priorityHandlerKey = "priorityHandlerKey";
    private final String priorityGoOnKey = "priorityGoOnKey";
    private int priorityIndex;

    /**
     * @param args 命令行参数
     */
    public CommandLineParser(String[] args) {
        this.args = args;
        this.templateParameters = new PriorityQueue<>((o1, o2) -> (int) o1.get(priorityIndexKey) - (int) o2.get(priorityIndexKey));
    }

    /**
     * @param parameter 参数名
     * @param message   提示信息
     * @param goOn      是否继续处理
     * @return
     */
    public CommandLineParser addHandler(String parameter, String message, boolean goOn) {
        this.priorityIndex++;
        return this.addHandler(parameter, message, goOn, this.priorityIndex);
    }

    /**
     * @param parameter 参数名
     * @param message   提示信息
     * @param goOn      是否继续处理
     * @param priority  处理优先级
     * @return
     */
    public CommandLineParser addHandler(String parameter, String message, boolean goOn, int priority) {
        return this.addHandler(parameter, (Object) message, goOn, priority);
    }

    /**
     * @param parameter 参数名
     * @param handler   处理器
     * @return
     */
    public CommandLineParser addHandler(String parameter, ParameterHandler handler) {
        this.priorityIndex++;
        return this.addHandler(parameter, handler, this.priorityIndex);
    }

    /**
     * @param parameter 参数名
     * @param handler   处理器
     * @param priority  优先级
     * @return
     */
    public CommandLineParser addHandler(String parameter, ParameterHandler handler, int priority) {
        return this.addHandler(parameter, handler, true, priority);
    }

    /**
     * 真正添加到队列中
     *
     * @param parameter
     * @param handler   没有做类型检查，外部不应该调用，protected方便继承override
     * @param goOn      对于Handler来说，该参数无意义
     * @param priority
     * @return
     */
    protected CommandLineParser addHandler(String parameter, Object handler, boolean goOn, int priority) {
        Map<String, Object> p = new HashMap<>();
        p.put(this.priorityIndexKey, priority); // 添加优先级别
        p.put(this.priorityParameterKey, parameter);
        p.put(this.priorityHandlerKey, handler);
        p.put(this.priorityGoOnKey, goOn);
        this.templateParameters.add(p);
        return this;
    }

    /**
     * 解析
     *
     * @return 解析链是否全部完成，任何一个解析结果为false，则返回false
     */
    public boolean parse() {
        Pattern pattern = Pattern.compile("-(\\S+)(\\s+(.*?)(?=\\s-\\S|$))");
        Map<String, String[]> actualParameter = new HashMap<>();
        Matcher matcher = pattern.matcher(String.join(" ", this.args));
        while (matcher.find()) {
            actualParameter.put(matcher.group(1), matcher.group(3).split("\\s+"));
        }

        while (null != this.templateParameters.peek()) {
            Map<String, Object> templateParameter = this.templateParameters.poll();

                String parameter = (String) templateParameter.get(CommandLineParser.this.priorityParameterKey);
                if (actualParameter.get(parameter) != null) {
                    Object handler = templateParameter.get(CommandLineParser.this.priorityHandlerKey);
                    if (handler instanceof String) {
                        System.err.println((String) handler);
                        boolean goOn = (boolean) templateParameter.get(this.priorityGoOnKey);
                        if (!goOn) return false;
                    } else if (handler instanceof ParameterHandler) {
                        if (!((ParameterHandler) handler).doHandler(actualParameter.get(parameter))) return false;
                    } else {
                        throw new RuntimeException("Unknow CommandLine Handler.");
                    }
                }
        }
        return true;
    }
}

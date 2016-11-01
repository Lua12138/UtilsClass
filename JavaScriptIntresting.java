import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

// 最低需要Java 6支持，Java 8中对改变的引擎，速度有显著的提升
// 在JDK目录下新增一个jjs命令，输入为javascript字符串，效果等同于下面的代码（该命令Java8下存在，6/7未测试）
public class JavaScriptIntresting {
    private static final String script =
            "function add(a, b){" +
                    "  return a + b;" +
                    "}" +
                    "add(1024,2048);";

    public static void main(String[] args) throws ScriptException {
        ScriptEngineManager manager = new ScriptEngineManager();
        ScriptEngine engine = manager.getEngineByName("JavaScript");

        System.out.println(engine.eval(script)); // output -> 3072.0
    }
}

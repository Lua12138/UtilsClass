import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

// 最低需要Java 8支持
// 在JDK目录下新增一个jjs命令，输入为javascript字符串，效果等同于下面的代码
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

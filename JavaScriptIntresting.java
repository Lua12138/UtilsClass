import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

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

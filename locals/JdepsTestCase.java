import com.sun.tools.jdeps.JdepsHelper;
import org.junit.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class JdepsTestCase {


    protected File[] getClasspath(File baseFile) {
        final String baseUrl = "C:\\modules-2\\";
        File currentFile;
        if (baseFile == null)
            currentFile = new File(baseUrl);
        else
            currentFile = baseFile;
        List<File> result = new ArrayList<>();

        File[] subs = currentFile.listFiles(pathname -> pathname.isDirectory() || pathname.getName().endsWith(".jar"));
        for (File file : subs)
            if (file.isDirectory())
                Stream.of(this.getClasspath(file)).forEach(result::add);
            else result.add(file);

        return result.toArray(new File[result.size()]);
    }

    @Test
    public void invoke() {
        final String jar = "C:\\jdependency-1.1.jar"; // <- Jar files or Class files that need to be analyzed
        JdepsHelper helper = new JdepsHelper();
        File[] classpaths = this.getClasspath(null); // <- classpath
        for (File classpath : classpaths)
            helper.addClasspath(classpath);

        JdepsHelper.AnalyzeResult result = helper.analyze(jar);

        if (result.success()) {
            result.dependencyMap().entrySet().parallelStream().forEach(entry -> {
                System.err.printf("%s Dependency ->%n", entry.getKey().getName());
                entry.getValue().parallelStream()
                        .map(File::getName)
                        .peek(f -> System.err.print('\t'))
                        .forEach(System.err::println);
            });
        }
    }
}

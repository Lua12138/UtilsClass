package com.sun.tools.jdeps; // <- 必须使用这个包名 / Muse use the package name

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

import static com.sun.tools.jdeps.Analyzer.Type.SUMMARY;

/**
 * 用于分析包之间的依赖关系，基于JDK8工具制作。
 * 包名不能修改，必须是这个包。
 * 运行该类需要将%JAVA_HOME%/lib/tools.jar导入classpath
 * 该类用JDK自带的jdeps命令相同
 * 使用方法参考JdepsTestCase.java
 * <p>
 * Analysis of Jar or Class (file/package) dependencies.
 * Package name cannot be modified.
 * Under JDK8 test passed,
 * Need to import %JAVA_HOME%/lib/tools.jar to classpath
 * the standard reference JdepsTestCase.java
 */
public class JdepsHelper extends JdepsTask {
    public class AnalyzeResult {
        private String message;
        private int code;
        private Map<File, Set<File>> dependencyMap;

        public boolean success() {
            return this.code == 0;
        }

        public String message() {
            return message;
        }

        public Map<File, Set<File>> dependencyMap() {
            return dependencyMap;
        }
    }

    protected class MyVisitor implements Analyzer.Visitor {
        private Map<String, Set<String>> dependencyMap;

        public MyVisitor() {
            this.dependencyMap = new HashMap<>();
        }

        public Map<String, Set<String>> getDependencyMap() {
            return dependencyMap;
        }

        @Override
        public void visitDependence(String origin, Archive originArchive, String target, Archive targetArchive) {
            if (!PlatformClassPath.JDKArchive.isProfileArchive(targetArchive)) {
                Set<String> dependency = this.dependencyMap.get(originArchive.getPathName());
                if (dependency == null) {
                    dependency = new HashSet<>();
                    this.dependencyMap.put(originArchive.getPathName(), dependency);
                }
                dependency.add(targetArchive.getPathName());
            }
        }
    }

    protected Set<File> classpath;
    protected Object options;
    protected Object[] nullObject;

    public JdepsHelper() {
        this.classpath = new HashSet<>();
        this.nullObject = new Object[]{};
        this.options = this.getPrivateField("options");//new Options(this);
    }

    public JdepsHelper addClasspath(File classpath) {
        this.classpath.add(classpath);
        return this;
    }

    public JdepsHelper addClasspath(String classpath) {
        return this.addClasspath(new File(classpath));
    }

    public JdepsHelper removeClasspath(File classpath) {
        return this.removeClasspath(classpath.getAbsolutePath());
    }

    public JdepsHelper removeClasspath(String classpath) {
        this.classpath = this.classpath.parallelStream().filter(file -> !file.getAbsolutePath().equals(classpath)).collect(Collectors.toSet());
        return this;
    }

    public JdepsHelper cleanClasspath() {
        this.classpath.clear();
        return this;
    }

    protected String buildClasspath() {
        StringBuilder builder = new StringBuilder();
        for (File file : this.classpath)
            builder.append(file.getAbsolutePath()).append(';');

        return builder.toString().replaceAll(";$", "");
    }

    protected String[] buildArgs(String classOrJar) {
        List<String> args = new ArrayList<>();
        args.add("-s");
        args.add("-R");
        if (this.classpath.size() > 0) {
            args.add("-cp");
            args.add(this.buildClasspath());
        }
        args.add(classOrJar);

        return args.toArray(new String[args.size()]);
    }

    protected Object invokePrivateMethod(String methodName, Object[] patameters, Class<?>... parameterTypes) {
        try {
            Method method = JdepsTask.class.getDeclaredMethod(methodName, parameterTypes);
            method.setAccessible(true);
            return method.invoke(this, parameterTypes);
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
            e.printStackTrace();
        }
        return null;
    }

    protected Object getPrivateField(String fieldName) {
        return this.getPrivateField(fieldName, JdepsTask.class, this);
    }

    protected Object getPrivateField(String fieldName, Class<?> clazz, Object object) {
        try {

            Field field = clazz.getDeclaredField(fieldName);
            field.setAccessible(true);
            return field.get(object);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
        }
        return null;
    }

    protected void findDependencies() throws IOException {
        this.invokePrivateMethod("findDependencies", this.nullObject);
    }

    protected boolean isJDKArchive(Archive archive) {
        return (boolean) this.invokePrivateMethod("isJDKArchive", new Object[]{archive}, Archive.class);
    }

    protected void showReplacements(Analyzer analyzer) {
        this.invokePrivateMethod("showReplacements", new Object[]{analyzer}, Analyzer.class);
    }

    protected List<Archive> getSourceLocations() {
        return (List<Archive>) this.getPrivateField("sourceLocations");
    }

    protected Analyzer run0() throws IOException {
        // parse classfiles and find all dependencies
        this.findDependencies();

        Analyzer analyzer = new Analyzer((Analyzer.Type) this.getPrivateField("verbose", this.options.getClass(), this.options),
                (origin, originArchive, target, targetArchive) -> {
                    if ((boolean) JdepsHelper.this.getPrivateField("findJDKInternals", JdepsHelper.this.options.getClass(), JdepsHelper.this.options)) {
                        // accepts target that is JDK class but not exported
                        return isJDKArchive(targetArchive) &&
                                !((PlatformClassPath.JDKArchive) targetArchive).isExported(target.getClassName());
                    } else if ((boolean) JdepsHelper.this.getPrivateField("filterSameArchive", JdepsHelper.this.options.getClass(), JdepsHelper.this.options)) {
                        // accepts origin and target that from different archive
                        return originArchive != targetArchive;
                    }
                    return true;
                });

        // analyze the dependencies
        analyzer.run(this.getSourceLocations());

        return analyzer;
    }

    public AnalyzeResult analyze(String classOrJar) {
        AnalyzeResult result = new AnalyzeResult();
        try {
            this.handleOptions(this.buildArgs(classOrJar));
            Analyzer analyzer = this.run0();

            MyVisitor visitor1 = new MyVisitor();
            this.getSourceLocations().stream()
                    .filter(archive -> !archive.isEmpty())
                    .forEachOrdered(archive -> analyzer.visitDependences(archive, visitor1, SUMMARY));
            result.dependencyMap = new HashMap<>();

            result.dependencyMap = visitor1.getDependencyMap().entrySet().parallelStream()
                    .collect(Collectors.toMap(entry -> new File(entry.getKey()),
                            entry -> entry.getValue()
                                    .parallelStream()
                                    .map(File::new)
                                    .collect(Collectors.toSet())));

            result.code = 0;
        } catch (BadArgs badArgs) {
            badArgs.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }

    public AnalyzeResult analyze(File classOrJar) {
        return this.analyze(classOrJar.getAbsolutePath());
    }
}

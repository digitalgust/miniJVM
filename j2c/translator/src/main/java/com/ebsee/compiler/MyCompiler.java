/* SPDX-License-Identifier: MIT */

package com.ebsee.compiler;


import javax.tools.*;
import javax.lang.model.SourceVersion;
import java.io.File;
import java.util.*;

/**
 * @author gust
 */
public class MyCompiler {
    public static String srcFileEncode = "utf-8";

    public static final String JAVA_EXT = ".java";
    private static final Map<String, String> skippedSources = new LinkedHashMap<>();

    private static boolean accept(File file, String extension) {
        String name = file.getName();
        String base = name.substring(0, name.length() - extension.length());
        String reason = null;
        if (base.equals("package-info")) reason = "package metadata is not an executable AOT class";
        else if (base.equals("module-info")) reason = "Java modules are not supported by this runtime";
        else if (!SourceVersion.isIdentifier(base) || SourceVersion.isKeyword(base)) reason = "invalid Java class filename";
        if (reason == null) return true;
        skippedSources.put(file.getAbsolutePath(), reason);
        return false;
    }

    public static void printSkippedSources() {
        System.out.println("[j2c] Skipped unsupported source/class files: " + skippedSources.size());
        for (Map.Entry<String, String> entry : skippedSources.entrySet()) {
            System.out.println("[j2c] SKIP " + entry.getKey() + " : " + entry.getValue());
        }
    }

    /**
     * compiles a java source file with the given <code>fileName</code>
     */

    static public void compile(String srcPath, String outputPath) {
        skippedSources.clear();
        List<File> files = new ArrayList<>();
        String[] paths = srcPath.split(File.pathSeparator);
        for (String s : paths) {
            find(s, null, files, JAVA_EXT);
        }
        compileSources(files, outputPath);
    }

    /**
     * find files from root , file path save in paths, File save in files
     *
     * @param root
     * @param paths
     * @param files
     * @param fileExt
     */
    public static void find(String root, List<String> paths, List<File> files, String fileExt) {
        try {
            File dir = new File(root);

            if (dir.exists()) {
                File[] all = dir.listFiles();

                //
                if (all != null) {
                    for (File f : all) {
                        if (f.isFile() && f.getName().endsWith(fileExt)) {
                            if ((JAVA_EXT.equals(fileExt) || ".class".equals(fileExt)) && !accept(f, fileExt)) continue;
                            String classFile = f.getAbsolutePath();
                            if (paths != null) paths.add(classFile);
                            if (files != null) files.add(f);
                        } else {
                            find(f.getAbsolutePath(), paths, files, fileExt);
                        }
                    }
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }


    /**
     *
     */
    static public void compile(List<File> srcList, String outputPath) {
        skippedSources.clear();
        List<File> sources = new ArrayList<>();
        for (File source : srcList) {
            if (source.getName().endsWith(JAVA_EXT) && accept(source, JAVA_EXT)) sources.add(source);
        }
        compileSources(sources, outputPath);
    }

    private static void compileSources(List<File> srcList, String outputPath) {
//          //simp compiler
//        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
//        compiler.run(System.in, System.out, System.err,
//                "-encoding", "UTF-8", "-d", outputPath, files);

        /*

         * the compiler will send its messages to this listener

         */
        DiagnosticListener listener = new DiagnosticListener() {

            @Override
            public void report(Diagnostic diagnostic) {

//                System.out.println("compile: " + diagnostic);
//                System.out.println(diagnostic.getSource().toString());
//                System.out.println("line: " + diagnostic.getLineNumber());
                System.out.println("compile :" + diagnostic + "\n  " + diagnostic.getLineNumber());
            }
        };

        try {
            //getting the compiler object
            JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();

            if (compiler == null) {
                throw new IllegalStateException("Please run translator with JDK 1.8 instead of a JRE");
            }

            try (StandardJavaFileManager manager = compiler.getStandardFileManager(null, null, null)) {
                Iterable<? extends JavaFileObject> files = manager.getJavaFileObjectsFromFiles(srcList);
                Iterable<String> options = Arrays.asList("-encoding", srcFileEncode, "-d", outputPath, "-g");
                JavaCompiler.CompilationTask task = compiler.getTask(null, manager, listener, options, null, files);
                if (!Boolean.TRUE.equals(task.call())) {
                    throw new IllegalStateException("Java compilation failed; C conversion aborted");
                }
            }
        } catch (Exception e) {
            throw new IllegalStateException("Cannot compile Java sources", e);
        }
    }
}


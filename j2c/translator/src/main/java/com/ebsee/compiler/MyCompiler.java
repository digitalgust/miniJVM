package com.ebsee.compiler;


import javax.tools.*;
import java.io.File;
import java.util.*;

/**
 * @author gust
 */
public class MyCompiler {
    public static String srcFileEncode = "utf-8";

    public static final String JAVA_EXT = ".java";

    /**
     * compiles a java source file with the given <code>fileName</code>
     */

    static public void compile(String srcPath, String outputPath) {
        List<File> files = new ArrayList<>();
        String[] paths = srcPath.split(File.pathSeparator);
        for (String s : paths) {
            find(s, null, files, JAVA_EXT);
        }
        compile(files, outputPath);
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
                System.out.println("compile :" + diagnostic + "\n  " + diagnostic.getLineNumber() + "\n  " + diagnostic.getSource().toString());
            }
        };

        try {
            //getting the compiler object
            JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();

            if (compiler == null) {
                System.out.println("[ERROR]Please using jdk 1.8 run translator ,instead jre.");
                System.exit(1);
            }

            StandardJavaFileManager manager = compiler.getStandardFileManager(null, null, null);

            Iterable<? extends JavaFileObject> files = manager.getJavaFileObjectsFromFiles(srcList);

            Iterable options = Arrays.asList("-encoding", srcFileEncode, "-d", outputPath, "-g");

            JavaCompiler.CompilationTask task = compiler.getTask(null, manager, listener, options, null, files);

            // the compilation occures here
            task.call();
            manager.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}


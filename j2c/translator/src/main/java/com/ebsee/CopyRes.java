package com.ebsee;

import com.ebsee.j2c.Util;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;


/**
 *
 */
public class CopyRes {

    static List<String> jars = new ArrayList<>();


    public static void main(String[] args) throws IOException {
        String resPath = "../../minijvm/java/src/main/resource/"
//                + File.pathSeparator + "../../minijvm_test/src/main/resource/"//
                + File.pathSeparator + "../../extlib/xgui/src/main/resource"//
//                + File.pathSeparator + "../../mobile/java/ExApp/src/main/java"//
//                + File.pathSeparator + "../../../g3d/src/main/java/"//
                ;
        String outputPath = "../app/generted/res/";


        if (args.length < 2) {
            System.out.println("Posix :");
            System.out.println("require : jdk1.8");
            System.out.println("jar resource files and copy to dest dir:");
            System.out.println("java -cp ./translator/dist/translator.jar com.ebsee.CopyRes ./app/resource:./abc/resource  ./app/out/res/");
        } else {
            resPath = args[0] + "/";
            outputPath = args[2] + "/";
        }

        System.out.println("resource *.* path          : " + resPath);
        System.out.println("jar output path            : " + outputPath);

        File f;
        boolean res;
        f = new File(outputPath);
        System.out.println(f.getAbsolutePath() + (f.exists() ? " exists " : " not exists"));
        res = Util.deleteTree(f);
        res = f.mkdirs();

        long startAt = System.currentTimeMillis();
        jarAndCopy(resPath, outputPath);

        FileOutputStream fos=new FileOutputStream(outputPath+"/jars.c");
        BufferedWriter bw=new BufferedWriter(new OutputStreamWriter(fos));
        bw.write("void ");

        System.out.println("jar and copy success , cost :" + (System.currentTimeMillis() - startAt));
    }

    private static void jarAndCopy(String resPath, String outputPath) {
        try {
            String[] allres = resPath.split(File.pathSeparator);
            for (String p : allres) {
                File f = new File(p);
                if (f.exists()) {

                    String jarFileName = p.replace('\\', '_');
                    jarFileName = jarFileName.replace('/', '_');
                    jarFileName = jarFileName.replaceAll("\\.", "_");
                    while (jarFileName.indexOf("__") >= 0) {
                        jarFileName = jarFileName.replaceAll("__", "_");
                    }
                    String jarPath = outputPath + File.separator + jarFileName + ".jar";


                    jarfile(jarPath, f);


                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    public static void jarfile(String outputJarPath, File resDir) throws IOException {

        // 指定要打包的文件夹路径
        String sourceFolderPath = resDir.getCanonicalPath();

        // 指定输出的 JAR 文件路径
        //String outputJarPath = "/path/to/your/output.jar";

        try {
            // 构建打包命令
            String jarCommand = "jar -cf " + outputJarPath + " -C " + sourceFolderPath + " .";

            // 调用命令行执行 jar 命令
            Process process = Runtime.getRuntime().exec(jarCommand);

            // 获取并输出命令执行的结果
            int exitCode = process.waitFor();
            System.out.println("Command executed with exit code: " + exitCode + " " + outputJarPath);
            jars.add("/res/" + outputJarPath);

            // 如果有必要，你还可以读取进程的输出流和错误流
//            /*
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }
//            */

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static void jarfile1(String zipFilePath, File resDir) throws IOException {
        // 指定要创建的 Zip 文件路径
        //String zipFilePath = "example.zip";

        // 指定要添加到 Zip 文件的文件列表
        //String[] sourceFiles = {"file1.txt", "file2.txt", "file3.txt"};

        int parentPathLen = resDir.getCanonicalPath().length();

        List<File> sourceFiles = new ArrayList<>();
        listTreeFile(resDir.getCanonicalPath(), sourceFiles, null);

        try {
            // 创建 ZipOutputStream 对象，指定输出流为 Zip 文件
            FileOutputStream fos = new FileOutputStream(zipFilePath);
            ZipOutputStream zipOut = new ZipOutputStream(fos);

            // 遍历文件列表，将每个文件添加到 Zip 文件中
            for (File sourceFile : sourceFiles) {
                // 创建 ZipEntry 对象，表示 Zip 文件中的一个条目
                String entryPath = sourceFile.getPath();
                entryPath = entryPath.substring(parentPathLen);
                entryPath = entryPath.replace('\\', '/');

                ZipEntry zipEntry = new ZipEntry(entryPath);

                // 将 ZipEntry 添加到 ZipOutputStream
                zipOut.putNextEntry(zipEntry);

                // 将文件内容写入 Zip 文件
                FileInputStream fis = new FileInputStream(sourceFile);
                byte[] buffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = fis.read(buffer)) > 0) {
                    zipOut.write(buffer, 0, bytesRead);
                }
                fis.close();

                // 关闭当前 ZipEntry，表示完成添加一个文件
                zipOut.closeEntry();
            }

            // 关闭 ZipOutputStream
            zipOut.close();

            System.out.println("Zip 文件创建成功：" + zipFilePath);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public static void listTreeFile(String path, List<File> filePaths, FileFilter filter) {
        File f = new File(path);
        if (f.isFile()) {
            filePaths.add(f);
        } else {
            File[] files = f.listFiles(filter);
            if (files == null) return;
            for (int i = 0; i < files.length; i++) {
                File f1 = files[i];
                listTreeFile(f1.getAbsolutePath(), filePaths, filter);
            }
        }
    }

}



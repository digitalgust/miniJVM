package com.ebsee.classparser;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Open the user specified .class files or directories and perform the tasks
 * Handle the user inputs from the main class
 *
 * @author Deshan Dissanayake
 */

public class FileHelper {

    private List<String> fileList = new ArrayList<>();
    private File file;



    /**
     * List all files recursively
     */
    public void openFile (String[] files) {
        for (int i = 0; i < files.length; i++) {
            file = new File(files[i]);
            if (file.isFile()) {
                if (file.getName().contains(".class")) {
                    fileList.add(file.getPath());
                }
            } else if (file.isDirectory()){
                listFilesInFolder(file);
            }
        }
    }



    /**
     * List files in a directory.
     * This method will create a String array
     * which is similar to inputting list of .class files
     * in the command line
     */
    private void listFilesInFolder (File folder) {

        String filePath = "";
        String fileName = "";
        List<String> tempFileList = new ArrayList<>();

        for (File fileEntry : folder.listFiles()) {
            if (fileEntry.isDirectory()) {
                listFilesInFolder(fileEntry);
            } else {
                fileName = fileEntry.getName();
                if (fileName.contains(".class")) {
                    filePath = fileEntry.getPath();
                    tempFileList.add(filePath);
                } else {
                    continue;
                }
            }
        }

        String[] arr = tempFileList.toArray(new String[tempFileList.size()]);
        openFile(arr);
    }




    /**
     * =======================================================================
     * Getters and Setters
     * =======================================================================
     */

    public List<String> getFileList() {
        return fileList;
    }
}

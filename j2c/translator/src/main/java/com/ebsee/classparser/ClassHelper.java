package com.ebsee.classparser;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Open the user specified class and output the information
 *
 * @author Deshan Dissanayake
 */

public class ClassHelper {

    private static String IS_SUPER_CLASS = "java/lang/Object";
    private static String INVOKE_SPECIAL = "invokespecial";

    private FileHelper fileHelper;

    private String option;                      /* User option. should be either -m, -c, -o or -p */
    private String[] files;                       /* Holds user entered file(s)/directory(ies) */
    private List<String> fileList;                    /* Validated and extracted file list with their absolute paths */
    private ClassFile classFile;                   /* Holds all the data about a .class file */
    private List<ClassFile> classFileList;               /* A list of all ClassFile objects created during the runtime. i.e. all inputted classes */


    public ClassHelper(String option, String[] files) {
        this.option = option;
        this.files = files;

        fileHelper = new FileHelper();
        fileList = new ArrayList<>();
        classFileList = new ArrayList<>();
    }


    public void openClass(String file) {
        try {
            classFile = new ClassFile(file, option);
            classFileList.add(classFile);

        } catch (ClassFileParserException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Open and parse all the classes given
     */
    public void openClasses() {

        fileHelper.openFile(files);
        fileList = fileHelper.getFileList();

        for (Object o : fileList) {
            openClass(o.toString());

        }
    }

    public ClassFile getClassFile(String className) {
        for (ClassFile cf : classFileList) {

            if (cf.getThisClassName().equals(className)) {
                return cf;
            }
        }
        return null;
    }


    public List<ClassFile> getClassFileList() {
        return classFileList;
    }


}

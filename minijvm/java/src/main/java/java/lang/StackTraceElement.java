/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package java.lang;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author gust
 */
public class StackTraceElement {

    private Class declaringClass;
    private String methodName;
    private String fileName;
    private int lineNumber;

    StackTraceElement parent;

    /**
     * @return the declaringClass
     */
    Class getDeclaringClass() {
        return declaringClass;
    }

    public String getClassName() {
        return declaringClass.getName();
    }

    /**
     * @return the methodName
     */
    public String getMethodName() {
        return methodName;
    }

    /**
     * @return the fileName
     */
    public String getFileName() {
        return fileName;
    }

    /**
     * @return the lineNumber
     */
    public int getLineNumber() {
        return lineNumber;
    }

    public String toString() {

        StringBuilder stack = new StringBuilder();
        stack.append(getDeclaringClass());
        stack.append(".").append(getMethodName());
        stack.append("(").append(getFileName());
        stack.append(":").append(getLineNumber()).append(")");
        return stack.toString();

    }
}

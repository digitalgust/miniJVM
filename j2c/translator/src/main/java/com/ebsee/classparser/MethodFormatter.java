package com.ebsee.classparser;

import java.lang.reflect.*;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Deshan Dissanayake
 * <p>
 * Format method's Modifirer, Parameters and return types
 */
public class MethodFormatter {

    private String methodName;
    private int accessFlags;
    private String accessModifier;
    private String descriptor;
    private String returnType;
    private List<String> parameterList;
    private String parameters;
    private String dataType;
    private String methodSignature;
    private String genericType;


    /**
     * Parse access & property modifiers
     */
    public String setModifier(int af) {

        accessFlags = af;

        // todo not implemented yet

        return accessModifier;
    }


    /**
     * Parse the return type of method from the descriptor
     * Package names has been omitted to improve the readability
     */
    public String parseReturnType(String type) {

        descriptor = type;

        returnType = descriptor.substring(descriptor.lastIndexOf(")") + 1);

        /* Check for array as return type */
//        if (returnType.charAt(0) == '[') {
//            returnType = returnType.substring(1);
//            returnType = parseDataType(returnType);
//            returnType = returnType + "[]";
//        } else {
//            returnType = parseDataType(returnType);
//        }

        return returnType;
    }


    /**
     * Parse the parameterList of method from the descriptor
     * Package names has been omitted to improve the readability
     */
    public List<String> parseParameters(String type) {

        descriptor = type;

        /* Remove [] form the description */
        String allParams = descriptor.substring(1, descriptor.lastIndexOf(")"));

        /* Init the list - if no params in the method [] will be printed */
        parameterList = new ArrayList<>();

        /* Declare & Init temp String var */
        String tempStr = "";
        char firstChar;

        if (!allParams.isEmpty()) {                 // filter out null parameter methods

            boolean hasMore = false;
            boolean isArray = false;

            do {

                firstChar = allParams.charAt(0);

                if (firstChar == 'B' || firstChar == 'C' || firstChar == 'D' || firstChar == 'F' ||
                        firstChar == 'I' || firstChar == 'J' || firstChar == 'S' || firstChar == 'Z') {
                    tempStr = String.valueOf(firstChar);

                    if (isArray) {
                        parameterList.add(parseDataType(tempStr) + "[]");
                    } else {
                        parameterList.add(parseDataType(tempStr));
                    }

                    if (allParams.length() > 1) {
                        allParams = allParams.substring(1);
                        hasMore = true;
                    } else {
                        hasMore = false;
                    }

                    isArray = false;

                } else if (firstChar == 'L') {
                    tempStr = allParams.substring(0, allParams.indexOf(";") + 1);
                    if (isArray) {
                        parameterList.add(parseDataType(tempStr) + "[]");
                    } else {
                        if (parseDataType(tempStr).equals("List") || parseDataType(tempStr).equals("ArrayList")) {
                            getGenericType(parseDataType(tempStr));  //todo this is where get generic type called
                            parameterList.add(parseDataType(tempStr) + "<" + genericType + ">");
                        } else {
                            parameterList.add(parseDataType(tempStr));
                        }
                    }

                    if (allParams.length() > allParams.indexOf(";") + 1) {
                        allParams = allParams.substring(allParams.indexOf(";") + 1);
                        hasMore = true;
                    } else {
                        hasMore = false;
                    }

                    isArray = false;

                } else if (firstChar == '[') {
                    allParams = allParams.substring(1);
                    isArray = true;
                    hasMore = true;
                }

            } while (hasMore);
        }
        return parameterList;
    }


    /**
     * parse return types & parameterList to Java form from JVM form
     */
    private String parseDataType(String str) {
        this.dataType = str;

        switch (dataType) {
            case "V":
                dataType = "void";
                break;
            case "B":
                dataType = "byte";
                break;
            case "C":
                dataType = "char";
                break;
            case "D":
                dataType = "double";
                break;
            case "F":
                dataType = "float";
                break;
            case "I":
                dataType = "int";
                break;
            case "J":
                dataType = "long";
                break;
            case "S":
                dataType = "short";
                break;
            case "Z":
                dataType = "boolean";
                break;
            default:
                dataType = trimPackageName(dataType);
                break;
        }

        return dataType;
    }


    /**
     * Trim package names and trailing ; from return types and arguments
     */
    private String trimPackageName(String str) {
        this.dataType = str;

        /* Remove leading letter "L" */
        dataType = dataType.substring(1);

        /* Discard package names */
        dataType = dataType.substring(dataType.lastIndexOf("/") + 1);

        /* Remove tailing ";" */
        if (dataType.indexOf(";") < 0) {
            int debug = 1;
        }
        dataType = dataType.substring(0, dataType.indexOf(";"));

        return dataType;
    }


    /**
     * Get generic type of List or ArrayList
     */
    private void getGenericType(String s) {

        // todo method not implemented yet

        genericType = "";
    }


    /**
     * Generate method signature from method name & parameterList
     */
    public String getMethodSignature(String name, List<String> params) {

        methodName = name;
        parameterList = params;

        if (methodName.equals("<init>") || methodName.equals("<clinit>")) {
            methodSignature = methodName;
        } else {
            parameters = parameterList.toString();
            parameters = parameters.substring(parameters.indexOf('[') + 1, parameters.lastIndexOf(']'));
            methodSignature = methodName + "(" + parameters + ")";
        }

        return methodSignature;
    }
}

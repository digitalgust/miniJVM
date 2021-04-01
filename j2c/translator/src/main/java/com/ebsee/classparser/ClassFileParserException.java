package com.ebsee.classparser;
/**
 * Thrown when parsing fails on a Java .class file. Subclasses of this
 * exception type are used for particular types of parsing failure.
 *
 * @author David Cooper
 */
public abstract class ClassFileParserException extends Exception
{
    public ClassFileParserException(String msg)
    {
        super(msg);
    }

    public ClassFileParserException(String msg, Throwable cause)
    {
        super(msg, cause);
    }
}
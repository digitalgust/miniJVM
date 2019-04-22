/*
 * Copyright C 2003 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 *
 */
package java.lang;

import com.sun.cldc.i18n.uclc.*;

/**
 * The Character class wraps a value of the primitive type <code>char</code> in
 * an object. An object of type <code>Character</code> contains a single field
 * whose type is <code>char</code>.
 * <p>
 * In addition, this class provides several methods for determining the type of
 * a character and converting characters from uppercase to lowercase and vice
 * versa.
 * <p>
 * Character information is based on the Unicode Standard, version 3.0. However,
 * in order to reduce footprint, by default the character property and case
 * conversion operations in CLDC are available only for the ISO Latin-1 range of
 * characters. Other Unicode character blocks can be supported as necessary.
 * <p>
 *
 * @author Lee Boynton
 * @author Guy Steele
 * @author Akira Tanaka
 * @version 12/17/01 (CLDC 1.1)
 * @since JDK1.0, CLDC 1.0
 */

/*
 * Implementation note:
 * 
 * The character property and case conversion facilities 
 * provided by the CLDC reference implementation can be
 * extended by overriding an implementation class called 
 * DefaultCaseConverter.  Refer to the end of this file
 * for details.
 */
public final class Character implements Comparable<Character> {

    /**
     * The minimum radix available for conversion to and from Strings.
     *
     * @see java.lang.Integer#toString(int, int)
     * @see java.lang.Integer#valueOf(java.lang.String)
     */
    public static final int MIN_RADIX = 2;

    /**
     * The maximum radix available for conversion to and from Strings.
     *
     * @see java.lang.Integer#toString(int, int)
     * @see java.lang.Integer#valueOf(java.lang.String)
     */
    public static final int MAX_RADIX = 36;

    /**
     * The constant value of this field is the smallest value of type
     * <code>char</code>.
     *
     * @since JDK1.0.2
     */
    public static final char MIN_VALUE = '\u0000';

    /**
     * The constant value of this field is the largest value of type
     * <code>char</code>.
     *
     * @since JDK1.0.2
     */
    public static final char MAX_VALUE = '\uffff';

    public static final int MIN_SUPPLEMENTARY_CODE_POINT = 0x010000;

    public static final char MIN_HIGH_SURROGATE = '\uD800';
    public static final char MAX_HIGH_SURROGATE = '\uDBFF';
    public static final char MIN_LOW_SURROGATE = '\uDC00';
    public static final char MAX_LOW_SURROGATE = '\uDFFF';

    public static final int MIN_CODE_POINT = 0x000000;
    public static final int MAX_CODE_POINT = 0x10ffff;
    /**
     * The value of the Character.
     */
    private char value;

    public static final Class<Character> TYPE = (Class<Character>) Class.getPrimitiveClass("char");

    /**
     * Constructs a <code>Character</code> object and initializes it so that it
     * represents the primitive <code>value</code> argument.
     *
     * @param value value for the new <code>Character</code> object.
     */
    public Character(char value) {
        this.value = value;
    }

    /**
     * Returns the value of this Character object.
     *
     * @return the primitive <code>char</code> value represented by this object.
     */
    public char charValue() {
        return value;
    }

    /**
     * Returns a hash code for this Character.
     *
     * @return a hash code value for this object.
     */
    public int hashCode() {
        return (int) value;
    }

    /**
     * Compares this object against the specified object. The result is
     * <code>true</code> if and only if the argument is not <code>null</code>
     * and is a <code>Character</code> object that represents the same
     * <code>char</code> value as this object.
     *
     * @param obj the object to compare with.
     * @return  <code>true</code> if the objects are the same; <code>false</code>
     * otherwise.
     */
    public boolean equals(Object obj) {
        if (obj instanceof Character) {
            return value == ((Character) obj).charValue();
        }
        return false;
    }

    /**
     * Returns a String object representing this character's value. Converts
     * this <code>Character</code> object to a string. The result is a string
     * whose length is <code>1</code>. The string's sole component is the
     * primitive <code>char</code> value represented by this object.
     *
     * @return a string representation of this object.
     */
    public String toString() {
        char buf[] = {value};
        return String.valueOf(buf);
    }

    /**
     * Determines if the specified character is a lowercase character.
     * <p>
     * Note that by default CLDC only supports the ISO Latin-1 range of
     * characters.
     * <p>
     * Of the ISO Latin-1 characters (character codes 0x0000 through 0x00FF),
     * the following are lowercase:
     * <p>
     * a b c d e f g h i j k l m n o p q r s t u v w x y z
     * &#92;u00DF &#92;u00E0 &#92;u00E1 &#92;u00E2 &#92;u00E3 &#92;u00E4
     * &#92;u00E5 &#92;u00E6 &#92;u00E7 &#92;u00E8 &#92;u00E9 &#92;u00EA
     * &#92;u00EB &#92;u00EC &#92;u00ED &#92;u00EE &#92;u00EF &#92;u00F0
     * &#92;u00F1 &#92;u00F2 &#92;u00F3 &#92;u00F4 &#92;u00F5 &#92;u00F6
     * &#92;u00F8 &#92;u00F9 &#92;u00FA &#92;u00FB &#92;u00FC &#92;u00FD
     * &#92;u00FE &#92;u00FF
     *
     * @param ch the character to be tested.
     * @return  <code>true</code> if the character is lowercase;
     * <code>false</code> otherwise.
     * @since JDK1.0
     */
    public static boolean isLowerCase(char ch) {
        return caseConverter().isLowerCase(ch);
    }

    public static boolean isLowerCase(int ch) {
        return caseConverter().isLowerCase((char) ch);
    }

    /**
     * Determines if the specified character is an uppercase character.
     * <p>
     * Note that by default CLDC only supports the ISO Latin-1 range of
     * characters.
     * <p>
     * Of the ISO Latin-1 characters (character codes 0x0000 through 0x00FF),
     * the following are uppercase:
     * <p>
     * A B C D E F G H I J K L M N O P Q R S T U V W X Y Z
     * &#92;u00C0 &#92;u00C1 &#92;u00C2 &#92;u00C3 &#92;u00C4 &#92;u00C5
     * &#92;u00C6 &#92;u00C7 &#92;u00C8 &#92;u00C9 &#92;u00CA &#92;u00CB
     * &#92;u00CC &#92;u00CD &#92;u00CE &#92;u00CF &#92;u00D0 &#92;u00D1
     * &#92;u00D2 &#92;u00D3 &#92;u00D4 &#92;u00D5 &#92;u00D6 &#92;u00D8
     * &#92;u00D9 &#92;u00DA &#92;u00DB &#92;u00DC &#92;u00DD &#92;u00DE
     *
     * @param ch the character to be tested.
     * @return  <code>true</code> if the character is uppercase;
     * <code>false</code> otherwise.
     * @see java.lang.Character#isLowerCase(char)
     * @see java.lang.Character#toUpperCase(char)
     * @since 1.0
     */
    public static boolean isUpperCase(char ch) {
        return caseConverter().isUpperCase(ch);
    }

    public static boolean isUpperCase(int ch) {
        return caseConverter().isUpperCase((char) ch);
    }

    /**
     * Determines if the specified character is a digit.
     *
     * @param ch the character to be tested.
     * @return  <code>true</code> if the character is a digit; <code>false</code>
     * otherwise.
     * @since JDK1.0
     */
    public static boolean isDigit(char ch) {
        return caseConverter().isDigit(ch);
    }

    /**
     * The given character is mapped to its lowercase equivalent; if the
     * character has no lowercase equivalent, the character itself is returned.
     * <p>
     * Note that by default CLDC only supports the ISO Latin-1 range of
     * characters.
     *
     * @param ch the character to be converted.
     * @return the lowercase equivalent of the character, if any; otherwise the
     * character itself.
     * @see java.lang.Character#isLowerCase(char)
     * @see java.lang.Character#isUpperCase(char)
     * @see java.lang.Character#toUpperCase(char)
     * @since JDK1.0
     */
    public static char toLowerCase(char ch) {
        return caseConverter().toLowerCase(ch);
    }

    public static int toLowerCase(int codePoint) {
        return caseConverter().toLowerCase((char) codePoint);
    }

    /**
     * Converts the character argument to uppercase; if the character has no
     * uppercase equivalent, the character itself is returned.
     * <p>
     * Note that by default CLDC only supports the ISO Latin-1 range of
     * characters.
     *
     * @param ch the character to be converted.
     * @return the uppercase equivalent of the character, if any; otherwise the
     * character itself.
     * @see java.lang.Character#isLowerCase(char)
     * @see java.lang.Character#isUpperCase(char)
     * @see java.lang.Character#toLowerCase(char)
     * @since JDK1.0
     */
    public static char toUpperCase(char ch) {
        return caseConverter().toUpperCase(ch);
    }

    public static int toUpperCase(int ch) {
        return caseConverter().toUpperCase((char) ch);
    }

    /**
     * Returns the numeric value of the character <code>ch</code> in the
     * specified radix.
     *
     * @param ch the character to be converted.
     * @param radix the radix.
     * @return the numeric value represented by the character in the specified
     * radix.
     * @see java.lang.Character#isDigit(char)
     * @since JDK1.0
     */
    public static int digit(char ch, int radix) {
        return caseConverter().digit(ch, radix);
    }

    /*
     * Implementation note:
     *
     * The code below allows the default case converter class
     * to be overridden by defining a system property called 
     * "java.lang.Character.caseConverter".
     *
     * By default, the system only supports the ISO Latin-1
     * range of characters for character properties and 
     * case conversion.
     */
    static DefaultCaseConverter cc;

    static DefaultCaseConverter caseConverter() {

        if (cc != null) {
            return cc;
        }

        String ccName = "?";

        try {
            /* Get the default encoding name */
            ccName = System.getProperty("java.lang.Character.caseConverter");
            if (ccName == null) {
                ccName = "com.sun.cldc.i18n.uclc.DefaultCaseConverter";
            }

            /* Using the decoder names lookup a class to implement the reader */
            Class clazz = Class.forName(ccName);

            /* Return a new instance */
            cc = (DefaultCaseConverter) clazz.newInstance();

        } catch (Exception x) {
            throw new RuntimeException("Cannot find case converter class " + ccName + " -> " + x.getMessage());
        }

        return cc;
    }

    private static final byte X[] = new byte[1024];
    private static final String X_DATA
            = "\u0100\u0302\u0504\u0706\u0908\u0B0A\u0D0C\u0F0E\u1110\u1312\u1514\u1716\u1918"
            + "\u1B1A\u1C1C\u1C1C\u1C1C\u1C1C\u1E1D\u201F\u2221\u2423\u2625\u2827\u2A29\u2C2B"
            + "\u2E2D\u1C1C\u302F\u3231\u3433\u1C35\u1C1C\u3736\u3938\u3B3A\u1C1C\u1C1C\u1C1C"
            + "\u1C1C\u1C1C\u1C1C\u1C1C\u1C1C\u1C1C\u1C1C\u1C1C\u1C1C\u1C1C\u1C1C\u1C1C\u1C1C"
            + "\u1C1C\u1C1C\u1C1C\u1C1C\u1C1C\u1C1C\u1C1C\u1C1C\u3C3C\u3E3D\u403F\u4241\u4443"
            + "\u4645\u4847\u4A49\u4C4B\u4E4D\u504F\u1C1C\u5251\u5453\u5555\u5756\u5758\u1C1C"
            + "\u5A59\u1C5B\u1C1C\u1C1C\u1C1C\u1C1C\u1C1C\u1C1C\u1C1C\u1C1C\u1C1C\u1C1C\u1C1C"
            + "\u1C1C\u1C1C\u1C1C\u1C1C\u1C1C\u5D5C\u5F5E\u3860\u1C61\u6362\u6564\u6766\u6866"
            + "\u1C1C\u1C1C\u1C1C\u1C1C\u1C1C\u1C1C\u1C1C\u1C1C\u1C1C\u1C1C\u1C1C\u1C1C\u1C1C"
            + "\u1C1C\u1C1C\u1C1C\u1C1C\u1C1C\u1C1C\u1C1C\u1C1C\u1C1C\u1C1C\u1C1C\u1C1C\u1C1C"
            + "\u1C1C\u1C1C\u1C1C\u1C1C\u1C1C\u1C1C\u1C1C\u1C1C\u1C1C\u1C1C\u1C1C\u1C1C\u1C1C"
            + "\u1C1C\u1C1C\u1C1C\u1C1C\u1C1C\u1C1C\u1C1C\u1C1C\u1C1C\u1C1C\u1C1C\u1C1C\u1C1C"
            + "\u3838\u3838\u3838\u3838\u3838\u3838\u3838\u3838\u3838\u3838\u3838\u3838\u3838"
            + "\u3838\u3838\u3838\u3838\u3838\u3838\u3838\u3838\u3838\u3838\u3838\u3838\u3838"
            + "\u3838\u3838\u3838\u3838\u3838\u3838\u3838\u3838\u3838\u3838\u3838\u3838\u3838"
            + "\u3838\u3838\u3838\u3838\u3838\u3838\u3838\u3838\u3838\u3838\u3838\u3838\u3838"
            + "\u3838\u3838\u3838\u3838\u3838\u3838\u3838\u3838\u3838\u3838\u3838\u3838\u3838"
            + "\u3838\u3838\u3838\u3838\u3838\u3838\u3838\u3838\u3838\u3838\u3838\u3838\u3838"
            + "\u3838\u3838\u3838\u3838\u3838\u3838\u3838\u3838\u3838\u3838\u3838\u3838\u3838"
            + "\u3838\u3838\u3838\u3838\u3838\u3838\u3838\u3838\u3838\u3838\u3838\u3838\u3838"
            + "\u3838\u3838\u3838\u3838\u3838\u3838\u3838\u3838\u3838\u3838\u3838\u3838\u3838"
            + "\u3838\u3838\u3838\u3838\u3838\u3838\u3838\u3838\u3838\u3838\u3838\u3838\u3838"
            + "\u3838\u3838\u3838\u3838\u3838\u3838\u3838\u3838\u3838\u3838\u3838\u3838\u3838"
            + "\u3838\u3838\u3838\u3838\u3838\u3838\u3838\u3838\u3838\u3838\u3838\u3838\u3838"
            + "\u3838\u3838\u3838\u3838\u3838\u3838\u3838\u1C69\u1C1C\u1C1C\u1C1C\u1C1C\u1C1C"
            + "\u1C1C\u1C1C\u1C1C\u1C1C\u1C1C\u1C1C\u1C1C\u1C1C\u1C1C\u1C1C\u1C1C\u1C1C\u1C1C"
            + "\u1C1C\u1C1C\u1C1C\u1C1C\u1C1C\u1C1C\u3838\u3838\u3838\u3838\u3838\u3838\u3838"
            + "\u3838\u3838\u3838\u3838\u3838\u3838\u3838\u3838\u3838\u3838\u3838\u3838\u3838"
            + "\u3838\u3838\u3838\u3838\u3838\u3838\u3838\u3838\u3838\u3838\u3838\u3838\u3838"
            + "\u3838\u3838\u3838\u3838\u3838\u3838\u3838\u3838\u3838\u3838\u3838\u3838\u3838"
            + "\u3838\u3838\u3838\u3838\u3838\u3838\u3838\u3838\u3838\u3838\u3838\u3838\u3838"
            + "\u3838\u3838\u3838\u3838\u3838\u3838\u3838\u3838\u3838\u3838\u3838\u3838\u3838"
            + "\u3838\u3838\u3838\u3838\u3838\u3838\u3838\u3838\u3838\u3838\u3838\u3838\u3838"
            + "\u3838\u3838\u1C6A\u6B6B\u6B6B\u6B6B\u6B6B\u6B6B\u6B6B\u6B6B\u6B6B\u6B6B\u6B6B"
            + "\u6B6B\u6B6B\u6B6B\u6B6B\u6B6B\u6B6B\u6C6C\u6C6C\u6C6C\u6C6C\u6C6C\u6C6C\u6C6C"
            + "\u6C6C\u6C6C\u6C6C\u6C6C\u6C6C\u6C6C\u6C6C\u6C6C\u6C6C\u6C6C\u6C6C\u6C6C\u6C6C"
            + "\u6C6C\u6C6C\u6C6C\u6C6C\u6C6C\u6C6C\u6C6C\u6C6C\u6C6C\u6C6C\u6C6C\u6C6C\u6C6C"
            + "\u6C6C\u6C6C\u6C6C\u6C6C\u6C6C\u6C6C\u6C6C\u6C6C\u6C6C\u6C6C\u6C6C\u6C6C\u6C6C"
            + "\u6C6C\u6C6C\u6C6C\u6C6C\u3838\u3838\u1C6D\u1C1C\u6F6E\u7170\u7272\u7272\u7473"
            + "\u7675\u7877\u7972\u7B7A\u7D7C";

    // The Y table has 4032 entries for a total of 8064 bytes.
    private static final short Y[] = new short[4032];
    private static final String Y_DATA
            = "\000\000\000\000\002\004\004\000\000\000\000\000\000\000\004\004\006\010\012"
            + "\014\016\020\022\024\026\026\026\026\026\030\032\034\036\040\040\040\040\040"
            + "\040\040\040\040\040\040\040\042\044\046\050\052\052\052\052\052\052\052\052"
            + "\052\052\052\052\054\056\060\000\000\000\000\000\000\000\000\000\000\000\000"
            + "\000\000\000\000\062\064\064\066\070\072\074\076\100\102\104\106\110\112\114"
            + "\116\120\120\120\120\120\120\120\120\120\120\120\122\120\120\120\124\126\126"
            + "\126\126\126\126\126\126\126\126\126\130\126\126\126\132\134\134\134\134\134"
            + "\134\134\134\134\134\134\134\134\134\134\134\134\134\134\134\134\134\134\134"
            + "\136\134\134\134\140\142\142\142\142\142\142\142\144\134\134\134\134\134\134"
            + "\134\134\134\134\134\134\134\134\134\134\134\134\134\134\134\134\134\146\142"
            + "\142\150\152\134\134\154\156\160\144\162\164\156\166\170\134\172\174\176\134"
            + "\134\134\200\202\204\134\206\210\212\142\214\134\216\134\220\220\220\222\224"
            + "\226\222\230\142\142\142\142\142\142\142\232\134\134\134\134\134\134\134\134"
            + "\134\234\226\134\236\236\134\134\134\134\134\134\134\134\134\134\134\134\134"
            + "\134\134\236\236\236\236\236\236\236\236\236\236\236\236\236\236\236\236\236"
            + "\236\236\236\236\236\236\236\236\236\236\236\172\240\242\244\246\250\172\172"
            + "\252\254\172\172\256\172\172\260\172\262\264\172\172\172\172\172\172\266\172"
            + "\172\270\272\172\172\172\274\172\172\172\172\172\172\172\172\172\172\276\236"
            + "\236\236\300\300\300\300\302\304\300\300\300\306\306\306\306\306\306\306\300"
            + "\306\306\306\306\306\306\310\300\300\302\306\306\236\236\236\236\236\236\236"
            + "\236\236\236\236\312\312\312\312\312\312\312\312\312\312\312\312\312\312\312"
            + "\312\312\312\312\312\312\312\312\312\312\312\312\312\312\312\312\312\312\312"
            + "\312\236\236\236\236\236\236\236\236\236\236\236\236\236\312\236\236\236\236"
            + "\236\236\236\236\236\314\236\236\316\236\320\236\236\306\322\324\326\330\332"
            + "\334\120\120\120\120\120\120\120\120\336\120\120\120\120\340\342\344\126\126"
            + "\126\126\126\126\126\126\346\126\126\126\126\350\352\354\356\360\362\236\364"
            + "\364\364\364\134\134\134\134\134\134\134\366\216\236\236\236\236\236\236\370"
            + "\372\372\372\372\372\374\372\120\120\120\120\120\120\120\120\120\120\120\120"
            + "\120\120\120\120\126\126\126\126\126\126\126\126\126\126\126\126\126\126\126"
            + "\126\376\u0100\u0100\u0100\u0100\u0100\u0102\u0100\134\134\134\134\134\134"
            + "\134\134\134\134\134\134\134\134\134\134\134\u0104\312\u0106\236\236\236\236"
            + "\134\134\134\134\134\134\134\134\134\134\134\134\134\134\134\134\134\134\134"
            + "\134\134\134\134\134\u0108\142\u010A\u010C\u010A\u010C\u010A\236\134\134\134"
            + "\134\134\134\134\134\134\134\134\134\134\134\236\134\134\134\134\236\134\236"
            + "\236\236\236\236\236\236\236\236\236\236\236\236\236\236\236\236\236\236\236"
            + "\236\236\236\236\236\236\236\u010E\u0110\u0110\u0110\u0110\u0110\u0110\u0110"
            + "\u0110\u0110\u0110\u0110\u0110\u0110\u0110\u0110\u0110\u0110\u0110\u0112\u0114"
            + "\314\314\314\u0116\u0118\u0118\u0118\u0118\u0118\u0118\u0118\u0118\u0118\u0118"
            + "\u0118\u0118\u0118\u0118\u0118\u0118\u0118\u0118\u011A\u011C\236\236\236\u011E"
            + "\u0120\u0120\u0120\u0120\u0120\u0120\u0120\u0120\u011E\u0120\u0120\u0120\u0120"
            + "\u0120\u0120\u0120\u0120\u0120\u0120\u0120\u011E\u0120\u0122\u0122\u0124\u0126"
            + "\236\236\236\236\236\u0128\u0128\u0128\u0128\u0128\u0128\u0128\u0128\u0128"
            + "\u0128\u0128\u0128\u0128\u012A\236\236\u0128\u012C\u012E\236\236\236\236\236"
            + "\236\236\236\236\236\236\u012E\236\236\236\236\236\236\u0130\236\u0130\u0132"
            + "\u0128\u0128\u0128\u0128\u0128\u0128\u0128\u0128\u0128\u0128\u0128\u0128\u012A"
            + "\236\236\u0134\u0128\u0128\u0128\u0128\u0136\u0120\u0120\u0120\u0126\236\236"
            + "\236\236\236\236\u0138\u0138\u0138\u0138\u0138\u013A\u013C\236\u013E\u0128"
            + "\u0128\u0128\u0128\u0128\u0128\u0128\u0128\u0128\u0128\u0128\u0128\u0128\u0128"
            + "\u0128\u0128\u0128\u0128\u0128\u0128\u0128\u0128\u0128\u0128\u0128\u0128\u0128"
            + "\u0128\u0128\u0128\u0128\u0128\u0128\u0128\u0128\236\u0128\u0128\u012A\u0128"
            + "\u0128\u0128\u0128\u0128\u0128\u0128\u012A\u0128\u0128\u0140\u0120\u0120\u0120"
            + "\u0142\u0144\u0120\u0120\u0146\u0148\u014A\u0120\u0120\236\026\026\026\026"
            + "\026\236\236\236\236\236\236\236\236\236\236\236\236\236\236\236\236\236\236"
            + "\236\236\236\236\236\236\236\236\236\236\236\236\236\236\236\236\236\u014C"
            + "\u014E\u0150\220\220\220\220\220\220\220\220\220\220\220\220\220\220\220\220"
            + "\220\220\220\220\220\220\220\220\220\220\236\u0152\u0154\u0156\312\312\312"
            + "\u014E\u0154\u0156\236\u0104\312\u0106\236\220\220\220\220\220\312\314\u0158"
            + "\u0158\u0158\u0158\u0158\320\236\236\236\236\236\236\236\u014C\u0154\u0150"
            + "\220\220\220\u015A\u0150\u015A\u0150\220\220\220\220\220\220\220\220\220\220"
            + "\u015A\220\220\220\u015A\u015A\236\220\220\236\u0106\u0154\u0156\312\u0106"
            + "\u015C\u015E\u015C\u0156\236\236\236\236\u015C\236\236\220\u0150\220\312\236"
            + "\u0158\u0158\u0158\u0158\u0158\220\u0160\u0162\u0162\u0164\u0166\236\236\236"
            + "\u0106\u0150\220\220\u015A\236\u0150\u015A\u0150\220\220\220\220\220\220\220"
            + "\220\220\220\u015A\220\220\220\u015A\220\u0150\u015A\220\236\u0106\u0154\u0156"
            + "\u0106\236\u014C\u0106\u014C\312\236\236\236\236\236\u0150\220\u015A\u015A"
            + "\236\236\236\u0158\u0158\u0158\u0158\u0158\312\220\u015A\236\236\236\236\236"
            + "\u014C\u014E\u0150\220\220\220\u0150\u0150\220\u0150\220\220\220\220\220\220"
            + "\220\220\220\220\u015A\220\220\220\u015A\220\u0150\220\220\236\u0152\u0154"
            + "\u0156\312\312\u014C\u014E\u015C\u0156\236\u0166\236\236\236\236\236\236\236"
            + "\u015A\236\236\u0158\u0158\u0158\u0158\u0158\236\236\236\236\236\236\236\236"
            + "\u014C\u0154\u0150\220\220\220\u015A\u0150\u015A\u0150\220\220\220\220\220"
            + "\220\220\220\220\220\u015A\220\220\220\u015A\220\236\220\220\236\u0152\u0156"
            + "\u0156\312\236\u015C\u015E\u015C\u0156\236\236\236\236\u014E\236\236\220\u0150"
            + "\220\236\236\u0158\u0158\u0158\u0158\u0158\u0166\236\236\236\236\236\236\236"
            + "\236\u014E\u0150\220\220\u015A\236\220\u015A\220\220\236\u0150\u015A\u015A"
            + "\220\236\u0150\u015A\236\220\u015A\236\220\220\220\220\u0150\220\236\236\u0154"
            + "\u014E\u015E\236\u0154\u015E\u0154\u0156\236\236\236\236\u015C\236\236\236"
            + "\236\236\236\236\u0168\u0158\u0158\u0158\u0158\u016A\u016C\236\236\236\236"
            + "\236\236\u015C\u0154\u0150\220\220\220\u015A\220\u015A\220\220\220\220\220"
            + "\220\220\220\220\220\220\u015A\220\220\220\220\220\u0150\220\220\236\236\312"
            + "\u014E\u0154\u015E\312\u0106\312\312\236\236\236\u014C\u0106\236\236\236\236"
            + "\220\236\236\u0158\u0158\u0158\u0158\u0158\236\236\236\236\236\236\236\236"
            + "\236\u0154\u0150\220\220\220\u015A\220\u015A\220\220\220\220\220\220\220\220"
            + "\220\220\220\u015A\220\220\220\220\220\u0150\220\220\236\236\u0156\u0154\u0154"
            + "\u015E\u014E\u015E\u0154\312\236\236\236\u015C\u015E\236\236\236\u015A\220"
            + "\236\236\u0158\u0158\u0158\u0158\u0158\236\236\236\236\236\236\236\236\236"
            + "\u0154\u0150\220\220\220\u015A\220\u015A\220\220\220\220\220\220\220\220\220"
            + "\220\220\u015A\220\220\220\220\220\220\220\220\236\236\u0154\u0156\312\236"
            + "\u0154\u015E\u0154\u0156\236\236\236\236\u015C\236\236\236\236\220\236\236"
            + "\u0158\u0158\u0158\u0158\u0158\236\236\236\236\236\236\236\236\u0150\220\220"
            + "\220\220\220\220\220\220\220\220\220\220\220\220\220\220\220\220\220\220\220"
            + "\220\u016E\u0170\220\312\312\312\u0106\236\u0172\220\220\220\u0174\312\312"
            + "\312\u0176\u0178\u0178\u0178\u0178\u0178\314\236\236\236\236\236\236\236\236"
            + "\236\236\236\236\236\236\236\236\236\236\u0150\u015A\u015A\u0150\u015A\u015A"
            + "\u0150\236\236\236\220\220\u0150\220\220\220\u0150\220\u0150\u0150\236\220"
            + "\u0150\u016E\u0170\220\312\312\312\u014C\u0152\236\220\220\u015A\316\312\312"
            + "\312\236\u0178\u0178\u0178\u0178\u0178\236\220\236\236\236\236\236\236\236"
            + "\236\236\236\236\236\236\236\236\236\236\u017A\u017A\314\314\314\314\314\314"
            + "\314\u017C\u017A\u017A\312\u017A\u017A\u017A\u017E\u017E\u017E\u017E\u017E"
            + "\u0180\u0180\u0180\u0180\u0180\u0104\u0104\u0104\u0182\u0182\u0154\220\220"
            + "\220\220\u0150\220\220\220\220\220\220\220\220\220\220\220\220\220\220\220"
            + "\220\236\236\236\u014C\312\312\312\312\312\312\u014E\312\312\u0184\312\312"
            + "\312\236\236\312\312\312\u014C\u014C\312\312\312\312\312\312\312\312\312\312"
            + "\236\u014C\312\312\312\u014C\236\236\236\236\236\236\236\236\236\236\236\236"
            + "\236\236\236\236\236\236\236\u0110\u0110\u0110\u0110\u0110\u0110\u0110\u0110"
            + "\u0110\u0110\u0110\u0110\u0110\u0110\u0110\u0110\u0110\u0110\u0110\236\236"
            + "\236\236\236\172\172\172\172\172\172\172\172\172\172\172\172\172\172\172\172"
            + "\172\172\172\276\236\u011C\236\236\220\220\220\220\220\220\220\220\220\220"
            + "\220\220\220\220\220\220\220\220\220\220\220\220\220\220\220\220\220\220\220"
            + "\220\220\220\220\220\220\220\220\220\220\220\220\220\220\220\220\236\236\u0150"
            + "\220\220\220\220\220\220\220\220\220\220\220\220\220\220\220\220\220\220\220"
            + "\220\220\220\220\220\220\220\220\220\220\220\220\220\220\u015A\236\236\220"
            + "\220\220\220\220\220\220\220\220\220\220\220\220\220\220\220\220\220\220\220"
            + "\220\220\220\220\220\220\220\220\220\220\220\220\220\220\220\220\220\220\220"
            + "\220\220\236\236\236\134\134\134\134\134\134\134\134\134\134\134\134\134\134"
            + "\134\134\134\134\134\134\134\134\134\134\134\134\134\134\134\134\134\134\134"
            + "\134\134\134\134\134\134\134\134\134\134\172\172\u0186\236\236\134\134\134"
            + "\134\134\134\134\134\134\134\134\134\134\134\134\134\134\134\134\134\134\134"
            + "\134\134\134\134\134\134\134\134\134\134\134\134\134\134\134\134\134\134\134"
            + "\134\134\134\134\236\236\236\u0188\u0188\u0188\u0188\u018A\u018A\u018A\u018A"
            + "\u0188\u0188\u0188\236\u018A\u018A\u018A\236\u0188\u0188\u0188\u0188\u018A"
            + "\u018A\u018A\u018A\u0188\u0188\u0188\u0188\u018A\u018A\u018A\u018A\u0188\u0188"
            + "\u0188\236\u018A\u018A\u018A\236\u018C\u018C\u018C\u018C\u018E\u018E\u018E"
            + "\u018E\u0188\u0188\u0188\u0188\u018A\u018A\u018A\u018A\u0190\u0192\u0192\u0194"
            + "\u0196\u0198\u019A\236\u0188\u0188\u0188\u0188\u018A\u018A\u018A\u018A\u0188"
            + "\u0188\u0188\u0188\u018A\u018A\u018A\u018A\u0188\u0188\u0188\u0188\u018A\u018A"
            + "\u018A\u018A\u0188\u019C\276\172\u018A\u019E\u01A0\u01A2\306\u019C\276\172"
            + "\u01A4\u01A4\u01A0\306\u0188\172\236\172\u018A\u01A6\u01A8\306\u0188\172\u01AA"
            + "\172\u018A\u01AC\u01AE\306\236\u019C\276\172\u01B0\u01B2\u01A0\310\u01B4\u01B4"
            + "\u01B4\u01B6\u01B4\u01B4\u01B8\u01BA\u01BC\u01BC\u01BC\014\u01BE\u01C0\u01BE"
            + "\u01C0\014\014\014\014\u01C2\u01B8\u01B8\u01C4\u01C6\u01C6\u01C8\014\u01CA"
            + "\u01CC\014\u01CE\u01D0\014\u01D2\u01D4\236\236\236\236\236\236\236\236\236"
            + "\236\236\236\236\236\236\236\236\u01B8\u01B8\u01B8\u01D6\236\102\102\102\u01D8"
            + "\u01D2\u01DA\u01DC\u01DC\u01DC\u01DC\u01DC\u01D8\u01D2\u01D4\236\236\236\236"
            + "\236\236\236\236\064\064\064\064\064\064\u01DE\236\236\236\236\236\236\236"
            + "\236\236\236\236\236\236\236\236\236\236\312\312\312\312\312\312\u01E0\u01E2"
            + "\u01E4\236\236\236\236\236\236\236\236\236\236\236\236\236\236\236\066\u01E6"
            + "\066\u01E8\066\u01EA\u01EC\u01EE\u01EC\u01F0\u01E8\066\u01EC\u01EC\u01EC\066"
            + "\066\066\u01E6\u01E6\u01E6\u01EC\u01EC\u01EE\u01EC\u01E8\u01F2\u01F4\u01F6"
            + "\236\236\236\236\236\236\236\236\236\236\236\236\u01F8\114\114\114\114\114"
            + "\u01FA\u01FC\u01FC\u01FC\u01FC\u01FC\u01FC\u01FE\u01FE\u0200\u0200\u0200\u0200"
            + "\u0200\u0200\u0202\u0202\u0204\u0206\236\236\236\236\236\236\u0208\u0208\u020A"
            + "\066\066\066\066\066\066\066\066\066\066\066\066\066\066\066\066\066\066\066"
            + "\066\066\066\066\066\066\066\066\066\066\066\u020A\u020A\066\066\066\066\066"
            + "\066\066\066\066\066\u020C\236\236\236\236\236\236\236\236\236\236\u020E\u0210"
            + "\032\u0208\u0210\u0210\u0210\u0208\u020E\u01D8\u020E\032\u0208\u0210\u0210"
            + "\u020E\u0210\032\032\032\u0208\u020E\u0210\u0210\u0210\u0210\u0208\u0208\u020E"
            + "\u020E\u0210\u0210\u0210\u0210\u0210\u0210\u0210\u0210\032\u0208\u0208\u0210"
            + "\u0210\u0208\u0208\u0208\u0208\u020E\032\032\u0210\u0210\u0210\u0210\u0208"
            + "\u0210\u0210\u0210\u0210\u0210\u0210\u0210\u0210\u0210\u0210\u0210\u0210\u0210"
            + "\u0210\u0210\032\u020E\u0210\032\u0208\u0208\032\u0208\u0208\u0208\u0208\u0210"
            + "\u0208\u0210\u0210\u0210\u0210\u0210\u0210\u0210\u0210\u0210\032\u0208\u0208"
            + "\u0210\u0208\u0208\u0208\u0208\u020E\u0210\u0210\u0208\u0210\u0208\u0208\u0210"
            + "\u0210\u0210\u0210\u0210\u0210\u0210\u0210\u0210\u0210\u0210\u0210\u0208\u0210"
            + "\236\236\236\236\236\236\236\u020C\066\066\066\u0210\u0210\066\066\066\066"
            + "\066\066\066\066\066\066\u0210\066\066\066\u0212\u0214\066\066\066\066\066"
            + "\u017A\u017A\u017A\u017A\u017A\u017A\u017A\u017A\u017A\u017A\u017A\u017A\u017A"
            + "\u017A\u017A\u017A\u017A\u017A\u017A\u017A\u017A\u017A\u017A\u017A\u017A\u017A"
            + "\u017A\u017A\u017A\u017A\u017A\u017A\u017A\u017A\u0166\236\236\066\066\066"
            + "\066\066\066\066\066\066\066\066\066\066\066\066\066\066\066\u020C\236\236"
            + "\236\236\236\236\236\236\236\236\236\236\236\066\066\066\066\066\u020C\236"
            + "\236\236\236\236\236\236\236\236\236\u0216\u0216\u0216\u0216\u0216\u0216\u0216"
            + "\u0216\u0216\u0216\u0218\u0218\u0218\u0218\u0218\u0218\u0218\u0218\u0218\u0218"
            + "\u021A\u021A\u021A\u021A\u021A\u021A\u021A\u021A\u021A\u021A\066\066\066\066"
            + "\066\066\066\066\066\066\066\066\066\u021C\u021C\u021C\u021C\u021C\u021C\u021C"
            + "\u021C\u021C\u021C\u021C\u021C\u021C\u021E\u021E\u021E\u021E\u021E\u021E\u021E"
            + "\u021E\u021E\u021E\u021E\u021E\u021E\u0220\236\236\236\236\236\236\236\236"
            + "\236\236\066\066\066\066\066\066\066\066\066\066\066\066\066\066\066\066\066"
            + "\066\066\066\066\066\066\066\066\066\066\066\066\066\066\066\066\066\066\066"
            + "\066\066\066\066\066\066\066\236\236\236\236\236\066\066\066\066\066\066\066"
            + "\066\066\066\066\066\066\066\066\066\066\066\066\066\066\066\066\066\066\066"
            + "\066\066\066\066\066\066\066\066\066\066\066\066\066\066\236\236\236\236\236"
            + "\236\236\236\066\066\066\066\066\066\066\066\066\066\236\236\236\066\066\066"
            + "\066\066\066\066\066\066\066\066\066\066\066\066\066\066\066\066\u0222\066"
            + "\u020C\066\066\236\066\066\066\066\066\066\066\066\066\066\066\066\066\066"
            + "\u0222\066\066\066\066\066\066\066\066\066\066\066\066\066\066\066\066\066"
            + "\u0222\u0222\066\u020C\236\u020C\066\066\066\u020C\u0222\066\066\066\236\236"
            + "\236\236\236\236\236\u0224\u0224\u0224\u0224\u0224\u0216\u0216\u0216\u0216"
            + "\u0216\u0226\u0226\u0226\u0226\u0226\u020C\236\066\066\066\066\066\066\066"
            + "\066\066\066\066\066\u0222\066\066\066\066\066\066\u020C\006\014\u0228\u022A"
            + "\016\016\016\016\016\066\016\016\016\016\u022C\u022E\u0230\u0232\u0232\u0232"
            + "\u0232\312\312\312\u0234\u0236\u0236\066\236\236\236\u0222\u0150\220\220\220"
            + "\220\220\220\220\220\220\220\220\220\220\220\220\220\220\220\220\220\220\220"
            + "\220\220\220\220\220\220\220\220\220\220\220\220\220\220\220\220\220\220\220"
            + "\u015A\236\u014C\u0238\300\316\u0150\220\220\220\220\220\220\220\220\220\220"
            + "\220\220\220\220\220\220\220\220\220\220\220\220\220\220\220\220\220\220\220"
            + "\220\220\220\220\220\220\220\220\220\220\220\220\220\220\220\u016E\300\316"
            + "\236\236\u0150\220\220\220\220\220\220\220\220\220\220\220\220\220\220\220"
            + "\220\220\220\220\u015A\236\u0150\220\220\220\220\220\220\220\220\220\220\220"
            + "\220\220\220\u015A\u017A\u0180\u0180\u017A\u017A\u017A\u017A\u017A\236\236"
            + "\236\236\236\236\236\236\236\236\236\236\236\236\236\236\u017A\u017A\u017A"
            + "\u017A\u017A\u017A\u017A\u017A\u017A\u017A\u017A\u017A\u017A\u017A\u0166\236"
            + "\u0180\u0180\u0180\u0180\u0180\u017A\u017A\u017A\u017A\u017A\u017A\u017A\u017A"
            + "\u017A\u017A\u017A\u017A\u017A\236\236\236\236\236\236\236\236\236\236\236"
            + "\236\236\236\u017A\u017A\u017A\u017A\u017A\u017A\u017A\u017A\u017A\u017A\u017A"
            + "\u017A\u017A\u017A\236\u023A\u023C\u023C\u023C\u023C\u023C\u017A\u017A\u017A"
            + "\u017A\u017A\u017A\u017A\u017A\u017A\u017A\u017A\u017A\u017A\u017A\u017A\u017A"
            + "\u017A\u017A\u017A\u0166\236\236\236\236\236\236\236\u017A\u017A\u017A\u017A"
            + "\u017A\u017A\236\236\u017A\u017A\u017A\u017A\u017A\u017A\u017A\u017A\u017A"
            + "\u017A\u017A\u017A\u017A\u017A\u017A\u017A\u017A\u017A\u017A\u017A\u017A\u017A"
            + "\u017A\u0166\u017A\u017A\u017A\u017A\u017A\u017A\u017A\u017A\u017A\u017A\u017A"
            + "\u017A\u017A\u017A\u017A\u017A\u017A\u017A\u017A\u017A\u017A\u017A\u017A\u017A"
            + "\u017A\u017A\u017A\u017A\u017A\u017A\u017A\u017A\u017A\u017A\u017A\u017A\u017A"
            + "\u017A\u017A\u017A\u017A\u017A\u017A\u017A\u017A\u017A\u017A\u017A\u017A\u017A"
            + "\u017A\u017A\u017A\u017A\u017A\u017A\u017A\u017A\u017A\u0166\236\u023A\u017A"
            + "\u017A\u017A\u017A\u017A\u017A\u017A\u017A\u017A\u017A\u017A\u017A\u017A\u017A"
            + "\u017A\u017A\u017A\236\u017A\u017A\u017A\u017A\u017A\u017A\u017A\u017A\u017A"
            + "\u017A\u017A\u017A\u017A\u017A\u017A\u0166\220\220\220\220\220\220\220\220"
            + "\220\220\220\220\220\220\220\220\220\220\220\236\236\236\236\236\236\236\236"
            + "\236\236\236\236\236\220\220\220\220\220\220\220\220\220\220\220\220\220\220"
            + "\220\220\220\220\236\236\236\236\236\236\236\236\236\236\236\236\236\236\u023E"
            + "\u023E\u023E\u023E\u023E\u023E\u023E\u023E\u023E\u023E\u023E\u023E\u023E\u023E"
            + "\u023E\u023E\u023E\u023E\u023E\u023E\u023E\u023E\u023E\u023E\u023E\u023E\u023E"
            + "\u023E\u023E\u023E\u023E\u023E\u0240\u0240\u0240\u0240\u0240\u0240\u0240\u0240"
            + "\u0240\u0240\u0240\u0240\u0240\u0240\u0240\u0240\u0240\u0240\u0240\u0240\u0240"
            + "\u0240\u0240\u0240\u0240\u0240\u0240\u0240\u0240\u0240\u0240\u0240\220\220"
            + "\220\220\220\220\220\220\220\220\220\220\220\220\220\220\220\220\220\220\220"
            + "\220\220\236\236\236\236\236\236\236\236\236\172\172\172\276\236\236\236\236"
            + "\236\u0242\172\172\236\236\236\u013E\u0128\u0128\u0128\u0128\u0244\u0128\u0128"
            + "\u0128\u0128\u0128\u0128\u012A\u0128\u0128\u012A\u012A\u0128\u0132\u012A\u0128"
            + "\u0128\u0128\u0128\u0128\u0128\u0128\u0128\u0128\u0128\u0128\u0128\u0128\u0128"
            + "\u0128\u0128\u0128\u0128\u0128\u0128\u0128\u0128\u0128\u0128\u0128\u0128\u0128"
            + "\u0128\u0128\u0128\u0128\u0128\u0128\u0128\u0128\u0128\u0128\u0128\u0128\u0128"
            + "\u0128\u0128\u0128\u0128\u0128\u0128\u0128\u0128\u0128\u0128\u0128\u0128\u0128"
            + "\u0128\236\236\236\236\236\236\236\236\236\236\236\236\236\236\236\236\u0132"
            + "\u0128\u0128\u0128\u0128\u0128\u0128\u0128\u0128\u0128\u0128\u0128\u0128\u0128"
            + "\u0128\u0128\u0128\u0128\u0128\u0128\u0128\u0128\u0128\u0128\u0128\u0128\u0128"
            + "\u0128\u0128\u0128\u0128\u0128\u0128\u0128\u0128\u0128\u0128\u0128\u0128\u0128"
            + "\u0128\u0128\u0128\u0128\u0128\u0128\u0128\u0128\u0128\u0128\u0128\u0128\u0128"
            + "\u0128\u0128\u0128\u0128\u0128\u0128\u0128\u0128\u0128\u0128\u0128\u0128\u0128"
            + "\u0128\u0128\u0128\u0128\u0128\u0128\u0128\u0128\u0128\u0128\u0128\u0128\u0128"
            + "\u0128\u0128\u0128\u0128\u0128\u0128\u0128\u01BE\236\236\236\236\236\236\236"
            + "\236\u0128\u0128\u0128\u0128\u0128\u0128\u0128\u0128\u0128\u0128\u0128\u0128"
            + "\u0128\u0128\u0128\u0128\u0128\u0128\u0128\u0128\u0128\u0128\u0128\u0128\u0128"
            + "\u0128\u0128\u0128\u0128\u0128\u0128\u0128\236\u0128\u0128\u0128\u0128\u0128"
            + "\u0128\u0128\u0128\u0128\u0128\u0128\u0128\u0128\u0128\u0128\u0128\u0128\u0128"
            + "\u0128\u0128\u0128\u0128\u0128\u0128\u0128\u0128\u0128\236\236\236\236\236"
            + "\236\236\236\236\236\236\236\236\236\236\236\236\236\236\236\u0128\u0128\u0128"
            + "\u0128\u0128\u0128\236\236\236\236\236\236\236\236\236\236\236\236\236\236"
            + "\236\236\236\236\u0246\u0246\236\236\236\236\236\236\u0248\u024A\u024C\u024E"
            + "\u024E\u024E\u024E\u024E\u024E\u024E\u0250\236\u0252\014\u01CE\u0254\014\u0256"
            + "\014\014\u022C\u024E\u024E\u01CC\014\074\u0208\u0258\u025A\014\236\236\u0128"
            + "\u012A\u012A\u0128\u0128\u0128\u0128\u0128\u0128\u0128\u0128\u0128\u0128\u0128"
            + "\u0128\u0128\u0128\u0128\u0128\u0128\u0128\u0128\u0128\u0128\u0128\u0128\u0128"
            + "\u0128\u0128\u0128\u0128\u0128\u0128\u0128\u0128\u0128\u0128\u0128\u012A\u025C"
            + "\u0252\014\u025E\014\u01BE\u0260\u0248\014\026\026\026\026\026\014\u0208\u0262"
            + "\036\040\040\040\040\040\040\040\040\040\040\040\040\u0264\u0266\046\050\052"
            + "\052\052\052\052\052\052\052\052\052\052\052\u0268\u026A\u0258\u0252\u01BE"
            + "\u026C\220\220\220\220\220\u026E\220\220\220\220\220\220\220\220\220\220\220"
            + "\220\220\220\220\220\220\220\220\220\220\220\300\220\220\220\220\220\220\220"
            + "\220\220\220\220\220\220\220\220\u015A\236\220\220\220\236\220\220\220\236"
            + "\220\220\220\236\220\u015A\236\u0270\u0272\u0274\u0276\u0208\u0208\u020A\u020C"
            + "\236\236\236\236\236\236\066\236";

    // The A table has 632 entries for a total of 2528 bytes.
    private static final int A[] = new int[632];
    private static final String A_DATA
            = "\001\u018F\001\u018F\001\u018F\004\u012F\004\u018F\004\u018F\004\u014C\000"
            + "\u0198\000\u0198\000\270\006\272\000\270\000\u0198\000\u0198\000\u0175\000"
            + "\u0176\000\u0198\000\271\000\370\000\264\000\370\000\230\003\u6069\003\u6069"
            + "\000\370\000\u0198\000\u0179\000\u0199\000\u0179\000\u0198\000\u0198\u0827"
            + "\uFE21\u0827\uFE21\u0827\uFE21\u0827\uFE21\000\u0175\000\u0198\000\u0176\000"
            + "\u019B\005\u0197\000\u019B\u0817\uFE22\u0817\uFE22\u0817\uFE22\u0817\uFE22"
            + "\000\u0175\000\u0199\000\u0176\000\u0199\001\u018F\000\u014C\000\u0198\006"
            + "\272\006\272\000\u019C\000\u019C\000\u019B\000\u019C\007\u0182\000\u0195\000"
            + "\u0199\000\u0194\000\u019C\000\u019B\000\274\000\271\000\u606B\000\u606B\000"
            + "\u019B\007\u0182\000\u019C\000\u0198\000\u019B\000\u506B\007\u0182\000\u0196"
            + "\000\u818B\000\u818B\000\u818B\000\u0198\u0827\041\u0827\041\u0827\041\000"
            + "\u0199\u0827\041\007\042\u0817\042\u0817\042\u0817\042\000\u0199\u0817\042"
            + "\uE1D7\042\147\041\127\042\uCE67\041\u3A17\042\007\042\147\041\127\042\147"
            + "\041\127\042\007\042\uE1E7\041\147\041\127\042\u4B17\042\007\042\u34A7\041"
            + "\u33A7\041\147\041\127\042\u3367\041\u3367\041\147\041\u13E7\041\u32A7\041"
            + "\u32E7\041\147\041\u33E7\041\007\042\u34E7\041\u3467\041\007\042\007\042\u34E7"
            + "\041\u3567\041\007\042\u35A7\041\007\041\147\041\127\042\u36A7\041\007\045"
            + "\007\042\u36A7\041\147\041\127\042\u3667\041\u3667\041\147\041\127\042\u36E7"
            + "\041\007\042\007\045\007\045\007\045\257\041\177\043\237\042\257\041\177\043"
            + "\237\042\237\042\147\041\127\042\u13D7\042\007\042\257\041\000\000\000\000"
            + "\007\042\u3497\042\u3397\042\007\042\u3357\042\u3357\042\007\042\u3297\042"
            + "\007\042\u32D7\042\u3357\042\007\042\007\042\u33D7\042\u3457\042\u34D7\042"
            + "\007\042\u34D7\042\u3557\042\007\042\007\042\uCAA7\042\007\042\u3697\042\u3697"
            + "\042\007\042\u3657\042\u3657\042\u36D7\042\007\042\007\042\000\000\007\044"
            + "\007\044\007\044\000\073\000\073\007\044\000\073\000\073\000\073\000\000\003"
            + "\046\003\046\000\070\000\070\007\044\000\000\000\070\000\000\u09A7\041\000"
            + "\070\u0967\041\u0967\041\u0967\041\000\000\u1027\041\000\000\u0FE7\041\u0FE7"
            + "\041\007\042\u0827\041\000\000\u0827\041\u0997\042\u0957\042\u0957\042\u0957"
            + "\042\007\042\u0817\042\u07D7\042\u0817\042\u1017\042\u0FD7\042\u0FD7\042\000"
            + "\000\u0F97\042\u0E57\042\007\041\007\041\007\041\u0BD7\042\u0D97\042\000\000"
            + "\007\041\000\000\u1597\042\u1417\042\000\000\u1427\041\u1427\041\u1427\041"
            + "\u1427\041\000\000\000\000\u1417\042\u1417\042\u1417\042\u1417\042\000\000"
            + "\000\074\003\046\003\046\000\000\007\045\147\041\127\042\000\000\000\000\147"
            + "\041\000\000\u0C27\041\u0C27\041\u0C27\041\u0C27\041\000\000\000\000\007\044"
            + "\000\000\u0C17\042\u0C17\042\u0C17\042\u0C17\042\007\042\000\000\000\070\000"
            + "\000\003\106\003\106\003\106\000\130\003\106\003\106\000\130\003\106\000\000"
            + "\007\105\007\105\007\105\000\000\007\105\000\130\000\130\000\000\000\000\000"
            + "\130\000\000\007\105\007\104\007\105\007\105\003\106\003\u40C9\003\u40C9\000"
            + "\270\000\330\000\330\000\130\003\106\007\105\000\130\007\105\003\106\000\107"
            + "\000\107\003\106\003\106\007\104\007\104\003\106\003\106\000\134\000\000\003"
            + "\046\003\046\003\050\000\000\007\045\003\046\007\045\003\050\003\050\003\050"
            + "\003\046\003\u7429\003\u7429\007\045\000\000\000\000\003\050\003\050\000\000"
            + "\006\072\006\072\000\u5A2B\000\u5A2B\000\u802B\000\u6E2B\000\074\000\000\000"
            + "\000\003\u7429\000\u742B\000\u802B\000\u802B\000\000\007\045\000\070\007\045"
            + "\003\046\000\000\006\072\007\044\003\046\003\046\000\074\003\u6029\003\u6029"
            + "\000\074\000\074\000\070\000\074\003\u4029\003\u4029\000\053\000\053\000\065"
            + "\000\066\003\046\000\070\007\042\u0ED7\042\uFE17\042\uFE17\042\uFE27\041\uFE27"
            + "\041\007\042\uFE17\042\000\000\uFE27\041\uED97\042\uED97\042\uEA97\042\uEA97"
            + "\042\uE717\042\uE717\042\uE017\042\uE017\042\uE417\042\uE417\042\uE097\042"
            + "\uE097\042\007\042\uFDD7\042\uEDA7\041\uEDA7\041\uFDE7\041\000\073\007\041"
            + "\000\073\uEAA7\041\uEAA7\041\uE727\041\uE727\041\000\000\000\073\007\042\uFE57"
            + "\042\uE427\041\uE427\041\uFE67\041\000\073\uE027\041\uE027\041\uE0A7\041\uE0A7"
            + "\041\004\u014C\004\u014C\004\u014C\004\354\001\u0190\001\u0190\001\060\001"
            + "\120\000\u0194\000\u0194\000\u0195\000\u0196\000\u0195\000\u0195\004\u010D"
            + "\004\u010E\001\u0190\000\000\000\270\000\270\000\270\000\u0198\000\u0198\000"
            + "\u0195\000\u0196\000\u0198\000\u0198\005\u0197\005\u0197\000\u0198\000\u0199"
            + "\000\u0175\000\u0176\000\000\000\u606B\000\000\000\271\000\271\000\u0176\007"
            + "\u0182\000\u406B\000\u406B\006\272\000\000\003\046\000\047\000\047\000\047"
            + "\000\047\003\046\007\u0181\000\u019C\000\u019C\007\u0181\007\u0182\007\u0181"
            + "\007\u0181\007\u0181\007\u0182\007\u0182\007\u0181\007\u0182\007\u0182\007"
            + "\u0185\007\u0185\007\u0185\007\u0185\000\000\000\000\000\u818B\000\u818B\000"
            + "\u458B\u0427\u422A\u0427\u422A\u0427\u802A\u0427\u802A\u0417\u622A\u0417\u622A"
            + "\u0417\u802A\u0417\u802A\007\u802A\007\u802A\007\u802A\000\000\000\u0199\000"
            + "\u0199\000\u0199\000\u019C\000\u019C\000\000\000\u0199\000\u0179\000\u0179"
            + "\000\u0179\000\u019C\000\u0175\000\u0176\000\u019C\000\u438B\000\u438B\000"
            + "\u5B8B\000\u5B8B\000\u738B\000\u738B\u06A0\u019C\u06A0\u019C\u0690\u019C\u0690"
            + "\u019C\000\u6D8B\000\000\000\000\000\u019C\000\u578B\000\u578B\000\u6F8B\000"
            + "\u6F8B\000\u019C\007\u0184\000\u0198\007\u738A\000\u0194\000\u0195\000\u0196"
            + "\000\u0196\000\u019C\007\u402A\007\u402A\007\u402A\000\u0194\007\u0184\007"
            + "\u0184\007\u0184\003\046\007\044\000\000\000\074\000\u422B\000\u422B\000\063"
            + "\000\063\000\062\000\062\000\000\007\042\007\105\000\131\003\u0186\003\u0186"
            + "\000\u0198\000\u0194\000\u0194\005\u0197\005\u0197\000\u0195\000\u0196\000"
            + "\u0195\000\u0196\000\000\000\000\000\u0198\005\u0197\005\u0197\000\u0198\000"
            + "\000\000\u0199\000\000\000\u0198\006\u019A\000\000\001\u0190\006\u019A\000"
            + "\u0198\000\u0198\000\u0199\000\u0199\000\u0198\u0827\uFE21\000\u0195\000\u0198"
            + "\000\u0196\u0817\uFE22\000\u0195\000\u0199\000\u0196\000\u0198\000\070\007"
            + "\044\007\045\006\u019A\006\u019A\000\u0199\000\u019B\000\u019C\006\u019A\006"
            + "\u019A\000\000";

    public static final byte UNASSIGNED = 0,
            UPPERCASE_LETTER = 1,
            LOWERCASE_LETTER = 2,
            TITLECASE_LETTER = 3,
            MODIFIER_LETTER = 4,
            OTHER_LETTER = 5,
            NON_SPACING_MARK = 6,
            ENCLOSING_MARK = 7,
            COMBINING_SPACING_MARK = 8,
            DECIMAL_DIGIT_NUMBER = 9,
            LETTER_NUMBER = 10,
            OTHER_NUMBER = 11,
            SPACE_SEPARATOR = 12,
            LINE_SEPARATOR = 13,
            PARAGRAPH_SEPARATOR = 14,
            CONTROL = 15,
            FORMAT = 16,
            PRIVATE_USE = 18,
            SURROGATE = 19,
            DASH_PUNCTUATION = 20,
            START_PUNCTUATION = 21,
            END_PUNCTUATION = 22,
            CONNECTOR_PUNCTUATION = 23,
            OTHER_PUNCTUATION = 24,
            MATH_SYMBOL = 25,
            CURRENCY_SYMBOL = 26,
            MODIFIER_SYMBOL = 27,
            OTHER_SYMBOL = 28;

    /**
     * Instances of this class represent particular subsets of the Unicode
     * character set. The only family of subsets defined in the
     * <code>Character</code> class is <code>{@link Character.UnicodeBlock
     * UnicodeBlock}</code>. Other portions of the Java API may define other
     * subsets for their own purposes.
     *
     * @since 1.2
     */
    public static class Subset {

        private String name;

        /**
         * Constructs a new <code>Subset</code> instance.
         *
         * @param name The name of this subset
         */
        protected Subset(String name) {
            this.name = name;
        }

        /**
         * Compares two <code>Subset</code> objects for equality. This method
         * returns <code>true</code> if and only if <code>x</code> and
         * <code>y</code> refer to the same object, and because it is final it
         * guarantees this for all subclasses.
         */
        public final boolean equals(Object obj) {
            return (this == obj);
        }

        /**
         * Returns the standard hash code as defined by the <code>{@link
         * Object#hashCode}</code> method. This method is final in order to
         * ensure that the <code>equals</code> and <code>hashCode</code> methods
         * will be consistent in all subclasses.
         */
        public final int hashCode() {
            return super.hashCode();
        }

        /**
         * Returns the name of this subset.
         */
        public final String toString() {
            return name;
        }

    }

    /**
     * A family of character subsets representing the character blocks defined
     * by the Unicode 2.0 specification. Any given character is contained by at
     * most one Unicode block.
     *
     * @since 1.2
     */
    public static final class UnicodeBlock extends Subset {

        private UnicodeBlock(String name) {
            super(name);
        }

        /**
         * Constant for the Unicode character block of the same name.
         */
        public static final UnicodeBlock BASIC_LATIN
                = new UnicodeBlock("BASIC_LATIN"),
                LATIN_1_SUPPLEMENT
                = new UnicodeBlock("LATIN_1_SUPPLEMENT"),
                LATIN_EXTENDED_A
                = new UnicodeBlock("LATIN_EXTENDED_A"),
                LATIN_EXTENDED_B
                = new UnicodeBlock("LATIN_EXTENDED_B"),
                IPA_EXTENSIONS
                = new UnicodeBlock("IPA_EXTENSIONS"),
                SPACING_MODIFIER_LETTERS
                = new UnicodeBlock("SPACING_MODIFIER_LETTERS"),
                COMBINING_DIACRITICAL_MARKS
                = new UnicodeBlock("COMBINING_DIACRITICAL_MARKS"),
                GREEK
                = new UnicodeBlock("GREEK"),
                CYRILLIC
                = new UnicodeBlock("CYRILLIC"),
                ARMENIAN
                = new UnicodeBlock("ARMENIAN"),
                HEBREW
                = new UnicodeBlock("HEBREW"),
                ARABIC
                = new UnicodeBlock("ARABIC"),
                DEVANAGARI
                = new UnicodeBlock("DEVANAGARI"),
                BENGALI
                = new UnicodeBlock("BENGALI"),
                GURMUKHI
                = new UnicodeBlock("GURMUKHI"),
                GUJARATI
                = new UnicodeBlock("GUJARATI"),
                ORIYA
                = new UnicodeBlock("ORIYA"),
                TAMIL
                = new UnicodeBlock("TAMIL"),
                TELUGU
                = new UnicodeBlock("TELUGU"),
                KANNADA
                = new UnicodeBlock("KANNADA"),
                MALAYALAM
                = new UnicodeBlock("MALAYALAM"),
                THAI
                = new UnicodeBlock("THAI"),
                LAO
                = new UnicodeBlock("LAO"),
                TIBETAN
                = new UnicodeBlock("TIBETAN"),
                GEORGIAN
                = new UnicodeBlock("GEORGIAN"),
                HANGUL_JAMO
                = new UnicodeBlock("HANGUL_JAMO"),
                LATIN_EXTENDED_ADDITIONAL
                = new UnicodeBlock("LATIN_EXTENDED_ADDITIONAL"),
                GREEK_EXTENDED
                = new UnicodeBlock("GREEK_EXTENDED"),
                GENERAL_PUNCTUATION
                = new UnicodeBlock("GENERAL_PUNCTUATION"),
                SUPERSCRIPTS_AND_SUBSCRIPTS
                = new UnicodeBlock("SUPERSCRIPTS_AND_SUBSCRIPTS"),
                CURRENCY_SYMBOLS
                = new UnicodeBlock("CURRENCY_SYMBOLS"),
                COMBINING_MARKS_FOR_SYMBOLS
                = new UnicodeBlock("COMBINING_MARKS_FOR_SYMBOLS"),
                LETTERLIKE_SYMBOLS
                = new UnicodeBlock("LETTERLIKE_SYMBOLS"),
                NUMBER_FORMS
                = new UnicodeBlock("NUMBER_FORMS"),
                ARROWS
                = new UnicodeBlock("ARROWS"),
                MATHEMATICAL_OPERATORS
                = new UnicodeBlock("MATHEMATICAL_OPERATORS"),
                MISCELLANEOUS_TECHNICAL
                = new UnicodeBlock("MISCELLANEOUS_TECHNICAL"),
                CONTROL_PICTURES
                = new UnicodeBlock("CONTROL_PICTURES"),
                OPTICAL_CHARACTER_RECOGNITION
                = new UnicodeBlock("OPTICAL_CHARACTER_RECOGNITION"),
                ENCLOSED_ALPHANUMERICS
                = new UnicodeBlock("ENCLOSED_ALPHANUMERICS"),
                BOX_DRAWING
                = new UnicodeBlock("BOX_DRAWING"),
                BLOCK_ELEMENTS
                = new UnicodeBlock("BLOCK_ELEMENTS"),
                GEOMETRIC_SHAPES
                = new UnicodeBlock("GEOMETRIC_SHAPES"),
                MISCELLANEOUS_SYMBOLS
                = new UnicodeBlock("MISCELLANEOUS_SYMBOLS"),
                DINGBATS
                = new UnicodeBlock("DINGBATS"),
                CJK_SYMBOLS_AND_PUNCTUATION
                = new UnicodeBlock("CJK_SYMBOLS_AND_PUNCTUATION"),
                HIRAGANA
                = new UnicodeBlock("HIRAGANA"),
                KATAKANA
                = new UnicodeBlock("KATAKANA"),
                BOPOMOFO
                = new UnicodeBlock("BOPOMOFO"),
                HANGUL_COMPATIBILITY_JAMO
                = new UnicodeBlock("HANGUL_COMPATIBILITY_JAMO"),
                KANBUN
                = new UnicodeBlock("KANBUN"),
                ENCLOSED_CJK_LETTERS_AND_MONTHS
                = new UnicodeBlock("ENCLOSED_CJK_LETTERS_AND_MONTHS"),
                CJK_COMPATIBILITY
                = new UnicodeBlock("CJK_COMPATIBILITY"),
                CJK_UNIFIED_IDEOGRAPHS
                = new UnicodeBlock("CJK_UNIFIED_IDEOGRAPHS"),
                HANGUL_SYLLABLES
                = new UnicodeBlock("HANGUL_SYLLABLES"),
                SURROGATES_AREA
                = new UnicodeBlock("SURROGATES_AREA"),
                PRIVATE_USE_AREA
                = new UnicodeBlock("PRIVATE_USE_AREA"),
                CJK_COMPATIBILITY_IDEOGRAPHS
                = new UnicodeBlock("CJK_COMPATIBILITY_IDEOGRAPHS"),
                ALPHABETIC_PRESENTATION_FORMS
                = new UnicodeBlock("ALPHABETIC_PRESENTATION_FORMS"),
                ARABIC_PRESENTATION_FORMS_A
                = new UnicodeBlock("ARABIC_PRESENTATION_FORMS_A"),
                COMBINING_HALF_MARKS
                = new UnicodeBlock("COMBINING_HALF_MARKS"),
                CJK_COMPATIBILITY_FORMS
                = new UnicodeBlock("CJK_COMPATIBILITY_FORMS"),
                SMALL_FORM_VARIANTS
                = new UnicodeBlock("SMALL_FORM_VARIANTS"),
                ARABIC_PRESENTATION_FORMS_B
                = new UnicodeBlock("ARABIC_PRESENTATION_FORMS_B"),
                HALFWIDTH_AND_FULLWIDTH_FORMS
                = new UnicodeBlock("HALFWIDTH_AND_FULLWIDTH_FORMS"),
                SPECIALS
                = new UnicodeBlock("SPECIALS");

        private static final char blockStarts[] = {
            '\u0000',
            '\u0080',
            '\u0100',
            '\u0180',
            '\u0250',
            '\u02B0',
            '\u0300',
            '\u0370',
            '\u0400',
            '\u0500', // unassigned
            '\u0530',
            '\u0590',
            '\u0600',
            '\u0700', // unassigned
            '\u0900',
            '\u0980',
            '\u0A00',
            '\u0A80',
            '\u0B00',
            '\u0B80',
            '\u0C00',
            '\u0C80',
            '\u0D00',
            '\u0D80', // unassigned
            '\u0E00',
            '\u0E80',
            '\u0F00',
            '\u0FC0', // unassigned
            '\u10A0',
            '\u1100',
            '\u1200', // unassigned
            '\u1E00',
            '\u1F00',
            '\u2000',
            '\u2070',
            '\u20A0',
            '\u20D0',
            '\u2100',
            '\u2150',
            '\u2190',
            '\u2200',
            '\u2300',
            '\u2400',
            '\u2440',
            '\u2460',
            '\u2500',
            '\u2580',
            '\u25A0',
            '\u2600',
            '\u2700',
            '\u27C0', // unassigned
            '\u3000',
            '\u3040',
            '\u30A0',
            '\u3100',
            '\u3130',
            '\u3190',
            '\u3200',
            '\u3300',
            '\u3400', // unassigned
            '\u4E00',
            '\uA000', // unassigned
            '\uAC00',
            '\uD7A4', // unassigned
            '\uD800',
            '\uE000',
            '\uF900',
            '\uFB00',
            '\uFB50',
            '\uFE00', // unassigned
            '\uFE20',
            '\uFE30',
            '\uFE50',
            '\uFE70',
            '\uFEFF', // special
            '\uFF00',
            '\uFFF0'
        };

        private static final UnicodeBlock blocks[] = {
            BASIC_LATIN,
            LATIN_1_SUPPLEMENT,
            LATIN_EXTENDED_A,
            LATIN_EXTENDED_B,
            IPA_EXTENSIONS,
            SPACING_MODIFIER_LETTERS,
            COMBINING_DIACRITICAL_MARKS,
            GREEK,
            CYRILLIC,
            null,
            ARMENIAN,
            HEBREW,
            ARABIC,
            null,
            DEVANAGARI,
            BENGALI,
            GURMUKHI,
            GUJARATI,
            ORIYA,
            TAMIL,
            TELUGU,
            KANNADA,
            MALAYALAM,
            null,
            THAI,
            LAO,
            TIBETAN,
            null,
            GEORGIAN,
            HANGUL_JAMO,
            null,
            LATIN_EXTENDED_ADDITIONAL,
            GREEK_EXTENDED,
            GENERAL_PUNCTUATION,
            SUPERSCRIPTS_AND_SUBSCRIPTS,
            CURRENCY_SYMBOLS,
            COMBINING_MARKS_FOR_SYMBOLS,
            LETTERLIKE_SYMBOLS,
            NUMBER_FORMS,
            ARROWS,
            MATHEMATICAL_OPERATORS,
            MISCELLANEOUS_TECHNICAL,
            CONTROL_PICTURES,
            OPTICAL_CHARACTER_RECOGNITION,
            ENCLOSED_ALPHANUMERICS,
            BOX_DRAWING,
            BLOCK_ELEMENTS,
            GEOMETRIC_SHAPES,
            MISCELLANEOUS_SYMBOLS,
            DINGBATS,
            null,
            CJK_SYMBOLS_AND_PUNCTUATION,
            HIRAGANA,
            KATAKANA,
            BOPOMOFO,
            HANGUL_COMPATIBILITY_JAMO,
            KANBUN,
            ENCLOSED_CJK_LETTERS_AND_MONTHS,
            CJK_COMPATIBILITY,
            null,
            CJK_UNIFIED_IDEOGRAPHS,
            null,
            HANGUL_SYLLABLES,
            null,
            SURROGATES_AREA,
            PRIVATE_USE_AREA,
            CJK_COMPATIBILITY_IDEOGRAPHS,
            ALPHABETIC_PRESENTATION_FORMS,
            ARABIC_PRESENTATION_FORMS_A,
            null,
            COMBINING_HALF_MARKS,
            CJK_COMPATIBILITY_FORMS,
            SMALL_FORM_VARIANTS,
            ARABIC_PRESENTATION_FORMS_B,
            SPECIALS,
            HALFWIDTH_AND_FULLWIDTH_FORMS,
            SPECIALS
        };

        /**
         * Returns the object representing the Unicode block containing the
         * given character, or <code>null</code> if the character is not a
         * member of a defined block.
         *
         * @param c The character in question
         * @return The <code>UnicodeBlock</code> instance representing the
         * Unicode block of which this character is a member, or
         * <code>null</code> if the character is not a member of any Unicode
         * block
         */
        public static UnicodeBlock of(char c) {
            int top, bottom, current;
            bottom = 0;
            top = blockStarts.length;
            current = top / 2;
            // invariant: top > current >= bottom && ch >= unicodeBlockStarts[bottom]
            while (top - bottom > 1) {
                if (c >= blockStarts[current]) {
                    bottom = current;
                } else {
                    top = current;
                }
                current = (top + bottom) / 2;
            }
            return blocks[current];
        }

        public static UnicodeBlock of(int c) {
            return of((char) c);
        }

        public static final UnicodeBlock forName(String blockName) {
            UnicodeBlock block = BASIC_LATIN; //todo gust
            if (block == null) {
                throw new IllegalArgumentException();
            }
            return block;
        }
    }

    // In all, the character property tables require 11616 bytes.
    static {
        { // THIS CODE WAS AUTOMATICALLY CREATED BY GenerateCharacter:
            int len = X_DATA.length();
            int j = 0;
            for (int i = 0; i < len; ++i) {
                int c = X_DATA.charAt(i);
                for (int k = 0; k < 2; ++k) {
                    X[j++] = (byte) c;
                    c >>= 8;
                }
            }
            if (j != 1024) {
                throw new RuntimeException();
            }
        }
        { // THIS CODE WAS AUTOMATICALLY CREATED BY GenerateCharacter:
            if (Y_DATA.length() != 4032) {
                throw new RuntimeException();
            }
            for (int i = 0; i < 4032; ++i) {
                Y[i] = (short) Y_DATA.charAt(i);
            }
        }
        { // THIS CODE WAS AUTOMATICALLY CREATED BY GenerateCharacter:
            int len = A_DATA.length();
            int j = 0;
            int charsInEntry = 0;
            int entry = 0;
            for (int i = 0; i < len; ++i) {
                entry |= A_DATA.charAt(i);
                if (++charsInEntry == 2) {
                    A[j++] = entry;
                    entry = 0;
                    charsInEntry = 0;
                } else {
                    entry <<= 16;
                }
            }
            if (j != 632) {
                throw new RuntimeException();
            }
        }

    }

    public static Character valueOf(char i) {
        return new Character(i);
    }

    public int compareTo(Character o) {
        return value - o.value;
    }

    public static char[] toChars(int codePoint) {
        return new char[]{(char) codePoint};
    }

    public static int charCount(int c) {
        return 1;
    }

    public static int codePointAt(char[] a, int index, int limit) {
        if (index >= limit || limit < 0 || limit > a.length) {
            throw new IndexOutOfBoundsException();
        }
        return a[index];
    }

    public static int codePointAt(char[] a, int index) {
        if (index < 0 || index > a.length) {
            throw new IndexOutOfBoundsException();
        }
        return a[index];
    }

    public static int codePointAt(CharSequence seq, int index) {
        return seq.charAt(index);
    }

    public static int codePointBefore(char[] a, int index, int start) {
        if (index <= start || start < 0 || start >= a.length) {
            throw new IndexOutOfBoundsException();
        }
        return a[--index];
    }

    public static int codePointBefore(char[] a, int index) {
        if (index < 0 || index > a.length) {
            throw new IndexOutOfBoundsException();
        }
        return a[--index];
    }

    public static int codePointBefore(CharSequence seq, int index) {
        return seq.charAt(--index);
    }

    public static boolean isLetter(char ch) {
        return ch >= 'a' && ch <= 'z' || ch >= 'A' && ch <= 'Z';
    }

    public static boolean isLetter(int codePoint) {
        return codePoint >= 'a' && codePoint <= 'z' || codePoint >= 'A' && codePoint <= 'Z';
    }

    public static boolean isDigit(int codePoint) {
        return codePoint >= '0' && codePoint <= '9';
    }

    public static boolean isLetterOrDigit(char ch) {
        return isLetter(ch) || isDigit(ch);
    }

    public static boolean isLetterOrDigit(int codePoint) {
        return isLetter(codePoint) || isDigit(codePoint);
    }

    public static int getType(int ch) {
        return getType((char) ch);
    }

    public static int getType(char ch) {
        return A[Y[(X[ch >> 6] << 5) | ((ch >> 1) & 0x1F)] | (ch & 0x1)] & 0x1F;
    }

    public static boolean isJavaIdentifierPart(int ch) {
        return (A[Y[(X[ch >> 6] << 5) | ((ch >> 1) & 0x1F)] | (ch & 0x1)] & 0x00030000) != 0;
    }

    public static boolean isJavaIdentifierPart(char ch) {
        return (A[Y[(X[ch >> 6] << 5) | ((ch >> 1) & 0x1F)] | (ch & 0x1)] & 0x00030000) != 0;
    }

    public static boolean isJavaIdentifierStart(int ch) {
        return (A[Y[(X[ch >> 6] << 5) | ((ch >> 1) & 0x1F)] | (ch & 0x1)] & 0x00070000) >= 0x00050000;
    }

    public static boolean isJavaIdentifierStart(char ch) {
        return (A[Y[(X[ch >> 6] << 5) | ((ch >> 1) & 0x1F)] | (ch & 0x1)] & 0x00070000) >= 0x00050000;
    }

    public static boolean isHighSurrogate(char ch) {
        return false;
    }

    public static boolean isLowSurrogate(char ch) {
        return false;
    }

    public static boolean isUnicodeIdentifierStart(char ch) {
        return (A[Y[(X[ch >> 6] << 5) | ((ch >> 1) & 0x1F)] | (ch & 0x1)] & 0x00070000) == 0x00070000;
    }

    public static boolean isUnicodeIdentifierStart(int ch) {
        return (A[Y[(X[ch >> 6] << 5) | ((ch >> 1) & 0x1F)] | (ch & 0x1)] & 0x00070000) == 0x00070000;
    }

    public static boolean isUnicodeIdentifierPart(char ch) {
        return (A[Y[(X[ch >> 6] << 5) | ((ch >> 1) & 0x1F)] | (ch & 0x1)] & 0x00010000) != 0;
    }

    public static boolean isUnicodeIdentifierPart(int ch) {
        return (A[Y[(X[ch >> 6] << 5) | ((ch >> 1) & 0x1F)] | (ch & 0x1)] & 0x00010000) != 0;
    }

    public static boolean isIdentifierIgnorable(char ch) {
        return (A[Y[(X[ch >> 6] << 5) | ((ch >> 1) & 0x1F)] | (ch & 0x1)] & 0x00070000) == 0x00010000;
    }

    public static boolean isIdentifierIgnorable(int ch) {
        return (A[Y[(X[ch >> 6] << 5) | ((ch >> 1) & 0x1F)] | (ch & 0x1)] & 0x00070000) == 0x00010000;
    }

    public static boolean isISOControl(int ch) {
        return (ch <= 0x009F) && ((ch <= 0x001F) || (ch >= 0x007F));
    }

    public static boolean isWhitespace(int ch) {
        return (A[Y[(X[ch >> 6] << 5) | ((ch >> 1) & 0x1F)] | (ch & 0x1)] & 0x00070000) == 0x00040000;
    }

    public static boolean isSpaceChar(int ch) {
        return (((((1 << SPACE_SEPARATOR)
                | (1 << LINE_SEPARATOR)
                | (1 << PARAGRAPH_SEPARATOR))
                >> (A[Y[(X[ch >> 6] << 5) | ((ch >> 1) & 0x1F)] | (ch & 0x1)] & 0x1F)) & 1) != 0);
    }

    public static boolean isSpace(int ch) {
        return (ch <= 0x0020)
                && (((((1L << 0x0009)
                | (1L << 0x000A)
                | (1L << 0x000C)
                | (1L << 0x000D)
                | (1L << 0x0020)) >> ch) & 1L) != 0);
    }

    public static boolean isSupplementaryCodePoint(int codePoint) {
        return codePoint >= MIN_SUPPLEMENTARY_CODE_POINT
                && codePoint <= MAX_CODE_POINT;
    }

    public static boolean isTitleCase(char ch) {
        return (A[Y[(X[ch >> 6] << 5) | ((ch >> 1) & 0x1F)] | (ch & 0x1)] & 0x1F) == TITLECASE_LETTER;
    }

    public static boolean isMirrored(char ch) {
        return false;//todo gust
    }

    public static boolean isTitleCase(int ch) {
        return (A[Y[(X[ch >> 6] << 5) | ((ch >> 1) & 0x1F)] | (ch & 0x1)] & 0x1F) == TITLECASE_LETTER;
    }

    public static boolean isMirrored(int ch) {
        return false;//todo gust
    }

    public static boolean isDefined(char ch) {
        return (A[Y[(X[ch >> 6] << 5) | ((ch >> 1) & 0x1F)] | (ch & 0x1)] & 0x1F) != UNASSIGNED;
    }

    public static boolean isDefined(int ch) {
        return (A[Y[(X[ch >> 6] << 5) | ((ch >> 1) & 0x1F)] | (ch & 0x1)] & 0x1F) != UNASSIGNED;
    }

    public static char toTitleCase(char ch) {
        int val = A[Y[(X[ch >> 6] << 5) | ((ch >> 1) & 0x1F)] | (ch & 0x1)];
        if ((val & 0x00080000) != 0) {
            // There is a titlecase equivalent.  Perform further checks:
            if ((val & 0x00100000) == 0) {
                // The character does not have an uppercase equivalent, so it must
                // already be uppercase; so add 1 to get the titlecase form.
                return (char) (ch + 1);
            } else if ((val & 0x00200000) == 0) {
                // The character does not have a lowercase equivalent, so it must
                // already be lowercase; so subtract 1 to get the titlecase form.
                return (char) (ch - 1);
            } else {
                // The character has both an uppercase equivalent and a lowercase
                // equivalent, so it must itself be a titlecase form; return it.
                return ch;
            }
        } else if ((val & 0x00100000) != 0) {
            // This character has no titlecase equivalent but it does have an
            // uppercase equivalent, so use that (subtract the signed case offset).
            return (char) (ch - (val >> 22));
        } else {
            return ch;
        }
    }

    public static char toTitleCase(int ch) {
        return toTitleCase((char) ch);
    }

}

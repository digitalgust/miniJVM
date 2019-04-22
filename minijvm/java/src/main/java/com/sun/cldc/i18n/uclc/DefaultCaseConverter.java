/*
 * Copyright C 2003 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package com.sun.cldc.i18n.uclc;

import java.io.*;
import com.sun.cldc.i18n.*;

/**
 * Default class converting the case of characters.
 *
 * @author  Nik Shaylor
 * @version 1.0 04/04/2000
 */
public class DefaultCaseConverter {

   /**
     * Determines if the specified character is a lowercase character.
     * The default case converter in CLDC only supports the ISO Latin-1 
     * range of characters.
     * <p>
     * Of the ISO Latin-1 characters (character codes 0x0000 through 0x00FF),
     * the following are lowercase:
     * <p>
     * a b c d e f g h i j k l m n o p q r s t u v w x y z
     * &#92;u00DF &#92;u00E0 &#92;u00E1 &#92;u00E2 &#92;u00E3 &#92;u00E4 &#92;u00E5 &#92;u00E6 &#92;u00E7
     * &#92;u00E8 &#92;u00E9 &#92;u00EA &#92;u00EB &#92;u00EC &#92;u00ED &#92;u00EE &#92;u00EF &#92;u00F0
     * &#92;u00F1 &#92;u00F2 &#92;u00F3 &#92;u00F4 &#92;u00F5 &#92;u00F6 &#92;u00F8 &#92;u00F9 &#92;u00FA
     * &#92;u00FB &#92;u00FC &#92;u00FD &#92;u00FE &#92;u00FF
     *
     * @param   ch   the character to be tested.
     * @return  <code>true</code> if the character is lowercase;
     *          <code>false</code> otherwise.
     * @since   JDK1.0
     */
    public static boolean isLowerCase(char ch) {
        return (ch >= 'a'  && ch <= 'z')
            || (ch >= 0xDF && ch <= 0xF6)
            || (ch >= 0xF8 && ch <= 0xFF);
    }

   /**
     * Determines if the specified character is an uppercase character.
     * The default case converter in CLDC only supports the ISO Latin-1 
     * range of characters.
     * <p>
     * Of the ISO Latin-1 characters (character codes 0x0000 through 0x00FF),
     * the following are uppercase:
     * <p>
     * A B C D E F G H I J K L M N O P Q R S T U V W X Y Z
     * &#92;u00C0 &#92;u00C1 &#92;u00C2 &#92;u00C3 &#92;u00C4 &#92;u00C5 &#92;u00C6 &#92;u00C7
     * &#92;u00C8 &#92;u00C9 &#92;u00CA &#92;u00CB &#92;u00CC &#92;u00CD &#92;u00CE &#92;u00CF &#92;u00D0
     * &#92;u00D1 &#92;u00D2 &#92;u00D3 &#92;u00D4 &#92;u00D5 &#92;u00D6 &#92;u00D8 &#92;u00D9 &#92;u00DA
     * &#92;u00DB &#92;u00DC &#92;u00DD &#92;u00DE
     *
     * @param   ch   the character to be tested.
     * @return  <code>true</code> if the character is uppercase;
     *          <code>false</code> otherwise.
     * @see     java.lang.Character#isLowerCase(char)
     * @see     java.lang.Character#toUpperCase(char)
     * @since   1.0
     */
    public static boolean isUpperCase(char ch) {
        return (ch >= 'A'  && ch <= 'Z')
            || (ch >= 0xC0 && ch <= 0xD6)
            || (ch >= 0xD8 && ch <= 0xDE );
    }

    /**
     * The given character is mapped to its lowercase equivalent; if the
     * character has no lowercase equivalent, the character itself is
     * returned.  The default case converter in CLDC only supports
     * the ISO Latin-1 range of characters.
     *
     * @param   ch   the character to be converted.
     * @return  the lowercase equivalent of the character, if any;
     *          otherwise the character itself.
     * @see     java.lang.Character#isLowerCase(char)
     * @see     java.lang.Character#isUpperCase(char)
     * @see     java.lang.Character#toUpperCase(char)
     * @since   JDK1.0
     */
    public static char toLowerCase(char ch) {
        if (isUpperCase(ch)) {
            if (ch <= 'Z') {
                return (char)(ch + ('a' - 'A'));
            } else {
                return (char)(ch + 0x20);
            }
        } else {
            return ch;
        }
    }

    /**
     * Converts the character argument to uppercase; if the
     * character has no lowercase equivalent, the character itself is
     * returned.  The default case converter in CLDC only supports
     * the ISO Latin-1 range of characters.
     *
     * @param   ch   the character to be converted.
     * @return  the uppercase equivalent of the character, if any;
     *          otherwise the character itself.
     * @see     java.lang.Character#isLowerCase(char)
     * @see     java.lang.Character#isUpperCase(char)
     * @see     java.lang.Character#toLowerCase(char)
     * @since   JDK1.0
     */
    public static char toUpperCase(char ch) {
        if (isLowerCase(ch)) {
            if (ch <= 'z') {
                return (char)(ch - ('a' - 'A'));
            } else {
                return (char)(ch - 0x20);
            }
        } else {
            return ch;
        }
    }

    /**
     * Determines if the specified character is a digit.
     * This is currently only supported for ISO Latin-1 digits: "0" through "9".
     *
     * @param   ch   the character to be tested.
     * @return  <code>true</code> if the character is a digit;
     *          <code>false</code> otherwise.
     * @since   JDK1.0
     */
    public static boolean isDigit(char ch) {
        return ch >= '0' && ch <= '9';
    }

    /**
     * Returns the numeric value of the character <code>ch</code>
     * in the specified radix.
     * This is only supported for ISO Latin-1 characters.
     *
     * @param   ch      the character to be converted.
     * @param   radix   the radix.
     * @return  the numeric value represented by the character in the
     *          specified radix.
     * @see     java.lang.Character#isDigit(char)
     * @since   JDK1.0
     */
    public static int digit(char ch, int radix) {
        int value = -1;
        if (radix >= Character.MIN_RADIX && radix <= Character.MAX_RADIX) {
          if (isDigit(ch)) {
              value = ch - '0';
          }
          else if (isUpperCase(ch) || isLowerCase(ch)) {
              // Java supradecimal digit
              value = (ch & 0x1F) + 9;
          }
        }
        return (value < radix) ? value : -1;
    }

}


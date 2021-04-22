package com.ebsee.j2c;

import com.ebsee.classparser.Method;

import java.lang.reflect.Modifier;
import java.util.List;

/**
 * Java Signature. Example: (IDJ)V
 */
public class JSignature {

    private String javaArgs;
    private String javaArgsWithThis;
    private String javaResult;
    private String result;
    private List<String> args;
    int slotSize = 0;
    String javaSignature;

    public JSignature(Method m) {
        javaSignature = m.getDescriptor();
        int posA = javaSignature.indexOf('(');
        if (posA == -1) throw new IllegalArgumentException();
        int posB = javaSignature.indexOf(')');
        if (posB == -1) throw new IllegalArgumentException();

        this.javaArgs = javaSignature.substring(posA + 1, posB);
        javaArgsWithThis = javaArgs;
        if ((m.getAccessFlags() & Modifier.STATIC) == 0) {
            javaArgsWithThis = "L" + m.getClassFile().getThisClassName() + ";" + javaArgsWithThis;
        }
        this.javaResult = javaSignature.substring(posB + 1);
        this.args = Util.getJavaMethodSignatureCtypes(javaArgsWithThis);
        this.result = Util.getJavaSignatureCtype(this.javaResult);

        for (String s : args) {
            slotSize += Util.getSlot_by_Ctype(s);
        }
    }

    public String getJavaArgs() {
        return javaArgs;
    }

    public String getJavaResult() {
        return javaResult;
    }

    public List<String> getCtypeArgs() {
        return this.args;
    }


    public int getSlotSizeofArgs() {
        return slotSize;
    }

    public String getResult() {
        return this.result;
    }

    public int getSlotSizeOfResult() {
        return Util.getSlot_by_Ctype(result);
    }

    public String getCTypeOfResult() {
        return result;
    }

    public String getCTypeArgsString() {
        StringBuilder sb = new StringBuilder();
        int slot = 0;
        sb.append(Util.STR_RUNTIME_TYPE_NAME).append(" *runtime");
        for (String s : args) {
            sb.append(", ");
            sb.append(s).append(" p").append(slot);
            if (Util.LONG.equals(s) || Util.DOUBLE.equals(s)) slot++; // long & double have 2 slots
            slot++;
        }
        return sb.toString();
    }

    private String parse2ctype(String s) {
        switch (s) {
            case "V": {
                return ("void");
            }
            case "I": {
                return ("s32");
            }
            case "J": {
                return ("s64");
            }
            case "B": {
                return ("s8");
            }
            case "C": {
                return ("u16");
            }
            case "S": {
                return ("s16");
            }
            case "F": {
                return ("f32");
            }
            case "D": {
                return ("f64");
            }
            case "Z": {
                return ("s32");
            }
            default: {
                if (s.startsWith("L")) {
                    return ("struct JObject *");
                } else if (s.startsWith("[")) {
                    return ("struct JArray *");
                } else {
                    throw new RuntimeException("type error :" + s);
                }
            }
        }
    }

    public String getMethodType() {
        StringBuilder sb = new StringBuilder();
        sb.append(Util.STR_RUNTIME_TYPE_NAME).append(" *");
        for (int i = 0, imax = args.size(); i < imax; i++) {
            sb.append(",");
            String s = args.get(i);
            sb.append(s);
        }
        return result + " (*__func_p) " + "(" + sb.toString() + ")";
    }

    public static String enumArgs(List<String> types, String prefix) {
        StringBuilder tmp = new StringBuilder();
        int index = 0;
        for (int i = 0; i < types.size(); i++) {
            if (i != 0) tmp.append(", ");
            String type = types.get(i);
            tmp.append(type);
            tmp.append(" ");
            tmp.append(prefix);
            tmp.append(index++);
            if (Util.LONG.equals(type) || Util.DOUBLE.equals(type)) index++; // long & double have 2 slots
        }
        return tmp.toString();
    }

    public String getJavaSignature() {
        return javaSignature;
    }

}

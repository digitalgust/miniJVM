package test;

import org.mini.json.JsonParser;
import org.mini.util.SysLog;

import java.io.InputStream;
import java.util.ArrayList;

public class JsonParseBench {
    public static void main(String[] args) {
        JsonParseBench jp = new JsonParseBench();
        long startAt = System.currentTimeMillis();
        jp.test();
        long endAt = System.currentTimeMillis();
        System.out.println("spent: " + (endAt - startAt));
    }

    void test() {
        ArrayList<Integer> list = new ArrayList();
        JsonParser parser = new JsonParser();
        String s = readFileFromJarAsString("/test/Xian_Nv_DaoShi.gltf", "utf-8");
        for (int i = 0; i < 2_000; i++) {
            JsonParser.JsonCell result = parse(parser, s);
            list.add(result.hashCode());
        }
        long hash = 0;
        for (int i = 0; i < list.size(); i++) {
            hash += list.get(i);
        }
        System.out.println("hash=" + hash);
    }

    JsonParser.JsonCell parse(JsonParser parser, String s) {
        JsonParser.JsonCell result = parser.parse(s, 0);
        return result;
    }

    public byte[] readFileFromJar(String path) {
        InputStream is = null;
        try {
            is = this.getClass().getResourceAsStream(path);
            if (is != null) {
                int av = is.available();

                if (av >= 0) {
                    byte[] b = new byte[av];
                    int r, read = 0;
                    while (read < av) {
                        r = is.read(b, read, av - read);
                        read += r;
                    }
                    return b;
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            try {
                if (is != null) {
                    is.close();
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        SysLog.error("load from jar fail : " + path);
        return null;
    }

    public String readFileFromJarAsString(String path, String encode) {
        try {
            byte[] cont = readFileFromJar(path);
            String s = new String(cont, encode);
            return s;
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }
}

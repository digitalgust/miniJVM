/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.mini.apploader;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author Gust
 */
public class MiniHttpServer extends Thread {

    UploadCompletedHandle uploadCompletedHandle;

    ServerSocket srvsock;

    boolean exit = false;

    int port = 8088;

    String header = "HTTP/1.0 200 OK\r\nContent-Type: text/html\r\nConnection: close\r\n\r\n";
    String responseText = header + "<html>\n"
            + "<head>\n"
            + "<title>Upload jar</title>\n"
            + "<meta http-equiv='Content-Type' content='text/html; charset=utf-8'>\n"
            + "</head>\n"
            + "<body text='#000000' topmargin='10'>\n"
            + "<br>\n"
            + "<form name='form1' method='post' action='' enctype='multipart/form-data'>\n"
            + "  \n"
            + "    Choice a file： <br>\n"
            + "    <input type='file' name='link' style='width:400' class='tx1' value=''><br>\n"
            + "    <input type='submit' name='Submit' value='Upload File' class='bt'><br>\n"
            + "</form>\n"
            + "</body>\n"
            + "</html>";

    public MiniHttpServer() {
    }

    public MiniHttpServer(int port) {
        this.port = port;
    }

    public class RequestHandle extends Thread {

        Socket cltsock;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        private final HashMap<String, String> headerValues = new HashMap();
        private final HashMap<String, String> parameters = new HashMap();

        /**
         *
         * @param sock
         */
        public RequestHandle(Socket sock) {
            this.cltsock = sock;
        }

        @Override
        public void run() {
            try {
                InputStream in = cltsock.getInputStream();
                int ch = 0;
                //receive header
                while ((ch = in.read()) != -1) {
                    if (ch == '\n') {
                        String s = new String(baos.toByteArray());
                        baos.reset();
                        if (s.length() == 0) {
                            break;
                        }
                        //System.out.println("header:" + s);
                        String[] tokens = s.split(":");
                        if (tokens.length > 1) {
                            headerValues.put(tokens[0].toLowerCase().trim(), tokens[1].trim());
                        }
                    } else if (ch != '\r') {
                        baos.write(ch);
                    }
                }
                //read body
                baos.reset();
                int contentLength = getContentLength();
                if (contentLength != -1) {
                    byte[] b = new byte[2048];
                    int read;
                    while ((read = in.read(b)) != -1) {
                        baos.write(b, 0, read);
                        //System.out.print(" " + baos.size());
                        if (baos.size() == contentLength) {
                            break;
                        }
                    }
                }
                // 如果是form提交数据
                String contentType = getContentType();
                if (contentType != null) {
                    if (contentType.indexOf("application/x-www-form-urlencoded") >= 0) {
                        byte[] tmpb = baos.toByteArray();
                        parsePara(new String(tmpb, "utf-8"));
                        baos.reset();
                    } else if (contentType.indexOf("multipart/form-data") >= 0) {
                        byte[] tmpb = baos.toByteArray();
                        try {
                            MultipartFormData parser = new MultipartFormData();
                            parser.parseData(this, tmpb);
                            //
                            try {
                                if (uploadCompletedHandle != null) {
                                    uploadCompletedHandle.onCompleted(parser.getFiles());
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        } catch (Exception e) {
                        }
                        baos.reset();
                    }

                }
                //
                OutputStream out = cltsock.getOutputStream();
                String sbuf = responseText;
                int sent = 0;
                while ((sent) < sbuf.length()) {
                    out.write(sbuf.charAt(sent));
                    sent++;
                }
                cltsock.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }

        private void parsePara(String s) {

            String[] tokens;
            String[] ps = s.split("&");
            for (String para : ps) {
                if (para.indexOf('=') > 0) {
                    tokens = para.split("=");
                    if (tokens.length > 1) {
                        try {
                            tokens[0] = URLDecoder.decode(tokens[0], "utf-8");
                            tokens[1] = URLDecoder.decode(tokens[1], "utf-8");
                        } catch (Exception ex) {
                        }
                        parameters.put(tokens[0], tokens[1]);
                    }
                }
            }
        }

        public String getContentType() {
            String str = headerValues.get("content-type");
            return (str != null) ? str : "";
        }

        public int getContentLength() {
            String lengthStr = headerValues.get("content-length");
            return (lengthStr != null) ? Integer.parseInt(lengthStr) : -1;
        }

        public Map<String, String> getHeaders() {
            return headerValues;
        }

        public String getParameter(String pName) {
            return parameters.get(pName);
        }

        public void putParameter(String k, String v) {
            parameters.put(k, v);
        }
    }

    public class MultipartFormData {

        final static String BOUNDARY_TAG = "boundary";
        final static String DISPOSITION_TAG = "content-disposition";
        final static String FORMDATA_TAG = "form-data";
        final static String ATTACHMENT_TAG = "attachment";
        final static String CONTENT_TYPE_TAG = "content-type";
        final static String FIELD_NAME_TAG = "name";
        final static String FILE_NAME_TAG = "filename";
        final static String MIXED_TAG = "multipart/mixed";
        //

        final static String FORM_DECODE = "utf-8";
        final static String FORM_ENCODE = "GBK";
        //
        byte[] key0 = new byte[]{'\r', '\n'};
        //
        //
        private final List<UploadFile> files = new ArrayList();
        RequestHandle request;

        /**
         *
         * @param request
         * @param data
         */
        public void parseData(MiniHttpServer.RequestHandle request, byte[] data) {
            this.request = request;
            if (data != null) {
                ContentReader creader = new ContentReader(data);
                String ct = request.getContentType();
                if (!ct.contains(BOUNDARY_TAG)) {
                    return;
                }
                String boundary = ct.substring(ct.indexOf("=") + 1);

                byte[] tmpb = null;
                while ((tmpb = creader.readUntil(boundary)) != null) {
                    if (tmpb.length > 0) {
                        BoundPart bp = new BoundPart();
                        bp.parse(tmpb);
                    }
                }

            }
        }

        /**
         * @return the files
         */
        public List<UploadFile> getFiles() {
            return files;
        }

        /**
         * 区块
         */
        class BoundPart {

            int pos = 0;

            BoundPart() {
            }

            void parse(byte[] data) {
                ContentReader creader = new ContentReader(data);
                Map<String, String> map = new HashMap();
                while (true) {//找出所有字段及文件

                    String line = creader.readLine();
                    if (line == null) {
                        return;
                    }
                    line = line.trim();
                    //是空行
                    if (line.length() == 0) {
                        break;
                    }
                    if (line.toLowerCase().startsWith(DISPOSITION_TAG)) {
                        String[] strs = line.split(";");

                        for (String str : strs) {
                            int splitPos = str.indexOf('=');
                            if (splitPos < 0) {
                                splitPos = str.indexOf(':');
                            }
                            if (splitPos < 0) {
                                continue;
                            }
                            String[] kv = new String[2];
                            kv[0] = str.substring(0, splitPos).replace('"', ' ');
                            kv[1] = str.substring(splitPos + 1).replace('"', ' ');
                            map.put(kv[0].trim().toLowerCase(), kv[1].trim());
                        }
                    }
                }
                String fileName = map.get(FILE_NAME_TAG);
                //get filename from path
                fileName = fileName.replace('\\', '/');
                fileName = fileName.indexOf('/') > 0 ? fileName.substring(fileName.indexOf('/') + 1) : fileName;
                //
                String fieldName = map.get(FIELD_NAME_TAG);
                byte[] b = creader.readRemain();
                if (fileName != null) { //是文件
                    UploadFile uf = new UploadFile();
                    uf.data = b;
                    uf.filename = fileName;
                    uf.type = map.get(CONTENT_TYPE_TAG);
                    uf.field = fieldName;
                    getFiles().add(uf);
                } else {
                    //查是否是form字段还是mixed的文件传输
                    String ct = map.get(CONTENT_TYPE_TAG);
                    if (ct != null && ct.toLowerCase().indexOf(MIXED_TAG) > 0) {//是复合类型
                        //找新的分离串
                        String boundary = ct.substring(ct.indexOf("=") + 1);
                        ContentReader subreader = new ContentReader(b);
                        byte[] tmpb = null;
                        while ((tmpb = subreader.readUntil(boundary)) != null) {
                            if (tmpb.length > 0) {
                                BoundPart bp = new BoundPart();
                                bp.parse(tmpb);
                            }
                        }
                    } else {
                        try {
                            String val = new String(b, FORM_DECODE);
                            request.putParameter(fieldName, val);
                        } catch (UnsupportedEncodingException ex) {
                        }
                    }
                }
            }
        }

        /**
         * 内容读入器
         */
        class ContentReader {

            byte[] data;
            int pos = 0;

            ContentReader(byte[] pd) {
                data = pd;
            }

            String readLine() {
                int tmppos = MiniHttpServer.binSearch(data, key0, pos);
                if (tmppos < 0) {
                    return null;
                }
                String line = null;
                try {
                    line = new String(data, pos, tmppos - pos, FORM_DECODE);
                    pos = tmppos + key0.length;
                } catch (UnsupportedEncodingException ex) {
                    return null;
                }
                line = line.trim();
                return line;
            }

            byte[] readUntil(String boundary) {
                if (pos >= data.length) {
                    return null;
                }
                String bound_spit = "--" + boundary + "\r\n";
                String bound_end = "--" + boundary + "--";
                int boundLen = bound_spit.length();
                int nextpos = MiniHttpServer.binSearch(data, bound_spit.getBytes(), pos);
                int endpos = MiniHttpServer.binSearch(data, bound_end.getBytes(), pos);
                if (nextpos < 0) {
                    if (endpos > 0) { //如果下个块就是尾巴,则读到尾
                        boundLen = bound_end.length();
                        nextpos = endpos;
                    } else {
                        return null;
                    }
                }
                int len = nextpos - pos;
                int start = pos;
                pos += len + boundLen;
                if (len >= key0.length) {
                    len -= key0.length;//去掉末尾的回车换行
                }
                byte[] tmp = new byte[len];
                System.arraycopy(data, start, tmp, 0, len);
                return tmp;
            }

            byte[] readRemain() {
                int len = data.length - pos;
                byte[] tmp = new byte[len];
                System.arraycopy(data, pos, tmp, 0, len);
                pos += len;
                return tmp;
            }
        }

    }

    /**
     * 收到的文件
     */
    public class UploadFile {

        public String type;
        public String filename;
        public String field;
        public byte[] data;
    };

    public interface UploadCompletedHandle {

        void onCompleted(List<UploadFile> files);
    }

    @Override
    public void run() {
        try {
            srvsock = new ServerSocket(port);
            System.out.println("server socket listen " + port + " ...");
            while (!exit) {
                try {
                    Socket cltsock;
                    try {
                        cltsock = srvsock.accept();
                    } catch (IOException e) {
                        exit = true;
                        e.printStackTrace();
                        break;
                    }
                    System.out.println("accepted client socket:" + cltsock);

                    RequestHandle handler = new RequestHandle(cltsock);
                    handler.start();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            System.out.println("server " + port + " closed");
            srvsock.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * @return the uploadCompletedHandle
     */
    public UploadCompletedHandle getUploadCompletedHandle() {
        return uploadCompletedHandle;
    }

    /**
     * @param aUploadCompletedHandle the uploadCompletedHandle to set
     */
    public void setUploadCompletedHandle(UploadCompletedHandle aUploadCompletedHandle) {
        uploadCompletedHandle = aUploadCompletedHandle;
    }

    public void stopServer() {
        try {
            srvsock.close();
        } catch (Exception e) {
        }
        exit = true;
    }

    public boolean isRunning() {
        return !exit;
    }

    public int getPort() {
        return port;
    }

    /**
     * 在src中搜索key
     *
     * @param src
     * @param key
     * @return
     */
    static public int binSearch(byte[] src, byte[] key, int startPos) {
        if (src == null || key == null || src.length == 0 || key.length == 0 || startPos >= src.length) {
            return -1;
        }
        for (int i = startPos, iLen = src.length - key.length; i <= iLen; i++) {
            if (src[i] == key[0]) {
                boolean march = true;
                for (int j = 1; j < key.length; j++) {
                    if (src[i + j] != key[j]) {
                        march = false;
                    }
                }
                if (march) {
                    return i;
                }
            }
        }
        return -1;
    }
}

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.mini.http;

import org.mini.reflect.ReflectArray;
import org.mini.util.SysLog;
import org.mini.vm.RefNative;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * @author Gust
 */
public class MiniHttpServer extends Thread {

    UploadCompletedHandle uploadCompletedHandle;

    ServerSocket srvsock;

    boolean exit = false;

    public static final int DEFAULT_PORT = 18088;
    public static final SrvLogger DEFAULT_LOGGER = new SrvLogger() {
        @Override
        public void log(String s) {
            SysLog.info(s);
        }
    };
    int port = DEFAULT_PORT;
    SrvLogger logger = DEFAULT_LOGGER;
    UserServlet userServlet;

    public MiniHttpServer() {
        this(DEFAULT_PORT, DEFAULT_LOGGER);
    }

    public MiniHttpServer(int port, SrvLogger log) {
        this.port = port;
        if (log != null)
            this.logger = log;
    }

    abstract static public class SrvLogger {
        public abstract void log(String s);
    }

    public UserServlet getUserServlet() {
        return userServlet;
    }

    public void setUserServlet(UserServlet userServlet) {
        this.userServlet = userServlet;
    }

    public static class HttpHandler extends Thread {
        MiniHttpServer server;
        Socket cltsock;

        HttpRequest req;
        HttpResponse res;
        OutputStream out = null;

        UserServlet userServlet;
        static final String header = "HTTP/1.0 200 OK\r\n";

        /**
         * @param sock
         */
        public HttpHandler(MiniHttpServer server, Socket sock) {
            this.server = server;
            this.cltsock = sock;
            cltsock.setSoLinger(true, 30);
            req = new HttpRequest();
            req.handler = this;
            res = new HttpResponse();
            res.handler = this;
            userServlet = server.getUserServlet();
        }

        @Override
        public void run() {
            InputStream in = null;
            try {
                in = cltsock.getInputStream();
                int ch = 0;
                // receive header
                while ((ch = in.read()) != -1) {
                    if (ch == '\n') {
                        String s = new String(req.baos.toByteArray());
                        req.baos.reset();
                        if (s.length() == 0) {
                            break;
                        }
                        if (s.startsWith("GET") || s.startsWith("POST")) {
                            req.setRequests(s);
                            continue;
                        }
                        // System.out.println("header:" + s);
                        String[] tokens = s.split(":");
                        if (tokens.length > 1) {
                            req.requestHeader.put(tokens[0].toLowerCase().trim(), tokens[1].trim());
                        }
                    } else if (ch != '\r') {
                        req.baos.write(ch);
                    }
                }
                // read body
                req.baos.reset();
                int contentLength = req.getContentLength();
                if (contentLength != -1) {
                    int part10percent = contentLength / 10;
                    int p = 1;

                    byte[] b = new byte[4096];
                    int read;
                    while ((read = in.read(b)) != -1) {
                        req.baos.write(b, 0, read);
                        // System.out.print(" " + baos.size());
                        if (req.baos.size() == contentLength) {
                            break;
                        }

                        if (contentLength / part10percent > p) {
                            p++;
                            server.logger.log("Received http data " + p + "0%");
                        }
                    }
                    server.logger.log("Received http data " + req.baos.size());
                }

                req.parsePostPara();
                HttpSession session = server.getSession(req);
                if (session != null) {
                    session.save2Client(res);
                }

                boolean ret = false;
                if (userServlet != null) {
                    ret = userServlet.doHttp(req, res);
                }
                if (!ret) {
                    userServlet = new DefaultUserServlet();
                    userServlet.doHttp(req, res);
                }
                //
                if (out == null) {
                    out = getOutputStream();
                }
                out.flush();
            } catch (IOException ex) {
                ex.printStackTrace();
            } finally {
                if (out != null) {
                    try {
                        out.close();
                    } catch (IOException ignored) {
                    }
                }
                if (in != null) {
                    try {
                        in.close();
                    } catch (IOException ignored) {
                    }
                }
                try {
                    cltsock.close();
                } catch (IOException ignored) {
                }
            }
        }

        public OutputStream getOutputStream() {
            if (out == null) {
                try {
                    out = cltsock.getOutputStream();
                    out.write(header.getBytes());
                    for (String k : res.responseHeader.keySet()) {
                        out.write((k + ": " + res.responseHeader.get(k) + "\r\n").getBytes());
                    }
                    //out.write(("Content-Length: " + res.baos.size() + "\r\n\r\n").getBytes());
                    out.write(("\r\n").getBytes());

                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
            return out;
        }

    }

    public abstract static class UserServlet {

        public static void parsePara(HttpRequest req, String s) {

            String[] tokens;
            String[] ps = s.split("&");
            for (String para : ps) {
                if (para.indexOf('=') > 0) {
                    tokens = para.split("=");
                    if (tokens.length > 1) {
                        try {
                            tokens[0] = decode(tokens[0], "utf-8");
                            tokens[1] = decode(tokens[1], "utf-8");
                        } catch (Exception ex) {
                        }
                        req.parameters.put(tokens[0], tokens[1]);
                    }
                }
            }
        }

        public static String decode(String s, String enc)
                throws UnsupportedEncodingException {

            boolean needToChange = false;
            int numChars = s.length();
            StringBuffer sb = new StringBuffer(numChars > 500 ? numChars / 2 : numChars);
            int i = 0;

            if (enc.length() == 0) {
                throw new UnsupportedEncodingException("URLDecoder: empty string enc parameter");
            }

            char c;
            byte[] bytes = null;
            while (i < numChars) {
                c = s.charAt(i);
                switch (c) {
                    case '+':
                        sb.append(' ');
                        i++;
                        needToChange = true;
                        break;
                    case '%':
                        /*
                         * Starting with this instance of %, process all
                         * consecutive substrings of the form %xy. Each
                         * substring %xy will yield a byte. Convert all
                         * consecutive bytes obtained this way to whatever
                         * character(s) they represent in the provided
                         * encoding.
                         */

                        try {

                            // (numChars-i)/3 is an upper bound for the number
                            // of remaining bytes
                            if (bytes == null)
                                bytes = new byte[(numChars - i) / 3];
                            int pos = 0;

                            while (((i + 2) < numChars) &&
                                    (c == '%')) {
                                bytes[pos++] = (byte) Integer.parseInt(s.substring(i + 1, i + 3), 16);
                                i += 3;
                                if (i < numChars)
                                    c = s.charAt(i);
                            }

                            // A trailing, incomplete byte encoding such as
                            // "%x" will cause an exception to be thrown

                            if ((i < numChars) && (c == '%'))
                                throw new IllegalArgumentException(
                                        "URLDecoder: Incomplete trailing escape (%) pattern");

                            sb.append(new String(bytes, 0, pos, enc));
                        } catch (NumberFormatException e) {
                            throw new IllegalArgumentException(
                                    "URLDecoder: Illegal hex characters in escape (%) pattern - "
                                            + e.getMessage());
                        }
                        needToChange = true;
                        break;
                    default:
                        sb.append(c);
                        i++;
                        break;
                }
            }

            return (needToChange ? sb.toString() : s);
        }

        public boolean doHttp(HttpRequest req, HttpResponse res) throws IOException {
            return false;
        }
    }

    public static class DefaultUserServlet extends UserServlet {

        String responseText = "<html>\n"
                + "<head>\n"
                + "<title>Upload jar</title>\n"
                + "<meta http-equiv='Content-Type' content='text/html; charset=utf-8'>\n"
                + "</head>\n"
                + "<body text='#000000' topmargin='10'>\n"
                + "<br>\n"
                + "Please select miniJVM plugin *.jar to upload and install.<br/>\n"
                + "The root directory of this jar file must contain a config.txt file.<br/>\n"
                + "<form name='form1' method='post' action='' enctype='multipart/form-data'/>\n"
                + "  \n"
                + "1.    Choice a jar file. <br/>\n"
                + "    <input type='file' name='link' style='width:400' class='tx1' value=''/><br/>\n"
                + "2.    Upload file when selected a jar. <br/>\n"
                + "    <input type='submit' name='Submit' value='Upload File' class='bt'/><br/>\n"
                + "</form>\n"
                + "</body>\n"
                + "</html>";

        @Override
        public boolean doHttp(HttpRequest req, HttpResponse res) throws IOException {
            // 如果是form提交数据
            if ("/".equals(req.getPath())) {
                String contentType = req.getContentType();
                if (contentType != null) {
                    if (contentType.indexOf("application/x-www-form-urlencoded") >= 0) {
                        byte[] tmpb = req.baos.toByteArray();
                        parsePara(req, new String(tmpb, "utf-8"));
                        req.baos.reset();
                    } else if (contentType.indexOf("multipart/form-data") >= 0) {
                        byte[] tmpb = req.baos.toByteArray();
                        try {
                            MultipartFormData parser = new MultipartFormData();
                            parser.parseData(req, tmpb);
                            //
                            try {
                                if (req.handler.server.uploadCompletedHandle != null) {
                                    req.handler.server.uploadCompletedHandle.onCompleted(parser.getFiles());
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        } catch (Exception e) {
                        }
                        req.baos = new ByteArrayOutputStream();
                    }

                }

                res.responseHeader.put("Content-Type", "text/html");
                res.responseHeader.put("Connection", "close");
                res.responseHeader.put("Server", "MiniHttpServer");
                res.getOutputStream().write(responseText.getBytes("utf-8"));
                return true;
            }
            return false;
        }
    }

    public static class HttpResponse {
        HttpHandler handler;
        public final HashMap<String, String> responseHeader = new HashMap();

        public OutputStream getOutputStream() {
            return handler.getOutputStream();
        }

        public void setResponseHeader(String key, String value) {
            responseHeader.put(key, value);
        }

        public String getResponseHeader(String key) {
            return responseHeader.get(key);
        }
    }

    public static class HttpRequest {
        public static final int UNKNOWN = 0;
        public static final int GET = 1;
        public static final int POST = 2;
        HttpHandler handler;
        private String sessionId;
        public final HashMap<String, String> requestHeader = new HashMap();
        public final HashMap<String, String> parameters = new HashMap();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        String requests = null;
        int requestType = UNKNOWN;
        String path = null;

        public void setRequests(String req) {
            requests = req;
            if (requests != null) {
                if (requests.startsWith("POST"))
                    requestType = POST;
                if (requests.startsWith("GET"))
                    requestType = GET;
            }
            path = requests.substring(requests.indexOf(" ") + 1);
            path = path.substring(0, path.indexOf(" "));
            if (path.indexOf("?") >= 0) {
                int i = path.indexOf("?");
                String params = path.substring(i + 1);
                UserServlet.parsePara(this, params);
                path = path.substring(0, i);
            }
        }

        public String getContentType() {
            String str = requestHeader.get("content-type");
            return (str != null) ? str : "";
        }

        public int getContentLength() {
            String lengthStr = requestHeader.get("content-length");
            return (lengthStr != null) ? Integer.parseInt(lengthStr) : -1;
        }

        public Map<String, String> getHeaders() {
            return requestHeader;
        }

        public String getParameter(String pName) {
            return parameters.get(pName);
        }

        public void putParameter(String k, String v) {
            parameters.put(k, v);
        }

        public int getRequestType() {
            return requestType;
        }

        public String getPath() {
            return path;
        }

        void parsePostPara() {
            String contentType = getContentType();
            if (contentType != null) {
                if (contentType.indexOf("application/x-www-form-urlencoded") >= 0) {
                    byte[] tmpb = baos.toByteArray();
                    try {
                        String params = new String(tmpb, "utf-8");
                        UserServlet.parsePara(this, params);
                        baos.reset();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }


        public HttpSession getSession(boolean create) {
            HttpSession s = null;
            if (sessionId == null) {
                if (create) {
                    s = handler.server.getSession(this);
                    if (s != null) {
                        sessionId = s.getSessionId();
                    }
                }
            } else {
                s = handler.server.getSession(sessionId);
            }
            return s;
        }

    }

    static class MultipartFormData {

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
        HttpRequest req;

        /**
         * @param req
         * @param data
         */
        public void parseData(HttpRequest req, byte[] data) {
            this.req = req;
            if (data != null) {
                ContentReader creader = new ContentReader(data);
                String ct = req.getContentType();
                if (!ct.contains(BOUNDARY_TAG)) {
                    return;
                }
                String boundary = ct.substring(ct.indexOf("=") + 1);
                try {
                    FileOutputStream fos = new FileOutputStream("tmp.dat");
                    fos.write(data);
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }

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
                while (true) {// 找出所有字段及文件

                    String line = creader.readLine();
                    if (line == null) {
                        return;
                    }
                    line = line.trim();
                    // 是空行
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
                //
                String fieldName = map.get(FIELD_NAME_TAG);
                byte[] b = creader.readRemain();
                if (fileName != null) { // 是文件
                    // get filename from path
                    fileName = fileName.replace('\\', '/');
                    fileName = fileName.indexOf('/') > 0 ? fileName.substring(fileName.lastIndexOf('/') + 1) : fileName;
                    //
                    UploadFile uf = new UploadFile();
                    uf.data = b;
                    uf.filename = fileName;
                    uf.type = map.get(CONTENT_TYPE_TAG);
                    uf.field = fieldName;
                    getFiles().add(uf);
                } else {
                    // 查是否是form字段还是mixed的文件传输
                    String ct = map.get(CONTENT_TYPE_TAG);
                    if (ct != null && ct.toLowerCase().indexOf(MIXED_TAG) > 0) {// 是复合类型
                        // 找新的分离串
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
                            req.putParameter(fieldName, val);
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
                long dataAddr = ReflectArray.getBodyPtr(data);
                long keyAddr = ReflectArray.getBodyPtr(key0);
                int tmppos = RefNative.heap_bin_search(dataAddr + pos, data.length - pos, keyAddr, key0.length);// MiniHttpServer.binSearch(data,
                // key0,
                // pos);
                if (tmppos < 0) {
                    return null;
                }
                String line = null;
                try {
                    line = new String(data, pos, tmppos, FORM_DECODE);
                    pos += tmppos + key0.length;
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

                byte[] bound_split = ("--" + boundary + "\r\n").getBytes();
                byte[] bound_end = ("--" + boundary + "--").getBytes();

                long dataAddr = ReflectArray.getBodyPtr(data);
                long splitAddr = ReflectArray.getBodyPtr(bound_split);
                long endAddr = ReflectArray.getBodyPtr(bound_end);

                int boundLen = bound_split.length;
                int nextpos = RefNative.heap_bin_search(dataAddr + pos, data.length - pos, splitAddr,
                        bound_split.length);// MiniHttpServer.binSearch(data, bound_split.getBytes(), pos);
                if (nextpos < 0) {
                    int endpos = RefNative.heap_bin_search(dataAddr + pos, data.length - pos, endAddr,
                            bound_end.length);// MiniHttpServer.binSearch(data, bound_end.getBytes(), pos);
                    if (endpos > 0) { // 如果下个块就是尾巴,则读到尾
                        boundLen = bound_end.length;
                        nextpos = endpos;
                    } else {
                        return null;
                    }
                }
                int len = nextpos;
                int start = pos;
                pos += len + boundLen;
                if (len >= key0.length) {
                    len -= key0.length;// 去掉末尾的回车换行
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
    static public class UploadFile {

        public String type;
        public String filename;
        public String field;
        public byte[] data;
    }

    ;

    public interface UploadCompletedHandle {

        void onCompleted(List<UploadFile> files);
    }

    @Override
    public void run() {
        try {
            srvsock = new ServerSocket(port);
            logger.log("server socket listen " + port + " ...");
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
                    logger.log("accepted client socket:" + cltsock);

                    HttpHandler handler = new HttpHandler(this, cltsock);
                    handler.start();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            logger.log("server " + port + " closed");
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
        int keyLastPos = key.length - 1;
        for (int i = startPos, iLen = src.length - key.length; i <= iLen; i++) {
            if (src[i] == key[0] && src[i + keyLastPos] == key[keyLastPos]) {
                boolean march = true;
                for (int j = 1; j < keyLastPos; j++) {
                    if (src[i + j] != key[j]) {
                        march = false;
                        break;
                    }
                }
                if (march) {
                    return i;
                }
            }
        }
        return -1;
    }

    /**
     * ====================================================
     * cookie session
     */

    private Map<String, HttpSession> sessions = Collections.synchronizedMap(new HashMap<String, HttpSession>());
    private static Random ran = new Random();
    private long lastRemoveAt = 0;
    private static long PERIOD_REMOVE = 5 * 60 * 1000; //删除间隔

    String getSessionId() {
        String is = null;
        while (is == null) {
            long i1 = Math.abs(ran.nextInt());
            long i2 = Math.abs(ran.nextInt());
            is = Long.toHexString((i1 << 32) | i2);
            if (sessions.containsKey(is)) {
                is = null;
            }
        }
        return is;
    }

    // Set-Cookie:
    // PREF=ID=0565f77e132de138:NW=1:TM=10Array808264Array:LM=10Array808264Array:S=KaeaCFPo4ArrayRiA_d8;
    // expires=Sun, 17-Jan-2038 1Array:14:07 GMT; path=/; domain=.google.com
    public HttpSession getSession(HttpRequest req) {
        HttpSession session = null;
        String s = req.getHeaders().get("Cookie");
        if (s == null) {
            session = new HttpSession(req.handler.server);
            putSession(session.getId(), session);
        } else {
            String[] strs = s.split(";");
            for (String item : strs) {
                String t = item.trim();
                int eqpos = t.indexOf("=");
                if ((t.startsWith(HttpSession.SESSION_FLAG) || t.startsWith(HttpSession.JSESSION_FLAG)) && eqpos >= 0) {
                    String id = t.substring(eqpos + 1);
                    session = sessions.get(id);
                    if (session != null) {
                        session.clientKnowMe();
                    } else {
                        session = new HttpSession(id);
                        session.clientKnowMe();
                        putSession(id, session);
                    }
                    break;
                }
            }
            if (session == null) {
                session = new HttpSession(req.handler.server);
                putSession(session.getId(), session);
            }
        }
        if (session != null) {
            req.sessionId = session.getSessionId();
        }
        return session;
    }

    public HttpSession getSession(String id) {
        return sessions.get(id);
    }

    /**
     * 在添加时进行检测过期session移除
     *
     * @param id
     * @param s
     */
    protected synchronized void putSession(String id, HttpSession s) {
        if (s != null && sessions.get(id) == null) {
            sessions.put(id, s);
        }
    }

    synchronized void removeSession(String id) {
        sessions.remove(id);
        removeExpire();
    }

    void removeExpire() {
        long curMils = System.currentTimeMillis();
        if (curMils < lastRemoveAt + PERIOD_REMOVE) {
            //周期性删除
            return;
        }
        lastRemoveAt = curMils;
        // 删除过期元素
        for (Iterator<HttpSession> it = sessions.values().iterator(); it.hasNext(); ) {
            HttpSession elem = it.next();
            if (elem.expire < curMils) {
                it.remove();
                sessions.remove(elem.getSessionId());
            }
        }
    }

    static public String time2Str(long t) {
        if (t <= 0) {
            t = System.currentTimeMillis();
        }
        SimpleDateFormat gmtOutFmt = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss z");
        Calendar cd = Calendar.getInstance();
        cd.setTimeInMillis(t);
        gmtOutFmt.setTimeZone(TimeZone.getTimeZone("GMT"));
        String timeStr = gmtOutFmt.format(cd.getTime());
        return timeStr;
    }

    public static class HttpSession {

        // ------------------------------------------------------------
        // 此部分为Session功能
        static final int SESSION_DEFAULT_INTERVAL = 30 * 60; // second
        static final String SESSION_FLAG = "SESSIONID";
        static final String JSESSION_FLAG = "JSESSIONID";
        String sessionId;
        long expire;// ms
        long lastAccess = 0;// ms
        int interval;
        Map<String, Object> kv = new HashMap();
        MiniHttpServer server;

        // ------------------------------------------------------------
        public HttpSession(MiniHttpServer server) {
            this.server = server;
            sessionId = server.getSessionId();
            setMaxInactiveInterval(SESSION_DEFAULT_INTERVAL);
        }

        public HttpSession(String pSessionid) {
            sessionId = pSessionid;
            setMaxInactiveInterval(SESSION_DEFAULT_INTERVAL);
        }

        public int getAttributeSize() {
            return kv.size();
        }

        public String getId() {
            return sessionId;
        }

        public void setAttribute(String name, Object value) {
            kv.put(name, value);
        }

        // 得到值
        public Object getAttribute(String name) {
            return kv.get(name);
        }

        public long getCreationTime() {
            return expire - interval * 1000;
        }

        public long getLastAccessedTime() {
            return lastAccess;
        }

        /**
         * 设置session保留时间
         *
         * @param interval
         */
        final public void setMaxInactiveInterval(int interval) {
            expire = System.currentTimeMillis() + interval * 1000;
            this.interval = interval;
            lastAccess = 0;
        }

        public int getInterval() {
            return interval;
        }

        /**
         * @return the sessionId
         */
        public String getSessionId() {
            return sessionId;
        }

        /**
         * @param sessionId the sessionId to set
         */
        public void setSessionId(String sessionId) {
            this.sessionId = sessionId;
        }

        void invalidate() {
            server.removeSession(sessionId);
        }

        /**
         * 客户端是否知道我,如果cookie被禁用,则每次请求都为不可知
         *
         * @return
         */
        public boolean isNew() {
            return lastAccess == 0;
        }

        void clientKnowMe() {
            lastAccess = System.currentTimeMillis();
        }

        public void save2Client(HttpResponse res) {
            if (isNew()) {
                StringBuilder sb = new StringBuilder();
                sb.append(SESSION_FLAG).append("=").append(sessionId).append(";");
                sb.append(" expires=").append(MiniHttpServer.time2Str(expire)).append(";");
                sb.append(" path=/;");
                // sb.append("domain=.a.com;");
                res.setResponseHeader("Set-Cookie", sb.toString());
                // Log.info(Log.RUNTIME,  "SYS", "Set-Cookie="+sb.toString());
            }
        }

        @Override
        public String toString() {
            return sessionId;
        }
    }

}

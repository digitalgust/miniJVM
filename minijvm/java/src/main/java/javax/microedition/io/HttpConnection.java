/* SPDX-License-Identifier: MIT */
package javax.microedition.io;

import java.io.IOException;

public interface HttpConnection extends ContentConnection {
    String HEAD = "HEAD";
    String GET = "GET";
    String POST = "POST";

    int HTTP_OK = 200;
    int HTTP_CREATED = 201;
    int HTTP_ACCEPTED = 202;
    int HTTP_NOT_AUTHORITATIVE = 203;
    int HTTP_NO_CONTENT = 204;
    int HTTP_RESET = 205;
    int HTTP_PARTIAL = 206;
    int HTTP_MULT_CHOICE = 300;
    int HTTP_MOVED_PERM = 301;
    int HTTP_MOVED_TEMP = 302;
    int HTTP_SEE_OTHER = 303;
    int HTTP_NOT_MODIFIED = 304;
    int HTTP_USE_PROXY = 305;
    int HTTP_TEMP_REDIRECT = 307;
    int HTTP_BAD_REQUEST = 400;
    int HTTP_UNAUTHORIZED = 401;
    int HTTP_PAYMENT_REQUIRED = 402;
    int HTTP_FORBIDDEN = 403;
    int HTTP_NOT_FOUND = 404;
    int HTTP_BAD_METHOD = 405;
    int HTTP_NOT_ACCEPTABLE = 406;
    int HTTP_PROXY_AUTH = 407;
    int HTTP_CLIENT_TIMEOUT = 408;
    int HTTP_CONFLICT = 409;
    int HTTP_GONE = 410;
    int HTTP_LENGTH_REQUIRED = 411;
    int HTTP_PRECON_FAILED = 412;
    int HTTP_ENTITY_TOO_LARGE = 413;
    int HTTP_REQ_TOO_LONG = 414;
    int HTTP_UNSUPPORTED_TYPE = 415;
    int HTTP_UNSUPPORTED_RANGE = 416;
    int HTTP_EXPECT_FAILED = 417;
    int HTTP_INTERNAL_ERROR = 500;
    int HTTP_NOT_IMPLEMENTED = 501;
    int HTTP_BAD_GATEWAY = 502;
    int HTTP_UNAVAILABLE = 503;
    int HTTP_GATEWAY_TIMEOUT = 504;
    int HTTP_VERSION = 505;

    String getURL();
    String getProtocol();
    String getHost();
    String getFile();
    String getRef();
    String getQuery();
    int getPort();
    String getRequestMethod();
    void setRequestMethod(String method) throws IOException;
    String getRequestProperty(String key);
    void setRequestProperty(String key, String value) throws IOException;
    int getResponseCode() throws IOException;
    String getResponseMessage() throws IOException;
    long getExpiration() throws IOException;
    long getDate() throws IOException;
    long getLastModified() throws IOException;
    String getHeaderField(String name) throws IOException;
    int getHeaderFieldInt(String name, int defaultValue) throws IOException;
    long getHeaderFieldDate(String name, long defaultValue) throws IOException;
    String getHeaderField(int index) throws IOException;
    String getHeaderFieldKey(int index) throws IOException;
}

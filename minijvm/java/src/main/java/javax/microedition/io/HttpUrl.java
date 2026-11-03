/* SPDX-License-Identifier: MIT */
package javax.microedition.io;

import java.io.IOException;

/** Small URI parser used by the MIDP HTTP protocol implementation. */
public final class HttpUrl {
    public String scheme;
    public String authority;
    public String path;
    public String query;
    public String fragment;
    public String host;
    public int port = -1;
    public String machine;
    public String domain;

    public HttpUrl(String url) {
        parse(url, null);
    }

    public HttpUrl(String defaultScheme, String partialUrl) {
        parse(partialUrl, defaultScheme);
    }

    private void parse(String input, String defaultScheme) {
        scheme = defaultScheme;
        if (input == null || input.length() == 0) {
            return;
        }
        if (input.indexOf(' ') >= 0) {
            throw new IllegalArgumentException("Space character in URL");
        }
        int cursor = 0;
        int schemeColon = input.indexOf(':');
        int firstDelimiter = firstOf(input, '/', '?', '#');
        if (schemeColon > 0 && (firstDelimiter < 0 || schemeColon < firstDelimiter)) {
            scheme = input.substring(0, schemeColon);
            cursor = schemeColon + 1;
        }

        int fragmentMark = input.indexOf('#', cursor);
        int end = fragmentMark < 0 ? input.length() : fragmentMark;
        if (fragmentMark >= 0 && fragmentMark + 1 < input.length()) {
            fragment = input.substring(fragmentMark + 1);
        }

        int queryMark = input.indexOf('?', cursor);
        if (queryMark >= 0 && queryMark < end) {
            if (queryMark + 1 < end) {
                query = input.substring(queryMark + 1, end);
            }
            end = queryMark;
        }

        if (cursor + 1 < end && input.charAt(cursor) == '/' &&
                input.charAt(cursor + 1) == '/') {
            int authorityStart = cursor + 2;
            int pathStart = input.indexOf('/', authorityStart);
            if (pathStart < 0 || pathStart > end) {
                pathStart = end;
            }
            if (authorityStart < pathStart) {
                authority = input.substring(authorityStart, pathStart);
                parseAuthority(authority);
            }
            cursor = pathStart;
        }
        if (cursor < end) {
            path = input.substring(cursor, end);
        }
    }

    private void parseAuthority(String value) {
        int hostStart = value.lastIndexOf('@') + 1;
        int hostEnd = value.length();
        int portStart = -1;
        if (hostStart < value.length() && value.charAt(hostStart) == '[') {
            int closing = value.indexOf(']', hostStart + 1);
            if (closing < 0) {
                throw new IllegalArgumentException("Unclosed IPv6 address");
            }
            hostEnd = closing + 1;
            if (hostEnd < value.length()) {
                if (value.charAt(hostEnd) != ':') {
                    throw new IllegalArgumentException("Invalid authority");
                }
                portStart = hostEnd + 1;
            }
        } else {
            int colon = value.indexOf(':', hostStart);
            if (colon >= 0) {
                hostEnd = colon;
                portStart = colon + 1;
            }
        }
        if (hostEnd <= hostStart) {
            return;
        }
        host = value.substring(hostStart, hostEnd);
        // An empty port keeps the legacy unspecified-port value (-1).
        if (portStart >= 0 && portStart < value.length()) {
            try {
                port = Integer.parseInt(value.substring(portStart));
            } catch (NumberFormatException invalid) {
                throw new IllegalArgumentException("Invalid port");
            }
            if (port <= 0 || port > 65535) {
                throw new IllegalArgumentException("Port out of range");
            }
        }
        if (host.charAt(0) != '[' && !Character.isDigit(host.charAt(0))) {
            int dot = host.indexOf('.');
            if (dot >= 0) {
                machine = host.substring(0, dot);
                domain = dot + 1 < host.length() ? host.substring(dot + 1) : "";
            } else {
                machine = host;
            }
        }
    }

    public void addBaseUrl(String baseUrl) throws IOException {
        addBaseUrl(new HttpUrl(baseUrl));
    }

    public void addBaseUrl(HttpUrl base) {
        if (base == null || authority != null) {
            return;
        }
        if (scheme == null) scheme = base.scheme;
        authority = base.authority;
        host = base.host;
        port = base.port;
        machine = base.machine;
        domain = base.domain;
        if (path == null) {
            path = base.path;
        } else if (path.length() > 0 && path.charAt(0) != '/' &&
                base.path != null) {
            int slash = base.path.lastIndexOf('/');
            path = (slash < 0 ? "" : base.path.substring(0, slash + 1)) + path;
        }
    }

    public String toString() {
        StringBuffer result = new StringBuffer();
        if (scheme != null) result.append(scheme).append(':');
        if (authority != null) result.append("//").append(authority);
        if (path != null) result.append(path);
        if (query != null) result.append('?').append(query);
        if (fragment != null) result.append('#').append(fragment);
        return result.toString();
    }

    private static int firstOf(String value, char a, char b, char c) {
        int result = -1;
        int index = value.indexOf(a);
        if (index >= 0) result = index;
        index = value.indexOf(b);
        if (index >= 0 && (result < 0 || index < result)) result = index;
        index = value.indexOf(c);
        if (index >= 0 && (result < 0 || index < result)) result = index;
        return result;
    }
}

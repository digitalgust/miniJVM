

package java.net;

import java.io.*;
import java.nio.ByteBuffer;


public final class URI
        implements Comparable, Serializable {

    // Note: Comments containing the word "ASSERT" indicate places where a
    // throw of an InternalError should be replaced by an appropriate assertion
    // statement once asserts are enabled in the build.

    static final long serialVersionUID = -6052424284110960213L;


    // -- Properties and components of this instance --

    // Components of all URIs: [<scheme>:]<scheme-specific-part>[#<fragment>]
    private transient String scheme;        // null ==> relative URI
    private transient String fragment;

    // Hierarchical URI components: [//<authority>]<path>[?<query>]
    private transient String authority;        // Registry or server

    // Server-based authority: [<userInfo>@]<host>[:<port>]
    private transient String userInfo;
    private transient String host;        // null ==> registry-based
    private transient int port = -1;        // -1 ==> undefined

    // Remaining components of hierarchical URIs
    private transient String path;        // null ==> opaque
    private transient String query;

    // The remaining fields may be computed on demand

    private volatile transient String schemeSpecificPart;
    private volatile transient int hash;    // Zero ==> undefined

    private volatile transient String decodedUserInfo = null;
    private volatile transient String decodedAuthority = null;
    private volatile transient String decodedPath = null;
    private volatile transient String decodedQuery = null;
    private volatile transient String decodedFragment = null;
    private volatile transient String decodedSchemeSpecificPart = null;

    /**
     * The string form of this URI.
     *
     * @serial
     */
    private volatile String string;        // The only serializable field


    // -- Constructors and factories --

    private URI() {
    }                // Used internally

    public URI(String str) throws URISyntaxException {
        new Parser(str).parse(false);
    }

    public URI(String scheme,
               String userInfo, String host, int port,
               String path, String query, String fragment)
            throws URISyntaxException {
        String s = toString(scheme, null,
                null, userInfo, host, port,
                path, query, fragment);
        checkPath(s, scheme, path);
        new Parser(s).parse(true);
    }

    public URI(String scheme,
               String authority,
               String path, String query, String fragment)
            throws URISyntaxException {
        String s = toString(scheme, null,
                authority, null, null, -1,
                path, query, fragment);
        checkPath(s, scheme, path);
        new Parser(s).parse(false);
    }

    public URI(String scheme, String host, String path, String fragment)
            throws URISyntaxException {
        this(scheme, null, host, -1, path, null, fragment);
    }

    public URI(String scheme, String ssp, String fragment)
            throws URISyntaxException {
        new Parser(toString(scheme, ssp,
                null, null, null, -1,
                null, null, fragment))
                .parse(false);
    }

    public static URI create(String str) {
        try {
            return new URI(str);
        } catch (URISyntaxException x) {
            IllegalArgumentException y = new IllegalArgumentException();
            throw y;
        }
    }


    // -- Operations --

    public URI parseServerAuthority()
            throws URISyntaxException {
        // We could be clever and cache the error message and index from the
        // exception thrown during the original parse, but that would require
        // either more fields or a more-obscure representation.
        if ((host != null) || (authority == null))
            return this;
        defineString();
        new Parser(string).parse(true);
        return this;
    }

    public URI normalize() {
        return normalize(this);
    }

    public URI resolve(URI uri) {
        return resolve(this, uri);
    }

    public URI resolve(String str) {
        return resolve(URI.create(str));
    }

    public URI relativize(URI uri) {
        return relativize(this, uri);
    }

    public URL toURL()
            throws MalformedURLException {
        if (!isAbsolute())
            throw new IllegalArgumentException("URI is not absolute");
        return new URL(toString());
    }

    // -- Component access methods --

    public String getScheme() {
        return scheme;
    }

    public boolean isAbsolute() {
        return scheme != null;
    }

    public boolean isOpaque() {
        return path == null;
    }

    public String getRawSchemeSpecificPart() {
        defineSchemeSpecificPart();
        return schemeSpecificPart;
    }

    public String getSchemeSpecificPart() {
        if (decodedSchemeSpecificPart == null)
            decodedSchemeSpecificPart = decode(getRawSchemeSpecificPart());
        return decodedSchemeSpecificPart;
    }

    public String getRawAuthority() {
        return authority;
    }

    public String getAuthority() {
        if (decodedAuthority == null)
            decodedAuthority = decode(authority);
        return decodedAuthority;
    }

    public String getRawUserInfo() {
        return userInfo;
    }

    public String getUserInfo() {
        if ((decodedUserInfo == null) && (userInfo != null))
            decodedUserInfo = decode(userInfo);
        return decodedUserInfo;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public String getRawPath() {
        return path;
    }

    public String getPath() {
        if ((decodedPath == null) && (path != null))
            decodedPath = decode(path);
        return decodedPath;
    }

    public String getRawQuery() {
        return query;
    }

    public String getQuery() {
        if ((decodedQuery == null) && (query != null))
            decodedQuery = decode(query);
        return decodedQuery;
    }

    public String getRawFragment() {
        return fragment;
    }

    public String getFragment() {
        if ((decodedFragment == null) && (fragment != null))
            decodedFragment = decode(fragment);
        return decodedFragment;
    }

    // -- Equality, comparison, hash code, toString, and serialization --

    public boolean equals(Object ob) {
        if (ob == this)
            return true;
        if (!(ob instanceof URI))
            return false;
        URI that = (URI) ob;
        if (this.isOpaque() != that.isOpaque()) return false;
        if (!equalIgnoringCase(this.scheme, that.scheme)) return false;
        if (!equal(this.fragment, that.fragment)) return false;

        // Opaque
        if (this.isOpaque())
            return equal(this.schemeSpecificPart, that.schemeSpecificPart);

        // Hierarchical
        if (!equal(this.path, that.path)) return false;
        if (!equal(this.query, that.query)) return false;

        // Authorities
        if (this.authority == that.authority) return true;
        if (this.host != null) {
            // Server-based
            if (!equal(this.userInfo, that.userInfo)) return false;
            if (!equalIgnoringCase(this.host, that.host)) return false;
            if (this.port != that.port) return false;
        } else if (this.authority != null) {
            // Registry-based
            if (!equal(this.authority, that.authority)) return false;
        } else if (this.authority != that.authority) {
            return false;
        }

        return true;
    }

    public int hashCode() {
        if (hash != 0)
            return hash;
        int h = hashIgnoringCase(0, scheme);
        h = hash(h, fragment);
        if (isOpaque()) {
            h = hash(h, schemeSpecificPart);
        } else {
            h = hash(h, path);
            h = hash(h, query);
            if (host != null) {
                h = hash(h, userInfo);
                h = hashIgnoringCase(h, host);
                h += 1949 * port;
            } else {
                h = hash(h, authority);
            }
        }
        hash = h;
        return h;
    }

    public int compareTo(Object ob) {
        URI that = (URI) ob;
        int c;

        if ((c = compareIgnoringCase(this.scheme, that.scheme)) != 0)
            return c;

        if (this.isOpaque()) {
            if (that.isOpaque()) {
                // Both opaque
                if ((c = compare(this.schemeSpecificPart,
                        that.schemeSpecificPart)) != 0)
                    return c;
                return compare(this.fragment, that.fragment);
            }
            return +1;            // Opaque > hierarchical
        } else if (that.isOpaque()) {
            return -1;            // Hierarchical < opaque
        }

        // Hierarchical
        if ((this.host != null) && (that.host != null)) {
            // Both server-based
            if ((c = compare(this.userInfo, that.userInfo)) != 0)
                return c;
            if ((c = compareIgnoringCase(this.host, that.host)) != 0)
                return c;
            if ((c = this.port - that.port) != 0)
                return c;
        } else {
            // If one or both authorities are registry-based then we simply
            // compare them in the usual, case-sensitive way.  If one is
            // registry-based and one is server-based then the strings are
            // guaranteed to be unequal, hence the comparison will never return
            // zero and the compareTo and equals methods will remain
            // consistent.
            if ((c = compare(this.authority, that.authority)) != 0) return c;
        }

        if ((c = compare(this.path, that.path)) != 0) return c;
        if ((c = compare(this.query, that.query)) != 0) return c;
        return compare(this.fragment, that.fragment);
    }

    public String toString() {
        defineString();
        return string;
    }

    public String toASCIIString() {
        defineString();
        return encode(string);
    }


    // -- Serialization support --

    private void writeObject(ObjectOutputStream os)
            throws IOException {
        defineString();
        os.defaultWriteObject();    // Writes the string field only
    }

    private void readObject(ObjectInputStream is)
            throws ClassNotFoundException, IOException {
        port = -1;            // Argh
        is.defaultReadObject();
        try {
            new Parser(string).parse(false);
        } catch (URISyntaxException x) {
            IOException y = new IOException("Invalid URI");
            throw y;
        }
    }


    // -- End of public methods --


    // -- Utility methods for string-field comparison and hashing --

    // These methods return appropriate values for null string arguments,
    // thereby simplifying the equals, hashCode, and compareTo methods.
    //
    // The case-ignoring methods should only be applied to strings whose
    // characters are all known to be US-ASCII.  Because of this restriction,
    // these methods are faster than the similar methods in the String class.

    // US-ASCII only
    private static int toLower(char c) {
        if ((c >= 'A') && (c <= 'Z'))
            return c + ('a' - 'A');
        return c;
    }

    private static boolean equal(String s, String t) {
        if (s == t) return true;
        if ((s != null) && (t != null)) {
            if (s.length() != t.length())
                return false;
            if (s.indexOf('%') < 0)
                return s.equals(t);
            int n = s.length();
            for (int i = 0; i < n; ) {
                char c = s.charAt(i);
                char d = t.charAt(i);
                if (c != '%') {
                    if (c != d)
                        return false;
                    i++;
                    continue;
                }
                i++;
                if (toLower(s.charAt(i)) != toLower(t.charAt(i)))
                    return false;
                i++;
                if (toLower(s.charAt(i)) != toLower(t.charAt(i)))
                    return false;
                i++;
            }
            return true;
        }
        return false;
    }

    // US-ASCII only
    private static boolean equalIgnoringCase(String s, String t) {
        if (s == t) return true;
        if ((s != null) && (t != null)) {
            int n = s.length();
            if (t.length() != n)
                return false;
            for (int i = 0; i < n; i++) {
                if (toLower(s.charAt(i)) != toLower(t.charAt(i)))
                    return false;
            }
            return true;
        }
        return false;
    }

    private static int hash(int hash, String s) {
        if (s == null) return hash;
        return hash * 127 + s.hashCode();
    }

    // US-ASCII only
    private static int hashIgnoringCase(int hash, String s) {
        if (s == null) return hash;
        int h = hash;
        int n = s.length();
        for (int i = 0; i < n; i++)
            h = 31 * h + toLower(s.charAt(i));
        return h;
    }

    private static int compare(String s, String t) {
        if (s == t) return 0;
        if (s != null) {
            if (t != null)
                return s.compareTo(t);
            else
                return +1;
        } else {
            return -1;
        }
    }

    // US-ASCII only
    private static int compareIgnoringCase(String s, String t) {
        if (s == t) return 0;
        if (s != null) {
            if (t != null) {
                int sn = s.length();
                int tn = t.length();
                int n = sn < tn ? sn : tn;
                for (int i = 0; i < n; i++) {
                    int c = toLower(s.charAt(i)) - toLower(t.charAt(i));
                    if (c != 0)
                        return c;
                }
                return sn - tn;
            }
            return +1;
        } else {
            return -1;
        }
    }


    // -- String construction --

    // If a scheme is given then the path, if given, must be absolute
    //
    private static void checkPath(String s, String scheme, String path)
            throws URISyntaxException {
        if (scheme != null) {
            if ((path != null)
                    && ((path.length() > 0) && (path.charAt(0) != '/')))
                throw new URISyntaxException(s,
                        "Relative path in absolute URI");
        }
    }

    private void appendAuthority(StringBuffer sb,
                                 String authority,
                                 String userInfo,
                                 String host,
                                 int port) {
        if (host != null) {
            sb.append("//");
            if (userInfo != null) {
                sb.append(quote(userInfo, L_USERINFO, H_USERINFO));
                sb.append('@');
            }
            boolean needBrackets = ((host.indexOf(':') >= 0)
                    && !host.startsWith("[")
                    && !host.endsWith("]"));
            if (needBrackets) sb.append('[');
            sb.append(host);
            if (needBrackets) sb.append(']');
            if (port != -1) {
                sb.append(':');
                sb.append(port);
            }
        } else if (authority != null) {
            sb.append("//");
            sb.append(quote(authority,
                    L_REG_NAME | L_SERVER,
                    H_REG_NAME | H_SERVER));
        }
    }

    private void appendSchemeSpecificPart(StringBuffer sb,
                                          String opaquePart,
                                          String authority,
                                          String userInfo,
                                          String host,
                                          int port,
                                          String path,
                                          String query) {
        if (opaquePart != null) {
            sb.append(quote(opaquePart, L_URIC, H_URIC));
        } else {
            appendAuthority(sb, authority, userInfo, host, port);
            if (path != null)
                sb.append(quote(path, L_PATH, H_PATH));
            if (query != null) {
                sb.append('?');
                sb.append(quote(query, L_URIC, H_URIC));
            }
        }
    }

    private void appendFragment(StringBuffer sb, String fragment) {
        if (fragment != null) {
            sb.append('#');
            sb.append(quote(fragment, L_URIC, H_URIC));
        }
    }

    private String toString(String scheme,
                            String opaquePart,
                            String authority,
                            String userInfo,
                            String host,
                            int port,
                            String path,
                            String query,
                            String fragment) {
        StringBuffer sb = new StringBuffer();
        if (scheme != null) {
            sb.append(scheme);
            sb.append(':');
        }
        appendSchemeSpecificPart(sb, opaquePart,
                authority, userInfo, host, port,
                path, query);
        appendFragment(sb, fragment);
        return sb.toString();
    }

    private void defineSchemeSpecificPart() {
        if (schemeSpecificPart != null) return;
        StringBuffer sb = new StringBuffer();
        appendSchemeSpecificPart(sb, null,
                authority, userInfo, host, port,
                path, query);
        if (sb.length() == 0) return;
        schemeSpecificPart = sb.toString();
    }

    private void defineString() {
        if (string != null) return;

        StringBuffer sb = new StringBuffer();
        if (scheme != null) {
            sb.append(scheme);
            sb.append(':');
        }
        if (isOpaque()) {
            sb.append(schemeSpecificPart);
        } else {
            if (host != null) {
                sb.append("//");
                if (userInfo != null) {
                    sb.append(userInfo);
                    sb.append('@');
                }
                boolean needBrackets = ((host.indexOf(':') >= 0)
                        && !host.startsWith("[")
                        && !host.endsWith("]"));
                if (needBrackets) sb.append('[');
                sb.append(host);
                if (needBrackets) sb.append(']');
                if (port != -1) {
                    sb.append(':');
                    sb.append(port);
                }
            } else if (authority != null) {
                sb.append("//");
                sb.append(authority);
            }
            if (path != null)
                sb.append(path);
            if (query != null) {
                sb.append('?');
                sb.append(query);
            }
        }
        if (fragment != null) {
            sb.append('#');
            sb.append(fragment);
        }
        string = sb.toString();
    }


    // -- Normalization, resolution, and relativization --

    // RFC2396 5.2 (6)
    private static String resolvePath(String base, String child,
                                      boolean absolute) {
        int i = base.lastIndexOf('/');
        int cn = child.length();
        String path = "";

        if (cn == 0) {
            // 5.2 (6a)
            if (i >= 0)
                path = base.substring(0, i + 1);
        } else {
            StringBuffer sb = new StringBuffer(base.length() + cn);
            // 5.2 (6a)
            if (i >= 0)
                sb.append(base.substring(0, i + 1));
            // 5.2 (6b)
            sb.append(child);
            path = sb.toString();
        }

        // 5.2 (6c-f)
        String np = normalize(path);

        // 5.2 (6g): If the result is absolute but the path begins with "../",
        // then we simply leave the path as-is

        return np;
    }

    // RFC2396 5.2
    private static URI resolve(URI base, URI child) {
        // check if child if opaque first so that NPE is thrown
        // if child is null.
        if (child.isOpaque() || base.isOpaque())
            return child;

        // 5.2 (2): Reference to current document (lone fragment)
        if ((child.scheme == null) && (child.authority == null)
                && child.path.equals("") && (child.fragment != null)
                && (child.query == null)) {
            if ((base.fragment != null)
                    && child.fragment.equals(base.fragment)) {
                return base;
            }
            URI ru = new URI();
            ru.scheme = base.scheme;
            ru.authority = base.authority;
            ru.userInfo = base.userInfo;
            ru.host = base.host;
            ru.port = base.port;
            ru.path = base.path;
            ru.fragment = child.fragment;
            ru.query = base.query;
            return ru;
        }

        // 5.2 (3): Child is absolute
        if (child.scheme != null)
            return child;

        URI ru = new URI();        // Resolved URI
        ru.scheme = base.scheme;
        ru.query = child.query;
        ru.fragment = child.fragment;

        // 5.2 (4): Authority
        if (child.authority == null) {
            ru.authority = base.authority;
            ru.host = base.host;
            ru.userInfo = base.userInfo;
            ru.port = base.port;

            String cp = (child.path == null) ? "" : child.path;
            if ((cp.length() > 0) && (cp.charAt(0) == '/')) {
                // 5.2 (5): Child path is absolute
                ru.path = child.path;
            } else {
                // 5.2 (6): Resolve relative path
                ru.path = resolvePath(base.path, cp, base.isAbsolute());
            }
        } else {
            ru.authority = child.authority;
            ru.host = child.host;
            ru.userInfo = child.userInfo;
            ru.host = child.host;
            ru.port = child.port;
            ru.path = child.path;
        }

        // 5.2 (7): Recombine (nothing to do here)
        return ru;
    }

    // If the given URI's path is normal then return the URI;
    // o.w., return a new URI containing the normalized path.
    //
    private static URI normalize(URI u) {
        if (u.isOpaque() || (u.path == null) || (u.path.length() == 0))
            return u;

        String np = normalize(u.path);
        if (np == u.path)
            return u;

        URI v = new URI();
        v.scheme = u.scheme;
        v.fragment = u.fragment;
        v.authority = u.authority;
        v.userInfo = u.userInfo;
        v.host = u.host;
        v.port = u.port;
        v.path = np;
        v.query = u.query;
        return v;
    }

    // If both URIs are hierarchical, their scheme and authority components are
    // identical, and the base path is a prefix of the child's path, then
    // return a relative URI that, when resolved against the base, yields the
    // child; otherwise, return the child.
    //
    private static URI relativize(URI base, URI child) {
        // check if child if opaque first so that NPE is thrown
        // if child is null.
        if (child.isOpaque() || base.isOpaque())
            return child;
        if (!equalIgnoringCase(base.scheme, child.scheme)
                || !equal(base.authority, child.authority))
            return child;

        String bp = normalize(base.path);
        String cp = normalize(child.path);
        if (!bp.equals(cp)) {
            if (!bp.endsWith("/"))
                bp = bp + "/";
            if (!cp.startsWith(bp))
                return child;
        }

        URI v = new URI();
        v.path = cp.substring(bp.length());
        v.query = child.query;
        v.fragment = child.fragment;
        return v;
    }


    // -- Path normalization --

    // The following algorithm for path normalization avoids the creation of a
    // string object for each segment, as well as the use of a string buffer to
    // compute the final result, by using a single char array and editing it in
    // place.  The array is first split into segments, replacing each slash
    // with '\0' and creating a segment-index array, each element of which is
    // the index of the first char in the corresponding segment.  We then walk
    // through both arrays, removing ".", "..", and other segments as necessary
    // by setting their entries in the index array to -1.  Finally, the two
    // arrays are used to rejoin the segments and compute the final result.
    //
    // This code is based upon src/solaris/native/java/io/canonicalize_md.c


    // Check the given path to see if it might need normalization.  A path
    // might need normalization if it contains duplicate slashes, a "."
    // segment, or a ".." segment.  Return -1 if no further normalization is
    // possible, otherwise return the number of segments found.
    //
    // This method takes a string argument rather than a char array so that
    // this test can be performed without invoking path.toCharArray().
    //
    static private int needsNormalization(String path) {
        boolean normal = true;
        int ns = 0;            // Number of segments
        int end = path.length() - 1;    // Index of last char in path
        int p = 0;            // Index of next char in path

        // Skip initial slashes
        while (p <= end) {
            if (path.charAt(p) != '/') break;
            p++;
        }
        if (p > 1) normal = false;

        // Scan segments
        while (p <= end) {

            // Looking at "." or ".." ?
            if ((path.charAt(p) == '.')
                    && ((p == end)
                    || ((path.charAt(p + 1) == '/')
                    || ((path.charAt(p + 1) == '.')
                    && ((p + 1 == end)
                    || (path.charAt(p + 2) == '/')))))) {
                normal = false;
            }
            ns++;

            // Find beginning of next segment
            while (p <= end) {
                if (path.charAt(p++) != '/')
                    continue;

                // Skip redundant slashes
                while (p <= end) {
                    if (path.charAt(p) != '/') break;
                    normal = false;
                    p++;
                }

                break;
            }
        }

        return normal ? -1 : ns;
    }


    // Split the given path into segments, replacing slashes with nulls and
    // filling in the given segment-index array.
    //
    // Preconditions:
    //   segs.length == Number of segments in path
    //
    // Postconditions:
    //   All slashes in path replaced by '\0'
    //   segs[i] == Index of first char in segment i (0 <= i < segs.length)
    //
    static private void split(char[] path, int[] segs) {
        int end = path.length - 1;    // Index of last char in path
        int p = 0;            // Index of next char in path
        int i = 0;            // Index of current segment

        // Skip initial slashes
        while (p <= end) {
            if (path[p] != '/') break;
            path[p] = '\0';
            p++;
        }

        while (p <= end) {

            // Note start of segment
            segs[i++] = p++;

            // Find beginning of next segment
            while (p <= end) {
                if (path[p++] != '/')
                    continue;
                path[p - 1] = '\0';

                // Skip redundant slashes
                while (p <= end) {
                    if (path[p] != '/') break;
                    path[p++] = '\0';
                }
                break;
            }
        }

        if (i != segs.length)
            throw new InternalError();    // ASSERT
    }


    // Join the segments in the given path according to the given segment-index
    // array, ignoring those segments whose index entries have been set to -1,
    // and inserting slashes as needed.  Return the length of the resulting
    // path.
    //
    // Preconditions:
    //   segs[i] == -1 implies segment i is to be ignored
    //   path computed by split, as above, with '\0' having replaced '/'
    //
    // Postconditions:
    //   path[0] .. path[return value] == Resulting path
    //
    static private int join(char[] path, int[] segs) {
        int ns = segs.length;        // Number of segments
        int end = path.length - 1;    // Index of last char in path
        int p = 0;            // Index of next path char to write

        if (path[p] == '\0') {
            // Restore initial slash for absolute paths
            path[p++] = '/';
        }

        for (int i = 0; i < ns; i++) {
            int q = segs[i];        // Current segment
            if (q == -1)
                // Ignore this segment
                continue;

            if (p == q) {
                // We're already at this segment, so just skip to its end
                while ((p <= end) && (path[p] != '\0'))
                    p++;
                if (p <= end) {
                    // Preserve trailing slash
                    path[p++] = '/';
                }
            } else if (p < q) {
                // Copy q down to p
                while ((q <= end) && (path[q] != '\0'))
                    path[p++] = path[q++];
                if (q <= end) {
                    // Preserve trailing slash
                    path[p++] = '/';
                }
            } else
                throw new InternalError(); // ASSERT false
        }

        return p;
    }


    // Remove "." segments from the given path, and remove segment pairs
    // consisting of a non-".." segment followed by a ".." segment.
    //
    private static void removeDots(char[] path, int[] segs) {
        int ns = segs.length;
        int end = path.length - 1;

        for (int i = 0; i < ns; i++) {
            int dots = 0;        // Number of dots found (0, 1, or 2)

            // Find next occurrence of "." or ".."
            do {
                int p = segs[i];
                if (path[p] == '.') {
                    if (p == end) {
                        dots = 1;
                        break;
                    } else if (path[p + 1] == '\0') {
                        dots = 1;
                        break;
                    } else if ((path[p + 1] == '.')
                            && ((p + 1 == end)
                            || (path[p + 2] == '\0'))) {
                        dots = 2;
                        break;
                    }
                }
                i++;
            } while (i < ns);
            if ((i > ns) || (dots == 0))
                break;

            if (dots == 1) {
                // Remove this occurrence of "."
                segs[i] = -1;
            } else {
                // If there is a preceding non-".." segment, remove both that
                // segment and this occurrence of ".."; otherwise, leave this
                // ".." segment as-is.
                int j;
                for (j = i - 1; j >= 0; j--) {
                    if (segs[j] != -1) break;
                }
                if (j >= 0) {
                    int q = segs[j];
                    if (!((path[q] == '.')
                            && (path[q + 1] == '.')
                            && (path[q + 2] == '\0'))) {
                        segs[i] = -1;
                        segs[j] = -1;
                    }
                }
            }
        }
    }


    // DEVIATION: If the normalized path is relative, and if the first
    // segment could be parsed as a scheme name, then prepend a "." segment
    //
    private static void maybeAddLeadingDot(char[] path, int[] segs) {

        if (path[0] == '\0')
            // The path is absolute
            return;

        int ns = segs.length;
        int f = 0;            // Index of first segment
        while (f < ns) {
            if (segs[f] >= 0)
                break;
            f++;
        }
        if ((f >= ns) || (f == 0))
            // The path is empty, or else the original first segment survived,
            // in which case we already know that no leading "." is needed
            return;

        int p = segs[f];
        while ((p < path.length) && (path[p] != ':') && (path[p] != '\0')) p++;
        if (p >= path.length || path[p] == '\0')
            // No colon in first segment, so no "." needed
            return;

        // At this point we know that the first segment is unused,
        // hence we can insert a "." segment at that position
        path[0] = '.';
        path[1] = '\0';
        segs[0] = 0;
    }


    // Normalize the given path string.  A normal path string has no empty
    // segments (i.e., occurrences of "//"), no segments equal to ".", and no
    // segments equal to ".." that are preceded by a segment not equal to "..".
    // In contrast to Unix-style pathname normalization, for URI paths we
    // always retain trailing slashes.
    //
    private static String normalize(String ps) {

        // Does this path need normalization?
        int ns = needsNormalization(ps);    // Number of segments
        if (ns < 0)
            // Nope -- just return it
            return ps;

        char[] path = ps.toCharArray();        // Path in char-array form

        // Split path into segments
        int[] segs = new int[ns];        // Segment-index array
        split(path, segs);

        // Remove dots
        removeDots(path, segs);

        // Prevent scheme-name confusion
        maybeAddLeadingDot(path, segs);

        // Join the remaining segments and return the result
        String s = new String(path, 0, join(path, segs));
        if (s.equals(ps)) {
            // string was already normalized
            return ps;
        }
        return s;
    }


    // -- Character classes for parsing --

    // RFC2396 precisely specifies which characters in the US-ASCII charset are
    // permissible in the various components of a URI reference.  We here
    // define a set of mask pairs to aid in enforcing these restrictions.  Each
    // mask pair consists of two longs, a low mask and a high mask.  Taken
    // together they represent a 128-bit mask, where bit i is set iff the
    // character with value i is permitted.
    //
    // This approach is more efficient than sequentially searching arrays of
    // permitted characters.  It could be made still more efficient by
    // precompiling the mask information so that a character's presence in a
    // given mask could be determined by a single table lookup.

    // Compute the low-order mask for the characters in the given string
    private static long lowMask(String chars) {
        int n = chars.length();
        long m = 0;
        for (int i = 0; i < n; i++) {
            char c = chars.charAt(i);
            if (c < 64)
                m |= (1L << c);
        }
        return m;
    }

    // Compute the high-order mask for the characters in the given string
    private static long highMask(String chars) {
        int n = chars.length();
        long m = 0;
        for (int i = 0; i < n; i++) {
            char c = chars.charAt(i);
            if ((c >= 64) && (c < 128))
                m |= (1L << (c - 64));
        }
        return m;
    }

    // Compute a low-order mask for the characters
    // between first and last, inclusive
    private static long lowMask(char first, char last) {
        long m = 0;
        int f = Math.max(Math.min(first, 63), 0);
        int l = Math.max(Math.min(last, 63), 0);
        for (int i = f; i <= l; i++)
            m |= 1L << i;
        return m;
    }

    // Compute a high-order mask for the characters
    // between first and last, inclusive
    private static long highMask(char first, char last) {
        long m = 0;
        int f = Math.max(Math.min(first, 127), 64) - 64;
        int l = Math.max(Math.min(last, 127), 64) - 64;
        for (int i = f; i <= l; i++)
            m |= 1L << i;
        return m;
    }

    // Tell whether the given character is permitted by the given mask pair
    private static boolean match(char c, long lowMask, long highMask) {
        if (c < 64)
            return ((1L << c) & lowMask) != 0;
        if (c < 128)
            return ((1L << (c - 64)) & highMask) != 0;
        return false;
    }

    // Character-class masks, in reverse order from RFC2396 because
    // initializers for static fields cannot make forward references.

    // digit    = "0" | "1" | "2" | "3" | "4" | "5" | "6" | "7" |
    //            "8" | "9"
    private static final long L_DIGIT = lowMask('0', '9');
    private static final long H_DIGIT = 0L;

    // upalpha  = "A" | "B" | "C" | "D" | "E" | "F" | "G" | "H" | "I" |
    //            "J" | "K" | "L" | "M" | "N" | "O" | "P" | "Q" | "R" |
    //            "S" | "T" | "U" | "V" | "W" | "X" | "Y" | "Z"
    private static final long L_UPALPHA = 0L;
    private static final long H_UPALPHA = highMask('A', 'Z');

    // lowalpha = "a" | "b" | "c" | "d" | "e" | "f" | "g" | "h" | "i" |
    //            "j" | "k" | "l" | "m" | "n" | "o" | "p" | "q" | "r" |
    //            "s" | "t" | "u" | "v" | "w" | "x" | "y" | "z"
    private static final long L_LOWALPHA = 0L;
    private static final long H_LOWALPHA = highMask('a', 'z');

    // alpha         = lowalpha | upalpha
    private static final long L_ALPHA = L_LOWALPHA | L_UPALPHA;
    private static final long H_ALPHA = H_LOWALPHA | H_UPALPHA;

    // alphanum      = alpha | digit
    private static final long L_ALPHANUM = L_DIGIT | L_ALPHA;
    private static final long H_ALPHANUM = H_DIGIT | H_ALPHA;

    // hex           = digit | "A" | "B" | "C" | "D" | "E" | "F" |
    //                         "a" | "b" | "c" | "d" | "e" | "f"
    private static final long L_HEX = L_DIGIT;
    private static final long H_HEX = highMask('A', 'F') | highMask('a', 'f');

    // mark          = "-" | "_" | "." | "!" | "~" | "*" | "'" |
    //                 "(" | ")"
    private static final long L_MARK = lowMask("-_.!~*'()");
    private static final long H_MARK = highMask("-_.!~*'()");

    // unreserved    = alphanum | mark
    private static final long L_UNRESERVED = L_ALPHANUM | L_MARK;
    private static final long H_UNRESERVED = H_ALPHANUM | H_MARK;

    // reserved      = ";" | "/" | "?" | ":" | "@" | "&" | "=" | "+" |
    //                 "$" | "," | "[" | "]"
    // Added per RFC2732: "[", "]"
    private static final long L_RESERVED = lowMask(";/?:@&=+$,[]");
    private static final long H_RESERVED = highMask(";/?:@&=+$,[]");

    // The zero'th bit is used to indicate that escape pairs and non-US-ASCII
    // characters are allowed; this is handled by the scanEscape method below.
    private static final long L_ESCAPED = 1L;
    private static final long H_ESCAPED = 0L;

    // uric          = reserved | unreserved | escaped
    private static final long L_URIC = L_RESERVED | L_UNRESERVED | L_ESCAPED;
    private static final long H_URIC = H_RESERVED | H_UNRESERVED | H_ESCAPED;

    // pchar         = unreserved | escaped |
    //                 ":" | "@" | "&" | "=" | "+" | "$" | ","
    private static final long L_PCHAR
            = L_UNRESERVED | L_ESCAPED | lowMask(":@&=+$,");
    private static final long H_PCHAR
            = H_UNRESERVED | H_ESCAPED | highMask(":@&=+$,");

    // All valid path characters
    private static final long L_PATH = L_PCHAR | lowMask(";/");
    private static final long H_PATH = H_PCHAR | highMask(";/");

    // Dash, for use in domainlabel and toplabel
    private static final long L_DASH = lowMask("-");
    private static final long H_DASH = highMask("-");

    // Dot, for use in hostnames
    private static final long L_DOT = lowMask(".");
    private static final long H_DOT = highMask(".");

    // userinfo      = *( unreserved | escaped |
    //                    ";" | ":" | "&" | "=" | "+" | "$" | "," )
    private static final long L_USERINFO
            = L_UNRESERVED | L_ESCAPED | lowMask(";:&=+$,");
    private static final long H_USERINFO
            = H_UNRESERVED | H_ESCAPED | highMask(";:&=+$,");

    // reg_name      = 1*( unreserved | escaped | "$" | "," |
    //                     ";" | ":" | "@" | "&" | "=" | "+" )
    private static final long L_REG_NAME
            = L_UNRESERVED | L_ESCAPED | lowMask("$,;:@&=+");
    private static final long H_REG_NAME
            = H_UNRESERVED | H_ESCAPED | highMask("$,;:@&=+");

    // All valid characters for server-based authorities
    private static final long L_SERVER
            = L_USERINFO | L_ALPHANUM | L_DASH | lowMask(".:@[]");
    private static final long H_SERVER
            = H_USERINFO | H_ALPHANUM | H_DASH | highMask(".:@[]");

    // scheme        = alpha *( alpha | digit | "+" | "-" | "." )
    private static final long L_SCHEME = L_ALPHA | L_DIGIT | lowMask("+-.");
    private static final long H_SCHEME = H_ALPHA | H_DIGIT | highMask("+-.");

    // uric_no_slash = unreserved | escaped | ";" | "?" | ":" | "@" |
    //                 "&" | "=" | "+" | "$" | ","
    private static final long L_URIC_NO_SLASH
            = L_UNRESERVED | L_ESCAPED | lowMask(";?:@&=+$,");
    private static final long H_URIC_NO_SLASH
            = H_UNRESERVED | H_ESCAPED | highMask(";?:@&=+$,");


    // -- Escaping and encoding --

    private final static char[] hexDigits = {
            '0', '1', '2', '3', '4', '5', '6', '7',
            '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'
    };

    private static void appendEscape(StringBuffer sb, byte b) {
        sb.append('%');
        sb.append(hexDigits[(b >> 4) & 0x0f]);
        sb.append(hexDigits[(b >> 0) & 0x0f]);
    }

    private static void appendEncoded(StringBuffer sb, char c) {
        ByteBuffer bb = null;
        try {
            byte[] ns = ("" + c).getBytes("utf-8");
            bb = ByteBuffer.wrap(ns);
        } catch (Exception x) {
            assert false;
        }
        while (bb.hasRemaining()) {
            int b = bb.get() & 0xff;
            if (b >= 0x80)
                appendEscape(sb, (byte) b);
            else
                sb.append((char) b);
        }
    }

    // Quote any characters in s that are not permitted
    // by the given mask pair
    //
    private static String quote(String s, long lowMask, long highMask) {
        int n = s.length();
        StringBuffer sb = null;
        boolean allowNonASCII = ((lowMask & L_ESCAPED) != 0);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c < '\u0080') {
                if (!match(c, lowMask, highMask)) {
                    if (sb == null) {
                        sb = new StringBuffer();
                        sb.append(s.substring(0, i));
                    }
                    appendEscape(sb, (byte) c);
                } else {
                    if (sb != null)
                        sb.append(c);
                }
            } else if (allowNonASCII
                    && (Character.isSpaceChar(c)
                    || Character.isISOControl(c))) {
                if (sb == null) {
                    sb = new StringBuffer();
                    sb.append(s.substring(0, i));
                }
                appendEncoded(sb, c);
            } else {
                if (sb != null)
                    sb.append(c);
            }
        }
        return (sb == null) ? s : sb.toString();
    }

    // Encodes all characters >= \u0080 into escaped, normalized UTF-8 octets,
    // assuming that s is otherwise legal
    //
    private static String encode(String s) {
        try {
            return URLEncoder.encode(s, "utf-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return s;
    }

    private static int decode(char c) {
        if ((c >= '0') && (c <= '9'))
            return c - '0';
        if ((c >= 'a') && (c <= 'f'))
            return c - 'a' + 10;
        if ((c >= 'A') && (c <= 'F'))
            return c - 'A' + 10;
        assert false;
        return -1;
    }

    private static byte decode(char c1, char c2) {
        return (byte) (((decode(c1) & 0xf) << 4)
                | ((decode(c2) & 0xf) << 0));
    }

    // Evaluates all escapes in s, applying UTF-8 decoding if needed.  Assumes
    // that escapes are well-formed syntactically, i.e., of the form %XX.  If a
    // sequence of escaped octets is not valid UTF-8 then the erroneous octets
    // are replaced with '\uFFFD'.
    //
    private static String decode(String s) {
        return URLDecoder.decode(s);
    }


    // -- Parsing --

    // For convenience we wrap the input URI string in a new instance of the
    // following internal class.  This saves always having to pass the input
    // string as an argument to each internal scan/parse method.

    private class Parser {

        private String input;        // URI input string
        private boolean requireServerAuthority = false;

        Parser(String s) {
            input = s;
            string = s;
        }

        // -- Methods for throwing URISyntaxException in various ways --

        private void fail(String reason) throws URISyntaxException {
            throw new URISyntaxException(input, reason);
        }

        private void fail(String reason, int p) throws URISyntaxException {
            throw new URISyntaxException(input, reason, p);
        }

        private void failExpecting(String expected, int p)
                throws URISyntaxException {
            fail("Expected " + expected, p);
        }

        private void failExpecting(String expected, String prior, int p)
                throws URISyntaxException {
            fail("Expected " + expected + " following " + prior, p);
        }


        // -- Simple access to the input string --

        // Return a substring of the input string
        //
        private String substring(int start, int end) {
            return input.substring(start, end);
        }

        // Return the char at position p,
        // assuming that p < input.length()
        //
        private char charAt(int p) {
            return input.charAt(p);
        }

        // Tells whether start < end and, if so, whether charAt(start) == c
        //
        private boolean at(int start, int end, char c) {
            return (start < end) && (charAt(start) == c);
        }

        // Tells whether start + s.length() < end and, if so,
        // whether the chars at the start position match s exactly
        //
        private boolean at(int start, int end, String s) {
            int p = start;
            int sn = s.length();
            if (sn > end - p)
                return false;
            int i = 0;
            while (i < sn) {
                if (charAt(p++) != s.charAt(i)) {
                    break;
                }
                i++;
            }
            return (i == sn);
        }


        // -- Scanning --

        // The various scan and parse methods that follow use a uniform
        // convention of taking the current start position and end index as
        // their first two arguments.  The start is inclusive while the end is
        // exclusive, just as in the String class, i.e., a start/end pair
        // denotes the left-open interval [start, end) of the input string.
        //
        // These methods never proceed past the end position.  They may return
        // -1 to indicate outright failure, but more often they simply return
        // the position of the first char after the last char scanned.  Thus
        // a typical idiom is
        //
        //     int p = start;
        //     int q = scan(p, end, ...);
        //     if (q > p)
        //         // We scanned something
        //         ...;
        //     else if (q == p)
        //         // We scanned nothing
        //         ...;
        //     else if (q == -1)
        //         // Something went wrong
        //         ...;


        // Scan a specific char: If the char at the given start position is
        // equal to c, return the index of the next char; otherwise, return the
        // start position.
        //
        private int scan(int start, int end, char c) {
            if ((start < end) && (charAt(start) == c))
                return start + 1;
            return start;
        }

        // Scan forward from the given start position.  Stop at the first char
        // in the err string (in which case -1 is returned), or the first char
        // in the stop string (in which case the index of the preceding char is
        // returned), or the end of the input string (in which case the length
        // of the input string is returned).  May return the start position if
        // nothing matches.
        //
        private int scan(int start, int end, String err, String stop) {
            int p = start;
            while (p < end) {
                char c = charAt(p);
                if (err.indexOf(c) >= 0)
                    return -1;
                if (stop.indexOf(c) >= 0)
                    break;
                p++;
            }
            return p;
        }

        // Scan a potential escape sequence, starting at the given position,
        // with the given first char (i.e., charAt(start) == c).
        //
        // This method assumes that if escapes are allowed then visible
        // non-US-ASCII chars are also allowed.
        //
        private int scanEscape(int start, int n, char first)
                throws URISyntaxException {
            int p = start;
            char c = first;
            if (c == '%') {
                // Process escape pair
                if ((p + 3 <= n)
                        && match(charAt(p + 1), L_HEX, H_HEX)
                        && match(charAt(p + 2), L_HEX, H_HEX)) {
                    return p + 3;
                }
                fail("Malformed escape pair", p);
            } else if ((c > 128)
                    && !Character.isSpaceChar(c)
                    && !Character.isISOControl(c)) {
                // Allow unescaped but visible non-US-ASCII chars
                return p + 1;
            }
            return p;
        }

        // Scan chars that match the given mask pair
        //
        private int scan(int start, int n, long lowMask, long highMask)
                throws URISyntaxException {
            int p = start;
            while (p < n) {
                char c = charAt(p);
                if (match(c, lowMask, highMask)) {
                    p++;
                    continue;
                }
                if ((lowMask & L_ESCAPED) != 0) {
                    int q = scanEscape(p, n, c);
                    if (q > p) {
                        p = q;
                        continue;
                    }
                }
                break;
            }
            return p;
        }

        // Check that each of the chars in [start, end) matches the given mask
        //
        private void checkChars(int start, int end,
                                long lowMask, long highMask,
                                String what)
                throws URISyntaxException {
            int p = scan(start, end, lowMask, highMask);
            if (p < end)
                fail("Illegal character in " + what, p);
        }

        // Check that the char at position p matches the given mask
        //
        private void checkChar(int p,
                               long lowMask, long highMask,
                               String what)
                throws URISyntaxException {
            checkChars(p, p + 1, lowMask, highMask, what);
        }


        // -- Parsing --

        // [<scheme>:]<scheme-specific-part>[#<fragment>]
        //
        void parse(boolean rsa) throws URISyntaxException {
            requireServerAuthority = rsa;
            int ssp;            // Start of scheme-specific part
            int n = input.length();
            int p = scan(0, n, "/?#", ":");
            if ((p >= 0) && at(p, n, ':')) {
                if (p == 0)
                    failExpecting("scheme name", 0);
                checkChar(0, L_ALPHA, H_ALPHA, "scheme name");
                checkChars(1, p, L_SCHEME, H_SCHEME, "scheme name");
                scheme = substring(0, p);
                p++;            // Skip ':'
                ssp = p;
                if (at(p, n, '/')) {
                    p = parseHierarchical(p, n);
                } else {
                    int q = scan(p, n, "", "#");
                    if (q <= p)
                        failExpecting("scheme-specific part", p);
                    checkChars(p, q, L_URIC, H_URIC, "opaque part");
                    p = q;
                }
            } else {
                ssp = 0;
                p = parseHierarchical(0, n);
            }
            schemeSpecificPart = substring(ssp, p);
            if (at(p, n, '#')) {
                checkChars(p + 1, n, L_URIC, H_URIC, "fragment");
                fragment = substring(p + 1, n);
                p = n;
            }
            if (p < n)
                fail("end of URI", p);
        }

        // [//authority]<path>[?<query>]
        //
        // DEVIATION from RFC2396: We allow an empty authority component as
        // long as it's followed by a non-empty path, query component, or
        // fragment component.  This is so that URIs such as "file:///foo/bar"
        // will parse.  This seems to be the intent of RFC2396, though the
        // grammar does not permit it.  If the authority is empty then the
        // userInfo, host, and port components are undefined.
        //
        // DEVIATION from RFC2396: We allow empty relative paths.  This seems
        // to be the intent of RFC2396, but the grammar does not permit it.
        // The primary consequence of this deviation is that "#f" parses as a
        // relative URI with an empty path.
        //
        private int parseHierarchical(int start, int n)
                throws URISyntaxException {
            int p = start;
            if (at(p, n, '/') && at(p + 1, n, '/')) {
                p += 2;
                int q = scan(p, n, "", "/?#");
                if (q > p) {
                    p = parseAuthority(p, q);
                } else if (q < n) {
                    // DEVIATION: Allow empty authority prior to non-empty
                    // path, query component or fragment identifier
                } else
                    failExpecting("authority", p);
            }
            int q = scan(p, n, "", "?#"); // DEVIATION: May be empty
            checkChars(p, q, L_PATH, H_PATH, "path");
            path = substring(p, q);
            p = q;
            if (at(p, n, '?')) {
                p++;
                q = scan(p, n, "", "#");
                checkChars(p, q, L_URIC, H_URIC, "query");
                query = substring(p, q);
                p = q;
            }
            return p;
        }

        // authority     = server | reg_name
        //
        // Ambiguity: An authority that is a registry name rather than a server
        // might have a prefix that parses as a server.  We use the fact that
        // the authority component is always followed by '/' or the end of the
        // input string to resolve this: If the complete authority did not
        // parse as a server then we try to parse it as a registry name.
        //
        private int parseAuthority(int start, int n)
                throws URISyntaxException {
            int p = start;
            int q = p;
            URISyntaxException ex = null;

            boolean serverChars = (scan(p, n, L_SERVER, H_SERVER) == n);
            boolean regChars = (scan(p, n, L_REG_NAME, H_REG_NAME) == n);

            if (regChars && !serverChars) {
                // Must be a registry-based authority
                authority = substring(p, n);
                return n;
            }

            if (serverChars) {
                // Might be (probably is) a server-based authority, so attempt
                // to parse it as such.  If the attempt fails, try to treat it
                // as a registry-based authority.
                try {
                    q = parseServer(p, n);
                    if (q < n)
                        failExpecting("end of authority", q);
                    authority = substring(p, n);
                } catch (URISyntaxException x) {
                    // Undo results of failed parse
                    userInfo = null;
                    host = null;
                    port = -1;
                    if (requireServerAuthority) {
                        // If we're insisting upon a server-based authority,
                        // then just re-throw the exception
                        throw x;
                    } else {
                        // Save the exception in case it doesn't parse as a
                        // registry either
                        ex = x;
                        q = p;
                    }
                }
            }

            if (q < n) {
                if (regChars) {
                    // Registry-based authority
                    authority = substring(p, n);
                } else if (ex != null) {
                    // Re-throw exception; it was probably due to
                    // a malformed IPv6 address
                    throw ex;
                } else {
                    fail("Illegal character in authority", q);
                }
            }

            return n;
        }


        // [<userinfo>@]<host>[:<port>]
        //
        private int parseServer(int start, int n)
                throws URISyntaxException {
            int p = start;
            int q;

            // userinfo
            q = scan(p, n, "/?#", "@");
            if ((q >= p) && at(q, n, '@')) {
                checkChars(p, q, L_USERINFO, H_USERINFO, "user info");
                userInfo = substring(p, q);
                p = q + 1;        // Skip '@'
            }

            // hostname, IPv4 address, or IPv6 address
            if (at(p, n, '[')) {
                // DEVIATION from RFC2396: Support IPv6 addresses, per RFC2732
                p++;
                q = scan(p, n, "/?#", "]");
                if ((q > p) && at(q, n, ']')) {
                    parseIPv6Reference(p, q);
                    p = q + 1;
                } else {
                    failExpecting("closing bracket for IPv6 address", q);
                }
            } else {
                q = parseIPv4Address(p, n);
                if (q <= p)
                    q = parseHostname(p, n);
                p = q;
            }

            // port
            if (at(p, n, ':')) {
                p++;
                q = scan(p, n, "", "/");
                if (q > p) {
                    checkChars(p, q, L_DIGIT, H_DIGIT, "port number");
                    try {
                        port = Integer.parseInt(substring(p, q));
                    } catch (NumberFormatException x) {
                        fail("Malformed port number", p);
                    }
                    p = q;
                }
            }
            if (p < n)
                failExpecting("port number", p);

            return p;
        }

        // Scan a string of decimal digits whose value fits in a byte
        //
        private int scanByte(int start, int n)
                throws URISyntaxException {
            int p = start;
            int q = scan(p, n, L_DIGIT, H_DIGIT);
            if (q <= p) return q;
            if (Integer.parseInt(substring(p, q)) > 255) return p;
            return q;
        }

        // Scan an IPv4 address.
        //
        // If the strict argument is true then we require that the given
        // interval contain nothing besides an IPv4 address; if it is false
        // then we only require that it start with an IPv4 address.
        //
        // If the interval does not contain or start with (depending upon the
        // strict argument) a legal IPv4 address characters then we return -1
        // immediately; otherwise we insist that these characters parse as a
        // legal IPv4 address and throw an exception on failure.
        //
        // We assume that any string of decimal digits and dots must be an IPv4
        // address.  It won't parse as a hostname anyway, so making that
        // assumption here allows more meaningful exceptions to be thrown.
        //
        private int scanIPv4Address(int start, int n, boolean strict)
                throws URISyntaxException {
            int p = start;
            int q;
            int m = scan(p, n, L_DIGIT | L_DOT, H_DIGIT | H_DOT);
            if ((m <= p) || (strict && (m != n)))
                return -1;
            for (; ; ) {
                // Per RFC2732: At most three digits per byte
                // Further constraint: Each element fits in a byte
                if ((q = scanByte(p, m)) <= p) break;
                p = q;
                if ((q = scan(p, m, '.')) <= p) break;
                p = q;
                if ((q = scanByte(p, m)) <= p) break;
                p = q;
                if ((q = scan(p, m, '.')) <= p) break;
                p = q;
                if ((q = scanByte(p, m)) <= p) break;
                p = q;
                if ((q = scan(p, m, '.')) <= p) break;
                p = q;
                if ((q = scanByte(p, m)) <= p) break;
                p = q;
                if (q < m) break;
                return q;
            }
            fail("Malformed IPv4 address", q);
            return -1;
        }

        // Take an IPv4 address: Throw an exception if the given interval
        // contains anything except an IPv4 address
        //
        private int takeIPv4Address(int start, int n, String expected)
                throws URISyntaxException {
            int p = scanIPv4Address(start, n, true);
            if (p <= start)
                failExpecting(expected, start);
            return p;
        }

        // Attempt to parse an IPv4 address, returning -1 on failure but
        // allowing the given interval to contain [:<characters>] after
        // the IPv4 address.
        //
        private int parseIPv4Address(int start, int n) {
            int p;

            try {
                p = scanIPv4Address(start, n, false);
            } catch (URISyntaxException x) {
                return -1;
            } catch (NumberFormatException nfe) {
                return -1;
            }

            if (p > start && p < n) {
                // IPv4 address is followed by something - check that
                // it's a ":" as this is the only valid character to
                // follow an address.
                if (charAt(p) != ':') {
                    p = -1;
                }
            }

            if (p > start)
                host = substring(start, p);

            return p;
        }

        // hostname      = domainlabel [ "." ] | 1*( domainlabel "." ) toplabel [ "." ]
        // domainlabel   = alphanum | alphanum *( alphanum | "-" ) alphanum
        // toplabel      = alpha | alpha *( alphanum | "-" ) alphanum
        //
        private int parseHostname(int start, int n)
                throws URISyntaxException {
            int p = start;
            int q;
            int l = -1;            // Start of last parsed label

            do {
                // domainlabel = alphanum [ *( alphanum | "-" ) alphanum ]
                q = scan(p, n, L_ALPHANUM, H_ALPHANUM);
                if (q <= p)
                    break;
                l = p;
                if (q > p) {
                    p = q;
                    q = scan(p, n, L_ALPHANUM | L_DASH, H_ALPHANUM | H_DASH);
                    if (q > p) {
                        if (charAt(q - 1) == '-')
                            fail("Illegal character in hostname", q - 1);
                        p = q;
                    }
                }
                q = scan(p, n, '.');
                if (q <= p)
                    break;
                p = q;
            } while (p < n);

            if ((p < n) && !at(p, n, ':'))
                fail("Illegal character in hostname", p);

            if (l < 0)
                failExpecting("hostname", start);

            // for a fully qualified hostname check that the rightmost
            // label starts with an alpha character.
            if (l > start && !match(charAt(l), L_ALPHA, H_ALPHA)) {
                fail("Illegal character in hostname", l);
            }

            host = substring(start, p);
            return p;
        }


        // IPv6 address parsing, from RFC2373: IPv6 Addressing Architecture
        //
        // Bug: The grammar in RFC2373 Appendix B does not allow addresses of
        // the form ::12.34.56.78, which are clearly shown in the examples
        // earlier in the document.  Here is the original grammar:
        //
        //   IPv6address = hexpart [ ":" IPv4address ]
        //   hexpart     = hexseq | hexseq "::" [ hexseq ] | "::" [ hexseq ]
        //   hexseq      = hex4 *( ":" hex4)
        //   hex4        = 1*4HEXDIG
        //
        // We therefore use the following revised grammar:
        //
        //   IPv6address = hexseq [ ":" IPv4address ]
        //                 | hexseq [ "::" [ hexpost ] ]
        //                 | "::" [ hexpost ]
        //   hexpost     = hexseq | hexseq ":" IPv4address | IPv4address
        //   hexseq      = hex4 *( ":" hex4)
        //   hex4        = 1*4HEXDIG
        //
        // This covers all and only the following cases:
        //
        //   hexseq
        //   hexseq : IPv4address
        //   hexseq ::
        //   hexseq :: hexseq
        //   hexseq :: hexseq : IPv4address
        //   hexseq :: IPv4address
        //   :: hexseq
        //   :: hexseq : IPv4address
        //   :: IPv4address
        //   ::
        //
        // Additionally we constrain the IPv6 address as follows :-
        //
        //  i.  IPv6 addresses without compressed zeros should contain
        //      exactly 16 bytes.
        //
        //  ii. IPv6 addresses with compressed zeros should contain
        //      less than 16 bytes.

        private int ipv6byteCount = 0;

        private int parseIPv6Reference(int start, int n)
                throws URISyntaxException {
            int p = start;
            int q;
            boolean compressedZeros = false;

            q = scanHexSeq(p, n);

            if (q > p) {
                p = q;
                if (at(p, n, "::")) {
                    compressedZeros = true;
                    p = scanHexPost(p + 2, n);
                } else if (at(p, n, ':')) {
                    p = takeIPv4Address(p + 1, n, "IPv4 address");
                    ipv6byteCount += 4;
                }
            } else if (at(p, n, "::")) {
                compressedZeros = true;
                p = scanHexPost(p + 2, n);
            }
            if (p < n)
                fail("Malformed IPv6 address", start);
            if (ipv6byteCount > 16)
                fail("IPv6 address too long", start);
            if (!compressedZeros && ipv6byteCount < 16)
                fail("IPv6 address too short", start);
            if (compressedZeros && ipv6byteCount == 16)
                fail("Malformed IPv6 address", start);

            host = substring(start - 1, p + 1);
            return p;
        }

        private int scanHexPost(int start, int n)
                throws URISyntaxException {
            int p = start;
            int q;

            if (p == n)
                return p;

            q = scanHexSeq(p, n);
            if (q > p) {
                p = q;
                if (at(p, n, ':')) {
                    p++;
                    p = takeIPv4Address(p, n, "hex digits or IPv4 address");
                    ipv6byteCount += 4;
                }
            } else {
                p = takeIPv4Address(p, n, "hex digits or IPv4 address");
                ipv6byteCount += 4;
            }
            return p;
        }

        // Scan a hex sequence; return -1 if one could not be scanned
        //
        private int scanHexSeq(int start, int n)
                throws URISyntaxException {
            int p = start;
            int q;

            q = scan(p, n, L_HEX, H_HEX);
            if (q <= p)
                return -1;
            if (at(q, n, '.'))        // Beginning of IPv4 address
                return -1;
            if (q > p + 4)
                fail("IPv6 hexadecimal digit sequence too long", p);
            ipv6byteCount += 2;
            p = q;
            while (p < n) {
                if (!at(p, n, ':'))
                    break;
                if (at(p + 1, n, ':'))
                    break;        // "::"
                p++;
                q = scan(p, n, L_HEX, H_HEX);
                if (q <= p)
                    failExpecting("digits for an IPv6 address", p);
                if (at(q, n, '.')) {    // Beginning of IPv4 address
                    p--;
                    break;
                }
                if (q > p + 4)
                    fail("IPv6 hexadecimal digit sequence too long", p);
                ipv6byteCount += 2;
                p = q;
            }

            return p;
        }

    }

}

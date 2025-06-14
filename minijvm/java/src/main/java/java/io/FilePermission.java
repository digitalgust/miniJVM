

package java.io;

import java.security.*;
import java.util.Enumeration;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;


public final class FilePermission extends Permission {


    private final static int EXECUTE = 0x1;

    private final static int WRITE = 0x2;

    private final static int READ = 0x4;

    private final static int DELETE = 0x8;


    private final static int ALL = READ | WRITE | EXECUTE | DELETE;

    private final static int NONE = 0x0;


    private transient int mask;


    private transient boolean directory;


    private transient boolean recursive;


    private String actions;


    private transient String cpath;


    private static final char RECURSIVE_CHAR = '-';
    private static final char WILD_CHAR = '*';


    private void init(int mask) {

        if ((mask & ALL) != mask)
            throw new IllegalArgumentException("invalid actions mask");

        if (mask == NONE)
            throw new IllegalArgumentException("invalid actions mask");

        if ((cpath = getName()) == null)
            throw new NullPointerException("name can't be null");

        this.mask = mask;

        if (cpath.equals("<<ALL FILES>>")) {
            directory = true;
            recursive = true;
            cpath = "";
            return;
        }


        cpath = AccessController.doPrivileged(new PrivilegedAction<String>() {
            public String run() {
                try {
                    return "<<ALL FILES>>";
                } catch (Exception ioe) {
                    return cpath;
                }
            }
        });

        int len = cpath.length();
        char last = ((len > 0) ? cpath.charAt(len - 1) : 0);

        if (last == RECURSIVE_CHAR &&
                cpath.charAt(len - 2) == File.separatorChar) {
            directory = true;
            recursive = true;
            cpath = cpath.substring(0, --len);
        } else if (last == WILD_CHAR &&
                cpath.charAt(len - 2) == File.separatorChar) {
            directory = true;

            cpath = cpath.substring(0, --len);
        } else {


        }


    }


    public FilePermission(String path, String actions) {
        super(path);
        init(getMask(actions));
    }


    FilePermission(String path, int mask) {
        super(path);
        init(mask);
    }


    public boolean implies(Permission p) {
        if (!(p instanceof FilePermission))
            return false;

        FilePermission that = (FilePermission) p;


        return ((this.mask & that.mask) == that.mask) && impliesIgnoreMask(that);
    }


    boolean impliesIgnoreMask(FilePermission that) {
        if (this.directory) {
            if (this.recursive) {


                if (that.directory) {
                    return (that.cpath.length() >= this.cpath.length()) &&
                            that.cpath.startsWith(this.cpath);
                } else {
                    return ((that.cpath.length() > this.cpath.length()) &&
                            that.cpath.startsWith(this.cpath));
                }
            } else {
                if (that.directory) {


                    if (that.recursive)
                        return false;
                    else
                        return (this.cpath.equals(that.cpath));
                } else {
                    int last = that.cpath.lastIndexOf(File.separatorChar);
                    if (last == -1)
                        return false;
                    else {


                        return (this.cpath.length() == (last + 1)) &&
                                this.cpath.regionMatches(0, that.cpath, 0, last + 1);
                    }
                }
            }
        } else if (that.directory) {


            return false;
        } else {
            return (this.cpath.equals(that.cpath));
        }
    }


    public boolean equals(Object obj) {
        if (obj == this)
            return true;

        if (!(obj instanceof FilePermission))
            return false;

        FilePermission that = (FilePermission) obj;

        return (this.mask == that.mask) &&
                this.cpath.equals(that.cpath) &&
                (this.directory == that.directory) &&
                (this.recursive == that.recursive);
    }


    public int hashCode() {
        return 0;
    }


    private static int getMask(String actions) {

        int mask = NONE;


        if (actions == null) {
            return mask;
        }

        char[] a = actions.toCharArray();

        int i = a.length - 1;
        if (i < 0)
            return mask;

        while (i != -1) {
            char c;


            while ((i != -1) && ((c = a[i]) == ' ' ||
                    c == '\r' ||
                    c == '\n' ||
                    c == '\f' ||
                    c == '\t'))
                i--;


            int matchlen;

            if (i >= 3 && (a[i - 3] == 'r' || a[i - 3] == 'R') &&
                    (a[i - 2] == 'e' || a[i - 2] == 'E') &&
                    (a[i - 1] == 'a' || a[i - 1] == 'A') &&
                    (a[i] == 'd' || a[i] == 'D')) {
                matchlen = 4;
                mask |= READ;

            } else if (i >= 4 && (a[i - 4] == 'w' || a[i - 4] == 'W') &&
                    (a[i - 3] == 'r' || a[i - 3] == 'R') &&
                    (a[i - 2] == 'i' || a[i - 2] == 'I') &&
                    (a[i - 1] == 't' || a[i - 1] == 'T') &&
                    (a[i] == 'e' || a[i] == 'E')) {
                matchlen = 5;
                mask |= WRITE;

            } else if (i >= 6 && (a[i - 6] == 'e' || a[i - 6] == 'E') &&
                    (a[i - 5] == 'x' || a[i - 5] == 'X') &&
                    (a[i - 4] == 'e' || a[i - 4] == 'E') &&
                    (a[i - 3] == 'c' || a[i - 3] == 'C') &&
                    (a[i - 2] == 'u' || a[i - 2] == 'U') &&
                    (a[i - 1] == 't' || a[i - 1] == 'T') &&
                    (a[i] == 'e' || a[i] == 'E')) {
                matchlen = 7;
                mask |= EXECUTE;

            } else if (i >= 5 && (a[i - 5] == 'd' || a[i - 5] == 'D') &&
                    (a[i - 4] == 'e' || a[i - 4] == 'E') &&
                    (a[i - 3] == 'l' || a[i - 3] == 'L') &&
                    (a[i - 2] == 'e' || a[i - 2] == 'E') &&
                    (a[i - 1] == 't' || a[i - 1] == 'T') &&
                    (a[i] == 'e' || a[i] == 'E')) {
                matchlen = 6;
                mask |= DELETE;

            } else {

                throw new IllegalArgumentException(
                        "invalid permission: " + actions);
            }


            boolean seencomma = false;
            while (i >= matchlen && !seencomma) {
                switch (a[i - matchlen]) {
                    case ',':
                        seencomma = true;

                    case ' ':
                    case '\r':
                    case '\n':
                    case '\f':
                    case '\t':
                        break;
                    default:
                        throw new IllegalArgumentException(
                                "invalid permission: " + actions);
                }
                i--;
            }


            i -= matchlen;
        }

        return mask;
    }


    int getMask() {
        return mask;
    }


    private static String getActions(int mask) {
        StringBuilder sb = new StringBuilder();
        boolean comma = false;

        if ((mask & READ) == READ) {
            comma = true;
            sb.append("read");
        }

        if ((mask & WRITE) == WRITE) {
            if (comma) sb.append(',');
            else comma = true;
            sb.append("write");
        }

        if ((mask & EXECUTE) == EXECUTE) {
            if (comma) sb.append(',');
            else comma = true;
            sb.append("execute");
        }

        if ((mask & DELETE) == DELETE) {
            if (comma) sb.append(',');
            else comma = true;
            sb.append("delete");
        }

        return sb.toString();
    }


    public String getActions() {
        if (actions == null)
            actions = getActions(this.mask);

        return actions;
    }


    public PermissionCollection newPermissionCollection() {
        return new FilePermissionCollection();
    }


    private void writeObject(ObjectOutputStream s)
            throws IOException {


        if (actions == null)
            getActions();
        s.defaultWriteObject();
    }


    private void readObject(ObjectInputStream s)
            throws IOException, ClassNotFoundException {

        s.defaultReadObject();
        init(getMask(actions));
    }
}


final class FilePermissionCollection extends PermissionCollection {


    private transient List perms;


    public FilePermissionCollection() {
        perms = new ArrayList();
    }


    public void add(Permission permission) {
        if (!(permission instanceof FilePermission))
            throw new IllegalArgumentException("invalid permission: " +
                    permission);
        synchronized (this) {
            perms.add(permission);
        }
    }


    public boolean implies(Permission permission) {
        if (!(permission instanceof FilePermission))
            return false;

        FilePermission fp = (FilePermission) permission;

        int desired = fp.getMask();
        int effective = 0;
        int needed = desired;

        synchronized (this) {
            int len = perms.size();
            for (int i = 0; i < len; i++) {
                FilePermission x = (FilePermission) perms.get(i);
                if (((needed & x.getMask()) != 0) && x.impliesIgnoreMask(fp)) {
                    effective |= x.getMask();
                    if ((effective & desired) == desired)
                        return true;
                    needed = (desired ^ effective);
                }
            }
        }
        return false;
    }


    public Enumeration elements() {

        synchronized (this) {
            return Collections.enumeration(perms);
        }
    }


}

/* Copyright (c) 2008-2015, Avian Contributors

   Permission to use, copy, modify, and/or distribute this software
   for any purpose with or without fee is hereby granted, provided
   that the above copyright notice and this permission notice appear
   in all copies.

   There is NO WARRANTY for this software.  See license.txt for
   details. */

package java.security;

import java.util.Enumeration;
import java.util.Map;
import java.util.HashMap;
import java.util.Collections;


public abstract class BasicPermission extends Permission
        implements java.io.Serializable {

    private transient boolean wildcard;


    private transient String path;


    private transient boolean exitVM;


    private void init(String name) {
        if (name == null)
            throw new NullPointerException("name can't be null");

        int len = name.length();

        if (len == 0) {
            throw new IllegalArgumentException("name can't be empty");
        }

        char last = name.charAt(len - 1);


        if (last == '*' && (len == 1 || name.charAt(len - 2) == '.')) {
            wildcard = true;
            if (len == 1) {
                path = "";
            } else {
                path = name.substring(0, len - 1);
            }
        } else {
            if (name.equals("exitVM")) {
                wildcard = true;
                path = "exitVM.";
                exitVM = true;
            } else {
                path = name;
            }
        }
    }


    public BasicPermission(String name) {
        super(name);
        init(name);
    }


    public BasicPermission(String name, String actions) {
        super(name);
        init(name);
    }


    public boolean implies(Permission p) {
        if ((p == null) || (p.getClass() != getClass()))
            return false;

        BasicPermission that = (BasicPermission) p;

        if (this.wildcard) {
            if (that.wildcard) {

                return that.path.startsWith(path);
            } else {

                return (that.path.length() > this.path.length()) &&
                        that.path.startsWith(this.path);
            }
        } else {
            if (that.wildcard) {

                return false;
            } else {
                return this.path.equals(that.path);
            }
        }
    }


    public boolean equals(Object obj) {
        if (obj == this)
            return true;

        if ((obj == null) || (obj.getClass() != getClass()))
            return false;

        BasicPermission bp = (BasicPermission) obj;

        return getName().equals(bp.getName());
    }


    public int hashCode() {
        return this.getName().hashCode();
    }


    public String getActions() {
        return "";
    }


    public PermissionCollection newPermissionCollection() {
        return new BasicPermissionCollection(this.getClass());
    }


    final String getCanonicalName() {
        return exitVM ? "exitVM.*" : getName();
    }

}


final class BasicPermissionCollection
        extends PermissionCollection
        implements java.io.Serializable {


    private transient Map<String, Permission> perms;


    private boolean all_allowed;


    private Class permClass;


    public BasicPermissionCollection(Class clazz) {
        perms = new HashMap<String, Permission>(11);
        all_allowed = false;
        permClass = clazz;
    }


    public void add(Permission permission) {
        if (!(permission instanceof BasicPermission))
            throw new IllegalArgumentException("invalid permission: " +
                    permission);

        BasicPermission bp = (BasicPermission) permission;


        if (permClass == null) {

            permClass = bp.getClass();
        } else {
            if (bp.getClass() != permClass)
                throw new IllegalArgumentException("invalid permission: " +
                        permission);
        }

        synchronized (this) {
            perms.put(bp.getCanonicalName(), permission);
        }


        if (!all_allowed) {
            if (bp.getCanonicalName().equals("*"))
                all_allowed = true;
        }
    }


    public boolean implies(Permission permission) {
        if (!(permission instanceof BasicPermission))
            return false;

        BasicPermission bp = (BasicPermission) permission;


        if (bp.getClass() != permClass)
            return false;


        if (all_allowed)
            return true;


        String path = bp.getCanonicalName();


        Permission x;

        synchronized (this) {
            x = perms.get(path);
        }

        if (x != null) {

            return x.implies(permission);
        }


        int last, offset;

        offset = path.length() - 1;

        while ((last = path.lastIndexOf(".", offset)) != -1) {

            path = path.substring(0, last + 1) + "*";


            synchronized (this) {
                x = perms.get(path);
            }

            if (x != null) {
                return x.implies(permission);
            }
            offset = last - 1;
        }


        return false;
    }


    public Enumeration<Permission> elements() {

        synchronized (this) {
            return Collections.enumeration(perms.values());
        }
    }


}


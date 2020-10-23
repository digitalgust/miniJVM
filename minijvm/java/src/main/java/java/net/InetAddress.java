/* Copyright (c) 2008-2015, Avian Contributors

   Permission to use, copy, modify, and/or distribute this software
   for any purpose with or without fee is hereby granted, provided
   that the above copyright notice and this permission notice appear
   in all copies.

   There is NO WARRANTY for this software.  See license.txt for
   details. */
package java.net;

import org.mini.net.SocketNative;

import java.io.IOException;

public class InetAddress {

    private final String name;
    private final String ip;

    private InetAddress(String name) throws UnknownHostException {
        this.name = name;
        this.ip = new String(SocketNative.host2ip(SocketNative.toCStyle(name)));
    }

    public String getHostName() {
        return name;
    }

    public String getHostAddress() {
        try {
            return new InetAddress(name).toString();
        } catch (UnknownHostException e) {
            return null;    // Strange case
        }
    }

    public static InetAddress getByName(String name) throws UnknownHostException {
        try {
            return new InetAddress(name);
        } catch (IOException e) {
            UnknownHostException uhe = new UnknownHostException(name, e);
            throw uhe;
        }
    }

    public String getAddress() {

        return ip;
    }

    @Override
    public String toString() {

        return ip;
    }


    public boolean equals(Object o) {
        return o instanceof InetAddress && ((InetAddress) o).ip == ip;
    }

    public int hashCode() {
        return toString().hashCode();
    }
}

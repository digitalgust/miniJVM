package com.ebsee.invoke.bytecode;



public class DynamicClassLoader extends ClassLoader {
    byte[] classdata;

    @Override
    public Class findClass(String name) {
        return defineClass(name, classdata, 0, classdata.length);
    }

    public void setBytes(byte[] classdata,int offset,int len) {
        this.classdata = classdata;
    }


}

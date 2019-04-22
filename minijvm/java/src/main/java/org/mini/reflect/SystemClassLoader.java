/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.mini.reflect;

import org.mini.reflect.vm.RefNative;

/**
 *
 * @author gust
 */
public class SystemClassLoader extends ClassLoader{
    
    public SystemClassLoader(ClassLoader parent){
        
    }

    protected Class findClass(String name) throws ClassNotFoundException {
        Class c = RefNative.getClassByName(name);
        if (c == null) {
            throw new ClassNotFoundException(name);
        }
        return c;
    }

}

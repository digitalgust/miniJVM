/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.mini.reflect.vm;

import java.lang.invoke.CallSite;
import java.lang.invoke.MethodHandle;
import java.lang.reflect.Method;
import org.mini.reflect.ReflectMethod;

/**
 *
 * @author Gust
 */
public class LambdaUtil {

    public static Object newInterfaceInstance(CallSite callsite, Object... args) throws Throwable {
        MethodHandle mh = callsite.getTarget();
        return mh.invokeExact(args);
    }

    /**
     * 返回c MethodInfo 地址
     * 
     * @param callsite
     * @return 
     */
    public static long getMethodInfoHandle(CallSite callsite) {
        if (callsite != null && callsite.getTarget() != null) {
            Method m = callsite.getTarget().getMethod();
            return ReflectMethod.findMethod0(m.getDeclaringClass().getName(), m.getName(), m.getSignature());
        } else {
            return 0;
        }
    }
}

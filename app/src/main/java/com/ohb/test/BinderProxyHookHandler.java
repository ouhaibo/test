package com.ohb.test;

import android.os.IBinder;
import android.os.IInterface;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * Created by Administrator on 2016/7/28.
 */

/**
 * IBinder
 */
public class BinderProxyHookHandler implements InvocationHandler {

    private IBinder mBase;

    public BinderProxyHookHandler(IBinder base) {
        mBase = base;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if ("queryLocalInterface".equals(method.getName())) {
            Object base;
            Class stub = Class.forName("android.hardware.input.IInputManager$Stub");
            Method asInterface = stub.getDeclaredMethod("asInterface", IBinder.class);
            base = asInterface.invoke(null, mBase);
            return Proxy.newProxyInstance(proxy.getClass().getClassLoader(), new Class[]{IBinder.class, IInterface.class, Class.forName("android.hardware.input.IInputManager")}, new BinderHookHandler(base));
        }
        return method.invoke(mBase, args);
    }
}

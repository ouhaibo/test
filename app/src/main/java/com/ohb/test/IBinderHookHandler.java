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
public class IBinderHookHandler implements InvocationHandler {

    private IBinder mBase;
    private String mIBinderName;
    private String mIBinderStubClassName;
    private String mMethodName;

    public IBinderHookHandler(IBinder base, String IBinderName, String IBinderStubClassName, String methodName) {
        mBase = base;
        mIBinderName = IBinderName;
        mIBinderStubClassName = IBinderStubClassName;
        mMethodName = methodName;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if ("queryLocalInterface".equals(method.getName())) {
            Object base;
            Class stub = Class.forName(mIBinderStubClassName);
            Method method_asInterface = stub.getDeclaredMethod("asInterface", IBinder.class);
            base = method_asInterface.invoke(null, mBase);
            return Proxy.newProxyInstance(proxy.getClass().getClassLoader(), new Class[]{IBinder.class, IInterface.class, Class.forName(mIBinderName)}, new IBinderProxyHookHandler(base, mMethodName));
        }
        return method.invoke(mBase, args);
    }
}

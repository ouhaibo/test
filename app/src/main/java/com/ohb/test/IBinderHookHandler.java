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
    private String mIInterfaceName;
    private String mIInterfaceStubClassName;
    private String mMethodName;

    public IBinderHookHandler(IBinder base, String IInterfaceName, String InterfaceStubClassName, String methodName) {
        mBase = base;
        mIInterfaceName = IInterfaceName;
        mIInterfaceStubClassName = InterfaceStubClassName;
        mMethodName = methodName;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if ("queryLocalInterface".equals(method.getName())) {
            Object base;
            Class stub = Class.forName(mIInterfaceStubClassName);
            Method method_asInterface = stub.getDeclaredMethod("asInterface", IBinder.class);
            base = method_asInterface.invoke(null, mBase);
            return Proxy.newProxyInstance(proxy.getClass().getClassLoader(), new Class[]{IBinder.class, IInterface.class, Class.forName(mIInterfaceName)}, new IBinderProxyHookHandler(base, mMethodName));
        }
        return method.invoke(mBase, args);
    }
}

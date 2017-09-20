package com.ohb.test;

import android.os.IBinder;
import android.util.Log;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

/**
 * Created by Administrator on 2016/7/27.
 */

/**
 * IBinder&Stub
 */
public class IBinderProxyHookHandler implements InvocationHandler {

    private Object mBase;
    private String mMethodName;

    public IBinderProxyHookHandler(Object base, String methodName) {
        this.mBase = base;
        mMethodName = methodName;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if (mMethodName.equals(method.getName())) {
            Log.d("device ids:", "1,2,3,4,5");
            return new int[]{1, 2, 3, 4, 5};
        }
        return method.invoke(mBase, args);
    }
}

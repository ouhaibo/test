package com.ohb.test;

import android.util.Log;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

/**
 * Created by Administrator on 2016/10/7.
 */

public class HookIPackageManagerHandler implements InvocationHandler {
    Object mBase;

    public HookIPackageManagerHandler(Object base) {
        mBase = base;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        Log.d("PMS HOOK", "PMS has been hooked!");
        return method.invoke(mBase, args);
    }
}

package com.ohb.test;

import android.content.pm.PackageInfo;
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
        if (method.getName().equals("getPackageInfo")) {
            PackageInfo pi = (PackageInfo) method.invoke(mBase, args);
            if (pi == null) {
                pi = new PackageInfo();
            }
            return pi;
        }

        return method.invoke(mBase, args);
    }
}

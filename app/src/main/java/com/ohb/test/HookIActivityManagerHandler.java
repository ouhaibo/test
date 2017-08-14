package com.ohb.test;

import android.content.ComponentName;
import android.content.Intent;
import android.util.Log;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

/**
 * Created by Administrator on 2016/10/7.
 */

public class HookIActivityManagerHandler implements InvocationHandler {
    Object mBase;

    public HookIActivityManagerHandler(Object base) {
        mBase = base;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        Log.d("Hook AMS", "AMS has been hooked");
        if ("startActivity".equals(method.getName())) {
            Log.d("Hook AMS", "hook startActivity");
            Intent rawIntent = null;
            int index = 0;
            for (int i = 0; i < args.length; i++) {
                if (args[i] instanceof Intent) {
                    rawIntent = (Intent) args[i];
                    index = i;
                    break;
                }
            }
            Intent fakeIntent = new Intent();
            String fakePackageName = "com.ohb.test";
            ComponentName componentName = new ComponentName(fakePackageName, StubActivity.class.getCanonicalName());
            fakeIntent.setComponent(componentName);
            fakeIntent.putExtra("rawInent", rawIntent);//把启动真正Actvity的Intent保存起来
            args[index] = fakeIntent;
        }
        return method.invoke(mBase, args);
    }
}

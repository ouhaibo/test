package com.ohb.test;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityThread;
import android.app.Instrumentation;
import android.content.ContentProvider;
import android.content.Context;
import android.content.Loader;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.hardware.input.InputManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import android.util.Log;
import android.view.View;
import android.view.ViewRootImpl;

import java.lang.ref.SoftReference;
import java.lang.reflect.Field;
import java.lang.reflect.GenericDeclaration;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import dalvik.system.DexClassLoader;


public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        findViewById(R.id.tv0).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hook();
            }
        });
        findViewById(R.id.tv).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                InputManager inputManager = (InputManager) getSystemService(Context.INPUT_SERVICE);
                int[] ids = inputManager.getInputDeviceIds();
                Log.d("ids:", ids[0] + "," + ids[1] + "," + ids[2] + "," + ids[3] + "," + ids[4] + ",");
            }
        });

        findViewById(android.R.id.content).getViewTreeObserver().addOnGlobalLayoutListener(null);
    }

    private void hook_AMS() {
        try {
            Class<?> activityManagerNativeClass = Class.forName("android.app.ActivityManagerNative");
            Field gDefaultField = activityManagerNativeClass.getDeclaredField("gDefault");
            gDefaultField.setAccessible(true);
            Object gDefault = gDefaultField.get(null);

            Class<?> SingletonClass = Class.forName("android.util.Singleton");
            Field mInstanceField = SingletonClass.getDeclaredField("mInstance");
            mInstanceField.setAccessible(true);
            Object originalIActivityManager = mInstanceField.get(gDefault);
            Object hookedIActivityManager = Proxy.newProxyInstance(originalIActivityManager.getClass().getClassLoader(), new Class<?>[]{Class.forName("android.app.IActivityManager")}, new HookIActivityManagerHandler(originalIActivityManager));
            mInstanceField.set(gDefault, hookedIActivityManager);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void hook_PMS_InOneContext() {
        try {
            Class<?> activityThreadClass = Class.forName("android.app.ActivityThread");
            Field sPackageManagerField = activityThreadClass.getDeclaredField("sPackageManager");
            sPackageManagerField.setAccessible(true);
            Object originalIPackageManager = sPackageManagerField.get(null);

            Object hookedIPackageManager = Proxy.newProxyInstance(originalIPackageManager.getClass().getClassLoader(), new Class<?>[]{IInterface.class, Class.forName("android.content.pm.IPackageManager")}, new HookIPackageManagerHandler(originalIPackageManager));
            // 1. 替换掉ActivityThread里面的 sPackageManager 字段
            Field sCurrentActivityThreadField = activityThreadClass.getDeclaredField("sCurrentActivityThread");
            Object sCurrentActivityThread = sCurrentActivityThreadField.get(null);
            sPackageManagerField.set(sCurrentActivityThread, hookedIPackageManager);
            // 2. 替换 ApplicationPackageManager里面的 mPM对象
            PackageManager pm = getPackageManager();
            Field mPMField = pm.getClass().getDeclaredField("mPM");
            mPMField.setAccessible(true);
            mPMField.set(pm, hookedIPackageManager);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void hook() {
        try {
            Class<?> serviceManager = Class.forName("android.os.ServiceManager");
            Method getService = serviceManager.getDeclaredMethod("getService", String.class);
            IBinder rawInputManagerBinder = (IBinder) getService.invoke(null, Context.INPUT_SERVICE);

            IBinder hookedProxyBinder = (IBinder) Proxy.newProxyInstance(serviceManager.getClassLoader(), new Class<?>[]{IBinder.class}, new BinderProxyHookHandler(rawInputManagerBinder));

            Field sCache = serviceManager.getDeclaredField("sCache");
            sCache.setAccessible(true);
            HashMap<String, IBinder> map = (HashMap<String, IBinder>) sCache.get(null);
            map.put(Context.INPUT_SERVICE, hookedProxyBinder);
            sCache.set(null, map);
            sCache.setAccessible(false);

            Class<?> inputManagerClass = InputManager.class;
            Field sInstance = inputManagerClass.getDeclaredField("sInstance");
            sInstance.setAccessible(true);
            sInstance.set(null, null);
            sInstance.setAccessible(false);

            InputManager inputManager = (InputManager) getSystemService(Context.INPUT_SERVICE);
            Field mInputDevices = inputManagerClass.getDeclaredField("mInputDevices");
            mInputDevices.setAccessible(true);
            mInputDevices.set(inputManager, null);
            mInputDevices.setAccessible(false);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

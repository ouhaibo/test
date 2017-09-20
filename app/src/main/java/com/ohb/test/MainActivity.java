package com.ohb.test;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.input.InputManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.IInterface;
import android.util.Log;
import android.view.View;

import com.ohb.test.com.ohb.test.pulltorefresh.ActivityThreadHandlerCallback;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;


public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        findViewById(R.id.tv0).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hook_InputManager();
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

    private void hookActivityLaunch() {
        try {
            Class classActivityThread = Class.forName("android.app.ActivityThread");
            Field sCurrentActivityThreadField = classActivityThread.getDeclaredField("sCurrentActivityThread");
            sCurrentActivityThreadField.setAccessible(true);
            Object currentActivityThread = sCurrentActivityThreadField.get(null);

            Field mHField = classActivityThread.getDeclaredField("mH");
            mHField.setAccessible(true);
            Handler mH = (Handler) mHField.get(currentActivityThread);

            Field mCallbackField = Handler.class.getDeclaredField("mCallback");
            mCallbackField.setAccessible(true);
            mCallbackField.set(mH, new ActivityThreadHandlerCallback());
        } catch (Exception e) {
            e.printStackTrace();
        }
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

            // 2. 替换ApplicationPackageManager里面的 mPM字段
            PackageManager pm = getPackageManager();
            Field mPMField = pm.getClass().getDeclaredField("mPM");
            mPMField.setAccessible(true);
            mPMField.set(pm, hookedIPackageManager);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void hook_InputManager() {
        try {
            Class<?> serviceManager = Class.forName("android.os.ServiceManager");
            Method getService = serviceManager.getDeclaredMethod("getService", String.class);
            IBinder rawInputManagerBinder = (IBinder) getService.invoke(null, Context.INPUT_SERVICE);

            IBinder hookedProxyBinder = (IBinder) Proxy.newProxyInstance(serviceManager.getClassLoader(), new Class<?>[]{IBinder.class}, new IBinderHookHandler(rawInputManagerBinder, "android.hardware.input.IInputManager", "android.hardware.input.IInputManager$Stub", "getInputDeviceIds"));

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

package com.ohb.test;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.util.ArrayMap;

import java.io.File;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import dalvik.system.DexClassLoader;

/**
 * Created by Administrator on 2017/9/20.
 */

public class Utils {

    public static ApplicationInfo generateApplicationInfo(File apkFile) throws Exception {
        //---API 24:
        Class classPackageParser = Class.forName("android.content.pm.PackageParser");
        Class classPackage = Class.forName("android.content.pm.PackageParser$Package");
        Class classPackageUserState = Class.forName("android.content.pm.PackageUserState");

        Method methodGenerateApplicationInfo = classPackageParser.getDeclaredMethod("generateApplicationInfo", classPackage, int.class, classPackageUserState);

        Method methodParsePackage = classPackageParser.getDeclaredMethod("parsePackage", File.class, int.class);
        Object packageParser = classPackageParser.newInstance();

        Object objPackage = methodParsePackage.invoke(packageParser, apkFile, 0);
        Object packageUserState = classPackageUserState.newInstance();

        ApplicationInfo applicationInfo = (ApplicationInfo) methodGenerateApplicationInfo.invoke(null, objPackage, 0, packageUserState);
        String apkPath = apkFile.getAbsolutePath();
        applicationInfo.sourceDir = apkPath;
        applicationInfo.publicSourceDir = apkPath;
        //---
        return applicationInfo;
    }


    /**
     * @param context
     * @param apkFile
     * @throws Exception
     */
    public static void dynamicLoadWithCustomLoadedApk(Context context, File apkFile) throws Exception {
        DexClassLoader myClassloader = new DexClassLoader(apkFile.getAbsolutePath(), context.getDir("plugins", Context.MODE_PRIVATE).getAbsolutePath(), null, context.getClassLoader());

        Class classActivityThread = Class.forName("android.app.ActivityThread");
        Field field_sCurrentActivityThread = classActivityThread.getDeclaredField("sCurrentActivityThread");
        field_sCurrentActivityThread.setAccessible(true);
        Object sCurrentActivityThread = field_sCurrentActivityThread.get(null);

        Field field_mPackages = classActivityThread.getDeclaredField("mPackages");
        field_mPackages.setAccessible(true);
        ArrayMap mPackages = (ArrayMap) field_mPackages.get(sCurrentActivityThread);

        Class classCompatibilityInfo = Class.forName("android.content.res.CompatibilityInfo");
        Class classApplicationInfo = Class.forName("android.content.pm.ApplicationInfo");

        Method methodGetPackageInfoNoCheck = classActivityThread.getDeclaredMethod("getPackageInfoNoCheck", classApplicationInfo, classCompatibilityInfo);

        ApplicationInfo applicationInfo = generateApplicationInfo(apkFile);

        Field field_DEFAULT_COMPATIBILITY_INFO = classCompatibilityInfo.getDeclaredField("DEFAULT_COMPATIBILITY_INFO");
        field_DEFAULT_COMPATIBILITY_INFO.setAccessible(true);
        Object compatInfo = field_DEFAULT_COMPATIBILITY_INFO.get(null);

        Class classLoadedApk = Class.forName("android.app.LoadedApk");
        Object loadedApk = methodGetPackageInfoNoCheck.invoke(sCurrentActivityThread, applicationInfo, compatInfo);

        Field field_mClassLoader = classLoadedApk.getDeclaredField("mClassLoader");
        field_mClassLoader.setAccessible(true);
        field_mClassLoader.set(loadedApk, myClassloader);

        mPackages.put(applicationInfo.packageName, new WeakReference(loadedApk));

        field_DEFAULT_COMPATIBILITY_INFO.setAccessible(false);
        field_mClassLoader.setAccessible(false);
        field_mPackages.setAccessible(false);
        field_sCurrentActivityThread.setAccessible(false);

    }

}

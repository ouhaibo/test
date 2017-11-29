package com.ohb.test;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.UserHandle;
import android.util.ArrayMap;

import java.io.File;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;

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
        Object objPackageUserState = classPackageUserState.newInstance();

        ApplicationInfo applicationInfo = (ApplicationInfo) methodGenerateApplicationInfo.invoke(null, objPackage, 0, objPackageUserState);
        String apkPath = apkFile.getAbsolutePath();
        applicationInfo.sourceDir = apkPath;
        applicationInfo.publicSourceDir = apkPath;
        //---
        return applicationInfo;
    }

    /**
     * 为插件包生成一个LoadedApk对象，并注入ActivityThread中的mPackages集合
     *
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
        Object objLoadedApk = methodGetPackageInfoNoCheck.invoke(sCurrentActivityThread, applicationInfo, compatInfo);

        Field field_mClassLoader = classLoadedApk.getDeclaredField("mClassLoader");
        field_mClassLoader.setAccessible(true);
        field_mClassLoader.set(objLoadedApk, myClassloader);

        mPackages.put(applicationInfo.packageName, new WeakReference(objLoadedApk));

        field_DEFAULT_COMPATIBILITY_INFO.setAccessible(false);
        field_mClassLoader.setAccessible(false);
        field_mPackages.setAccessible(false);
        field_sCurrentActivityThread.setAccessible(false);

    }

    /**
     * 获取插件中的AndroidManifest文件中声明的BroadcastReceiver和它的IntentFilter
     *
     * @param apkFile
     * @return
     * @throws Exception
     */
    public static HashMap<ActivityInfo, List<? extends IntentFilter>> parseReceivers(File apkFile) throws Exception {
        Class<?> classPackageParser = Class.forName("android.content.pm.PackageParser");
        Class<?> classPackageParser$Package = Class.forName("android.content.pm.PackageParser$Package");
        Method method_parsePackage = classPackageParser.getDeclaredMethod("parsePackage", File.class, int.class);
        Object objPackageParser = classPackageParser.newInstance();
        Object objPackage = method_parsePackage.invoke(objPackageParser, apkFile, PackageManager.GET_RECEIVERS);

        Class<?> classPackageParser$Activity = Class.forName("android.content.pm.PackageParser$Activity");
        Field field_receivers = classPackageParser$Package.getDeclaredField("receivers");
        List receivers = (List) field_receivers.get(objPackage);

        Class<?> classUserHandle = UserHandle.class;
        Method method_getCallingUserId = classUserHandle.getDeclaredMethod("getCallingUserId");
        int userId = (int) method_getCallingUserId.invoke(null);

        Class<?> classPackageUserState = Class.forName("android.content.pm.PackageUserState");
        Object objPackageUserState = classPackageUserState.newInstance();

        Class<?> classPackageParser$Component = Class.forName("android.content.pm.PackageParser$Component");
        Field field_intents = classPackageParser$Component.getDeclaredField("intents");
        Method method_generateActivityInfo = classPackageParser.getDeclaredMethod("generateActivityInfo", classPackageParser$Activity, int.class, classPackageUserState, int.class);

        HashMap<ActivityInfo, List<? extends IntentFilter>> map = new HashMap<>();
        for (Object objReceiver : receivers) {
            ActivityInfo activityInfo = (ActivityInfo) method_generateActivityInfo.invoke(null, objReceiver, 0, objPackageUserState, userId);
            List<? extends IntentFilter> intentFilters = (List<? extends IntentFilter>) field_intents.get(receivers);
            map.put(activityInfo, intentFilters);
        }
        return map;
    }

    /**
     * 将插件包中的AndroidManifest中声明的BroadcastReceiver进行动态注册
     *
     * @param pluginClassLoader 插件包的ClassLoader
     * @param context
     * @param map               从插件包中获的AndroidManifest.xml中解析出的BroadcastReceiver和对应的IntentFilter
     * @throws Exception
     */
    public static void registerReceiverInPlugin(ClassLoader pluginClassLoader, Context context, HashMap<ActivityInfo, List<? extends IntentFilter>> map) throws Exception {
        for (ActivityInfo activityInfo : map.keySet()) {
            List<? extends IntentFilter> intentFilters = map.get(activityInfo);
            for (IntentFilter filter : intentFilters) {
                BroadcastReceiver receiver = (BroadcastReceiver) pluginClassLoader.loadClass(activityInfo.name).newInstance();
                context.registerReceiver(receiver, filter);
            }
        }
    }

    /**
     * hook掉在Host中静态注册的BroadcastReceiver
     * @param context          host的context
     * @param originalReceiver 静态注册的BroadcastReceiver
     * @param myRecevier       自己的BroadcastReceiver
     * @throws Exception
     */
    public static void hookReceiverDispatcher(Context context, BroadcastReceiver originalReceiver, BroadcastReceiver myRecevier) throws Exception {
        Class<?> classActivityThread = Class.forName("android.app.ActivityThread");
        Field field_sCurrentActivityThread = classActivityThread.getDeclaredField("sCurrentActivityThread");
        field_sCurrentActivityThread.setAccessible(true);
        Object objActivityThread = field_sCurrentActivityThread.get(null);
        Field field_mPackages = classActivityThread.getDeclaredField("mPackages");
        field_mPackages.setAccessible(true);
        ArrayMap mPackages = (ArrayMap) field_mPackages.get(objActivityThread);
        WeakReference reference = (WeakReference) mPackages.get(context.getPackageName());
        Object objLoadedApk = reference.get();
        Class<?> classLoadedApk = Class.forName("android.app.LoadedApk");
        Field field_mReceivers = classLoadedApk.getDeclaredField("mReceivers");
        field_mReceivers.setAccessible(true);
        ArrayMap mReceivers = (ArrayMap) field_mReceivers.get(objLoadedApk);
        ArrayMap receiver_dispather_pairs = (ArrayMap) mReceivers.get(context);
        Object objReceiverDispatcher = receiver_dispather_pairs.get(originalReceiver);
        Class<?> classReceiverDispatcher = Class.forName("android.app.LoadedApk$ReceiverDispatcher");
        Field field_mReceiver = classReceiverDispatcher.getDeclaredField("mReceiver");
        field_mReceiver.setAccessible(true);
        field_mReceiver.set(objReceiverDispatcher, myRecevier);
    }
}

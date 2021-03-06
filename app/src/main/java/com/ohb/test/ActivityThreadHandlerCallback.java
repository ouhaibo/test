package com.ohb.test;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Handler;
import android.os.Message;

import com.ohb.test.Utils;

import java.lang.reflect.Field;

/**
 * Created by Administrator on 2017/8/14.
 */

public class ActivityThreadHandlerCallback implements Handler.Callback {
    @Override
    public boolean handleMessage(Message msg) {
        switch (msg.what) {
            case 100://LAUNCH_ACTIVITY
                hookLaunchActivity(msg);
                break;
        }
        return false;
    }

    private void hookLaunchActivity(Message msg) {
        Object obj = msg.obj;//ActivityClientRecord
        try {
            Field intentField = obj.getClass().getDeclaredField("intent");
            intentField.setAccessible(true);
            Intent fakeIntent = (Intent) intentField.get(obj);
            Intent realIntent = fakeIntent.getParcelableExtra("realIntent");
            if (realIntent == null) {
                return;
            }
            fakeIntent.setComponent(realIntent.getComponent());

            Field fieldActivityInfo = obj.getClass().getDeclaredField("activityInfo");
            fieldActivityInfo.setAccessible(true);
            ActivityInfo activityInfo = (ActivityInfo) fieldActivityInfo.get(obj);
            activityInfo.applicationInfo = Utils.generateApplicationInfo(null);//ActivityThread中通过ApplicationInfo来获取LoadedApk
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

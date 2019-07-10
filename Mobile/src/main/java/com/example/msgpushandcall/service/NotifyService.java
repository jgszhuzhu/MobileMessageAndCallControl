package com.example.msgpushandcall.service;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.Notification;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Process;
import android.provider.CallLog;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.widget.RemoteViews;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;



@SuppressLint("Registered")
public class NotifyService extends NotificationListenerService {
    private String TAG  = NotifyService.class.getSimpleName();
    public static final String SEND_MSG_BROADCAST = "SEND_MSG_BROADCAST";

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        Log.i(TAG, "onNotificationPosted: ");
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn, RankingMap rankingMap) {

        String packageName = sbn.getPackageName();
//        if (!packageName.contains(ComeMessage.MMS) && !packageName.contains(ComeMessage.QQ) && !packageName.contains(ComeMessage.WX)) {
//            return;
//        }
        Intent intent = new Intent();
        intent.setAction(SEND_MSG_BROADCAST);
        Bundle bundle = new Bundle();
        bundle.putString("packageName", packageName);
        String content = null;
        if (sbn.getNotification().tickerText != null) {
            content = sbn.getNotification().tickerText.toString();
        }
        if (content == null) {
            Map<String, Object> info = getNotiInfo(sbn.getNotification());
            if (info != null) {
                content = info.get("title") + ":" + info.get("text");
            }
        }
        if(content == null || content.length() == 1){
            return;
        }
        intent.putExtra("content", content);
        intent.putExtras(bundle);

        Log.i(TAG, "onNotificationPosted: 通知"+content  +"  包名:"+sbn.getPackageName());
        Log.i(TAG, "onNotificationPosted: "+"未接电话"+(readMissCall()-1));
        this.sendBroadcast(intent);
    }


    public static  boolean  isRunning(Context context){
        ComponentName componentName  =  new ComponentName(context,NotifyService.class);
        ActivityManager manager  = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        boolean  isRunning = false;
        List<ActivityManager.RunningServiceInfo> runningService=manager.getRunningServices(1000);
        if (runningService==null){
            return false;
        }
        for(ActivityManager.RunningServiceInfo  serviceInfo :runningService){
            if (serviceInfo.service.equals(componentName)){
                if (serviceInfo.pid== Process.myPid()){
                    isRunning=true;
                }
            }
        }
        if (!isRunning){
            requesRebind(context);
        }
        return  isRunning;
    }
    private static void requesRebind(Context context){
        context.startService(new Intent(context,NotifyService.class));
        ComponentName  componentName  =  new ComponentName(context,NotifyService.class);
        PackageManager pm = context.getPackageManager();
        pm.setComponentEnabledSetting(new ComponentName(context, NotifyService.class),
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
        pm.setComponentEnabledSetting(new ComponentName(context, NotifyService.class),
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);
    }


    @Override
    public void onNotificationRemoved(StatusBarNotification sbn, RankingMap rankingMap) {
    }

    @Override
    public StatusBarNotification[] getActiveNotifications() {
        return super.getActiveNotifications();
    }

    /**
     * 读取未接电话数量
     * @return
     */
    private int readMissCall() {
        int result = 0;
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_CALL_LOG) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return -1;
        }
        Cursor cursor = getContentResolver().query(CallLog.Calls.CONTENT_URI, new String[]{
                CallLog.Calls.TYPE
        }, " type=? and new=?", new String[]{
                CallLog.Calls.MISSED_TYPE + "", "1"
        }, "date desc");

        if (cursor != null) {
            result = cursor.getCount();
            cursor.close();
        }
        return result;
    }
    /**
     * 反射取通知栏信息
     *
     * @param notification
     * @return 返回短信内容
     */
    private Map<String, Object> getNotiInfo(Notification notification) {
        int key = 0;
        if (notification == null)
            return null;
        RemoteViews views = notification.contentView;
        if (views == null)
            return null;
        Class secretClass = views.getClass();

        try {
            Map<String, Object> text = new HashMap<>();

            Field outerFields[] = secretClass.getDeclaredFields();
            for (int i = 0; i < outerFields.length; i++) {
                if (!outerFields[i].getName().equals("mActions"))
                    continue;

                outerFields[i].setAccessible(true);

                ArrayList<Object> actions = (ArrayList<Object>) outerFields[i].get(views);
                for (Object action : actions) {
                    Field innerFields[] = action.getClass().getDeclaredFields();
                    Object value = null;
                    Integer type = null;
                    for (Field field : innerFields) {
                        field.setAccessible(true);
                        if (field.getName().equals("value")) {
                            value = field.get(action);
                        } else if (field.getName().equals("type")) {
                            type = field.getInt(action);
                        }
                    }
                    // 经验所得 type 等于9 10为短信title和内容，不排除其他厂商拿不到的情况
                    if (type != null && (type == 9 || type == 10)) {
                        if (key == 0) {
                            text.put("title", value != null ? value.toString() : "");
                        } else if (key == 1) {
                            text.put("text", value != null ? value.toString() : "");
                        } else {
                            text.put(Integer.toString(key), value != null ? value.toString() : null);
                        }
                        key++;
                    }
                }
                key = 0;

            }
            return text;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}

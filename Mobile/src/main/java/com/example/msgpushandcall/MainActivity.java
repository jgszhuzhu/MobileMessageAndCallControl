package com.example.msgpushandcall;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.CallLog;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;

import com.example.msgpushandcall.service.ComeMessage;
import com.example.msgpushandcall.service.IComeMessage;
import com.example.msgpushandcall.service.NotifyService;
import com.example.msgpushandcall.utils.PhoneCallUtil;

public class MainActivity extends AppCompatActivity implements IComeMessage {

    private TelephonyManager telephonyManager;

    private PhoneCallListener callListener;
    private String TAG = MainActivity.class.getSimpleName();
    private static int lastCallState = TelephonyManager.CALL_STATE_IDLE;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.i(TAG, "onCreate: ");
        NotifyService.isRunning(MainActivity.this);
        findViewById(R.id.tv_phone).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                telephonyManager.listen(callListener, PhoneStateListener.LISTEN_CALL_STATE);
            }
        });
        if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.CALL_PHONE}, 1000);
        }

        findViewById(R.id.tv_push_msg).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ComeMessage comeMessage = new ComeMessage(MainActivity.this, MainActivity.this);
                if (!comeMessage.isEnabled()) {
                    comeMessage.openSetting();
                    comeMessage.toggleNotificationListenerService();
                }
            }
        });


        telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        callListener = new PhoneCallListener();
    }

    @Override
    public void comeShortMessage(String msg) {
        Log.i(TAG, "comeShortMessage: " + msg);
    }

    @Override
    public void comeWxMessage(String msg) {
        Log.i(TAG, "comeWxMessage: " + msg);
    }

    @Override
    public void comeQQmessage(String msg) {
        Log.i(TAG, "comeQQmessage: " + msg);
    }



    /**
     * 监听来电状态
     */
    public class PhoneCallListener extends PhoneStateListener {
        @Override
        public void onCallStateChanged(int currentCallState, String incomingNumber) {
            if (currentCallState == TelephonyManager.CALL_STATE_IDLE) {// 空闲
//TODO
            } else if (currentCallState == TelephonyManager.CALL_STATE_RINGING) {// 响铃
                       PhoneCallUtil.endPhone(MainActivity.this);
            } else if (currentCallState == TelephonyManager.CALL_STATE_OFFHOOK) {// 接听
//TODO
            }
            //未接来电数量统计
            if (lastCallState == TelephonyManager.CALL_STATE_RINGING &&
                    currentCallState == TelephonyManager.CALL_STATE_IDLE) {
                Log.i("test", "onReceive: " + readMissCall());
            }
            lastCallState = currentCallState;
            super.onCallStateChanged(currentCallState, incomingNumber);
        }
    }

    private int readMissCall() {
        int result = 0;
        if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.READ_CALL_LOG) != PackageManager.PERMISSION_GRANTED) {
            return -1;
        }
        Cursor cursor = MainActivity.this.getContentResolver().query(CallLog.Calls.CONTENT_URI, new String[]{
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
}

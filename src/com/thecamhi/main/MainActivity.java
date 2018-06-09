package com.thecamhi.main;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTabHost;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TabHost.TabSpec;

import com.hichip.R;
import com.hichip.base.HiLog;
import com.hichip.sdk.HiChipSDK;
import com.hichip.sdk.HiChipSDK.HiChipInitCallback;
import com.tencent.android.tpush.XGIOperateCallback;
import com.tencent.android.tpush.XGPushConfig;
import com.tencent.android.tpush.XGPushManager;
import com.thecamhi.base.DatabaseManager;
import com.thecamhi.base.HiTools;
import com.thecamhi.bean.HiDataValue;
import com.thecamhi.bean.MyCamera;
import com.thecamhi.base.SharePreUtils;

public class MainActivity extends FragmentActivity {

    private final static int HANDLE_MESSAGE_INIT_END = 0x90000001;
    private Class<?> fraList[] = {CameraFragment.class};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initXGPushSDK();
        initview();
        initTabHost();

        initSDK();

    }

    /**
     * 隐藏虚拟按键，并且全屏
     */
    protected void hideBottomUIMenu() {
        // 隐藏虚拟按键，并且全屏
        if (Build.VERSION.SDK_INT > 11 && Build.VERSION.SDK_INT < 19) { // lower api
            View v = this.getWindow().getDecorView();
            v.setSystemUiVisibility(View.GONE);
        } else if (Build.VERSION.SDK_INT >= 19) {
            // for new api versions.
            View decorView = getWindow().getDecorView();
            int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
            decorView.setSystemUiVisibility(uiOptions);
        }
    }

    // 初始化SDK
    private void initSDK() {
        HiChipSDK.init(new HiChipInitCallback() {

            @Override
            public void onSuccess() {
                Message msg = handler.obtainMessage();
                msg.what = HANDLE_MESSAGE_INIT_END;
                handler.sendMessage(msg);
                HiLog.e("SDK INIT success");
            }

            @Override
            public void onFali(int arg0, int arg1) {
                Message msg = handler.obtainMessage();
                msg.what = HANDLE_MESSAGE_INIT_END;
                handler.sendMessage(msg);
                HiLog.e("SDK INIT fail");
            }
        });

    }

    // 搭载4个fragment
    private void initTabHost() {
        String[] tabString = getResources().getStringArray(R.array.tab_name);
        FragmentTabHost tabHost = (FragmentTabHost) findViewById(R.id.main_fragment_tabhost);
        tabHost.setup(this, getSupportFragmentManager(), R.id.fragment_main_content);
        tabHost.getTabWidget().setDividerDrawable(android.R.color.transparent);
        LayoutInflater inflater = LayoutInflater.from(this);
        for (int i = 0; i < fraList.length; i++) {
            View view = inflater.inflate(R.layout.fragment_tabhost_switch_image, null);
            TabSpec tabItem = tabHost.newTabSpec(tabString[i]).setIndicator(view);
            tabHost.addTab(tabItem, fraList[i], null);
        }
    }

    private void initXGPushSDK() {

        // 开启logcat输出，方便debug，发布时请关闭
        XGPushConfig.enableDebug(this, false);
        XGPushManager.registerPush(this, new XGIOperateCallback() {
            @Override
            public void onSuccess(Object data, int flag) {
                HiLog.e("bruce 注册成功，设备token为：" + data);
                String token = (String) data;
                if (HiDataValue.ANDROID_VERSION >= 6 && !HiTools.checkPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                    Log.d("charming", getString(R.string.tips_no_permission));
                    return;
                }
                String localToken = SharePreUtils.getString("XGToken", MainActivity.this, "xgtoken");
                if (!TextUtils.isEmpty(token) && TextUtils.isEmpty(localToken) && !token.equalsIgnoreCase(localToken)) {
                    SharePreUtils.putString("XGToken", MainActivity.this, "xgtoken", token);
                }
                if (!TextUtils.isEmpty(token)) {
                    HiDataValue.XGToken = token;
                }
                Log.i("tedu", "--HiDataValue.XGToken-sgvd->" + HiDataValue.XGToken);
            }

            @Override
            public void onFail(Object data, int errCode, String msg) {
                HiLog.e("bruce 注册失败，为：" + msg);
            }
        });
        HiDataValue.XGToken = SharePreUtils.getString("XGToken", MainActivity.this, "xgtoken");
    }

    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case HANDLE_MESSAGE_INIT_END:
                    initCamera();
                    break;

            }

        }
    };

    // 获取本地数据
    private void initCamera() {
        try {
            PackageManager manager = getPackageManager();
            PackageInfo info = manager.getPackageInfo(getPackageName(), 0);
            Log.i("", "---------当前版本号为：---------->>" + info.versionName);
        } catch (NameNotFoundException e) {
            e.printStackTrace();
        }
        DatabaseManager manager = new DatabaseManager(this);
        SQLiteDatabase db = manager.getReadableDatabase();
        Cursor cursor = db.query(DatabaseManager.TABLE_DEVICE, new String[]{"dev_nickname", "dev_uid", "view_acc", "view_pwd", "dev_videoQuality", "dev_alarmState", "dev_pushState", "dev_serverData"}, null, null, null, null, null);
        HiLog.e("step1 " + "cursor is null" + (cursor == null ? "true" : "false"));
        try {
            while (cursor != null && cursor.moveToNext()) {
                if (HiDataValue.isDebug)
                    HiLog.e("step2 " + "cursor is null=" + (cursor == null ? "true" : "false"));
                String dev_nickname = cursor.getString(0);
                String dev_uid = cursor.getString(1);

                // 兼容很久之前老版本,没有做UID装换和添加"-"的版本
                dev_uid = dev_uid.toUpperCase();
                dev_uid = HiTools.handUid(dev_uid);
                if (TextUtils.isEmpty(dev_uid)) {
                    throw new IllegalArgumentException("--UID invalid--");
                }

                String dev_name = cursor.getString(2);
                String dev_pwd = cursor.getString(3);
                int dev_videoQuality = cursor.getInt(4);
                int dev_alarmState = cursor.getInt(5);
                int dev_pushState = cursor.getInt(6);
                String dev_serverData = cursor.getString(7);
                MyCamera camera = new MyCamera(getApplicationContext(), dev_nickname, dev_uid, dev_name, dev_pwd);
                camera.setVideoQuality(dev_videoQuality);
                camera.setAlarmState(dev_alarmState);
                camera.setPushState(dev_pushState);
                camera.setServerData(dev_serverData);
                camera.saveInCameraList();
                if (camera.getPushState() == 0) {
                    String pDID = camera.getUid();
                    SharedPreferences setting = MainActivity.this.getSharedPreferences("Subid_" + pDID, MainActivity.MODE_PRIVATE);
                    int subID = setting.getInt("pushon", -1);
                    if (subID == 1) {
                        camera.setPushState(1);
                    } else {
                        camera.setPushState(0);
                    }
                }
            }

        } catch (Exception e) {
            // 删除snapshot数据;
            // initCamera();
        } finally {
            cursor.close();
            cursor = null;
            db.close();
        }

        requestEnd();

    }

    @Override
    protected void onResume() {
        super.onResume();

    }

    public void requestEnd() {
        // 获取数据完毕，发送广播到CameraFragment界面去刷新adapter
        Intent intent = new Intent();
        intent.setAction(HiDataValue.ACTION_CAMERA_INIT_END);
        sendBroadcast(intent);
    }

    private void initview() {

        HiDataValue.ANDROID_VERSION = HiTools.getAndroidVersion();

        if (HiDataValue.ANDROID_VERSION >= 6) {
            HiTools.checkPermissionAll(MainActivity.this);
        }

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        for (MyCamera camera : HiDataValue.CameraList) {
            if (camera.isSetValueWithoutSave()) {
                camera.updateInDatabase(this);
            }
            DatabaseManager db = new DatabaseManager(MainActivity.this);
            if (db != null) {
                db.updateAlarmStateByUID(camera.getUid(), 0);
            }
            camera.disconnect();
        }
        HiChipSDK.uninit();
        //杀掉进程,可以让app白屏时间更短,camera 列表不会重复。
        int pid = android.os.Process.myPid();
        android.os.Process.killProcess(pid);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {

        switch (keyCode) {
            case KeyEvent.KEYCODE_BACK:
                finish();
                android.os.Process.killProcess(android.os.Process.myPid());
                break;
        }
        return true;
    }
}

package com.thecamhi.activity.setting;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.hichip.R;
import com.hichip.callback.ICameraIOSessionCallback;
import com.hichip.content.HiChipDefines;
import com.hichip.control.HiCamera;
import com.thecamhi.base.DownloadVideoUtils;
import com.thecamhi.bean.HiDataValue;
import com.thecamhi.bean.MyCamera;
import com.thecamhi.base.SharePreUtils;

/**
 * 摄像机设置界面
 *
 * @author lt
 */
public class AliveSettingActivity extends FragmentActivity implements OnClickListener, ICameraIOSessionCallback {
    private MyCamera mCamera;
    private boolean mIsSupportRF = false;
    public static Activity mActivity;
    private ImageView mIvRF;
    private View red_spot;
    private LinearLayout ll_parent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d("charming", "AliveSettingActivity is called");
        setContentView(R.layout.alive_setting_activity);
        String uid = getIntent().getStringExtra(HiDataValue.EXTRAS_KEY_UID);

        for (MyCamera camera : HiDataValue.CameraList) {
            if (uid.equals(camera.getUid())) {
                mCamera = camera;
                break;
            }
        }
        //非空判断,解决bugly上面的可疑bug。
        if (mCamera == null) {
            AliveSettingActivity.this.finish();
            Log.d("charming", getString(R.string.disconnect));
            return;
        }

        if (mCamera.getConnectState() == HiCamera.CAMERA_CONNECTION_STATE_LOGIN) {
            mIsSupportRF = mCamera.getCommandFunction(HiChipDefines.HI_P2P_IPCRF_ALL_INFO_GET);
            SharePreUtils.putBoolean("isSupportRF", AliveSettingActivity.this, mCamera.getUid(), mIsSupportRF);
        } else {
            mIsSupportRF = SharePreUtils.getBoolean("isSupportRF", AliveSettingActivity.this, mCamera.getUid());
        }

        mActivity = this;

        initView();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mCamera != null) {
            if (mIsSupportRF) {
                red_spot.setVisibility(mCamera.isAlarmLog() ? View.VISIBLE : View.GONE);
            }
            mCamera.registerIOSessionListener(this);
        }
        if (mCamera.getConnectState() != HiCamera.CAMERA_CONNECTION_STATE_LOGIN) {
            for (int i = 0; i < ll_parent.getChildCount(); i++) {
                View view = ll_parent.getChildAt(i);
                view.setEnabled(false);
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mCamera != null) {
            mCamera.unregisterIOSessionListener(this);

        }
    }

    private void initView() {

        ll_parent = (LinearLayout) findViewById(R.id.ll_parent);

        TextView timing_video = (TextView) findViewById(R.id.timing_video);
        if (!mCamera.getCommandFunction(HiChipDefines.HI_P2P_GET_REC_AUTO_PARAM) && !mCamera.getCommandFunction(HiChipDefines.HI_P2P_GET_REC_AUTO_SCHEDULE)) {
            timing_video.setVisibility(View.GONE);
        }
        timing_video.setOnClickListener(this);


        TextView sd_card_set = (TextView) findViewById(R.id.sd_card_set);
        sd_card_set.setOnClickListener(this);

        TextView equipment_time_setting = (TextView) findViewById(R.id.equipment_time_setting);
        equipment_time_setting.setOnClickListener(this);


        TextView system_settings = (TextView) findViewById(R.id.system_settings);
        if (!mCamera.getCommandFunction(HiChipDefines.HI_P2P_SET_RESET) && !mCamera.getCommandFunction(HiChipDefines.HI_P2P_SET_REBOOT)) {
            system_settings.setVisibility(View.GONE);
        }
        system_settings.setOnClickListener(this);

        TextView download_test = (TextView) findViewById(R.id.download_test);
        download_test.setOnClickListener(this);

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.timing_video: {
                Intent intent = new Intent(AliveSettingActivity.this, TimeVideoActivity.class);
                intent.putExtra(HiDataValue.EXTRAS_KEY_UID, mCamera.getUid());
                startActivity(intent);
            }
            break;
            case R.id.sd_card_set: {
                Intent intent = new Intent(AliveSettingActivity.this, SDCardSettingActivity.class);
                intent.putExtra(HiDataValue.EXTRAS_KEY_UID, mCamera.getUid());
                startActivity(intent);
            }
            break;
            case R.id.equipment_time_setting: {
                Intent intent = new Intent(AliveSettingActivity.this, TimeSettingActivity.class);
                intent.putExtra(HiDataValue.EXTRAS_KEY_UID, mCamera.getUid());
                startActivity(intent);
            }
            break;
            case R.id.system_settings: {
                Intent intent = new Intent(AliveSettingActivity.this, SystemSettingActivity.class);
                intent.putExtra(HiDataValue.EXTRAS_KEY_UID, mCamera.getUid());
                startActivity(intent);
            }
            break;
            case R.id.download_test: {
                DownloadVideoUtils downloadVideoUtils = new DownloadVideoUtils(mCamera.getUid());
                downloadVideoUtils.downloadVideo(System.currentTimeMillis() - 2 * 60 * 60 * 1000, System.currentTimeMillis());
            }
            break;
        }

    }

    @Override
    public void receiveIOCtrlData(HiCamera arg0, int arg1, byte[] arg2, int arg3) {

    }

    @Override
    public void receiveSessionState(HiCamera arg0, int arg1) {
        Message message = Message.obtain();
        message.what = HiDataValue.HANDLE_MESSAGE_SESSION_STATE;
        message.obj = arg0;
        message.arg1 = arg1;
        mHandler.sendMessage(message);

    }

    private Handler mHandler = new Handler() {
        public void handleMessage(android.os.Message msg) {
            switch (msg.what) {
                case HiDataValue.HANDLE_MESSAGE_SESSION_STATE:
                    if (msg.arg1 == HiCamera.CAMERA_CONNECTION_STATE_DISCONNECTED || msg.arg1 == HiCamera.CAMERA_CONNECTION_STATE_WRONG_PASSWORD) {
                        //finish();
                        for (int i = 0; i < ll_parent.getChildCount(); i++) {
                            View view = ll_parent.getChildAt(i);
                            view.setEnabled(false);
                        }
                    } else if (msg.arg1 == HiCamera.CAMERA_CONNECTION_STATE_LOGIN) {
                        for (int i = 0; i < ll_parent.getChildCount(); i++) {
                            View view = ll_parent.getChildAt(i);
                            view.setEnabled(true);
                        }
                    }
                    break;
            }
        }

        ;
    };
}

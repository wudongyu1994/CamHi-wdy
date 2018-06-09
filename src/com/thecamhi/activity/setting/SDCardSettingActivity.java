package com.thecamhi.activity.setting;

import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

import com.hichip.R;
import com.hichip.callback.ICameraIOSessionCallback;
import com.hichip.content.HiChipDefines;
import com.hichip.content.HiChipDefines.HI_P2P_S_SD_INFO;
import com.hichip.control.HiCamera;
import com.thecamhi.bean.HiDataValue;
import com.thecamhi.bean.MyCamera;

public class SDCardSettingActivity extends FragmentActivity implements ICameraIOSessionCallback {
    private MyCamera mCamera;
    private TextView available_space_size, storage_space_size;
    private HI_P2P_S_SD_INFO sd_info;
    private Button format_sd_card;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sd_card_setting);

        String uid = getIntent().getStringExtra(HiDataValue.EXTRAS_KEY_UID);
        for (MyCamera camera : HiDataValue.CameraList) {
            if (uid.equals(camera.getUid())) {
                mCamera = camera;
                //mCamera.sendIOCtrl(HiChipDefines.HI_P2P_GET_SD_INFO, new byte[0]);
                break;

            }
        }

        initView();
    }

    private void initView() {

        available_space_size = (TextView) findViewById(R.id.available_space_size);
        storage_space_size = (TextView) findViewById(R.id.storage_space_size);

        format_sd_card = (Button) findViewById(R.id.format_sd_card);
        format_sd_card.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                if (sd_info != null && sd_info.u32Status == 1) {// 有SD卡
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            mCamera.sendIOCtrl(HiChipDefines.HI_P2P_SET_FORMAT_SD, null);

                        }
                    }, 10000);
                }

            }
        });
    }

    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case HiDataValue.HANDLE_MESSAGE_RECEIVE_IOCTRL:
                    if (msg.arg2 == 0) {
                        // MyCamera camera = (MyCamera)msg.obj;
                        Bundle bundle = msg.getData();
                        byte[] data = bundle.getByteArray(HiDataValue.EXTRAS_KEY_DATA);
                        switch (msg.arg1) {
                            case HiChipDefines.HI_P2P_GET_SD_INFO:
                                sd_info = new HiChipDefines.HI_P2P_S_SD_INFO(data);
                                int used = sd_info.u32Space - sd_info.u32LeftSpace;
                                // sd_info.u32Status 1表示有SD卡 0表示没有
                                if (sd_info.u32Status == 0) {
                                    format_sd_card.setEnabled(false);
                                    available_space_size.setText("0 MB");
                                    storage_space_size.setText("0 MB");
                                } else {
                                    format_sd_card.setTextColor(Color.RED);
                                    if (sd_info.u32Space == 0) {
                                        available_space_size.setText("计算中");
                                        storage_space_size.setText("计算中");
                                    } else {
                                        available_space_size.setText(String.valueOf(sd_info.u32LeftSpace / 1024) + " MB");
                                        storage_space_size.setText(String.valueOf(sd_info.u32Space / 1024) + " MB");
                                    }
                                }

                                break;
                            case HiChipDefines.HI_P2P_SET_FORMAT_SD:
                                Log.d("charming", "format succeed!");
                                finish();
                                break;

                        }
                    }
                    break;
            }
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        if (mCamera != null) {
            mCamera.registerIOSessionListener(this);
            mCamera.sendIOCtrl(HiChipDefines.HI_P2P_GET_SD_INFO, new byte[0]);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mCamera != null) {
            mCamera.unregisterIOSessionListener(this);
        }
    }

    @Override
    public void receiveIOCtrlData(HiCamera arg0, int arg1, byte[] arg2, int arg3) {
        if (arg0 != mCamera)
            return;

        Bundle bundle = new Bundle();
        bundle.putByteArray(HiDataValue.EXTRAS_KEY_DATA, arg2);
        Message msg = handler.obtainMessage();
        msg.what = HiDataValue.HANDLE_MESSAGE_RECEIVE_IOCTRL;
        msg.obj = arg0;
        msg.arg1 = arg1;
        msg.arg2 = arg3;
        msg.setData(bundle);
        handler.sendMessage(msg);

    }

    @Override
    public void receiveSessionState(HiCamera arg0, int arg1) {
        // TODO Auto-generated method stub

    }
}

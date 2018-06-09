package com.thecamhi.activity.setting;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.hichip.R;
import com.hichip.callback.ICameraIOSessionCallback;
import com.hichip.content.HiChipDefines;
import com.hichip.control.HiCamera;
import com.hichip.data.HiDeviceInfo;
import com.thecamhi.bean.HiDataValue;
import com.thecamhi.bean.MyCamera;

/**
 * 计划录像Activity
 *
 * @author lt
 */
public class TimeVideoActivity extends FragmentActivity implements ICameraIOSessionCallback {
    private MyCamera mCamera = null;

    private HiChipDefines.HI_P2P_QUANTUM_TIME quantum_time;
    private HiChipDefines.HI_P2P_S_REC_AUTO_PARAM rec_param;
    private boolean isAllDayRec = true;

    private static final int GOKE_TIME = 600;
    private static final int HISI_TIME = 900;
    private int recordTime = HISI_TIME;
    private EditText video_time_et;
    private Button btn_record_application;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_timing_video);

        String uid = getIntent().getStringExtra(HiDataValue.EXTRAS_KEY_UID);
        for (MyCamera camera : HiDataValue.CameraList) {
            if (uid.equals(camera.getUid())) {
                mCamera = camera;
                mCamera.sendIOCtrl(HiChipDefines.HI_P2P_GET_REC_AUTO_PARAM, new byte[0]);
                mCamera.sendIOCtrl(HiChipDefines.HI_P2P_GET_REC_AUTO_SCHEDULE, new byte[0]);
                break;

            }
        }

        initView();

    }

    private void initView() {

        if (mCamera.getChipVersion() == HiDeviceInfo.CHIP_VERSION_GOKE) {
            recordTime = GOKE_TIME;
        }
        String recording_time_str = String.format("录像时间范围是 15秒至%d秒", recordTime);
        TextView tips_recording_time = (TextView) findViewById(R.id.tips_recording_time);
        tips_recording_time.setText(recording_time_str);

        video_time_et = (EditText) findViewById(R.id.video_time_et);

        btn_record_application = (Button) findViewById(R.id.btn_record_application);
        btn_record_application.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {

                updateRecodingParam();

            }
        });

    }


    public void updateRecodingParam() {
        if (quantum_time == null || rec_param == null) {
            return;
        }

        String str = video_time_et.getText().toString().trim();
        int rec_time_val = 0;
        if (str != null && str.length() > 0) {
            rec_time_val = Integer.valueOf(str);
        }

        if (rec_time_val < 15 || rec_time_val > recordTime) {

            Toast.makeText(TimeVideoActivity.this, String.format("录像时间范围是 15秒至%d秒", recordTime), Toast.LENGTH_LONG).show();
            return;
        }
        rec_param.u32Enable = 1;
        rec_param.u32FileLen = rec_time_val;

        mCamera.sendIOCtrl(HiChipDefines.HI_P2P_SET_REC_AUTO_PARAM, rec_param.parseContent());


        byte val = 80;

        for (int i = 0; i < 7; i++) {
            for (int j = 0; j < 48; j++) {
                quantum_time.sDayData[i][j] = val;
            }
        }

        quantum_time.u32QtType = HiChipDefines.HI_P2P_TYPE_PLAN;
        mCamera.sendIOCtrl(HiChipDefines.HI_P2P_SET_REC_AUTO_SCHEDULE, quantum_time.parseContent());

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

    }

    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case HiDataValue.HANDLE_MESSAGE_RECEIVE_IOCTRL: {
                    if (msg.arg2 == 0) {
                        //					MyCamera camera = (MyCamera)msg.obj;
                        Bundle bundle = msg.getData();
                        byte[] data = bundle.getByteArray(HiDataValue.EXTRAS_KEY_DATA);
                        switch (msg.arg1) {

                            case HiChipDefines.HI_P2P_GET_REC_AUTO_PARAM:
                                rec_param = new HiChipDefines.HI_P2P_S_REC_AUTO_PARAM(data);
                                video_time_et.setText(String.valueOf(rec_param.u32FileLen));
                                break;

                            case HiChipDefines.HI_P2P_GET_REC_AUTO_SCHEDULE:

                                quantum_time = new HiChipDefines.HI_P2P_QUANTUM_TIME(data);
                                for (int i = 0; i < 7; i++) {
                                    for (int j = 0; j < 48; j++) {
                                        if (quantum_time.sDayData[i][j] == 78) {
                                            isAllDayRec = false;
                                            break;
                                        }
                                    }
                                }

                                break;
                            case HiChipDefines.HI_P2P_GET_SD_INFO: {
                                HiChipDefines.HI_P2P_S_SD_INFO sd_info = new HiChipDefines.HI_P2P_S_SD_INFO(data);

                                int used = sd_info.u32Space - sd_info.u32LeftSpace;
                            }
                            break;
                            case HiChipDefines.HI_P2P_SET_REC_AUTO_SCHEDULE:
                                Log.d("charming", "setting succeed!");
                                finish();
                                break;


                        }
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
        }
    }


    @Override
    public void onPause() {
        super.onPause();
        if (mCamera != null) {
            mCamera.unregisterIOSessionListener(this);
        }
    }
}

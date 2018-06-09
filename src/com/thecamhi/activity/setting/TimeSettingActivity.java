package com.thecamhi.activity.setting;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.hichip.R;
import com.hichip.callback.ICameraIOSessionCallback;
import com.hichip.content.HiChipDefines;
import com.hichip.content.HiChipDefines.HI_P2P_S_TIME_PARAM;
import com.hichip.content.HiChipDefines.HI_P2P_S_TIME_ZONE;
import com.hichip.content.HiChipDefines.HI_P2P_S_TIME_ZONE_EXT;
import com.hichip.control.HiCamera;
import com.hichip.sdk.HiChipP2P;
import com.hichip.system.HiDefaultData;
import com.thecamhi.bean.HiDataValue;
import com.thecamhi.bean.MyCamera;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

public class TimeSettingActivity extends FragmentActivity implements ICameraIOSessionCallback, OnClickListener {
    private MyCamera mCamera;
    private TextView tvDeviceTime;
    private RelativeLayout mtlTimeZone;
    private TextView mTvTimeZone;
    private String[] strings;
    protected int index = 0;
    private int deviceTimezonIndex = -1;
    private int mTz = -1;
    private int mDesmode = 0;
    protected HI_P2P_S_TIME_ZONE timezone;
    protected HI_P2P_S_TIME_ZONE_EXT time_ZONE_EXT;
    public static final String REQUESTCODE_INDEX = "INDEX";
    public static final int REQUSTCIDE_119 = 0X119;
    private boolean mIsSupportZoneExt;

    private RelativeLayout mRlXls;
    private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_equipment_time_setting);

        String uid = getIntent().getStringExtra(HiDataValue.EXTRAS_KEY_UID);

        for (MyCamera camera : HiDataValue.CameraList) {
            if (uid.equals(camera.getUid())) {
                mCamera = camera;
                // mCamera.sendIOCtrl(HiChipDefines.HI_P2P_GET_TIME_PARAM, new
                // byte[0]);
                mIsSupportZoneExt = mCamera.getCommandFunction(HiChipDefines.HI_P2P_GET_TIME_ZONE_EXT);
                if (mIsSupportZoneExt) {// 支持新时区
                    mCamera.sendIOCtrl(HiChipDefines.HI_P2P_GET_TIME_ZONE_EXT, new byte[0]);
                    strings = getResources().getStringArray(R.array.device_timezone_new);
                } else {
                    mCamera.sendIOCtrl(HiChipDefines.HI_P2P_GET_TIME_ZONE, new byte[0]);
                    strings = getResources().getStringArray(R.array.device_timezone_old);
                }
                mCamera.sendIOCtrl(HiChipDefines.HI_P2P_GET_TIME_PARAM, new byte[0]);
                break;
            }
        }

        initView();
        setListenersAndGetData();
    }

    private void setListenersAndGetData() {
        mtlTimeZone.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                Intent intent = new Intent(TimeSettingActivity.this, TimeZoneListActivity.class);
                intent.putExtra(REQUESTCODE_INDEX, index);
                if (timezone != null) {
                    intent.putExtra("u32DstMode", timezone.u32DstMode);
                }
                intent.putExtra("stringarray", strings);
                intent.putExtra("boolean", mIsSupportZoneExt);
                startActivityForResult(intent, REQUSTCIDE_119);
            }
        });

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUSTCIDE_119 && resultCode == RESULT_OK) {
            mTz = data.getIntExtra(TimeZoneListActivity.KEY_TZ, 0);
            // mDesmode = data.getIntExtra(TimeZoneListActivity.KEY_DESMODE, 0);
            index = mTz;
            // mTvTimeZone.setText(strings[mTz]);
            if (mIsSupportZoneExt) {
                mTvTimeZone.setText(HiDefaultData.TimeZoneField1[index][1] + " " + strings[mTz]);
                mRlXls.setVisibility("1".equals(HiDefaultData.TimeZoneField1[index][2]) ? View.VISIBLE : View.GONE);
                mDesmode = 0;
            } else {
                mTvTimeZone.setText(strings[mTz]);
                mRlXls.setVisibility(HiDefaultData.TimeZoneField[index][1] == 1 ? View.VISIBLE : View.GONE);
                mDesmode = 0;
            }

        }
    }

    private void initView() {

        tvDeviceTime = (TextView) findViewById(R.id.tv_device_time);
        mtlTimeZone = (RelativeLayout) findViewById(R.id.rl_time_zone);
        mTvTimeZone = (TextView) findViewById(R.id.tv_time_zone);
        mRlXls = (RelativeLayout) findViewById(R.id.ll_xls);

        TextView phone_time_zone_et = (TextView) findViewById(R.id.phone_time_zone_et);

        TimeZone tz = TimeZone.getDefault();
        float tim = (float) tz.getRawOffset() / (3600000.0f);
        String gmt = null;
        gmt = "GMT" + tim;
        if (tim > 0) {
            gmt = "GMT+" + tim;
        }
        phone_time_zone_et.setText(gmt + "  " + tz.getDisplayName());

        Button setting_time_zone_btn = (Button) findViewById(R.id.setting_time_zone_btn);
        setting_time_zone_btn.setOnClickListener(this);
        Button synchronization_time_btn = (Button) findViewById(R.id.synchronization_time_btn);
        synchronization_time_btn.setOnClickListener(this);
    }

    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.arg2 != 0 && msg.what != HiDataValue.HANDLE_MESSAGE_RECEIVE_IOCTRL)
                return;
            switch (msg.what) {
                case HiDataValue.HANDLE_MESSAGE_SESSION_STATE:
                    if (msg.arg1 == HiCamera.CAMERA_CONNECTION_STATE_LOGIN) {
                        syncDeviceTime();
                    }
                    break;
                case HiDataValue.HANDLE_MESSAGE_RECEIVE_IOCTRL:
                    if (msg.arg2 == 0) {
                        handlerSuccess(msg);
                    } else {
                        switch (msg.arg1) {
                            case HiChipDefines.HI_P2P_SET_TIME_ZONE:
                            case HiChipDefines.HI_P2P_SET_TIME_ZONE_EXT:
                                Log.d("charming", "set time zone failed");
                                break;

                        }
                    }
                    break;
            }
        }

        private void handlerSuccess(Message msg) {
            Bundle bundle = msg.getData();
            byte[] data = bundle.getByteArray(HiDataValue.EXTRAS_KEY_DATA);
            switch (msg.arg1) {
                case HiChipDefines.HI_P2P_GET_TIME_PARAM:
                    HI_P2P_S_TIME_PARAM timeParam = new HiChipDefines.HI_P2P_S_TIME_PARAM(data);
                    StringBuffer sb = new StringBuffer();

                    sb.append(timeParam.u32Year + "-" + timeParam.u32Month + "-" + timeParam.u32Day + " " + timeParam.u32Hour + ":" + timeParam.u32Minute + ":" + timeParam.u32Second);
                    Date date1 = new Date();
                    try {
                        date1 = sdf.parse(sb.toString());
                    } catch (ParseException e) {
                    }
                    tvDeviceTime.setText(sdf.format(date1));
                    break;
                case HiChipDefines.HI_P2P_GET_TIME_ZONE:
                    timezone = new HiChipDefines.HI_P2P_S_TIME_ZONE(data);
                    index = -1;
                    for (int i = 0; i < HiDefaultData.TimeZoneField.length; i++) {
                        if (HiDefaultData.TimeZoneField[i][0] == timezone.s32TimeZone) {
                            index = i;
                            mTz = index;
                            deviceTimezonIndex = i;
                            break;
                        }
                    }
                    if (index == -1) {
                        mTvTimeZone.setText("获取设备时区失败");
                        Log.d("charming", "set time zone failed");
                    } else {
                        mTvTimeZone.setText(strings[index]);
                        mRlXls.setVisibility(HiDefaultData.TimeZoneField[index][1] == 1 ? View.VISIBLE : View.GONE);
                        mDesmode = timezone.u32DstMode == 1 ? 1 : 0;
                    }

                    break;

                case HiChipDefines.HI_P2P_GET_TIME_ZONE_EXT:// 新时区
                    if (data != null && data.length >= 36) {
                        time_ZONE_EXT = new HiChipDefines.HI_P2P_S_TIME_ZONE_EXT(data);
                        index = -1;
                        for (int i = 0; i < HiDefaultData.TimeZoneField1.length; i++) {
                            if (isEqual(time_ZONE_EXT.sTimeZone, HiDefaultData.TimeZoneField1[i][0])) {
                                index = i;
                                mTz = index;
                                deviceTimezonIndex = i;
                                break;
                            }
                        }
                        if (index == -1) {
                            mTvTimeZone.setText("获取设备时区失败");
                            Log.d("charming", "set time zone failed");
                        } else {
                            mTvTimeZone.setText(HiDefaultData.TimeZoneField1[index][1] + " " + strings[index]);
                            mRlXls.setVisibility("1".equals(HiDefaultData.TimeZoneField1[index][2]) ? View.VISIBLE : View.GONE);
                            mDesmode = time_ZONE_EXT.u32DstMode == 1 ? 1 : 0;
                        }
                    }

                    break;
                case HiChipDefines.HI_P2P_SET_TIME_PARAM:
                    // HiToast.showToast(TimeSettingActivity.this,
                    // getString(R.string.tips_device_time_setting_synchroned_time));
                    break;
                case HiChipDefines.HI_P2P_SET_TIME_ZONE:
                    mCamera.sendIOCtrl(HiChipDefines.HI_P2P_SET_REBOOT, new byte[0]);
                    Log.d("charming", "Setting Time Successfully,Camera Reboot Later");
                    // mCamera.sendIOCtrl(HiChipDefines.HI_P2P_GET_TIME_ZONE, new
                    // byte[0]);

                    break;
                case HiChipDefines.HI_P2P_SET_TIME_ZONE_EXT:
                    mCamera.sendIOCtrl(HiChipDefines.HI_P2P_SET_REBOOT, new byte[0]);
                    Log.d("charming", "Setting Time Successfully,Camera Reboot Later");
                    // mCamera.sendIOCtrl(HiChipDefines.HI_P2P_GET_TIME_ZONE_EXT,
                    // new byte[0]);
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

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.synchronization_time_btn:
                syncDeviceTime();
                break;
            case R.id.setting_time_zone_btn:
                if (mTvTimeZone.getText().toString().equals("获取设备时区失败")) {
                    return;
                }
                if ((deviceTimezonIndex == mTz || mTz == -1) && mRlXls.getVisibility() == View.GONE) {// 不能设置当前设备的时区
                    Log.d("charming", "You cannot set the time zone for the current device");
                    return;
                }
                sendTimeZone();
                break;
        }

    }

    private void syncDeviceTime() {
        Calendar cal = Calendar.getInstance(TimeZone.getDefault());
        cal.setTimeInMillis(System.currentTimeMillis());

        byte[] time = HiChipDefines.HI_P2P_S_TIME_PARAM.parseContent(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH), cal.get(Calendar.HOUR_OF_DAY),
                cal.get(Calendar.MINUTE), cal.get(Calendar.SECOND));

        mCamera.sendIOCtrl(HiChipDefines.HI_P2P_SET_TIME_PARAM, time);
        mCamera.sendIOCtrl(HiChipDefines.HI_P2P_GET_TIME_PARAM, new byte[0]);
    }

    public void sendTimeZone() {
        if (mIsSupportZoneExt) {
            if (mTz >= 0) {
                byte[] byte_time = HiDefaultData.TimeZoneField1[mTz][0].getBytes();
                if (byte_time.length <= 32) {
                    mCamera.sendIOCtrl(HiChipDefines.HI_P2P_SET_TIME_ZONE_EXT, HiChipDefines.HI_P2P_S_TIME_ZONE_EXT.parseContent(byte_time, mDesmode));
                }
            }

        } else {
            int tz = HiDefaultData.TimeZoneField[mTz][0];
            mCamera.sendIOCtrl(HiChipDefines.HI_P2P_SET_TIME_ZONE, HiChipDefines.HI_P2P_S_TIME_ZONE.parseContent(HiChipP2P.HI_P2P_SE_CMD_CHN, tz, mDesmode));
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
        if (arg0 == null && arg0 != mCamera) {
            return;
        }
        Message message = Message.obtain();
        message.what = HiDataValue.HANDLE_MESSAGE_SESSION_STATE;
        message.obj = arg0;
        message.arg1 = arg1;
        handler.sendMessage(message);

    }

    private boolean isEqual(byte[] bys, String str) {
        String string = new String(bys);
        String temp = string.substring(0, str.length());
        if (temp.equalsIgnoreCase(str)) {
            return true;
        }
        return false;
    }

}

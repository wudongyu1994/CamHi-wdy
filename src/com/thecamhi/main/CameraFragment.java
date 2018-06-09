package com.thecamhi.main;

import java.io.File;
import java.util.Calendar;
import java.util.TimeZone;
import java.util.regex.Pattern;

import android.Manifest;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.hichip.R;
import com.hichip.base.HiLog;
import com.hichip.callback.ICameraIOSessionCallback;
import com.hichip.content.HiChipDefines;
import com.hichip.control.HiCamera;
import com.tencent.android.tpush.XGPushConfig;
import com.thecamhi.activity.AddCameraActivity;
import com.thecamhi.activity.setting.AliveSettingActivity;
import com.thecamhi.base.HiTools;
import com.thecamhi.bean.CamHiDefines;
import com.thecamhi.bean.CamHiDefines.HI_P2P_ALARM_ADDRESS;
import com.thecamhi.bean.MyCamera.OnBindPushResult;
import com.thecamhi.base.SharePreUtils;

import com.thecamhi.bean.HiDataValue;
import com.thecamhi.bean.MyCamera;

public class CameraFragment extends Fragment implements ICameraIOSessionCallback, OnItemClickListener {
    private View layoutView;

    private CameraListAdapter adapter;
    private CameraBroadcastReceiver receiver;
    private ListView mListView;

    private String[] str_state;
    private boolean delModel = false;
    int ranNum;

    HiThreadConnect connectThread = null;

    public interface OnButtonClickListener {
        void onButtonClick(int btnId, MyCamera camera);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (receiver == null) {
            receiver = new CameraBroadcastReceiver();
            IntentFilter filter = new IntentFilter();
            filter.addAction(HiDataValue.ACTION_CAMERA_INIT_END);
            getActivity().registerReceiver(receiver, filter);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        layoutView = inflater.inflate(R.layout.fragment_camera, null);
        initView();
        ranNum = (int) (Math.random() * 10000);
        return layoutView;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    private void showDeleteCameraDialog(final MyCamera camera) {
        AlertDialog dialog = new AlertDialog.Builder(getActivity()).setTitle(getString(R.string.tip_reminder)).setMessage(getString(R.string.tips_msg_delete_camera)).setNegativeButton("取消", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        }).setPositiveButton("删除", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                camera.bindPushState(false, bindPushResult);
                SharePreUtils.putBoolean("cache", getActivity(), "isFirst", false);
                SharePreUtils.putBoolean("cache", getActivity(), "isFirstPbOnline", false);
                sendUnRegister(camera, 0);
                Message msg = handler.obtainMessage();
                msg.what = HiDataValue.HANDLE_MESSAGE_DELETE_FILE;
                msg.obj = camera;
                handler.sendMessageDelayed(msg, 1000);
            }
        }).create();
        dialog.show();

//        final NiftyDialogBuilder dialog = NiftyDialogBuilder.getInstance(getActivity());
//        dialog.withTitle(getString(R.string.tip_reminder)).withMessage(getString(R.string.tips_msg_delete_camera)).setButton1Click(new OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                dialog.dismiss();
//            }
//        }).setButton2Click(new OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                dialog.dismiss();
//                showjuHuaDialog();
//                camera.bindPushState(false, bindPushResult);
//                SharePreUtils.putBoolean("cache", getActivity(), "isFirst", false);
//                SharePreUtils.putBoolean("cache", getActivity(), "isFirstPbOnline", false);
//                sendUnRegister(camera, 0);
//                Message msg = handler.obtainMessage();
//                msg.what = HiDataValue.HANDLE_MESSAGE_DELETE_FILE;
//                msg.obj = camera;
//                handler.sendMessageDelayed(msg, 1000);
//            }
//        }).show();
    }

    private void initView() {
        mListView = (ListView) layoutView.findViewById(R.id.lv_swipemenu);
        LinearLayout add_camera_ll = (LinearLayout) layoutView.findViewById(R.id.add_camera_ll);
        add_camera_ll.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), AddCameraActivity.class);
                startActivity(intent);
            }
        });
        str_state = getActivity().getResources().getStringArray(R.array.connect_state);
        adapter = new CameraListAdapter(getActivity());
        mListView.setAdapter(adapter);
        mListView.setOnItemClickListener(this);
        adapter.setOnButtonClickListener(new OnButtonClickListener() {
            @Override
            public void onButtonClick(int btnId, final MyCamera camera) {
                switch (btnId) {
                    case R.id.setting_camera_item: {
                        Intent intent = new Intent();
                        intent.putExtra(HiDataValue.EXTRAS_KEY_UID, camera.getUid());
                        intent.setClass(getActivity(), AliveSettingActivity.class);
                        startActivity(intent);
                    }
                    break;

                    case R.id.delete_icon_camera_item:
                        showDeleteCameraDialog(camera);
                        break;
                }
            }

        });
    }

    private void sendUnRegister(MyCamera mCamera, int enable) {
        if (mCamera.getPushState() == 1) {
            return;
        }

        if (!mCamera.getCommandFunction(CamHiDefines.HI_P2P_ALARM_TOKEN_UNREGIST)) {
            return;
        }

        byte[] info = CamHiDefines.HI_P2P_ALARM_TOKEN_INFO.parseContent(0, mCamera.getPushState(), (int) (System.currentTimeMillis() / 1000 / 3600), enable);
        mCamera.sendIOCtrl(CamHiDefines.HI_P2P_ALARM_TOKEN_UNREGIST, info);
    }

    protected void sendRegisterToken(MyCamera mCamera) {
        Log.i("tedu", "--fasdf-mCamera.getPushState()->" + mCamera.getPushState());
        if (mCamera.getPushState() == 1 || mCamera.getPushState() == 0) {

            return;
        }

        if (!mCamera.getCommandFunction(CamHiDefines.HI_P2P_ALARM_TOKEN_REGIST)) {
            return;
        }

        byte[] info = CamHiDefines.HI_P2P_ALARM_TOKEN_INFO.parseContent(0, mCamera.getPushState(), (int) (System.currentTimeMillis() / 1000 / 3600), 1);

        mCamera.sendIOCtrl(CamHiDefines.HI_P2P_ALARM_TOKEN_REGIST, info);
    }

    OnBindPushResult bindPushResult = new OnBindPushResult() {
        @Override
        public void onBindSuccess(MyCamera camera) {

            if (!camera.handSubXYZ()) {
                camera.setServerData(HiDataValue.CAMERA_ALARM_ADDRESS);
            } else {
                camera.setServerData(HiDataValue.CAMERA_ALARM_ADDRESS_THERE);
            }
            camera.updateServerInDatabase(getActivity());
            sendServer(camera);
            sendRegisterToken(camera);
        }

        @Override
        public void onBindFail(MyCamera camera) {
        }

        @Override
        public void onUnBindSuccess(MyCamera camera) {
            camera.bindPushState(true, bindPushResult);
        }

        @Override
        public void onUnBindFail(MyCamera camera) {
            // ��SubId��ŵ�sharePrefence
            if (camera.getPushState() > 0) {
                SharePreUtils.putInt("subId", getActivity(), camera.getUid(), camera.getPushState());
            }

        }

    };

    private class CameraBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {

            if (intent.getAction().equals(HiDataValue.ACTION_CAMERA_INIT_END)) {
                if (adapter != null) {
                    adapter.notifyDataSetChanged();
                }

                if (HiDataValue.ANDROID_VERSION >= 6 && !HiTools.checkPermission(getActivity(), Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                    return;
                }
                new Handler().postAtTime(new Runnable() {

                    @Override
                    public void run() {
                        // TODO Auto-generated method stub
                        if (connectThread == null) {
                            connectThread = new HiThreadConnect();
                            connectThread.start();
                        }
                    }
                }, 100);


            }
        }
    }

    public class HiThreadConnect extends Thread {
        private int connnum = 0;

        public synchronized void run() {
            for (connnum = 0; connnum < HiDataValue.CameraList.size(); connnum++) {
                MyCamera camera = HiDataValue.CameraList.get(connnum);
                if (camera != null) {
                    if (camera.getConnectState() == HiCamera.CAMERA_CONNECTION_STATE_DISCONNECTED) {
                        camera.registerIOSessionListener(CameraFragment.this);
                        camera.connect();
                        try {
                            Thread.sleep(150);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }

                }
                Log.e("", "HiThreadConnect:" + connnum);
            }
            if (connectThread != null) {
                connectThread = null;
            }
        }

    }

    @Override
    public void onResume() {
        super.onResume();

    }


    // camera��adapter
    public class CameraListAdapter extends BaseAdapter {
        Context context;
        private LayoutInflater mInflater;
        OnButtonClickListener mListener;
        private String strState;

        public void setOnButtonClickListener(OnButtonClickListener listener) {
            mListener = listener;
        }

        public CameraListAdapter(Context context) {

            mInflater = LayoutInflater.from(context);
            this.context = context;
        }

        @Override
        public int getCount() {
            return HiDataValue.CameraList.size();
        }

        @Override
        public Object getItem(int position) {
            return HiDataValue.CameraList.get(position);
        }

        @Override
        public long getItemId(int arg0) {

            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            final MyCamera camera = HiDataValue.CameraList.get(position);
            if (camera == null) {
                return null;
            }
            ViewHolder holder = new ViewHolder();
            if (convertView == null) {
                convertView = mInflater.inflate(R.layout.camera_main_item, null);
                holder.setting = (ImageView) convertView.findViewById(R.id.setting_camera_item);
                holder.txt_nikename = (TextView) convertView.findViewById(R.id.nickname_camera_item);
                holder.txt_uid = (TextView) convertView.findViewById(R.id.uid_camera_item);
                holder.txt_state = (TextView) convertView.findViewById(R.id.state_camera_item);
                holder.delete_icon = (ImageView) convertView.findViewById(R.id.delete_icon_camera_item);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }
            if (holder != null) {
                holder.txt_nikename.setText(camera.getNikeName());
                holder.txt_uid.setText(camera.getUid());
                int state = camera.getConnectState();

                switch (state) {
                    case 0:// DISCONNECTED
                        holder.txt_state.setTextColor(getResources().getColor(R.color.color_disconnected));
                        break;
                    case -8:
                    case 1:// CONNECTING
                        holder.txt_state.setTextColor(getResources().getColor(R.color.color_connecting));
                        break;
                    case 2:// CONNECTED
                        holder.txt_state.setTextColor(getResources().getColor(R.color.color_connected));
                        break;
                    case 3:// WRONG_PASSWORD
                        holder.txt_state.setTextColor(getResources().getColor(R.color.color_pass_word));
                        break;
                    case 4:// STATE_LOGIN
                        holder.txt_state.setTextColor(getResources().getColor(R.color.color_login));
                        break;
                }
                if (state >= 0 && state <= 4) {
                    strState = str_state[state];
                    holder.txt_state.setText(strState);
                }
                if (state == -8) {// ҲҪ����Ϊ������...
                    holder.txt_state.setText(str_state[2]);
                }
                if (camera.isSystemState == 1 && camera.getConnectState() == HiCamera.CAMERA_CONNECTION_STATE_LOGIN) {
                    holder.txt_state.setText(getString(R.string.tips_restart));
                }
                if (camera.isSystemState == 2 && camera.getConnectState() == HiCamera.CAMERA_CONNECTION_STATE_LOGIN) {
                    holder.txt_state.setText(getString(R.string.tips_recovery));
                }
                if (camera.isSystemState == 3 && camera.getConnectState() == HiCamera.CAMERA_CONNECTION_STATE_LOGIN) {
                    holder.txt_state.setText(getString(R.string.tips_update));
                }
                holder.setting.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (mListener != null) {
                            mListener.onButtonClick(R.id.setting_camera_item, camera);
                        }
                    }
                });

                holder.delete_icon.setOnClickListener(new OnClickListener() {

                    @Override
                    public void onClick(View arg0) {
                        if (mListener != null) {
                            mListener.onButtonClick(R.id.delete_icon_camera_item, camera);
                        }

                    }
                });
            }

            return convertView;
        }

        public class ViewHolder {
            public TextView txt_nikename;
            public TextView txt_uid;
            public TextView txt_state;

            public ImageView setting;
            public ImageView delete_icon;

        }

    }

    @Override
    public void receiveIOCtrlData(HiCamera arg0, int arg1, byte[] arg2, int arg3) {
        if (arg1 == HiChipDefines.HI_P2P_GET_SNAP && arg3 == 0) {
            MyCamera camera = (MyCamera) arg0;
            if (!camera.reciveBmpBuffer(arg2)) {
                return;
            }
        }
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

        if (HiDataValue.isDebug)
            HiLog.v("uid:" + arg0.getUid() + "  state:" + arg1);

        Message msg = handler.obtainMessage();
        msg.what = HiDataValue.HANDLE_MESSAGE_SESSION_STATE;
        msg.arg1 = arg1;
        msg.obj = arg0;
        handler.sendMessage(msg);

    }

    private long startTime = 0;
    private long endTime = 0;

    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            MyCamera camera = (MyCamera) msg.obj;
            switch (msg.what) {
                case HiDataValue.HANDLE_MESSAGE_SESSION_STATE:
                    if (adapter != null)
                        adapter.notifyDataSetChanged();
                    switch (msg.arg1) {
                        case HiCamera.CAMERA_CONNECTION_STATE_DISCONNECTED:
                            break;
                        case HiCamera.CAMERA_CONNECTION_STATE_LOGIN:
                            startTime = System.currentTimeMillis();
                            camera.isSystemState = 0;
                            setTime(camera);
                            if (camera.getPushState() > 0) {
                                Log.i("tedu", "--XGToken-->" + HiDataValue.XGToken);
                                Log.i("tedu", "--setServer-->");
                                camera.bindPushState(true, bindPushResult);
                                setServer(camera);
                            }
                            if (!camera.getCommandFunction(HiChipDefines.HI_P2P_PB_QUERY_START_NODST)) {
                                if (camera.getCommandFunction(HiChipDefines.HI_P2P_GET_TIME_ZONE_EXT)) {
                                    camera.sendIOCtrl(HiChipDefines.HI_P2P_GET_TIME_ZONE_EXT, new byte[0]);
                                } else {
                                    camera.sendIOCtrl(HiChipDefines.HI_P2P_GET_TIME_ZONE, new byte[0]);
                                }
                            }
                            break;
                        case HiCamera.CAMERA_CONNECTION_STATE_WRONG_PASSWORD:
                            break;
                        case HiCamera.CAMERA_CONNECTION_STATE_CONNECTING:
                            break;
                    }
                    break;
                case HiDataValue.HANDLE_MESSAGE_RECEIVE_IOCTRL:
                    if (msg.arg2 == 0) {
                        handIOCTRLSucce(msg, camera);
                    }
                    break;

                case HiDataValue.HANDLE_MESSAGE_DELETE_FILE:
                    camera.disconnect();
                    camera.deleteInCameraList();
                    camera.deleteInDatabase(getActivity());
                    adapter.notifyDataSetChanged();
                    Log.d("charming", getString(R.string.tips_remove_success));
                    break;
            }
        }

        private void handIOCTRLSucce(Message msg, MyCamera camera) {
            Bundle bundle = msg.getData();
            byte[] data = bundle.getByteArray(HiDataValue.EXTRAS_KEY_DATA);
            switch (msg.arg1) {
                case HiChipDefines.HI_P2P_GET_SNAP:
                    adapter.notifyDataSetChanged();
                    if (camera.snapshot != null) {
                        File rootFolder = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/");
                        File sargetFolder = new File(rootFolder.getAbsolutePath() + "/android/data/" + getActivity().getResources().getString(R.string.app_name));

                        if (!rootFolder.exists()) {
                            rootFolder.mkdirs();
                        }
                        if (!sargetFolder.exists()) {
                            sargetFolder.mkdirs();
                        }
                    }
                    break;

                case HiChipDefines.HI_P2P_GET_TIME_ZONE: {

                    HiChipDefines.HI_P2P_S_TIME_ZONE timezone = new HiChipDefines.HI_P2P_S_TIME_ZONE(data);

                    if (timezone.u32DstMode == 1) {
                        camera.setSummerTimer(true);
                    } else {
                        camera.setSummerTimer(false);
                    }

                }
                break;
                case HiChipDefines.HI_P2P_GET_TIME_ZONE_EXT: {
                    HiChipDefines.HI_P2P_S_TIME_ZONE_EXT timezone = new HiChipDefines.HI_P2P_S_TIME_ZONE_EXT(data);
                    if (timezone.u32DstMode == 1) {
                        camera.setSummerTimer(true);
                    } else {
                        camera.setSummerTimer(false);
                    }
                    break;
                }
                case CamHiDefines.HI_P2P_ALARM_TOKEN_REGIST:

                    break;
                case CamHiDefines.HI_P2P_ALARM_TOKEN_UNREGIST:
                    break;
                case CamHiDefines.HI_P2P_ALARM_ADDRESS_SET:
                    Log.i("tedu", "---::::-�ɹ��Ļص���->");
                    break;
                case CamHiDefines.HI_P2P_ALARM_ADDRESS_GET:
                    break;
            }
        }
    };

    protected void setServer(MyCamera mCamera) {
        if (!mCamera.getCommandFunction(CamHiDefines.HI_P2P_ALARM_ADDRESS_SET)) {
            return;
        }
        // ������ݿⱣ��Ļ����ϵ�ַ�ͽ�󲢰��µĵ�ַ
        if (mCamera.getServerData() != null && !mCamera.getServerData().equals(HiDataValue.CAMERA_ALARM_ADDRESS)) {
            if (mCamera.getPushState() > 1) {
                if (HiDataValue.XGToken == null) {
                    if (HiDataValue.ANDROID_VERSION >= 6) {
                        if (!HiTools.checkPermission(getActivity(), Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                            ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 0);
                        }
                    }

                    HiDataValue.XGToken = XGPushConfig.getToken(getActivity());
                }
                mCamera.bindPushState(false, bindPushResult);
                return;
            }
        }
        // Log.i("tedu", "---start--->");
        // sendServer(mCamera);
        // Log.i("tedu", "---end--->");
        // sendRegisterToken(mCamera);

    }

    protected void sendServer(MyCamera mCamera) {
        // //����
        // mCamera.sendIOCtrl(CamHiDefines.HI_P2P_ALARM_ADDRESS_GET, null);
        Log.i("tedu", "--mCamera.getServerData()-->" + mCamera.getServerData());
        if (mCamera.getServerData() == null) {
            mCamera.setServerData(HiDataValue.CAMERA_ALARM_ADDRESS);
            mCamera.updateServerInDatabase(getActivity());
        }
        if (!mCamera.getCommandFunction(CamHiDefines.HI_P2P_ALARM_ADDRESS_SET)) {
            return;
        }
        Log.i("tedu", "--mCamera.push-->" + mCamera.push);
        if (mCamera.push != null) {
            String[] strs = mCamera.push.getPushServer().split("\\.");
            if (strs.length == 4 && isInteger(strs[0]) && isInteger(strs[1]) && isInteger(strs[2]) && isInteger(strs[3])) {
                byte[] info = HI_P2P_ALARM_ADDRESS.parseContent(mCamera.push.getPushServer());
                mCamera.sendIOCtrl(CamHiDefines.HI_P2P_ALARM_ADDRESS_SET, info);
                Log.i("tedu", "--:::-->" + mCamera.push.getPushServer());
            }

        }
    }

	/*
     * �Ƽ����ٶ���� �ж��Ƿ�Ϊ����
	 * 
	 * @param str ������ַ���
	 * 
	 * @return ����������true,���򷵻�false
	 */

    public static boolean isInteger(String str) {
        Pattern pattern = Pattern.compile("^[-\\+]?[\\d]*$");
        return pattern.matcher(str).matches();
    }

    private void setTime(MyCamera camera) {
        Calendar cal = Calendar.getInstance(TimeZone.getDefault());
        cal.setTimeInMillis(System.currentTimeMillis());

        byte[] time = HiChipDefines.HI_P2P_S_TIME_PARAM.parseContent(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH), cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), cal.get(Calendar.SECOND));

        camera.sendIOCtrl(HiChipDefines.HI_P2P_SET_TIME_PARAM, time);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        try {
            getActivity().unregisterReceiver(receiver);
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (connectThread != null) {
            connectThread.interrupt();
        }

    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        final MyCamera selectedCamera = HiDataValue.CameraList.get(position);

        if (selectedCamera.getConnectState() == HiCamera.CAMERA_CONNECTION_STATE_DISCONNECTED || selectedCamera.getConnectState() == HiCamera.CAMERA_CONNECTION_STATE_WRONG_PASSWORD) {
            if (HiDataValue.ANDROID_VERSION >= 6 && !HiTools.checkPermission(getActivity(), Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                showAlertDialog();
                return;
            }
            selectedCamera.connect();
            adapter.notifyDataSetChanged();
        } else {
            Log.d("charming", getString(R.string.click_offline_setting));
            return;
        }


    }

    private void showAlertDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage(getString(R.string.tips_no_permission));
        builder.setPositiveButton(getString(R.string.setting), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Intent intent = new Intent();
                intent.setAction("android.intent.action.MAIN");
                intent.setClassName("com.android.settings", "com.android.settings.ManageApplications");
                startActivity(intent);
            }
        });
        builder.setNegativeButton(getString(R.string.cancel), null);
        builder.setCancelable(false);
        builder.show();

    }

}

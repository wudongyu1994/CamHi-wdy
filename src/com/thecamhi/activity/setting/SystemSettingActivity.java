package com.thecamhi.activity.setting;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.FragmentActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

import com.hichip.R;
import com.hichip.base.HiLog;
import com.hichip.callback.ICameraIOSessionCallback;
import com.hichip.content.HiChipDefines;
import com.hichip.control.HiCamera;
import com.hichip.data.HiDeviceInfo;
import com.hichip.tools.Packet;
import com.thecamhi.base.HiTools;
import com.thecamhi.bean.HiDataValue;
import com.thecamhi.bean.MyCamera;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SystemSettingActivity extends FragmentActivity implements ICameraIOSessionCallback {
    private MyCamera mCamera;
    private static final int UPDATA_STATE_NONE = 0;
    private static final int UPDATA_STATE_CHECKING = 1;
    private static final int GET_UPDATE_VERSION_NUM = 0X9999;
    private static final int GET_UPDATE_VERSION_DATA = 0X10000;

    private int updateStatus = UPDATA_STATE_NONE;

    private static long updateTime = 0;

    private String redirectAddr = null;
    private boolean send = false;
    private Button upgrade_online_settings_btn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_system_setting);

        String uid = getIntent().getStringExtra(HiDataValue.EXTRAS_KEY_UID);
        for (MyCamera camera : HiDataValue.CameraList) {
            if (uid.equals(camera.getUid())) {
                mCamera = camera;

                break;

            }
        }
        if (mCamera == null) {
            Log.d("charming", "disconnect");
            finish();
        }
        initView();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mCamera.registerIOSessionListener(this);
    }

    private void initView() {


        Button restart_camera_btn = (Button) findViewById(R.id.restart_camera_btn);
        restart_camera_btn.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                mCamera.sendIOCtrl(HiChipDefines.HI_P2P_SET_REBOOT, new byte[0]);
            }
        });

        Button restore_factory_settings_btn = (Button) findViewById(R.id.restore_factory_settings_btn);
        restore_factory_settings_btn.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                mCamera.sendIOCtrl(HiChipDefines.HI_P2P_SET_RESET, new byte[0]);

            }
        });

        upgrade_online_settings_btn = (Button) findViewById(R.id.upgrade_online_settings_btn);
        if (mCamera.getCommandFunction(HiChipDefines.HI_P2P_GET_DEV_INFO_EXT)) {
            mCamera.sendIOCtrl(HiChipDefines.HI_P2P_GET_DEV_INFO_EXT, new byte[0]);
        } else {
            mCamera.sendIOCtrl(HiChipDefines.HI_P2P_GET_DEV_INFO, new byte[0]);
            mCamera.sendIOCtrl(HiChipDefines.HI_P2P_GET_VENDOR_INFO, new byte[0]);
        }
    }

    //// string=V11.1.8.1.4-20170707
    private void handVerionAndView(String string) {
        String[] strings = string.split("\\.");
        if (strings.length >= 2) {
            if (mCamera.getChipVersion() == HiDeviceInfo.CHIP_VERSION_GOKE) {
                upgrade_online_settings_btn.setVisibility(View.VISIBLE);
            } else if (mCamera.getChipVersion() == HiDeviceInfo.CHIP_VERSION_HISI && strings[0].equals("V11") && strings[1].equals("1") && strings[2].equals("8")) {
                upgrade_online_settings_btn.setVisibility(View.VISIBLE);
            } else {
                upgrade_online_settings_btn.setVisibility(View.GONE);
            }
            upgrade_online_settings_btn.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (updateStatus == UPDATA_STATE_NONE) {
                        showProgressDialog();
                        checkUpdate();
                    }

                }
            });
        }
    }

    private static String downloadUrl = "http://47.91.141.20/goke_update.html";// 锟斤拷锟斤拷锟斤拷锟斤拷址
    // private static String downloadUrl =
    // "http://58.64.153.34/V7.1.4.1.11.exe";//锟斤拷锟斤拷锟斤拷锟斤拷址

    // "http://58.64.153.34/V7.1.4.1.11.exe"

    private UpdateInfo updateInfo;

    private class UpdateInfo {
        String url;
        String ver;

        public UpdateInfo(String u, String v) {
            url = u;
            ver = v;
        }
    }

    private List<UpdateInfo> mListUpdataInfo = new ArrayList<UpdateInfo>();

    private class ThreadHttpResqust extends Thread {
        public ThreadHttpResqust() {

        }

        public void run() {

            String url = downloadUrl;

            // String urldown = null;
            // String ver = null;
            HttpResponse httpResponse = null;
            try {

                HttpGet httpGet = new HttpGet(url);
                httpResponse = new DefaultHttpClient().execute(httpGet);

                if (httpResponse.getStatusLine().getStatusCode() == 200) {

                    String result = EntityUtils.toString(httpResponse.getEntity());
                    HiLog.v("result:" + result);
                    try {
                        JSONObject resultJson = new JSONObject(result);
                        JSONArray listArray = resultJson.getJSONArray("list");
                        for (int i = 0; i < listArray.length(); i++) {
                            JSONObject jsonObj = listArray.getJSONObject(i);
                            String u = jsonObj.getString("url");
                            String v = jsonObj.getString("ver");
                            UpdateInfo updateInfo = new UpdateInfo(u, v);
                            mListUpdataInfo.add(updateInfo);

                            HiLog.v("url:" + u + "     ver:" + v);

                        }

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    if (mListUpdataInfo.size() > 0) {
                        Message msg = new Message();
                        msg.what = GET_UPDATE_VERSION_NUM;
                        msg.arg1 = 1;
                        // msg.obj = new UpdateInfo(urldown, ver);
                        handler.sendMessage(msg);
                        return;
                    }

                }

                HiLog.v("result: failed");
                Message msg = new Message();
                msg.what = GET_UPDATE_VERSION_NUM;

                msg.arg1 = 0;
                handler.sendMessage(msg);
            } catch (Exception e) {

                HiLog.v("Exception: " + e);
                Message msg = new Message();
                msg.what = GET_UPDATE_VERSION_NUM;

                msg.arg1 = 0;
                handler.sendMessage(msg);
            }
            /** HiCam鏈�鍚庣増鏈� **/

        }
    }

    // private String host = "58.64.153.34";
    // private String get = "/V7.1.4.1.11.exe";

    private class ThreadCheckRedirect extends Thread {
        public void run() {

            Socket socket = null;
            try {
                // InetAddress address = new InetAddress();
                // InetAddress addr = InetAddress.getByName("www.baidu.com");

                // socket = new Socket(addr,80);

                if (updateInfo == null) {

                }

                System.out.println("updateInfo:" + updateInfo);
                System.out.println("updateInfo.url:" + updateInfo.url);

                String host = null;
                int port = 80;
                socket = new Socket(); // 姝ゆ椂Socket瀵硅薄鏈粦瀹氭湰鍦扮鍙�,骞朵笖鏈繛鎺ヨ繙绋嬫湇鍔″櫒
                socket.setReuseAddress(true);

                // Pattern p = Pattern.compile("(?<=//|)((\\w)+\\.)+\\w+");
                // Matcher matcher = p.matcher(updateInfo.url);
                // if (matcher.find()) {
                // host = matcher.group();
                // System.out.println("host:"+host);
                // }
                //

                String host_temp = null;
                Pattern p2 = Pattern.compile("(?<=//|)((\\w)+\\.)+\\w+(:\\d{0,5})?");
                Matcher matcher2 = p2.matcher(updateInfo.url);
                if (matcher2.find()) {
                    host_temp = matcher2.group();
                    System.out.println("host2:" + host_temp);
                }

                // if(host_temp == null)
                // return;

                // 濡傛灉
                if (host_temp.contains(":") == false) {
                    host = host_temp;
                } else {

                    String[] ipPortArr = host_temp.split(":");
                    host = ipPortArr[0];

                    port = Integer.parseInt(ipPortArr[1]);
                    System.out.println("---00port:      " + ipPortArr[0]);
                    System.out.println("---port:    " + port);
                }

                SocketAddress remoteAddr = new InetSocketAddress(host, port);
                socket.connect(remoteAddr);

                // 鍚戞湇鍔″櫒绔涓�娆″彂閫佸瓧绗︿覆
                DataOutputStream doc = new DataOutputStream(socket.getOutputStream());

                // 鍚戞湇鍔″櫒绔浜屾鍙戦�佸瓧绗︿覆

                String sendhead = "GET /" + updateInfo.ver + ".exe HTTP/1.1\r\n";
                sendhead += "Accept: */*\r\n";
                sendhead += "Accept-Language: zh-cn\r\n";
                sendhead += "User-Agent: Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1; SV1)\r\n";
                sendhead += "Host: " + host + "\r\n";
                sendhead += "Connection: Keep-Alive\r\n";
                sendhead += "\r\n";

                System.out.println("sendhead:" + sendhead);
                doc.write(sendhead.getBytes());

                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                while (true) {
                    String str = in.readLine();
                    System.out.println("in.readLine:" + str);

                    if (str.contains("HTTP/")) {
                        // if (str.contains("200 OK")) {
                        // break;
                        // }
                        if (str.contains("302") || str.contains("301")) {
                            System.out.println("------------ 301 302 Found----------------");
                        } else {
                            break;
                        }
                    }

                    if (str.contains("Location:")) {

                        String newhttp = str.substring(9).trim();
                        System.out.println("1------------ newhttp----------------:" + newhttp);

                        redirectAddr = str.substring(9).trim();
                        break;
                    }
                }

                doc.close();
                in.close();
            } catch (UnknownHostException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (socket != null) {
                    try {
                        socket.close();
                    } catch (IOException e) {
                    }
                }

                Message msg = new Message();
                msg.what = GET_UPDATE_VERSION_DATA;
                msg.arg1 = 1;
                handler.sendMessage(msg);

            }

        }
    }

    private void checkUpdate() {

        new ThreadHttpResqust().start();

        updateStatus = UPDATA_STATE_CHECKING;
    }

    boolean isUpdate = false;

    private Handler handler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case HiChipDefines.HI_P2P_GET_DEV_INFO_EXT:
                    if (msg.arg2 == 0) {
                        byte[] data = msg.getData().getByteArray(HiDataValue.EXTRAS_KEY_DATA);
                        HiChipDefines.HI_P2P_GET_DEV_INFO_EXT deviceInfo = new HiChipDefines.HI_P2P_GET_DEV_INFO_EXT(data);
                        String version = Packet.getString(deviceInfo.aszSystemSoftVersion);
                        if (!TextUtils.isEmpty(version)) {
                            handVerionAndView(version);
                        }
                    }
                    break;
                case HiChipDefines.HI_P2P_GET_DEV_INFO:
                    if (msg.arg2 == 0) {
                        byte[] data = msg.getData().getByteArray(HiDataValue.EXTRAS_KEY_DATA);
                        HiChipDefines.HI_P2P_S_DEV_INFO info = new HiChipDefines.HI_P2P_S_DEV_INFO(data);
                        String version = Packet.getString(info.strSoftVer);
                        if (!TextUtils.isEmpty(version)) {
                            handVerionAndView(version);
                        }

                    }
                    break;

                case HiChipDefines.HI_P2P_SET_REBOOT:
                    mCamera.isSystemState = 1;
                    break;
                case HiChipDefines.HI_P2P_SET_RESET:// 出厂设置
                    mCamera.isSystemState = 2;
                    handler.postDelayed(new Runnable() {

                        @Override
                        public void run() {
                            SystemSettingActivity.this.finish();
                            AliveSettingActivity.mActivity.finish();

                        }
                    }, 5000);
                    break;
                case HiDataValue.HANDLE_MESSAGE_SESSION_STATE:
                    switch (msg.arg1) {
                        case HiCamera.CAMERA_CONNECTION_STATE_LOGIN:
                            mCamera.isSystemState = 0;
                            SystemSettingActivity.this.finish();
                            AliveSettingActivity.mActivity.finish();
                            break;
                    }
                    break;
                case GET_UPDATE_VERSION_NUM:
                    if (msg.arg1 == 1) {
                        updateInfo = (UpdateInfo) msg.obj;
                        send = true;
                        if (mCamera != null) {
                            mCamera.registerIOSessionListener(SystemSettingActivity.this);
                        }
                        // mCamera.sendIOCtrl(HiChipDefines.HI_P2P_GET_DEV_INFO_EXT,
                        // new byte[0]);
                        // String version=new
                        // String(mCamera.getDeciveInfo().aszSystemSoftVersion);
                        handleDeviceInfo();

                    } else {
                        updateStatus = UPDATA_STATE_NONE;
                        Log.d("charming", "update failed");
                        dismissProgressDialog();

                    }

                    break;

                case GET_UPDATE_VERSION_DATA:

                    // dismissLoadingProgress();
                    updateStatus = UPDATA_STATE_NONE;
                    showDialogOld();

                    break;

                case HiChipDefines.HI_P2P_SET_DOWNLOAD:
                    mCamera.isSystemState = 3;
                    break;

            }

        }

        private void handleDeviceInfo() {
            // Bundle bundle = msg.getData();
            // byte[] data = bundle.getByteArray(HiDataValue.EXTRAS_KEY_DATA);
            // byte[] bVersion = new byte[HiChipDefines.HI_P2P_MAX_VERLENGTH];
            // System.arraycopy(data, 4+32+32+4, bVersion, 0,
            // HiChipDefines.HI_P2P_MAX_VERLENGTH);
            String version = getString(mCamera.getDeciveInfo().aszSystemSoftVersion);

            for (UpdateInfo upinfo : mListUpdataInfo) {

                HiLog.v("version:" + version);
                String[] b_new = upinfo.ver.split("\\.");
                String[] b_old = version.split("\\.");

                if ((b_new.length == b_old.length) && b_old.length == 5) {
                    for (int i = 0; i < b_new.length; i++) {

                        if (i == b_new.length - 1) {

                            String[] last_new_array = b_new[i].split("-");
                            String[] last_old_array = b_old[i].split("-");

                            HiLog.v("last_new_array:" + last_new_array.length + "  last_old_array:" + last_old_array.length);
                            int newi = 0;
                            if (last_new_array.length >= 1) {
                                newi = Integer.parseInt(last_new_array[0]);
                            }
                            // string to int erro

                            int oldi = 0;
                            if (last_old_array.length >= 1) {
                                oldi = Integer.parseInt(last_old_array[0]);
                            }

                            HiLog.v("newi:" + newi + "  oldi:" + oldi);
                            if (newi > oldi) {
                                updateInfo = upinfo;
                                isUpdate = true;
                            }
                        } else {
                            if (!b_old[i].equals(b_new[i])) {
                                HiLog.v(i + "b_new:" + b_new[i]);
                                HiLog.v(i + "b_old:" + b_old[i]);
                                isUpdate = false;
                                break;
                            }
                        }
                    }
                }
                if (isUpdate) {
                    break;
                }
            }
            if (isUpdate) {

                HiLog.v("ThreadCheckRedirect  start");
                if (updateInfo == null)
                    return;

                redirectAddr = updateInfo.url + updateInfo.ver + ".exe";
                ThreadCheckRedirect tr = new ThreadCheckRedirect();
                tr.start();
            } else {
                showDialogNew();
                updateStatus = UPDATA_STATE_NONE;
            }
        }

    };

    private ProgressDialog progressDialog;

    private void dismissProgressDialog() {
        if (progressDialog != null) {
            progressDialog.cancel();
        }
        progressDialog = null;
    }

    private void showProgressDialog() {

        progressDialog = new ProgressDialog(SystemSettingActivity.this);
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progressDialog.setCancelable(true);
        progressDialog.setCanceledOnTouchOutside(false);
        progressDialog.setIcon(R.drawable.ic_launcher);
        progressDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {

            @Override
            public void onDismiss(DialogInterface dialog) {

            }
        });
        progressDialog.setOnKeyListener(new DialogInterface.OnKeyListener() {

            @Override
            public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                return false;
            }
        });
        progressDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {

            @Override
            public void onCancel(DialogInterface dialog) {
            }
        });
        progressDialog.setMessage("加载中");
        progressDialog.show();
    }

    private void showDialogNew() {
        dismissProgressDialog();
        send = false;
        if (mCamera != null) {
            mCamera.unregisterIOSessionListener(this);
        }
        new AlertDialog.Builder(SystemSettingActivity.this).setIcon(android.R.drawable.ic_dialog_alert).setTitle("警告").setMessage("已是最新版本，不需要更新").setPositiveButton(getText(R.string.btn_ok), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

            }
        }).show();
    }

    private void showDialogOld() {
        dismissProgressDialog();
        new AlertDialog.Builder(SystemSettingActivity.this).setIcon(android.R.drawable.ic_dialog_alert).setTitle("警告").setMessage("检测到有新版本，立即更新？").setPositiveButton(getText(R.string.btn_ok), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

                if (System.currentTimeMillis() - updateTime <= 180000)
                    return;

                // String dl = updateInfo.url+updateInfo.ver+".exe";
                String dl = redirectAddr;
                byte[] byt = new byte[128];
                byte[] bSvr = dl.getBytes();
                // Arrays.fill(byt, (byte)0);
                int len = bSvr.length > 128 ? 128 : bSvr.length;
                System.arraycopy(bSvr, 0, byt, 0, len);
                HiLog.v("dl:" + dl);
                updateTime = System.currentTimeMillis();
                send = true;
                if (mCamera != null) {
                    mCamera.registerIOSessionListener(SystemSettingActivity.this);
                }

                mCamera.sendIOCtrl(HiChipDefines.HI_P2P_SET_DOWNLOAD, HiChipDefines.HI_P2P_S_SET_DOWNLOAD.parseContent(0, byt));

                new AlertDialog.Builder(SystemSettingActivity.this).setIcon(android.R.drawable.ic_dialog_alert).setTitle("警告").setMessage("正在更新，请注意不要关闭机器").setPositiveButton(getText(R.string.btn_ok), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                }).show();

            }
        }).setNegativeButton(getText(R.string.cancel), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

            }
        }).show();
    }

    private static String getString(byte[] data) {

        StringBuilder sBuilder = new StringBuilder();

        for (int i = 0; i < data.length; i++) {

            if (data[i] == 0x0)
                break;

            sBuilder.append((char) data[i]);
        }

        return sBuilder.toString();
    }

    @Override
    public void receiveIOCtrlData(HiCamera arg0, int arg1, byte[] arg2, int arg3) {
        if (arg0 != mCamera)
            return;

        Bundle bundle = new Bundle();
        bundle.putByteArray(HiDataValue.EXTRAS_KEY_DATA, arg2);
        Message msg = handler.obtainMessage();
        msg.what = arg1;
        msg.obj = arg0;
        msg.arg1 = arg1;
        msg.arg2 = arg3;
        msg.setData(bundle);
        handler.sendMessage(msg);

    }

    @Override
    public void receiveSessionState(HiCamera arg0, int arg1) {
        if (arg0 != mCamera) {
            return;
        }
        Message message = Message.obtain();
        message.what = HiDataValue.HANDLE_MESSAGE_SESSION_STATE;
        message.arg1 = arg1;
        handler.sendMessage(message);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (send) {
            send = false;
            if (mCamera != null) {
                mCamera.unregisterIOSessionListener(this);
            }
        }
    }

}

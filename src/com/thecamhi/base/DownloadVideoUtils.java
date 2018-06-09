package com.thecamhi.base;

import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.widget.TextView;

import com.hichip.R;
import com.hichip.callback.ICameraDownloadCallback;
import com.hichip.callback.ICameraIOSessionCallback;
import com.hichip.content.HiChipDefines;
import com.hichip.control.HiCamera;
import com.hichip.tools.Packet;
import com.thecamhi.base.HiTools;
import com.thecamhi.bean.HiDataValue;
import com.thecamhi.bean.MyCamera;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;

/**
 * Created by cm on 2018/6/5.
 */

public class DownloadVideoUtils implements ICameraIOSessionCallback, ICameraDownloadCallback {

    public final static int HANDLE_MESSAGE_NETWORK_CHANGED = 0x100001;
    private MyCamera mCamera;


    private List<HiChipDefines.HI_P2P_FILE_INFO> file_list = Collections.synchronizedList(new ArrayList<HiChipDefines.HI_P2P_FILE_INFO>());

    // 回调的下载文件路径
    private String path;
    private boolean isDownloading = false;

    public DownloadVideoUtils(String uid) {
        for (MyCamera camera : HiDataValue.CameraList) {
            if (camera.getUid().equals(uid)) {
                mCamera = camera;
                break;
            }
        }

        registerCompent();
        if (mCamera == null) {
            return;
        } else {
            if (mCamera.getConnectState() != HiCamera.CAMERA_CONNECTION_STATE_LOGIN)
                return;
        }
    }

    private void registerCompent() {
        if (mCamera != null) {
            mCamera.registerIOSessionListener(this);
            mCamera.registerDownloadListener(this);
        }
    }

    public void downloadVideo(long startTime, long endTime) {
        Log.d("charming", "downloadVideo is called");
        file_list.clear();
        if (mCamera != null) {
            String timeStr = HiTools.sdfTimeSec(startTime) + " - " + HiTools.sdfTimeSec(endTime);
            Log.d("charming", "downloadVideo command send : " + timeStr);
            if (mCamera.getCommandFunction(HiChipDefines.HI_P2P_PB_QUERY_START_NODST)) {
                mCamera.sendIOCtrl(HiChipDefines.HI_P2P_PB_QUERY_START_NODST, HiChipDefines.HI_P2P_S_PB_LIST_REQ.parseContent(0, startTime, endTime, HiChipDefines.HI_P2P_EVENT_ALL));
            } else {
                mCamera.sendIOCtrl(HiChipDefines.HI_P2P_PB_QUERY_START, HiChipDefines.HI_P2P_S_PB_LIST_REQ.parseContent(0, startTime, endTime, HiChipDefines.HI_P2P_EVENT_ALL));
            }
            mCamera.registerIOSessionListener(this);
        }


    }


    @Override
    public void receiveIOCtrlData(HiCamera arg0, int arg1, byte[] arg2, int arg3) {

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
    public void callbackDownloadState(HiCamera camera, int total, int curSize, int state, String path) {
        if (camera != mCamera)
            return;
        Bundle bundle = new Bundle();
        bundle.putLong("total", total);
        bundle.putLong("curSize", curSize);
        bundle.putString("path", path);

        Message msg = handler.obtainMessage();
        msg.what = HiDataValue.HANDLE_MESSAGE_DOWNLOAD_STATE;
        msg.arg1 = state;
        msg.setData(bundle);
        handler.sendMessage(msg);

    }

    @Override
    public void receiveSessionState(HiCamera arg0, int arg1) {
        Message msg = handler.obtainMessage();
        msg.what = HiDataValue.HANDLE_MESSAGE_SESSION_STATE;
        msg.arg1 = arg1;
        msg.obj = arg0;
        handler.sendMessage(msg);
    }

    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case HANDLE_MESSAGE_NETWORK_CHANGED:
                    handNETWORK_CHANGED();
                    break;
                case HiDataValue.HANDLE_MESSAGE_SESSION_STATE:
                    switch (msg.arg1) {
                        case HiCamera.CAMERA_CONNECTION_STATE_DISCONNECTED:
                            if (isDownloading) {
                                if (!TextUtils.isEmpty(path)) {
                                    File file = new File(path);
                                    if (file != null && file.isFile() && file.exists()) {
                                        file.delete();
                                    }
                                }
                            }
                            Log.d("charming", "disconnect");
                            break;
                    }
                    break;
                case HiDataValue.HANDLE_MESSAGE_RECEIVE_IOCTRL:
                    Log.d("charming", "RECEIVE IOCTRL" + msg);
                    if (msg.arg2 == -1) {// IO的错误码
                        Log.d("charming", "connect to server error");
                        return;
                    }
                    if (msg.arg2 == 0) {
                        Bundle bundle = msg.getData();
                        byte[] data = bundle.getByteArray(HiDataValue.EXTRAS_KEY_DATA);
                        switch (msg.arg1) {
                            case HiChipDefines.HI_P2P_START_REC_UPLOAD_EXT:// 下载
                                break;
                            case HiChipDefines.HI_P2P_PB_QUERY_START_NODST:
                            case HiChipDefines.HI_P2P_PB_QUERY_START:
                                if (data.length >= 12) {
                                    byte flag = data[8];// 数据发送的结束标识符
                                    int cnt = data[9]; // 当前包的文件个数
                                    if (cnt > 0) {
                                        for (int i = 0; i < cnt; i++) {
                                            int pos = 12;
                                            int size = HiChipDefines.HI_P2P_FILE_INFO.sizeof();
                                            byte[] t = new byte[24];
                                            System.arraycopy(data, i * size + pos, t, 0, 24);
                                            HiChipDefines.HI_P2P_FILE_INFO file_info = new HiChipDefines.HI_P2P_FILE_INFO(t);
                                            long duration = file_info.sEndTime.getTimeInMillis() - file_info.sStartTime.getTimeInMillis();
                                            if (duration <= 1000 * 1000 && duration > 0) { // 1000秒，文件录像一般为15分钟，但是有可能会长一点所有就设置为1000
                                                Log.d("charming", file_info.sStartTime.toString() + " - " + file_info.sEndTime.toString());
                                                file_list.add(file_info);
                                            }
                                        }
                                    }

                                }
                                checkDownloadList();
                                break;
                        }
                    }
                    break;
                case HiDataValue.HANDLE_MESSAGE_DOWNLOAD_STATE:
                    handDownLoad(msg);
                    break;
            }
        }

        private void handDownLoad(Message msg) {
            Bundle bundle = msg.getData();
            switch (msg.arg1) {
                case DOWNLOAD_STATE_START:
                    isDownloading = true;
                    path = bundle.getString("path");
                    Log.d("charming", "download start " + path);
                    break;
                case DOWNLOAD_STATE_DOWNLOADING:
                    if (isDownloading == false) {
                        return;
                    }
                    float d;
                    long total = bundle.getLong("total");
                    if (total == 0) {
                        d = bundle.getLong("curSize") * 100 / (1024 * 1024);
                    } else {
                        d = bundle.getLong("curSize") * 100 / total;
                    }
                    if (d >= 100) {
                        d = 99;
                    }
                    int rate = (int) d;
                    String rateStr = "";
                    if (rate < 10) {
                        rateStr = " " + rate + "%";
                    } else {
                        rateStr = rate + "%";
                    }
                    Log.d("charming", "download start " + bundle.getString("path") + " : " + rateStr);
                    break;
                case DOWNLOAD_STATE_END:
                    isDownloading = false;
                    Log.d("charming", "download finish" + bundle.getString("path"));
                    checkDownloadList();
                    break;
                case DOWNLOAD_STATE_ERROR_PATH:
                    Log.d("charming", "download error path" + bundle.getString("path"));
                    break;
                case DOWNLOAD_STATE_ERROR_DATA:
                    if (mCamera != null && isDownloading) {
                        mCamera.stopDownloadRecording();
                        isDownloading = false;
                        mCamera.disconnect();
                        mCamera.connect();
                    }
                    break;

            }
        }

        private void handNETWORK_CHANGED() {
            if (!isDownloading) {
                return;
            }
            isDownloading = false;
            if (mCamera != null) {
                mCamera.stopDownloadRecording();
            }
            Log.d("charming", "network change");

        }
    };

    private void searchVideo() {

        long startTime = getBeforeHourTime(6);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        // String timeStr = HiTools.sdfTimeDay(System.currentTimeMillis()) + " -
        // "+ HiTools.sdfTimeSec(System.currentTimeMillis());
        // 改为默认搜索6个小时时间内的录像
        String timeStr = sdf.format(new java.util.Date(startTime)) + " - " + HiTools.sdfTimeSec(System.currentTimeMillis());
        /*
         * String timeStr= HiTools.sdfTimeDay(System.currentTimeMillis()) +" - "+ HiTools.sdfTimeSec(System.currentTimeMillis()); tv_search_duration.setText(timeStr);
		 */
        if (mCamera != null) {
            if (mCamera.getCommandFunction(HiChipDefines.HI_P2P_PB_QUERY_START_NODST)) {
                mCamera.sendIOCtrl(HiChipDefines.HI_P2P_PB_QUERY_START_NODST, HiChipDefines.HI_P2P_S_PB_LIST_REQ.parseContent(0, startTime, System.currentTimeMillis(), HiChipDefines.HI_P2P_EVENT_ALL));
            } else if (mCamera.getCommandFunction(HiChipDefines.HI_P2P_PB_QUERY_START_NEW)) {
                mCamera.sendIOCtrl(HiChipDefines.HI_P2P_PB_QUERY_START_NEW, HiChipDefines.HI_P2P_S_PB_LIST_REQ.parseContent(0, startTime, System.currentTimeMillis(), HiChipDefines.HI_P2P_EVENT_ALL));
            } else if (mCamera.getCommandFunction(HiChipDefines.HI_P2P_PB_QUERY_START_EXT)) {
                mCamera.sendIOCtrl(HiChipDefines.HI_P2P_PB_QUERY_START_EXT, HiChipDefines.HI_P2P_S_PB_LIST_REQ.parseContent(0, startTime, System.currentTimeMillis(), HiChipDefines.HI_P2P_EVENT_ALL));
            } else {
                mCamera.sendIOCtrl(HiChipDefines.HI_P2P_PB_QUERY_START, HiChipDefines.HI_P2P_S_PB_LIST_REQ.parseContent(0, startTime, System.currentTimeMillis(), HiChipDefines.HI_P2P_EVENT_ALL));
            }
        }
    }

    private void downloadRecording(final HiChipDefines.HI_P2P_FILE_INFO file_infos) {
        Log.d("charming", "downloadRecording is called");
        if (HiTools.isSDCardValid()) {

            File rootFolder = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/");
            File downloadFolder = new File(HiDataValue.ONLINE_VIDEO_PATH);
            File uidFolder = new File(downloadFolder.getAbsolutePath() + "/" + mCamera.getUid() + "");
            if (!rootFolder.exists()) {
                rootFolder.mkdirs();
            }
            if (!downloadFolder.exists()) {
                downloadFolder.mkdirs();
            }
            if (!uidFolder.exists()) {
                uidFolder.mkdirs();
            }

            final String download_path = uidFolder.getAbsoluteFile() + "/";

            // 创建UID文件夹
            final String fileName = splitFileName(file_infos.sStartTime.toString());
            File file = new File(download_path + fileName + ".avi");
            File file2 = new File(download_path + fileName + ".mp4");
            File file3 = new File(download_path + fileName + ".h264");

            if (file.exists() || file2.exists() || file3.exists()) {// 文件已下载过
                Log.d("charming", "file exists");
                checkDownloadList();
                return;
            }
            // //因为下载SDK加了耗时操作,所以放在要放在异步里处理
            new Thread() {
                public void run() {
                    // mCamera.startDownloadRecording(file_infos.sStartTime, download_path, fileName);
                    // 默认都下载264的文件(如果是文件本身是avi 则还是avi格式,如果是其他格式则变成264格式)
//                    byte[] timeBytes = parseContent(2018, 6, 1, 0, 12, 34, 56);
//                    HiChipDefines.STimeDay mytime = new HiChipDefines.STimeDay(timeBytes, 0);
                    Log.d("charming", "startDownloadRecording " + file_infos.sStartTime);
                    mCamera.startDownloadRecording2(file_infos.sStartTime, download_path, fileName, 2);
//                    mCamera.startDownloadRecording2(mytime, download_path, fileName, 2);
                }

                ;
            }.start();
            // mCamera.startDownloadRecording(file_infos.sStartTime, download_path, fileName);
        } else {
            Log.d("charming", "no sdcard");
        }
    }


    private void checkDownloadList() {
        if (file_list != null && file_list.size() > 0) {
            HiChipDefines.HI_P2P_FILE_INFO file_info = file_list.get(0);
            file_list.remove(0);
            downloadRecording(file_info);
        }
    }

    private String splitFileName(String str) {

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        long time = 0;
        try {
            time = sdf.parse(str).getTime();
        } catch (ParseException e) {

            e.printStackTrace();
        }

        SimpleDateFormat sf2 = new SimpleDateFormat("yyyyMMdd_HHmmss");

        return sf2.format(time);
    }


    /**
     * 获取当前时间制定一个小时之前
     */
    public long getBeforeHourTime(int hour) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, calendar.get(Calendar.HOUR_OF_DAY) - hour);
        return calendar.getTimeInMillis();

    }

    /**
     * 生成调用下载接口需要传入的时间数据格式
     */
    public static byte[] parseContent(int year, int month, int day, int wday, int hour, int minute, int second) {
        byte[] result = new byte[8];
        byte[] y = Packet.shortToByteArray_Little((short) year);
        System.arraycopy(y, 0, result, 0, 2);
        result[2] = (byte) month;
        result[3] = (byte) day;
        result[4] = (byte) hour;
        result[5] = (byte) minute;
        result[6] = (byte) second;
        result[7] = (byte) wday;
        return result;
    }
}

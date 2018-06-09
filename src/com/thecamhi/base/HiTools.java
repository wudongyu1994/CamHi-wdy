package com.thecamhi.base;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.w3c.dom.Text;

import com.hichip.R;
import com.thecamhi.bean.HiDataValue;
import com.thecamhi.bean.MyCamera;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.os.Environment;
import android.os.StatFs;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.text.format.Formatter;
import android.util.Log;
import android.view.View;

public class HiTools {

    public static int dip2px(Context context, float dpValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5f);
    }

    public static int px2dip(Context context, float pxValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (pxValue / scale + 0.5f);
    }

    /**
     * type=0,filename.jpg; type=1,filename.mp4; type=2,没有后缀名
     *
     * @param type
     * @return
     */
    public static String getFileNameWithTime(int type) {

        Calendar c = Calendar.getInstance();
        int mYear = c.get(Calendar.YEAR);
        int mMonth = c.get(Calendar.MONTH) + 1;
        int mDay = c.get(Calendar.DAY_OF_MONTH);
        int mHour = c.get(Calendar.HOUR_OF_DAY);
        int mMinute = c.get(Calendar.MINUTE);
        int mSec = c.get(Calendar.SECOND);
        // int mMilliSec = c.get(Calendar.MILLISECOND);

        StringBuffer sb = new StringBuffer();
        if (type == 0) {
            sb.append("IMG_");
        }
        sb.append(mYear);
        if (mMonth < 10)
            sb.append('0');
        sb.append(mMonth);
        if (mDay < 10)
            sb.append('0');
        sb.append(mDay);
        sb.append('_');
        if (mHour < 10)
            sb.append('0');
        sb.append(mHour);
        if (mMinute < 10)
            sb.append('0');
        sb.append(mMinute);
        if (mSec < 10)
            sb.append('0');
        sb.append(mSec);

        if (type == 0) {
            sb.append(".jpg");
        } else if (type == 1) {
            sb.append(".mp4");
        } else {

        }

        return sb.toString();
    }

    public static boolean isSDCardValid() {
        return Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED);
    }

    public static boolean saveImage(String fileName, Bitmap frame) {

        if (fileName == null || fileName.length() <= 0)
            return false;

        boolean bErr = false;
        FileOutputStream fos = null;

        try {

            fos = new FileOutputStream(fileName, false);
            frame.compress(Bitmap.CompressFormat.JPEG, 90, fos);
            fos.flush();
            fos.close();

        } catch (Exception e) {

            bErr = true;
            System.out.println("saveImage(.): " + e.getMessage());

        } finally {

            if (bErr) {

                if (fos != null) {
                    try {
                        fos.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                return false;
            }
        }

        return true;
    }

    public static String formetFileSize(long fileSize) {
        DecimalFormat df = new DecimalFormat("#.00");
        String fileSizeString = "";
        String wrongSize = "0B";
        if (fileSize == 0) {
            return wrongSize;
        }
        if (fileSize < 1024) {
            fileSizeString = df.format((double) fileSize) + "B";
        } else if (fileSize < 1048576) {
            fileSizeString = df.format((double) fileSize / 1024) + "KB";
        } else if (fileSize < 1073741824) {
            fileSizeString = df.format((double) fileSize / 1048576) + "MB";
        } else {
            fileSizeString = df.format((double) fileSize / 1073741824) + "GB";
        }
        return fileSizeString;
    }

    public static String sdfTimeSec(long timeLong) {
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String timeStr = df.format(timeLong);
        return timeStr;
    }

    public static String sdfTimeDay(long timeLong) {
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd 00:00:00");
        String timeStr = df.format(timeLong);
        return timeStr;
    }

    public static void saveBitmap(Bitmap bitmap, String fileName) {
        if (bitmap == null) {
            return;
        }
        File file = new File(fileName);
        if (file.exists()) {
            file.delete();
        }
        FileOutputStream out;
        try {
            out = new FileOutputStream(file);
            if (bitmap.compress(Bitmap.CompressFormat.PNG, 90, out)) {
                out.flush();
                out.close();
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static int getAndroidVersion() {

        char[] c = android.os.Build.VERSION.RELEASE.toCharArray();
        String str = null;
        if (c != null && c.length >= 1) {
            str = String.valueOf(c[0]);
        }

        if (str == null) {
            return 0;
        }

        return Integer.valueOf(str);
    }

    /**
     * 检查单个权限
     *
     * @param context
     * @param permission
     * @return true 有该权限
     */
    public static boolean checkPermission(Context context, String permission) {

        int checkCallPhonePermission = ContextCompat.checkSelfPermission(context, permission);
        if (checkCallPhonePermission == PackageManager.PERMISSION_GRANTED) {
            return true;
        }
        return false;
    }

    /**
     * 检查并征求用户当前app要用到所有的权限
     */
    public static void checkPermissionAll(Activity activity) {
        List<String> list = new ArrayList<String>();
        if (!HiTools.checkPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            list.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }
        if (!HiTools.checkPermission(activity, Manifest.permission.RECORD_AUDIO)) {
            list.add(Manifest.permission.RECORD_AUDIO);
        }
        if (!HiTools.checkPermission(activity, Manifest.permission.CAMERA)) {
            list.add(Manifest.permission.CAMERA);
        }
        if (list.size() > 0) {
            String[] permissions = new String[list.size()];
            for (int i = 0; i < list.size(); i++) {
                permissions[i] = list.get(i);
            }
            ActivityCompat.requestPermissions(activity, permissions, 0);
        }
    }

    /**
     * 得到SD可用内存
     **/
    @SuppressLint("NewApi")
    public static String getSdFree(Context context) {
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            StatFs fs = new StatFs(Environment.getExternalStorageDirectory().getPath());
            // Android API18之前：fs.getAvailableBlocks()*fs.getBlockSize()
            return Formatter.formatFileSize(context, (fs.getAvailableBytes()));
        }
        return "未装载";
    }

    // 获得机身可用内存
    public static String getRomAvailableSize(Context context) {
        File path = Environment.getDataDirectory();
        StatFs stat = new StatFs(path.getPath());
        long blockSize = stat.getBlockSize();
        long availableBlocks = stat.getAvailableBlocks();
        return Formatter.formatFileSize(context, blockSize * availableBlocks);
    }

    /**
     * 获取机身内存 返回long 字节数大小值(通用各种语言)
     * 推荐使用
     *
     * @param path
     * @return
     */
    @SuppressLint("NewApi")
    public static long getAvailableSize() {
        String path = Environment.getDataDirectory().getPath();
        StatFs fileStats = new StatFs(path);
        fileStats.restat(path);
        long len = (long) fileStats.getAvailableBlocksLong() * fileStats.getBlockSizeLong();
        return len / 1024 / 1024; // 注意与fileStats.getFreeBlocks()的区别
    }

    /**
     * 隐藏虚拟按键
     *
     * @param activity
     */
    public static void hideVirtualKey(Activity activity) {
        // activity.getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        // 设置屏幕始终在前面，不然点击鼠标，重新出现虚拟按键
        activity.getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav
                // bar
                | View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
                | View.SYSTEM_UI_FLAG_IMMERSIVE);

    }

    /**
     * 判断SD卡是或否挂载
     *
     * @return true SD卡可用
     */
    public static boolean isSDCardExist() {
        return Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED);
    }

    /**
     * 获取程序的包名
     *
     * @param context
     * @return 包名
     */
    public static String getPackagenName(Context context) {
        if (context == null)
            return null;
        PackageInfo packageInfo = null;
        try {
            packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
        } catch (NameNotFoundException e) {
            e.printStackTrace();
        }
        return packageInfo == null ? null : packageInfo.packageName;
    }

    /**
     * 判断数据库中某张表是否存在
     */
    public static boolean sqlTableIsExist(Context context, String tableName) {
        boolean result = false;
        if (tableName == null) {
            return false;
        }
        SQLiteDatabase db = null;
        Cursor cursor = null;
        try {
            // search.db数据库的名字
            db = context.openOrCreateDatabase(HiDataValue.DB_NAME, Context.MODE_PRIVATE, null);
            String sql = "select count(*) as c from Sqlite_master  where type ='table' and name ='" + tableName.trim() + "' ";
            cursor = db.rawQuery(sql, null);
            if (cursor.moveToNext()) {
                int count = cursor.getInt(0);
                if (count > 0) {
                    result = true;
                }
            }
        } catch (Exception e) {
        }
        if (db != null)
            db.close();
        if (cursor != null)
            cursor.close();
        return result;
    }

    /**
     * 得到当前一天的零点的时间戳
     *
     * @return
     */
    public static Date getStartTimeOfDay() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        // 毫秒值也得设置
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTime();
    }


    /**
     * UID的处理: 1：自动添加 "-"   2:UID非法判断
     *
     * @param str_uid 传入的UID
     * @return
     */
    public static String handUid(String str_uid) {
        String uid = "";
        String regEx = "[A-Z]{4}[-]?[0-9]{6}[-]?[A-Z]{5}";
        Pattern pattern = Pattern.compile(regEx);
        Matcher matcher = pattern.matcher(str_uid);
        if (!matcher.matches()) {
            regEx = "[A-Z]{5}[-]?[0-9]{6}[-]?[A-Z]{5}";
            pattern = Pattern.compile(regEx);
            matcher = pattern.matcher(str_uid);
            if (matcher.matches()) {// 处理 5 6 5的
                return hand_5_6_5(str_uid);
            } else {
                regEx = "[A-Z]{6}[-]?[0-9]{6}[-]?[A-Z]{5}";
                pattern = Pattern.compile(regEx);
                matcher = pattern.matcher(str_uid);
                if (matcher.matches()) {
                    return hand_6_6_5(str_uid);// 处理 6 6 5的
                }
            }

        } else {
            uid = hand_4_6_5(str_uid);
        }
        return TextUtils.isEmpty(uid) ? null : uid;
    }

    private static String hand_6_6_5(String str_uid) {
        String head = str_uid.substring(0, 6);
        StringBuffer sb2 = new StringBuffer();
        sb2.append(head + "-");
        if ("-".equals(str_uid.charAt(6) + "")) {//第一个位置添加了 "-"
            sb2.append(str_uid.substring(7, 13));
            if ("-".equals(str_uid.charAt(13) + "")) {//第二个位置添加了"-"
                sb2.append("-" + str_uid.substring(14, 19));
            } else {//第二个位置没有 "-"
                sb2.append("-" + str_uid.substring(13, 18));
            }
        } else {// 第一个位置没有添加"-"
            sb2.append(str_uid.substring(6, 12));
            if ("-".equals(str_uid.charAt(12) + "")) {// 第二个位置有"-"
                sb2.append("-" + str_uid.substring(13, 18));
            } else {// 第二个位置没有"-"
                sb2.append("-" + str_uid.substring(12, 17));
            }
        }
        return sb2.toString();
    }

    private static String hand_5_6_5(String str_uid) {
        String head = str_uid.substring(0, 5);
        StringBuffer sb2 = new StringBuffer();
        sb2.append(head + "-");
        if ("-".equals(str_uid.charAt(5) + "")) {//第一个位置添加了 "-"
            sb2.append(str_uid.substring(6, 12));
            if ("-".equals(str_uid.charAt(12) + "")) {//第二个位置添加了"-"
                sb2.append("-" + str_uid.substring(13, 18));
            } else {//第二个位置没有 "-"
                sb2.append("-" + str_uid.substring(12, 17));
            }
        } else {// 第一个位置没有添加"-"
            sb2.append(str_uid.substring(5, 11));
            if ("-".equals(str_uid.charAt(11) + "")) {// 第二个位置有"-"
                sb2.append("-" + str_uid.substring(12, 17));
            } else {// 第二个位置没有"-"
                sb2.append("-" + str_uid.substring(11, 16));
            }
        }
        return sb2.toString();
    }

    private static String hand_4_6_5(String str_uid) {
        String head = str_uid.substring(0, 4);
        StringBuffer sb1 = new StringBuffer();
        sb1.append(head + "-");
        if ("-".equals(str_uid.charAt(4) + "")) {// 第一位置添加了"-"
            sb1.append(str_uid.substring(5, 11));
            if ("-".equals(str_uid.charAt(11) + "")) {// 第二个位置有"-"
                sb1.append("-" + str_uid.substring(12, 17));
            } else {// 第二个位置没有"-"
                sb1.append("-" + str_uid.substring(11, 16));
            }
        } else {// 第一个位置没有添加"-"
            sb1.append(str_uid.substring(4, 10));
            if ("-".equals(str_uid.charAt(10) + "")) { // 第二个位置有"-"
                sb1.append("-" + str_uid.substring(11, 16));
            } else { // 第二个位置没有"-"
                sb1.append("-" + str_uid.substring(10, 15));
            }
        }
        return sb1.toString();
    }


}








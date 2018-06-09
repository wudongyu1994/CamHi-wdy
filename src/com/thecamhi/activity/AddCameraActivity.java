package com.thecamhi.activity;

import java.util.ArrayList;
import java.util.List;

import com.hichip.R;
import com.hichip.tools.HiSearchSDK.HiSearchResult;
import com.thecamhi.base.HiTools;
import com.thecamhi.bean.HiDataValue;
import com.thecamhi.bean.MyCamera;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.text.InputFilter;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;

public class AddCameraActivity extends FragmentActivity implements OnClickListener {
    private final static int REQUEST_SEARCH_CAMERA_IN_WIFI = 3;
    // private ScanResultAdapter adapter;
    private EditText add_camera_uid_edt, add_camera_name_et, add_camera_username_et, add_camera_psw_et;
    private Button completeButton;
    private List<HiSearchResult> list = new ArrayList<HiSearchResult>();
    private MyCamera camera;
    private boolean isSearch;// 用于记录是否正在搜索的状态

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d("charming", "AddCameraActivity is called");
        setContentView(R.layout.add_camera_view);
        initView();
    }

    private void initView() {

        LinearLayout search_in_lan_ll = (LinearLayout) findViewById(R.id.search_in_lan_ll);
        search_in_lan_ll.setOnClickListener(this);

        add_camera_name_et = (EditText) findViewById(R.id.add_camera_name_et);
        add_camera_username_et = (EditText) findViewById(R.id.add_camera_username_et);
        add_camera_username_et.setFilters(new InputFilter[]{new InputFilter.LengthFilter(31)});
        add_camera_uid_edt = (EditText) findViewById(R.id.add_camera_uid_edt);
        add_camera_psw_et = (EditText) findViewById(R.id.add_camera_psw_et);
        add_camera_psw_et.setFilters(new InputFilter[]{new InputFilter.LengthFilter(31)});

        completeButton = (Button) findViewById(R.id.complete);
        completeButton.setOnClickListener(this);

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case REQUEST_SEARCH_CAMERA_IN_WIFI: {
                    Bundle extras = data.getExtras();
                    String uid = extras.getString(HiDataValue.EXTRAS_KEY_UID).trim();
                    Log.d("charming", "add camera callback uid : " + uid);
                    Log.d("charming", "add camera callback uid : " + uid.toUpperCase());
                    add_camera_uid_edt.setText(uid.toUpperCase());
                    Log.d("charming", "add camera callback uid : " + add_camera_uid_edt.getText().toString());
                }
                break;
            }
        }

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.search_in_lan_ll: {
                Intent intent = new Intent();
                intent.setClass(AddCameraActivity.this, SearchCameraActivity.class);
                startActivityForResult(intent, REQUEST_SEARCH_CAMERA_IN_WIFI);
            }
            break;
            case R.id.complete:
                chickDone();
                break;
        }
    }

    private void chickDone() {
        String str_nike = add_camera_name_et.getText().toString();
        String str_uid = add_camera_uid_edt.getText().toString().trim().toUpperCase();
        String str_password = add_camera_psw_et.getText().toString().trim();
        String str_username = add_camera_username_et.getText().toString();

        if (str_nike.length() == 0) {
            Log.d("charming", getText(R.string.tips_null_nike).toString());
            return;
        }

        if (str_username.length() == 0) {
            Log.d("charming", getText(R.string.tips_null_username).toString());
            return;
        }

        for (int i = 0; i < HiDataValue.zifu.length; i++) {
            if (str_uid.contains(HiDataValue.zifu[i])) {
                Log.d("charming", getText(R.string.tips_invalid_uid).toString());
                return;
            }
        }
        if (HiDataValue.CameraList != null && HiDataValue.CameraList.size() >= 64) {
            Log.d("charming", getText(R.string.tips_limit_add_camera).toString());
            return;
        }

        if (TextUtils.isEmpty(str_uid)) {
            Log.d("charming", getText(R.string.tips_null_uid).toString());
            return;
        }

        String string = HiTools.handUid(str_uid);
        str_uid = string;
        if (str_uid == null) {
            Log.d("charming", getText(R.string.tips_invalid_uid).toString());
            return;
        }
        // 解决：用户名和密码同时输入：31个特殊字符，应用后app闪退且起不来
        if (str_username.getBytes().length > 64) {
            Log.d("charming", getText(R.string.tips_username_tolong).toString());
            return;
        }
        if (str_password.getBytes().length > 64) {
            Log.d("charming", getText(R.string.tips_password_tolong).toString());
            return;

        }

        for (MyCamera camera : HiDataValue.CameraList) {
            if (str_uid.equalsIgnoreCase(camera.getUid())) {
                Log.d("charming", getText(R.string.tips_add_camera_exists).toString());
                return;
            }
        }
        camera = new MyCamera(getApplicationContext(), str_nike, str_uid, str_username, str_password);
        camera.saveInDatabase(this);
        camera.saveInCameraList();
        Intent broadcast = new Intent();
        broadcast.setAction(HiDataValue.ACTION_CAMERA_INIT_END);
        sendBroadcast(broadcast);

        Bundle extras = new Bundle();
        extras.putString(HiDataValue.EXTRAS_KEY_UID, str_uid);
        Intent intent = new Intent();
        intent.putExtras(extras);
        this.setResult(RESULT_OK, intent);
        this.finish();
    }

}





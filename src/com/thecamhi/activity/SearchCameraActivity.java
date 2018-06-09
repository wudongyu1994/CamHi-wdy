package com.thecamhi.activity;

import java.util.ArrayList;
import java.util.List;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.FragmentActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import com.hichip.R;
import com.hichip.tools.HiSearchSDK;
import com.hichip.tools.HiSearchSDK.HiSearchResult;
import com.thecamhi.bean.HiDataValue;
import com.thecamhi.bean.MyCamera;

public class SearchCameraActivity extends FragmentActivity {
	private ProgressBar prsbLoading;
	private LinearLayout layFailSearch;
	private ListView listSearchResult;
	private SearchResultListAdapter adapter;

	private HiSearchSDK searchSDK;
	private List<HiSearchResult> list = new ArrayList<HiSearchResult>();
	private static final int isCheckData = 0 * 9995;
	Message msg2;
	private long oldClickTime = 0;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.d("charming", "SearchCameraActivity is called");
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON, WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		setContentView(R.layout.activity_search_camera);
		getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

		layFailSearch = (LinearLayout) findViewById(R.id.lay_fail_lan_search);
		prsbLoading = (ProgressBar) findViewById(R.id.progressBar2);
		listSearchResult = (ListView) findViewById(R.id.list_search_result);
		adapter = new SearchResultListAdapter(this);

		listSearchResult.setAdapter(adapter);

		listSearchResult.setOnItemClickListener(new AdapterView.OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

				HiSearchResult r = list.get(position);
				for (MyCamera camera : HiDataValue.CameraList) {
					if (r.uid.equalsIgnoreCase(camera.getUid())) {
						Log.d("charming",getString(R.string.tip_device_add));
						return;
					}
				}

				Bundle extras = new Bundle();
				extras.putString(HiDataValue.EXTRAS_KEY_UID, r.uid);

				Intent intent = new Intent();
				intent.putExtras(extras);

				intent.setClass(SearchCameraActivity.this, AddCameraActivity.class);

				SearchCameraActivity.this.setResult(RESULT_OK, intent);

				finish();

			}
		});

	}

	private CountDownTimer timer;

	private long oldRefreshTime;

	private void startSearch() {
		if (timer != null) {
			timer.cancel();
			timer = null;
		}
		oldRefreshTime = System.currentTimeMillis();
		int timeLong = 20000;

		if (adapter != null) {
			list.clear();
			listSearchResult.requestLayout();
			adapter.notifyDataSetChanged();
		}

		searchSDK = new HiSearchSDK(new HiSearchSDK.ISearchResult() {
			@Override
			public void onReceiveSearchResult(HiSearchResult result) {
				String temp = result.uid.substring(0, 4);
				if (!TextUtils.isEmpty(temp)) {
					Message msg = handler.obtainMessage();
					msg.obj = result;
					msg.what = HiDataValue.HANDLE_MESSAGE_SCAN_RESULT;
					handler.sendMessage(msg);
				}
			}
		});
		searchSDK.search2();
		timer = new CountDownTimer(timeLong, 1000) {
			@Override
			public void onFinish() {
				if (list == null || list.size() == 0) {
					searchSDK.stop();
					layFailSearch.setVisibility(View.VISIBLE);
					prsbLoading.setVisibility(View.GONE);
				}
			}

			@Override
			public void onTick(long arg0) {

			}
		}.start();
		prsbLoading.setVisibility(View.VISIBLE);
		layFailSearch.setVisibility(View.GONE);
		list.clear();
	}

	@Override
	protected void onPause() {
		super.onPause();

		searchSDK.stop();
		if (timer != null) {
			timer.cancel();
			timer = null;
		}

	}


	@Override
	protected void onDestroy() {
		super.onDestroy();

	}

	@Override
	protected void onResume() {
		super.onResume();

		startSearch();
	}

	private Handler handler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			// Bundle bundle = msg.getData();
			switch (msg.what) {
			case HiDataValue.HANDLE_MESSAGE_SCAN_RESULT:

				HiSearchResult searchResult = (HiSearchResult) msg.obj;

				if (adapter != null) {
					list.add(searchResult);
					listSearchResult.requestLayout();
					adapter.notifyDataSetChanged();

				}
				prsbLoading.setVisibility(View.GONE);

				if (list != null && list.size() > 0 && layFailSearch.getVisibility() == View.VISIBLE) {
					layFailSearch.setVisibility(View.GONE);
				}

				// btnRefresh.setEnabled(true);

				break;
			case HiDataValue.HANDLE_MESSAGE_SCAN_CHECK:
				if (msg.arg1 == isCheckData) {

					if (list == null || list.size() == 0) {
						searchSDK.stop();
						layFailSearch.setVisibility(View.VISIBLE);
						prsbLoading.setVisibility(View.GONE);
					}

				}
				break;
			}

		}

	};


	private class SearchResultListAdapter extends BaseAdapter {

		private LayoutInflater mInflater;

		public SearchResultListAdapter(Context context) {
			this.mInflater = LayoutInflater.from(context);
		}

		public int getCount() {

			return list.size();
		}

		public Object getItem(int position) {

			return list.get(position);
		}

		public long getItemId(int position) {

			return position;
		}

		public View getView(int position, View convertView, ViewGroup parent) {
			final HiSearchResult result = (HiSearchResult) getItem(position);
			ViewHolder holder = null;
			if (convertView == null) {
				holder = new ViewHolder();
				convertView = mInflater.inflate(R.layout.list_scan_result, null);
				holder = new ViewHolder();
				holder.uid = (TextView) convertView.findViewById(R.id.txt_camera_uid);
				holder.ip = (TextView) convertView.findViewById(R.id.txt_camera_ip);
				holder.state = (TextView) convertView.findViewById(R.id.txt_camera_state);
				convertView.setTag(holder);
			}else {
				holder = (ViewHolder) convertView.getTag();
			}
			holder.uid.setText(result.uid);
			holder.ip.setText(result.ip);
			holder.uid.setTextColor(Color.rgb(0x00, 0x00, 0x00));
			holder.state.setText(" ");
			for (MyCamera camera : HiDataValue.CameraList) {
				if (camera.getUid().equalsIgnoreCase((result.uid))) {
					holder.uid.setTextColor(Color.rgb(0x99, 0x99, 0x99));
					holder.state.setText(getString(R.string.aleary_add_device));
				}
			}
			return convertView;
		}

		public final class ViewHolder {
			public TextView uid;
			public TextView ip;
			public TextView state;
		}
	}
}

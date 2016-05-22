package com.siwe.dutschedule.ui;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.siwe.dutschedule.R;
import com.siwe.dutschedule.base.BaseUi;
import com.siwe.dutschedule.util.HttpUtil;
import com.siwe.dutschedule.util.VideoList;
import com.squareup.picasso.Picasso;

import io.vov.vitamio.LibsChecker;

public class UiVideo extends BaseUi {

	ListView lv;
	private ActionBar actionBar;
	private static final String VIDEO_CAPTURE_URL_PREFIX = "http://video.dlut.edu.cn/tvwall/";
	private static final String VIDEO_CAPTURE_URL_SUFFIX = "/00000001.jpg";
	private VideoListAdapter videoListAdapter;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.ui_exam);
		initActionBar();
		lv = (ListView) findViewById(R.id.lv);
		videoListAdapter = new VideoListAdapter();
		lv.setAdapter(videoListAdapter);
		lv.setOnItemClickListener(new ChannelClick());
		// 检测视频插件是否安装
		try {
			if (!LibsChecker.checkVitamioLibs(this))
				return;
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
			new AlertDialog.Builder(this).setMessage("安装插件失败！").create().show();
		}

	}
	
	void initActionBar() {
		actionBar = new ActionBar();
		actionBar.setTitle("大工电视墙");
		actionBar.bt_message.setVisibility(View.GONE);
		actionBar.bt_more.setVisibility(View.GONE);
		actionBar.bt_refresh.setVisibility(View.VISIBLE);
		actionBar.bt_refresh.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				showLoadBar();
				videoListAdapter.refresh();
			}
		});
		actionBar.bt_left.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				doFinish();
			}
		});
	}

	class ChannelClick implements OnItemClickListener {

		@Override
		public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
				long arg3) {
			Intent in = new Intent(UiVideo.this.getApplicationContext(),
					UiDisplay.class);
			in.putExtra(UiDisplay.EXTRAL_PATH_, VideoList.channelUrl[arg2]);
			startActivity(in);
		}
	}

	@Override
	public void onResume() {
		super.onResume();
		if (!HttpUtil.isWifiConnected(this)) {
			new AlertDialog.Builder(this)
			.setCancelable(false)
			.setIcon(R.drawable.ic_launcher)
			.setTitle("未连接Wi-Fi")
			.setMessage("    大工电视墙依托于校园网，请连接至校内Wi-Fi并登录后重试！")
			.setPositiveButton("确定",
					new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog,
								int which) {
							dialog.cancel();
							doFinish();
						}
					}).show();
		}
		if(this.videoListAdapter != null)
			videoListAdapter.refresh();
	}

	class VideoListAdapter extends BaseAdapter {

		public void refresh() {
			//使缓存失效
			for(String channelUrl : VideoList.channelUrl) {
				String channelPinYinName = channelUrl.substring(channelUrl.lastIndexOf("/") + 1, channelUrl.length());
				String videoCaptureURL = VIDEO_CAPTURE_URL_PREFIX + channelPinYinName + VIDEO_CAPTURE_URL_SUFFIX;
				Picasso.with(UiVideo.this.getApplicationContext())
						.invalidate(videoCaptureURL);
			}
			this.notifyDataSetChanged();
			hideLoadBar();
		}

		@Override
		public int getCount() {
			return VideoList.channelName.length;
		}

		@Override
		public Object getItem(int position) {
			return VideoList.channelName[position];
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			ViewHolder holder;
			if(convertView == null) {
				holder = new ViewHolder();
				convertView = getInflater().inflate(R.layout.item_list_video, null);
				holder.channelName = (TextView)convertView.findViewById(R.id.channel_name);
				holder.videoCapture = (ImageView)convertView.findViewById(R.id.video_capture);
				convertView.setTag(holder);
			} else {
				holder = (ViewHolder) convertView.getTag();
			}
			String channelUrl =  VideoList.channelUrl[position];
			String channelPinYinName = channelUrl.substring(channelUrl.lastIndexOf("/") + 1, channelUrl.length());
			//特例
			if(VideoList.channelName[position].equals("CCTV5高清"))
				channelPinYinName = "cctv5";
			String videoCaptureURL = VIDEO_CAPTURE_URL_PREFIX + channelPinYinName + VIDEO_CAPTURE_URL_SUFFIX;
			Picasso.with(UiVideo.this.getApplicationContext())
					.load(videoCaptureURL)
					.placeholder(R.drawable.icon_video)
					.error(R.drawable.icon_video)
					.into(holder.videoCapture);
			holder.channelName.setText(VideoList.channelName[position]);
			return convertView;
		}
	}

	static class ViewHolder {
		public ImageView videoCapture;
		public TextView channelName;
	}
}

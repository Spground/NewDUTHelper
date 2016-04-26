package com.siwe.dutschedule.base;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import com.baidu.frontia.FrontiaApplication;
import com.siwe.dutschedule.ui.UiPushNotice;
import com.umeng.message.PushAgent;
import com.umeng.message.UmengNotificationClickHandler;
import com.umeng.message.entity.UMessage;

import java.util.Calendar;

public class BaseApp extends FrontiaApplication {

	@Override
	public void onCreate() {
		Log.d("YYY", "start application at " + Calendar.getInstance().getTimeInMillis());
		super.onCreate();
		Log.d("YYY", "end application at " + Calendar.getInstance().getTimeInMillis());
		// 以下是您原先的代码实现，保持不变
		initUmengPush();

	}

	private void initUmengPush() {
		/**
		 * 该Handler是在BroadcastReceiver中被调用，故
		 * 如果需启动Activity，需添加Intent.FLAG_ACTIVITY_NEW_TASK
		 * */
		UmengNotificationClickHandler notificationClickHandler = new UmengNotificationClickHandler(){
			@Override
			public void dealWithCustomAction(Context context, UMessage msg) {
				Intent showNotice = new Intent(context, UiPushNotice.class);
				showNotice.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				showNotice.putExtra(UiPushNotice.EXTRAL_MSG, msg.custom);
				startActivity(showNotice);
			}
		};
		PushAgent mPushAgent = PushAgent.getInstance(this);
		mPushAgent.enable();
		mPushAgent.setNotificationClickHandler(notificationClickHandler);
	}

	private String s;
	private long l;
	private int i;
	
	public int getInt () {
		return i;
	}
	
	public void setInt (int i) {
		this.i = i;
	}
	
	public long getLong () {
		return l;
	}
	
	public void setLong (long l) {
		this.l = l;
	}
	
	public String getString () {
		return s;
	}
	
	public void setString (String s) {
		this.s = s;
	}
}
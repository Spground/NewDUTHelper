package com.siwe.dutschedule.ui;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.baidu.android.pushservice.PushConstants;
import com.baidu.android.pushservice.PushManager;
import com.siwe.dutschedule.R;
import com.siwe.dutschedule.base.BaseService;
import com.siwe.dutschedule.base.BaseUi;
import com.siwe.dutschedule.service.AutoLoginService;
import com.siwe.dutschedule.util.AppUtil;
import com.siwe.dutschedule.util.PushUtils;
import com.umeng.analytics.MobclickAgent;
import com.umeng.message.PushAgent;

import java.util.Calendar;

public class UiSplash extends BaseUi {
    boolean isFirstIn;

    private static final int GO_HOME = 0;
    private static final int GO_GUIDE = 1;
    private static final int GO_LOGIN = 2;
    private static final long SPLASH_DELAY_MILLIS = 2000;
    private SharedPreferences preferences;

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case GO_LOGIN:
                    forward(UiLogin.class);
                    break;
                case GO_GUIDE:
                    forward(UiGuide.class);
                    break;
                case GO_HOME:
                    forward(UiHome.class);
                    break;
            }
            doFinish();
            super.handleMessage(msg);
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.ui_splash);
        preferences = AppUtil.getSharedPreferences(this);
        initUmeng();
        initBaiduPush();
        initJump();
    }

    private void initUmeng() {
        // 错误收集
        MobclickAgent.onError(this);
        //友盟推送
        PushAgent mPushAgent = PushAgent.getInstance(this.getApplicationContext());
        if(!mPushAgent.isEnabled())
            mPushAgent.enable();
    }

    private void initBaiduPush() {
        // Push: 以apikey的方式登录，一般放在主Activity的onCreate中。
        Log.d("YYY", "before start work at "
                + Calendar.getInstance().getTimeInMillis());
        PushManager.startWork(getApplicationContext(),
                PushConstants.LOGIN_TYPE_API_KEY,
                PushUtils.getMetaValue(this, "api_key"));
        Log.d("YYY", "after start work at "
                + Calendar.getInstance().getTimeInMillis());
        Log.d("YYY", "after enableLbs at "
                + Calendar.getInstance().getTimeInMillis());
    }

    private void initJump() {
        isFirstIn = preferences.getBoolean("isFirst", true);
        if (isFirstIn) {
            mHandler.sendEmptyMessageDelayed(GO_GUIDE, SPLASH_DELAY_MILLIS);
        } else if (preferences.getBoolean("isSaved", false)) {
            // 启动登陆服务
            BaseService.start(this, AutoLoginService.class);
            mHandler.sendEmptyMessageDelayed(GO_HOME, SPLASH_DELAY_MILLIS);
        } else {
            mHandler.sendEmptyMessageDelayed(GO_LOGIN, SPLASH_DELAY_MILLIS);
        }
    }
}

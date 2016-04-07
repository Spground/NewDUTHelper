package com.siwe.dutschedule.ui;

import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

import com.siwe.dutschedule.R;
import com.siwe.dutschedule.base.BaseUi;

public class UiPushNotice extends BaseUi {

    public static final String EXTRAL_MSG = "notice_content";
    private String notice_content = "";

    private ActionBar actionBar;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.ui_push_notice);
        initActionBar();
        notice_content = getIntent().getStringExtra(EXTRAL_MSG);
        ((EditText)findViewById(R.id.notice_content)).setText(notice_content);
    }

    private void initActionBar() {
        actionBar = new ActionBar();
        actionBar.setTitle("重要通知");
        actionBar.bt_message.setVisibility(View.GONE);
        actionBar.bt_more.setVisibility(View.GONE);
        actionBar.bt_refresh.setVisibility(View.GONE);
        actionBar.bt_left.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                doFinish();
            }
        });

    }
}

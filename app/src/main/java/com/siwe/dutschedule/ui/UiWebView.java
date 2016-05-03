package com.siwe.dutschedule.ui;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.webkit.CookieManager;
import android.webkit.WebSettings;
import android.webkit.WebSettings.ZoomDensity;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;
import com.siwe.dutschedule.R;
import com.siwe.dutschedule.base.BaseUi;
import com.siwe.dutschedule.base.C;

import org.json.JSONException;
import org.json.JSONObject;

import cz.msebera.android.httpclient.Header;

public class UiWebView extends BaseUi {

    private WebView webview;
    private String url, title, id;
    private ActionBar actionBar;
    private AsyncHttpClient client = new AsyncHttpClient();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.ui_webview);
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            url = extras.getString("url");
            title = extras.getString("title");
            this.id = extras.getString("id");
        }
        this.initActionBar();
        webview = (WebView) findViewById(R.id.webView1);
        webview.getSettings().setJavaScriptEnabled(true);
        webview.getSettings().setBuiltInZoomControls(true); // 显示放大缩小 controler
        webview.getSettings().setSupportZoom(true); // 可以缩放
        webview.getSettings().setDefaultZoom(ZoomDensity.CLOSE);// 默认缩放模式 是// ZoomDensity.MEDIUM
        webview.setWebViewClient(new MyWebViewClient());

        webview.getSettings().setLoadsImagesAutomatically(true);
        webview.getSettings().setUseWideViewPort(true);
        webview.getSettings().setLoadWithOverviewMode(true);
        webview.getSettings().setBuiltInZoomControls(true);
        webview.getSettings().setDefaultTextEncodingName("UTF-8");
        webview.getSettings().setLayoutAlgorithm(WebSettings.LayoutAlgorithm.NORMAL);
        if (this.url != null)
            webview.loadUrl(url);
        else {
            loadHtmlFromLocal();
        }
    }

    private void loadHtmlFromLocal() {
        if (this.id == null)
            return;
        try {
            JSONObject jObj = new JSONObject(C.idg.initRequestParams);
            jObj.put("lx", "1");
            jObj.put("id", this.id);
            RequestParams params = new RequestParams();
            params.put("functionPyname", "grtzxxxx");
            params.put("param", jObj.toString());
            client.post(this.getApplicationContext(), C.api.idg_base, params, new JsonHttpResponseHandler() {
                @Override
                public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                    super.onSuccess(statusCode, headers, response);
                    try {
                        String html = response.getString("content");
                        Log.v("===TAG===", html);
                        webview.getSettings().setTextSize(WebSettings.TextSize.LARGEST);
                        webview.loadDataWithBaseURL(null, html, "text/html", "utf-8", null);
                    } catch (JSONException e) {
                        toastE("解析网页出错！");
                        e.printStackTrace();
                    }

                }

                @Override
                public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {
                    super.onFailure(statusCode, headers, responseString, throwable);
                    toastE("加载网页出错！");
                }
            });

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }


    private void initActionBar() {
        actionBar = new ActionBar();
        actionBar.setTitle(title);
        actionBar.bt_message.setVisibility(View.GONE);
        actionBar.bt_more.setVisibility(View.GONE);
        actionBar.bt_left.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                doFinish();
            }
        });
        actionBar.bt_refresh.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                if (url != null)
                    webview.reload();
            }
        });

    }

    @Override
    public void onResume() {
        super.onResume();
    }

    public class MyWebViewClient extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            view.loadUrl(url);
            return true;
        }

        // 开始加载
        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            super.onPageStarted(view, url, favicon);
            showLoadBar();

        }

        // 结束加载
        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
            hideLoadBar();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        CookieManager.getInstance().removeAllCookie();
        webview.clearCache(true);
        System.gc();
    }

    public boolean onKeyDown(int keyCoder, KeyEvent event) {
        if (webview.canGoBack() && keyCoder == KeyEvent.KEYCODE_BACK) {
            webview.goBack(); // goBack()表示返回webView的上一页面
            return true;
        } else if (keyCoder == KeyEvent.KEYCODE_BACK) {
            doFinish();
            return true;
        }
        return false;
    }
}

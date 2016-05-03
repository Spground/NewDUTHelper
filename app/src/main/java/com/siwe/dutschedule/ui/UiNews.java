package com.siwe.dutschedule.ui;

import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.ImageView;
import android.widget.TextView;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;
import com.siwe.dutschedule.R;
import com.siwe.dutschedule.adapter.NewsPagerAdapter;
import com.siwe.dutschedule.base.BaseMessage;
import com.siwe.dutschedule.base.BaseUi;
import com.siwe.dutschedule.base.C;
import com.siwe.dutschedule.model.AnotherNews;
import com.siwe.dutschedule.model.News;
import com.siwe.dutschedule.view.GeneraListView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;

import cz.msebera.android.httpclient.Header;

/**
 * @author linwei
 * 
 */
public class UiNews extends BaseUi implements GeneraListView.OnLoadMoreListener {// widget

	private ActionBar actionBar;
	private ViewPager mPager;
	private ArrayList<View> listViews = new ArrayList<>();
	private TextView[] bt_selector = new TextView[3];
	private ImageView cursor;
	private NewsPagerAdapter newsPagerAdapter;

	// data
	private ArrayList<ArrayList<News>> listNews = new ArrayList<>(3); // 容量为3
	private int screenW;
	private int one;
	public int currIndex = 0;
	private int taskIndex = 0;

	private AsyncHttpClient client = new AsyncHttpClient();
	private int curPageNo = 1;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.ui_news);
		this.initActionBar();
		this.initTopBtn();
		this.initCursor();
		this.initPagerViewData();
		doTaskRefresh();
	}

	@Override
	public void onResume() {
		super.onResume();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		listNews.clear();
		listViews.clear();
	}

	private void initCursor() {
		cursor = (ImageView) findViewById(R.id.score_cursor);
		DisplayMetrics dm = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(dm);
		screenW = dm.widthPixels;
		one = screenW / 3;
	}

	private void scrollCursor(int from, int to) {
		Animation animation = new TranslateAnimation(one * from, one * to, 0, 0);
		animation.setFillAfter(true);
		animation.setDuration(200);
		cursor.startAnimation(animation);
		this.currIndex = to;
	}

	private void initTopBtn() {

		bt_selector[0] = (TextView) findViewById(R.id.xiaoneitongzhi);
		bt_selector[1] = (TextView) findViewById(R.id.jiaowu);
		bt_selector[2] = (TextView) findViewById(R.id.chuangxin);
		
		bt_selector[0].setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				mPager.setCurrentItem(0, true);
			}
		});
		bt_selector[1].setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				mPager.setCurrentItem(1, true);
			}
		});
		bt_selector[2].setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				mPager.setCurrentItem(2, true);
			}
		});
	}

	void initActionBar() {
		actionBar = new ActionBar();
		actionBar.setTitle("校园公告");
		actionBar.bt_message.setVisibility(View.GONE);
		actionBar.bt_left.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				doFinish();
			}
		});
		actionBar.bt_refresh.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				doTaskRefresh();
			}
		});
		
		mPager = (ViewPager) findViewById(R.id.viewpager);
		for (int i = 0; i < 3; i++) {
			listViews.add(this.getLayout(R.layout.pager_news));
			listNews.add(new ArrayList<News>());
		}
	}

	protected void doTaskRefresh() {
		switch (currIndex) {
			case 0://校内通知
				curPageNo = 1;
				listNews.get(0).clear();
				newsPagerAdapter.notifyPageDataSetChanged(0);
				RequestParams params = new RequestParams();
				params.put("functionPyname", "grtzfycx");
				try {
					JSONObject jsonObject = new JSONObject(C.idg.initRequestParams);
					jsonObject.put("pageNo", String.valueOf(curPageNo));
					Log.v("===TAG===", jsonObject.toString());
					params.put("param", jsonObject.toString());
				} catch (JSONException e) {
					e.printStackTrace();
				}
				this.client.get(this.getApplicationContext(), C.api.idg_base, params, new JsonHttpResponseHandler() {
					@Override
					public void onSuccess(int statusCode, Header[] headers, JSONArray response) {
						super.onSuccess(statusCode, headers, response);
						Log.v("===TAG===", response.toString());
						if(response.length() == 0) {
							toast("没有数据");
						}
						listNews.get(0).clear();
						for(int i = 0; i < response.length(); i++) {
							try {
								JSONObject jObj = response.getJSONObject(i);
								AnotherNews news = new AnotherNews();
								news.setId(jObj.getString("id"));
								news.setDept(jObj.getString("dept"));
								news.setUptime(jObj.getString("time"));
								news.setTitle(jObj.getString("title"));
								listNews.get(0).add(news);
							} catch (JSONException e) {
								e.printStackTrace();
							}

						}
						newsPagerAdapter.notifyPageDataSetChanged(0);
						((GeneraListView)(listViews.get(0).findViewById(R.id.lv))).loadMoreComplete();
					}

					@Override
					public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {
						super.onFailure(statusCode, headers, responseString, throwable);
						toastE("服务器数据格式出错！");
					}
				}) ;
				break;
			case 1://教务处通知
				HashMap<String, String> urlParams = new HashMap<>();
				urlParams.put("type", String.valueOf(0));//0 教务处， 1 团委， 2 创院
				taskIndex = currIndex;
				try {
					this.doTaskAsync(C.task.news, C.api.news, urlParams);
				} catch (Exception e) {
					e.printStackTrace();
				}
				break;
			case 2://创院通知
				HashMap<String, String> urlParams0 = new HashMap<>();
				urlParams0.put("type", String.valueOf(2));//0 教务处， 1 团委， 2 创院
				taskIndex = currIndex;
				try {
					this.doTaskAsync(C.task.news, C.api.news, urlParams0);
				} catch (Exception e) {
					e.printStackTrace();
				}
				break;
			default:
				return;
		}

	}

	@Override
	public void onTaskComplete(int taskId, BaseMessage message) {
		super.onTaskComplete(taskId, message);
		if (!message.isSuccess()) {
			toastE("刷新失败");
			return;
		}
		try {
			listNews.get(taskIndex).clear();
			listNews.add(taskIndex,
					(ArrayList<News>) message.getResultList("News"));
			this.initPagerViewData();
		} catch (Exception e) {
			e.printStackTrace();
			toastE(C.err.server);
		}
	}

	private void initPagerViewData() {
		this.newsPagerAdapter = new NewsPagerAdapter(this, listViews, listNews);
		mPager.setAdapter(this.newsPagerAdapter);
		mPager.setOnPageChangeListener(new MyChangeListener());
		mPager.setCurrentItem(currIndex, true);
	}

	@Override
	public void onLoadMore() {
		//校内通知加载更多
		RequestParams params = new RequestParams();
		params.put("functionPyname", "grtzfycx");
		try {
			JSONObject jsonObject = new JSONObject(C.idg.initRequestParams);
			jsonObject.put("pageNo", String.valueOf(++curPageNo));
			Log.v("===TAG===", jsonObject.toString());
			params.put("param", jsonObject.toString());
		} catch (JSONException e) {
			e.printStackTrace();
		}
		this.client.get(this.getApplicationContext(), C.api.idg_base, params, new JsonHttpResponseHandler() {
			@Override
			public void onSuccess(int statusCode, Header[] headers, JSONArray response) {
				super.onSuccess(statusCode, headers, response);
				Log.v("===TAG===", response.toString());
				if(response.length() == 0) {
					((GeneraListView)(listViews.get(0).findViewById(R.id.lv))).noMoreData();
					--curPageNo;
					return;
				}
				for(int i = 0; i < response.length(); i++) {
					try {
						JSONObject jObj = response.getJSONObject(i);
						AnotherNews news = new AnotherNews();
						news.setId(jObj.getString("id"));
						news.setDept(jObj.getString("dept"));
						news.setUptime(jObj.getString("time"));
						news.setTitle(jObj.getString("title"));
						listNews.get(0).add(news);
						newsPagerAdapter.notifyPageDataSetChanged(0);
						((GeneraListView)(listViews.get(0).findViewById(R.id.lv))).loadMoreComplete();
					} catch (JSONException e) {
						e.printStackTrace();
					}

				}
			}

			@Override
			public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {
				super.onFailure(statusCode, headers, responseString, throwable);
				toastE("服务器数据格式出错！");
			}
		}) ;
	}

	private class MyChangeListener implements OnPageChangeListener {

		@Override
		public void onPageScrolled(int arg0, float arg1, int arg2) {

		}

		@Override
		public void onPageSelected(int position) {
			Log.e("===TAG===", "position is " + position);
			for(int i = 0; i < 3; i++)
				bt_selector[i].setTextColor(getResources().getColor(
						R.color.global_gray));
			bt_selector[position].setTextColor(getResources()
					.getColor(R.color.text));
			scrollCursor(currIndex, position);
			if(listNews.get(position).isEmpty()) {
				Log.e("===TAG===", "empty doTaskRefresh");
				doTaskRefresh();
			}
		}

		@Override
		public void onPageScrollStateChanged(int arg0) {

		}
	}
}

package com.siwe.dutschedule.view;

import android.widget.BaseAdapter;
import android.widget.ListView;

/**
 * Created by asus on 2016/5/3.
 */
import java.util.Date;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.siwe.dutschedule.R;

/**
 * customer ListView
 **/
public class GeneraListView extends ListView implements OnScrollListener {

    private final static int RELEASE_To_REFRESH = 0;// 下拉过程的状态值
    private final static int PULL_To_REFRESH = 1; // 从下拉返回到不刷新的状态值
    private final static int REFRESHING = 2;// 正在刷新的状态值
    private final static int DONE = 3;
    private final static int LOADING = 4;

    // 实际的padding的距离与界面上偏移距离的比例
    private final static int RATIO = 3;
    private LayoutInflater inflater;

    // ListView头部下拉刷新的布局
    private LinearLayout headerView;
    private TextView lvHeaderTipsTv;
    private TextView lvHeaderLastUpdatedTv;
    private ImageView lvHeaderArrowIv;
    private ProgressBar lvHeaderProgressBar;

    // 定义头部下拉刷新的布局的高度
    private int headerContentHeight;

    private RotateAnimation animation;
    private RotateAnimation reverseAnimation;

    private int startY;
    private int state;
    private boolean isBack;
    private boolean enablePullDownReresh = false;
    /**
     * 上滑加载更多
     **/
    private boolean enablePullUpLoadMore;
    private View footer;
    private int lastVisibleItem = 0;
    private int totalItemCount = 0;
    private boolean isLoadingMore = false;
    private OnLoadMoreListener onLoadMoreListener;
    private float mLastY = -1;
    private boolean isRecord = false;//ensure mLastY is assigned only one time at a logic event
    private final float MAX_TO_LOAD_MORE = 120.0f;
    private Context ctx;

    // 用于保证startY的值在一个完整的touch事件中只被记录一次
    private boolean isRecored;

    /**
     * load more callbakc interface
     */
    public interface OnLoadMoreListener {
        void onLoadMore();
    }

    /**
     * callback interface
     **/
    private OnRefreshListener refreshListener;

    private boolean isRefreshable = false;

    public GeneraListView(Context context) {
        super(context);
        init(context);

    }

    public GeneraListView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    /**
     * init everything
     *
     * @param context
     */
    private void init(Context context) {
        setCacheColorHint(context.getResources().getColor(android.R.color.transparent));
        inflater = LayoutInflater.from(context);
        headerView = (LinearLayout) inflater.inflate(R.layout.general_listview_refresh_head, null);
        lvHeaderTipsTv = (TextView) headerView
                .findViewById(R.id.lvHeaderTipsTv);
        lvHeaderLastUpdatedTv = (TextView) headerView
                .findViewById(R.id.lvHeaderLastUpdatedTv);

        lvHeaderArrowIv = (ImageView) headerView
                .findViewById(R.id.lvHeaderArrowIv);
        lvHeaderArrowIv.setMinimumWidth(70);
        lvHeaderArrowIv.setMinimumHeight(50);
        lvHeaderProgressBar = (ProgressBar) headerView
                .findViewById(R.id.lvHeaderProgressBar);
        measureView(headerView);
        headerContentHeight = headerView.getMeasuredHeight();
        // 设置内边距，正好距离顶部为一个负的整个布局的高度，正好把头部隐藏
        headerView.setPadding(0, -1 * headerContentHeight, 0, 0);
        // 重绘一下
        headerView.invalidate();
        // 将下拉刷新的布局加入ListView的顶部
        addHeaderView(headerView, null, false);
        // 设置滚动监听事件
        setOnScrollListener(this);
        // 设置旋转动画事件
        animation = new RotateAnimation(0, -180,
                RotateAnimation.RELATIVE_TO_SELF, 0.5f,
                RotateAnimation.RELATIVE_TO_SELF, 0.5f);
        animation.setInterpolator(new LinearInterpolator());
        animation.setDuration(250);
        animation.setFillAfter(true);
        reverseAnimation = new RotateAnimation(-180, 0,
                RotateAnimation.RELATIVE_TO_SELF, 0.5f,
                RotateAnimation.RELATIVE_TO_SELF, 0.5f);
        reverseAnimation.setInterpolator(new LinearInterpolator());
        reverseAnimation.setDuration(200);
        reverseAnimation.setFillAfter(true);
        // 一开始的状态就是下拉刷新完的状态，所以为DONE
        state = DONE;
        // 是否正在刷新
        isRefreshable = false;
        /**上滑加载更多**/
        initFooterView(context);
    }

    @Override
    public void onScrollStateChanged(AbsListView view, int scrollState) {

    }

    /**
     * 初始化footer view 默认是不可见的
     *
     * @param ctx
     */
    private void initFooterView(Context ctx) {
        this.ctx = ctx;
        LayoutInflater inflater = LayoutInflater.from(ctx);
        footer = inflater.inflate(R.layout.general_listview_loadmore_footer, null);
        this.footer.setVisibility(GONE);
        footer.findViewById(R.id.progressbar).setVisibility(View.GONE);
        //add footer view
        this.addFooterView(footer);
        this.setOnScrollListener(this);
    }

    @Override
    public void onScroll(AbsListView view, int firstVisibleItem,
                         int visibleItemCount, int totalItemCount) {
        isRefreshable = firstVisibleItem == 0;
        this.lastVisibleItem = firstVisibleItem + visibleItemCount;
        this.totalItemCount = totalItemCount;
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        if (isRefreshable && enablePullDownReresh) {
            switch (ev.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    if (!isRecored) {
                        isRecored = true;
                        startY = (int) ev.getY();// 手指按下时记录当前位置
                    }
                    break;
                case MotionEvent.ACTION_UP:
                    if (state != REFRESHING && state != LOADING) {
                        if (state == PULL_To_REFRESH) {
                            state = DONE;
                            changeHeaderViewByState();
                        }
                        if (state == RELEASE_To_REFRESH) {
                            state = REFRESHING;
                            changeHeaderViewByState();
                            onLvRefresh();
                        }
                    }
                    isRecored = false;
                    isBack = false;
                    break;

                case MotionEvent.ACTION_MOVE:
                    int tempY = (int) ev.getY();
                    if (!isRecored) {
                        isRecored = true;
                        startY = tempY;
                    }
                    if (state != REFRESHING && isRecored && state != LOADING) {
                        // 保证在设置padding的过程中，当前的位置一直是在head，否则如果当列表超出屏幕的话，当在上推的时候，列表会同时进行滚动
                        // 可以松手去刷新了
                        if (state == RELEASE_To_REFRESH) {
                            setSelection(0);
                            // 往上推了，推到了屏幕足够掩盖head的程度，但是还没有推到全部掩盖的地步
                            if (((tempY - startY) / RATIO < headerContentHeight)// 由松开刷新状态转变到下拉刷新状态
                                    && (tempY - startY) > 0) {
                                state = PULL_To_REFRESH;
                                changeHeaderViewByState();
                            }
                            // 一下子推到顶了
                            else if (tempY - startY <= 0) {// 由松开刷新状态转变到done状态
                                state = DONE;
                                changeHeaderViewByState();
                            }
                        }
                        // 还没有到达显示松开刷新的时候,DONE或者是PULL_To_REFRESH状态
                        if (state == PULL_To_REFRESH) {
                            setSelection(0);
                            // 下拉到可以进入RELEASE_TO_REFRESH的状态
                            if ((tempY - startY) / RATIO >= headerContentHeight) {// 由done或者下拉刷新状态转变到松开刷新
                                state = RELEASE_To_REFRESH;
                                isBack = true;
                                changeHeaderViewByState();
                            }
                            // 上推到顶了
                            else if (tempY - startY <= 0) {// 由DOne或者下拉刷新状态转变到done状态
                                state = DONE;
                                changeHeaderViewByState();
                            }
                        }
                        // done状态下
                        if (state == DONE) {
                            if (tempY - startY > 0) {
                                state = PULL_To_REFRESH;
                                changeHeaderViewByState();
                            }
                        }
                        // 更新headView的size
                        if (state == PULL_To_REFRESH) {
                            headerView.setPadding(0, -1 * headerContentHeight
                                    + (tempY - startY) / RATIO, 0, 0);

                        }
                        // 更新headView的paddingTop
                        if (state == RELEASE_To_REFRESH) {
                            headerView.setPadding(0, (tempY - startY) / RATIO
                                    - headerContentHeight, 0, 0);
                        }

                    }
                    break;
                default:
                    break;
            }
        }

        /**如果加载更多不可用， 直接返回**/
        if (!enablePullUpLoadMore)
            return super.onTouchEvent(ev);
        /**处理上滑加载更多**/
        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (!isRecord)
                    mLastY = ev.getY();
                isRecord = true;
                break;
            case MotionEvent.ACTION_MOVE:
                float deltaY = ev.getY() - mLastY;
                if (!isRecord) {
                    mLastY = ev.getY();
                    isRecord = true;
                }
                Log.v("===OnTouchEvent()===", "ACTION_MOVE" + "is " + deltaY);
                if (lastVisibleItem == totalItemCount && !isLoadingMore && deltaY < 0) {
                    //update height of footer
                    if (deltaY >= -40)
                        deltaY = -40;
                    if (-deltaY >= MAX_TO_LOAD_MORE)
                        ((TextView) footer.findViewById(R.id.text)).setText("松开加载更多");
                    else
                        ((TextView) footer.findViewById(R.id.text)).setText("上滑加载更多");
                    footer.setPadding(0, 40, 0, (int) (-deltaY));
                }
                break;
            case MotionEvent.ACTION_UP:
                isRecord = false;
                mLastY = -1;
                //reset height of footer
                if (lastVisibleItem == totalItemCount && !isLoadingMore) {
                    //bottom padding is enough to trigger the loading more action
                    if (footer.getPaddingBottom() >= MAX_TO_LOAD_MORE) {
                        isLoadingMore = true;
                        footer.findViewById(R.id.progressbar).setVisibility(VISIBLE);
                        ((TextView) footer.findViewById(R.id.text)).setText("正在加载...");
                        onLoadMoreListener.onLoadMore();
                    }
                    footer.setPadding(0, 40, 0, 40);
                }
                break;
            default:
                isRecord = false;
                mLastY = -1;
                break;
        }
        return super.onTouchEvent(ev);
    }

    /**
     * 当状态改变时候，调用该方法，以更新界面
     */
    private void changeHeaderViewByState() {
        switch (state) {
            case RELEASE_To_REFRESH:
                lvHeaderArrowIv.setVisibility(View.VISIBLE);
                lvHeaderProgressBar.setVisibility(View.GONE);
                lvHeaderTipsTv.setVisibility(View.VISIBLE);
                lvHeaderLastUpdatedTv.setVisibility(View.VISIBLE);
                lvHeaderArrowIv.clearAnimation();// 清除动画
                lvHeaderArrowIv.startAnimation(animation);// 开始动画效果
                lvHeaderTipsTv.setText("松开刷新");
                break;
            case PULL_To_REFRESH:
                lvHeaderProgressBar.setVisibility(View.GONE);
                lvHeaderTipsTv.setVisibility(View.VISIBLE);
                lvHeaderLastUpdatedTv.setVisibility(View.VISIBLE);
                lvHeaderArrowIv.clearAnimation();
                lvHeaderArrowIv.setVisibility(View.VISIBLE);
                // 是由RELEASE_To_REFRESH状态转变来的
                if (isBack) {
                    isBack = false;
                    lvHeaderArrowIv.clearAnimation();
                    lvHeaderArrowIv.startAnimation(reverseAnimation);

                    lvHeaderTipsTv.setText("下拉刷新");
                } else {
                    lvHeaderTipsTv.setText("下拉刷新");
                }
                break;

            case REFRESHING:

                headerView.setPadding(0, 0, 0, 0);

                lvHeaderProgressBar.setVisibility(View.VISIBLE);
                lvHeaderArrowIv.clearAnimation();
                lvHeaderArrowIv.setVisibility(View.GONE);
                lvHeaderTipsTv.setText("正在刷新...");
                lvHeaderLastUpdatedTv.setVisibility(View.VISIBLE);
                break;
            case DONE:
                headerView.setPadding(0, -1 * headerContentHeight, 0, 0);

                lvHeaderProgressBar.setVisibility(View.GONE);
                lvHeaderArrowIv.clearAnimation();
                lvHeaderArrowIv.setImageResource(R.drawable.ic_action_arrow_bottom);
                lvHeaderTipsTv.setText("下拉刷新");
                lvHeaderLastUpdatedTv.setVisibility(View.VISIBLE);
                break;
        }
    }

    /**
     * 此方法直接照搬自网络上的一个下拉刷新的demo，此处是“估计”headView的width以及height
     *
     * @param child
     */
    private void measureView(View child) {
        ViewGroup.LayoutParams params = child.getLayoutParams();
        if (params == null) {
            params = new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
        }
        int childWidthSpec = ViewGroup.getChildMeasureSpec(0, 0 + 0,
                params.width);
        int lpHeight = params.height;
        int childHeightSpec;
        if (lpHeight > 0) {
            childHeightSpec = MeasureSpec.makeMeasureSpec(lpHeight,
                    MeasureSpec.EXACTLY);
        } else {
            childHeightSpec = MeasureSpec.makeMeasureSpec(0,
                    MeasureSpec.UNSPECIFIED);
        }
        child.measure(childWidthSpec, childHeightSpec);
    }

    public void setonRefreshListener(OnRefreshListener refreshListener) {
        this.refreshListener = refreshListener;
        isRefreshable = true;
    }

    public interface OnRefreshListener {
        void onRefresh();
    }

    @SuppressWarnings("deprecation")
    public void onRefreshComplete() {
        state = DONE;
        lvHeaderLastUpdatedTv.setText("最近更新:" + new Date().toLocaleString());
        changeHeaderViewByState();
    }

    private void onLvRefresh() {
        if (refreshListener != null) {
            refreshListener.onRefresh();
        }
    }

    @SuppressWarnings("deprecation")
    public void setAdapter(BaseAdapter adapter) {
        lvHeaderLastUpdatedTv.setText("最近更新:" + new Date().toLocaleString());
        super.setAdapter(adapter);
    }

    public LinearLayout getHeaderView() {
        return this.headerView;
    }

    /**
     * 数据加载完成以后调用这个方法
     */
    public void loadMoreComplete() {
        if (!enablePullUpLoadMore)
            return;
        isLoadingMore = false;
        invalidate();
        footer.findViewById(R.id.progressbar).setVisibility(View.GONE);
        ((TextView) footer.findViewById(R.id.text)).setText("上滑加载更多");
    }

    /**
     * 没有更多数据
     */
    public void noMoreData() {
        if (!enablePullUpLoadMore)
            return;
        loadMoreComplete();
        Toast.makeText(this.ctx, "No more data", Toast.LENGTH_SHORT).show();
    }

    /**
     * 设置加载更多的回调
     *
     * @param onLoadMoreListener
     */
    public void setOnLoadMoreCallback(OnLoadMoreListener onLoadMoreListener) {
        this.onLoadMoreListener = onLoadMoreListener;
    }

    /**
     * 设置listview支持上滑加载更多功能
     *
     * @param enable
     */
    public void setEnablePullUpLoadMore(boolean enable) {
        this.enablePullUpLoadMore = enable;
        if (enable)
            this.footer.setVisibility(VISIBLE);
        else
            this.footer.setVisibility(GONE);
    }

    public void setEnablePullDownReresh(boolean enable) {
        this.enablePullDownReresh = enable;
    }
}

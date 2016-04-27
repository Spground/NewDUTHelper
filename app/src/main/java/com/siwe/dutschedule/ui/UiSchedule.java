package com.siwe.dutschedule.ui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.json.JSONObject;

import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.SparseArray;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.siwe.dutschedule.R;
import com.siwe.dutschedule.adapter.SchedulePagerAdapter;
import com.siwe.dutschedule.base.BaseMessage;
import com.siwe.dutschedule.base.BaseTask;
import com.siwe.dutschedule.base.BaseUi;
import com.siwe.dutschedule.base.C;
import com.siwe.dutschedule.model.Schedule;
import com.siwe.dutschedule.sqlite.ScheduleSqlite;
import com.siwe.dutschedule.util.AppUtil;
import com.siwe.dutschedule.util.TimeUtil;
import com.siwe.dutschedule.view.PopupManger;

public class UiSchedule extends BaseUi {

    // ViewPager
    private ViewPager mPager;// 页卡内容
    private ArrayList<View> scrollViews; // Tab页面列表

    // top button
    private Button[] btn_top = new Button[7];

    // Cursor
    private ImageView cursor;
    private int bmpW; // cursor origin width
    private int offset = 0; // 偏移量
    private int currIndex = 0; // 当前游标位置
    private int today;
    private int countFirst = 0;

    // data
    private static ArrayList<Schedule> datalist = new ArrayList<>();
    private TextView tv_weekthis;
    private int currentWeek;
    private int tempWeek;

    ArrayList<String> selectWeek = new ArrayList<>();
    private PopupManger popupManger;

    private SHOW_BY_WHAT curByWhat = SHOW_BY_WHAT.DAY.DAY;//默认按天显示
    enum SHOW_BY_WHAT {
        DAY,
        WEEK
    }
    private TextView byDayTV, byWeekTV;
    Animation leftIn = new TranslateAnimation(Animation.RELATIVE_TO_SELF, -1.0f, Animation.RELATIVE_TO_SELF, 0.0f,
            Animation.RELATIVE_TO_SELF, 0.0f, Animation.RELATIVE_TO_SELF, 0.0f);
    Animation leftOut = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 0.0f, Animation.RELATIVE_TO_SELF, -1.0f,
            Animation.RELATIVE_TO_SELF, 0.0f, Animation.RELATIVE_TO_SELF, 0.0f);
    Animation rightIn = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 1.0f, Animation.RELATIVE_TO_SELF, 0.0f,
            Animation.RELATIVE_TO_SELF, 0.0f, Animation.RELATIVE_TO_SELF, 0.0f);
    Animation rightOut = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 0.0f, Animation.RELATIVE_TO_SELF, 1.0f,
            Animation.RELATIVE_TO_SELF, 0.0f, Animation.RELATIVE_TO_SELF, 0.0f);
    private View mScheduleTable;
    private int[][] rIds = {
            {R.id.c00,R.id.c01, R.id.c02,R.id.c03,R.id.c04,R.id.c05,R.id.c06},
            {R.id.c10,R.id.c11, R.id.c12,R.id.c13,R.id.c14,R.id.c15,R.id.c16},
            {R.id.c20,R.id.c21, R.id.c22,R.id.c23,R.id.c24,R.id.c25,R.id.c26},
            {R.id.c30,R.id.c31, R.id.c32,R.id.c33,R.id.c34,R.id.c35,R.id.c36},
            {R.id.c40,R.id.c41, R.id.c42,R.id.c43,R.id.c44,R.id.c45,R.id.c46},
    };
    private int[] colors = new int[] {R.color.yellow,R.color.colorPrimaryDark, R.color.colorAccent,
    R.color.wechat_green, R.color.action_back, R.color.red, R.color.light_blue
    };
    SparseArray<SparseArray> martrix2D = new SparseArray<>(5);//课程的二维矩阵
    ScheduleTableDataProvider provider;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.ui_schedule);
        tempWeek = currentWeek = TimeUtil.getWeekOfTerm(this);
        today = TimeUtil.getDayOfWeek();
        this.initActionBar();
        this.initTopBtn();
        this.initCursor();
        this.initScheduleTableByWeek();
    }

    private void initScheduleTableByWeek() {
        mScheduleTable =  findViewById(R.id.schedule_table);
        provider = new ScheduleTableDataProvider(mScheduleTable);
        provider.updateDataSource(currentWeek);
    }

    @Override
    public void onResume() {
        super.onResume();
        this.doDbTask();// init data
    }

    @Override
    public void onDbReadComplete(int taskId) {
        this.refreshViewPager(currentWeek);
        if (datalist.size() == 0)
            toastE(C.err.emptydata);
        //如果是首次，则回到默认为当天课程
    }

    protected void doDbTask() {
        this.showLoadBar();
        sqlite = new ScheduleSqlite(this);
        datalist.clear();
        datalist.addAll((ArrayList<Schedule>) sqlite.query(null, null, null));
        sendMessage(BaseTask.DB_READ_COMPLETE, C.task.db_schedule);
    }

    private void doTaskRefresh() {
        SharedPreferences mShared = AppUtil.getSharedPreferences(this);
        HashMap<String, String> urlParams = new HashMap<String, String>();
        urlParams.put("stuid", mShared.getString("stuid", null));
        urlParams.put("pass", mShared.getString("pass", null));
        try {
            this.doTaskAsync(C.task.schedule, C.api.schedule, urlParams);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    @Override
    public void onTaskComplete(int taskId, BaseMessage message) {
        switch (taskId) {
            case C.task.schedule:
                if (!message.isSuccess()) {
                    toastE("读取失败");
                    return;
                }
                toastS("读取成功");
                try {
                    datalist.clear();
                    datalist.addAll((ArrayList<Schedule>) message
                            .getResultList("Schedule"));
                    // 写库
                    sqlite = new ScheduleSqlite(this);
                    sqlite.updateAll(datalist);
                    updateTermDate(message.getMessage());
                    tempWeek = currentWeek = TimeUtil.getWeekOfTerm(this);
                    this.refreshViewPager(currentWeek);
                    tempWeek = currentWeek;
                    updateWidget();
                } catch (Exception e) {
                    e.printStackTrace();
                    toastE(C.err.server);
                }
                break;
        }
    }

    /**
     * 每次更新课表后都更新本学期周次信息
     */
    private void updateTermDate(String msg) {
        try {
            JSONObject jsonObject = new JSONObject(msg);
            SharedPreferences preferences = AppUtil.getSharedPreferences(this);
            Editor editor = preferences.edit();
            Iterator<String> it = jsonObject.keys();
            while (it.hasNext()) {
                String jsonKey = it.next();
                editor.putString(jsonKey, jsonObject.getString(jsonKey));
            }
            editor.commit();
        } catch (Exception e) {
            System.out.println("####error");
        }
    }

    // ////////////////////
    void initActionBar() {
        tv_weekthis = (TextView) findViewById(R.id.weektext);
        byDayTV = (TextView)findViewById(R.id.showbyday);
        byWeekTV = (TextView)findViewById(R.id.showbyweek);
        byDayTV.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(v.getId() == R.id.showbyday)
                    switchScheduleDisplayForm(SHOW_BY_WHAT.DAY);
            }
        });
        byWeekTV.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(v.getId() == R.id.showbyweek)
                    switchScheduleDisplayForm(SHOW_BY_WHAT.WEEK);
            }
        });
        findViewById(R.id.LEFT_MENU).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                forward(UiHome.class);
                doFinish();
            }
        });
        findViewById(R.id.REFRESH).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                doTaskRefresh();
            }
        });
        findViewById(R.id.THISWEEK).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                initPopup();
                popupManger.show(tv_weekthis);
            }
        });
    }


    private void switchScheduleDisplayForm(SHOW_BY_WHAT byWhat) {
        if(byWhat == curByWhat)
            return;
        curByWhat = byWhat;
        //TODO 加属性动画
        this.byDayTV.setBackgroundColor(getResources().getColor(R.color.actionbar_view_normal));
        this.byWeekTV.setBackgroundColor(getResources().getColor(R.color.actionbar_view_normal));
        if(byWhat == SHOW_BY_WHAT.DAY) {
            rightOut.setDuration(500);
            mScheduleTable.startAnimation(rightOut);
            mScheduleTable.setVisibility(View.GONE);
            leftIn.setDuration(500);
            mPager.startAnimation(leftIn);
            mPager.setVisibility(View.VISIBLE);
            this.byDayTV.setBackgroundColor(getResources().getColor(R.color.actionbar_view_pressed));

        } else {//按周显示
            leftOut.setDuration(500);
            mPager.startAnimation(leftOut);
            mPager.setVisibility(View.GONE);
            rightIn.setDuration(500);
            mScheduleTable.startAnimation(rightIn);
            mScheduleTable.setVisibility(View.VISIBLE);
            this.byWeekTV.setBackgroundColor(getResources().getColor(R.color.actionbar_view_pressed));
            //更新数据源
            provider.updateDataSource(currentWeek);
        }
    }

    // //////////////////
    private void initTopBtn() {

        int[] topIds = new int[]{R.id.text1, R.id.text2, R.id.text3,
                R.id.text4, R.id.text5, R.id.text6, R.id.text7};
        for (int i = 0; i < 7; i++) {
            btn_top[i] = (Button) findViewById(topIds[i]);
            btn_top[i].setOnClickListener(new MyTopListener(i));
        }
    }

    private class MyTopListener implements OnClickListener {
        int position;

        public MyTopListener(int i) {
            this.position = i;
        }

        @Override
        public void onClick(View v) {
            mPager.setCurrentItem(position, true); // smooth move true
        }
    }

    // ///////////////////////
    private void initCursor() {

        cursor = (ImageView) findViewById(R.id.cursor);
        // get cursor width
        bmpW = BitmapFactory.decodeResource(getResources(), R.drawable.cursor)
                .getWidth();
        DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);
        int screenW = dm.widthPixels; // 获取分辨率宽度
        offset = (screenW / 7 - bmpW) / 2; // 计算偏移量
        Matrix matrix = new Matrix();
        matrix.postTranslate(offset, 0);
        cursor.setImageMatrix(matrix);

    }

    private void scrollCursor(int from, int to) {
        int one = offset * 2 + bmpW;
        Animation animation = new TranslateAnimation(one * from, one * to, 0, 0);
        animation.setFillAfter(true);
        animation.setDuration(300);
        cursor.startAnimation(animation);
        this.currIndex = to;
        System.out.println("scrollCursor:" + from + "——>" + to);
    }

    // //////////////////////
    private void refreshViewPager(int week) {
        if (week < 1)
            this.tv_weekthis.setText("假期");
        else {
            String text = String.format("第 %d 周", week);
            this.tv_weekthis.setText(text);
        }

        mPager = (ViewPager) findViewById(R.id.pager);
        scrollViews = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            scrollViews.add(this.getLayout(R.layout.pager_schedule));
        }
        mPager.setAdapter(new SchedulePagerAdapter(this, scrollViews, datalist,
                week));
        mPager.setOnPageChangeListener(new MyChangeListener());
        if (countFirst++ == 0) {
            mPager.setCurrentItem(today, true);
            scrollCursor(currIndex, today);
        } else {
            mPager.setCurrentItem(currIndex, true);
            scrollCursor(currIndex, currIndex);
        }

    }

    private class MyChangeListener implements OnPageChangeListener {

        @Override
        public void onPageScrolled(int arg0, float arg1, int arg2) {

        }

        @Override
        public void onPageSelected(int arg0) {
            scrollCursor(currIndex, arg0);
        }

        @Override
        public void onPageScrollStateChanged(int arg0) {

        }
    }

    private void initPopup() {
        popupManger = new PopupManger(this);
        selectWeek.clear();
        int weeks = TimeUtil.getAllTermWeeks(this);
        for (int i = 1; i <= weeks; i++) {
            selectWeek.add("第" + i + "周");
        }
        popupManger.setAdapter(new ArrayAdapter(this,
                R.layout.item_list_popup_menu, selectWeek));
        popupManger.setItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {
                popupManger.dismiss();
                if(curByWhat == SHOW_BY_WHAT.DAY) {
                    refreshViewPager(position + 1);
                }
                else {
                    provider.updateDataSource(position + 1);
                }
            }
        });
        popupManger.initPopup();
    }

    class ScheduleTableDataProvider {

        View mScheduleTable;
        Map<String, Integer> course_colors = new HashMap<>();
        public ScheduleTableDataProvider(View mScheduleTable) {
            this.mScheduleTable = mScheduleTable;
            for(int i = 0; i < martrix2D.size(); i++)
                martrix2D.put(i, new SparseArray<Schedule>(7));
            updateDataSource(currentWeek);
        }

        private Schedule getSchedule(int row, int col) {
            if(martrix2D.get(row) != null)
                return (Schedule) martrix2D.get(row).get(col);
            return null;
        }

        private int getDisplayColorId(int row, int col) {
            //TODO 分配颜色
            return colors[((row * 6) + col) % colors.length];
        }

        private int getDisplayColorId(String courseName) {
            if(course_colors.get(courseName) == null)
                return getResources().getColor(R.color.red);
            return course_colors.get(courseName);
        }

        public void updateDataSource(int curWeek) {

            if (curWeek < 1)
                tv_weekthis.setText("假期");
            else {
                String text = String.format("第 %d 周", curWeek);
                tv_weekthis.setText(text);
            }

            if(martrix2D != null)
                martrix2D.clear();
            course_colors.clear();
            int colorCount = 0;
            for(Schedule item : datalist) {
                int day, no;
                day = Integer.valueOf(item.getWeekday()) - 1;
                no = (Integer.valueOf(item.getSeque())) / 2;
                if (!TimeUtil.judgeIsTime(item, curWeek)) { // 不再上课周内
                    continue;
                }
                if(!course_colors.containsKey(item.getName())) {
                    course_colors.put(item.getName(), colors[(colorCount++) % colors.length]);
                }
                if(martrix2D.get(no) == null)
                    martrix2D.put(no, new SparseArray<Schedule>(7));
                ((SparseArray<Schedule>)martrix2D.get(no)).put(day, item);
            }
            updateView();
        }

        private void updateView() {
            for(int i = 0; i < 5; i++)
                for(int j = 0; j < 7; j++) {
                    Schedule item = getSchedule(i, j);
                    TextView view = ((TextView)mScheduleTable.findViewById(rIds[i][j]));
                    if(item != null) {
                        view.setText(item.getName() + "\n@\n" + item.getPosition());
                        view.setBackgroundResource(getDisplayColorId(item.getName()));
                        Log.v("===", "colorid is " + getDisplayColorId(item.getName()));
                        Log.v("===", "colors[0] is " + colors[0]);
                    } else {
                        view.setText("");
                        view.setBackgroundResource(android.R.color.transparent);
                    }
                }
        }
    }
}

package com.siwe.dutschedule.adapter;

import android.os.Bundle;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.siwe.dutschedule.R;
import com.siwe.dutschedule.base.BaseList;
import com.siwe.dutschedule.base.BaseModel;
import com.siwe.dutschedule.base.BaseUi;
import com.siwe.dutschedule.model.AnotherNews;
import com.siwe.dutschedule.model.News;
import com.siwe.dutschedule.ui.UiNews;
import com.siwe.dutschedule.ui.UiWebView;
import com.siwe.dutschedule.view.GeneraListView;

import java.util.ArrayList;

public class NewsPagerAdapter extends PagerAdapter {

    private BaseUi baseUi;
    private ArrayList<ArrayList<News>> listNews;
    private ArrayList<View> pages;
    private int index = 0;
    private News item;
    private ArrayList<BaseAdapter> adapters = new ArrayList<>(3);

    public NewsPagerAdapter(BaseUi baseUi, ArrayList<View> pages,
                            ArrayList<ArrayList<News>> listNews) {
        this.listNews = listNews;
        this.baseUi = baseUi;
        this.pages = pages;
        this.initData();
    }

    private void initData() {
        for (index = 0; index < 3; index++) {
            ArrayList<News> news = listNews.get(index);
            if (news == null)
                return;
            GeneraListView lv = (GeneraListView) pages.get(index).findViewById(R.id.lv);
            this.adapters.add(index, new MyAdapter(baseUi, news, index));
            lv.setAdapter(this.adapters.get(index));
            if(index != 0) {
                lv.setOnItemClickListener(new OnItemClickListener() {

                    @Override
                    public void onItemClick(AdapterView<?> arg0, View arg1,
                                            int position, long arg3) {
                        News mitem = listNews.get(((UiNews) baseUi).currIndex).get(position - 1);
                        Bundle bd = new Bundle();
                        bd.putString("title", mitem.getTitle());
                        bd.putString("url", mitem.getUrl());
                        baseUi.forward(UiWebView.class, bd);
                    }

                });
            } else {//校内通知
                lv.setOnLoadMoreCallback((UiNews)baseUi);
                lv.setEnablePullUpLoadMore(true);
                lv.setOnItemClickListener(new OnItemClickListener() {

                    @Override
                    public void onItemClick(AdapterView<?> arg0, View arg1,
                                            int position, long arg3) {
                        if((position - 1) < 0 || (position - 1) >= listNews.get(0).size())
                            return;
                        AnotherNews news = (AnotherNews) listNews.get(0).get(position - 1);
                        Bundle bd = new Bundle();
                        bd.putString("title", news.getTitle());
                        bd.putString("id", news.getId());
                        bd.putString("lx", "1");
                        baseUi.forward(UiWebView.class, bd);
                    }
                });
            }
        }
    }

    public void notifyPageDataSetChanged(int index) {
        this.adapters.get(index).notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        // TODO Auto-generated method stub
        return pages.size();
    }

    @Override
    public boolean isViewFromObject(View arg0, Object arg1) {
        // TODO Auto-generated method stub
        return (arg0 == arg1);
    }

    public void destroyItem(View arg0, int arg1, Object arg2) {
        ((ViewPager) arg0).removeView(pages.get(arg1));
    }

    @Override
    public Object instantiateItem(View arg0, int arg1) {

        ((ViewPager) arg0).addView(pages.get(arg1), 0);
        return pages.get(arg1);
    }

    class MyAdapter extends BaseList {
        public int index;
        public MyAdapter(BaseUi ui, ArrayList<? extends BaseModel> datalist,int  index) {
            super(ui, datalist);
            this.index = index;
        }

        private class ViewHolder {
            TextView title;
            TextView time;
            TextView dept;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder;
            if (convertView == null) {
                convertView = ui.getLayout(R.layout.item_list_news);
                holder = new ViewHolder();
                holder.title = (TextView) convertView
                        .findViewById(R.id.textView1);
                holder.time = (TextView) convertView
                        .findViewById(R.id.textView2);
                if(index == 0) {
                    holder.dept = (TextView) convertView
                            .findViewById(R.id.dept);
                    holder.dept.setVisibility(View.VISIBLE);
                }
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }
            item = (News) datalist.get(position);
            holder.title.setText(item.getTitle());
            holder.time.setText(item.getUptime());
            if(index == 0) {
                holder.dept.setText(((AnotherNews)datalist.get(position)).getDept());
            }
            return convertView;
        }

    }
}

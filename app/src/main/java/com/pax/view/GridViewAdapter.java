/*
 * ============================================================================
 * COPYRIGHT
 *              Pax CORPORATION PROPRIETARY INFORMATION
 *   This software is supplied under the terms of a license agreement or
 *   nondisclosure agreement with Pax Corporation and may not be copied
 *   or disclosed except in accordance with the terms in that agreement.
 *      Copyright (C) 2016 - ? Pax Corporation. All rights reserved.
 * Module Date: 2016-12-1
 * Module Author: Steven.W
 * Description:
 *
 * ============================================================================
 */
package com.pax.view;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.pax.abl.core.AAction;
import com.pax.abl.core.ATransaction;
import com.pax.edc.R;
import com.pax.edc.expandablerecyclerview.BaseViewHolder;

import java.util.List;

class GridViewAdapter extends BaseAdapter {

    private List<GridItem> itemList;
    private Context context;

    GridViewAdapter(Context context, List<GridItem> list) {
        this.context = context;
        itemList = list;
    }

    @Override
    public int getCount() {
        return itemList.size();
    }

    @Override
    public Object getItem(int position) {
        return itemList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view;
        if (convertView == null) {
            view = LayoutInflater.from(context).inflate(R.layout.grid_item, parent, false);
        } else {
            view = convertView;
        }

        ImageView iv = BaseViewHolder.get(view, R.id.iv_item);
        TextView tv = BaseViewHolder.get(view, R.id.tv_item);

        tv.setText(getViewText(position));
        iv.setImageResource(getViewIcon(position));

        return view;
    }

    private Integer getViewIcon(int position) {
        GridItem holder = itemList.get(position);
        return holder.getIcon();
    }

    private String getViewText(int position) {
        GridItem holder = itemList.get(position);
        return holder.getName();
    }

    static class GridItem {

        private String name;
        private int icon;
        private ATransaction trans;
        private Class<?> activity;
        private AAction action;
        private Intent intent;
        private int level = 0;

        GridItem(String name, int icon) {
            this.name = name;
            this.icon = icon;
//            this.trans = trans;
        }

        GridItem(String name, int icon, ATransaction trans) {
            this.name = name;
            this.icon = icon;
            this.trans = trans;
        }

        GridItem(String name, int icon, Class<?> act) {
            this.name = name;
            this.icon = icon;
            this.activity = act;
        }

        GridItem(String name, int icon, Class<?> act, int level) {
            this.name = name;
            this.icon = icon;
            this.activity = act;
            this.level = level;
        }

        GridItem(String name, int icon, AAction action) {
            this.name = name;
            this.icon = icon;
            this.action = action;
        }

        GridItem(String name, int icon, Intent intent) {
            this.name = name;
            this.icon = icon;
            this.intent = intent;
        }

        public int getIcon() {
            return icon;
        }

        public void setIcon(int icon) {
            this.icon = icon;
        }

        public ATransaction getTrans() {
            return trans;
        }

        public void setTrans(ATransaction trans) {
            this.trans = trans;
        }

        public Class<?> getActivity() {
            return activity;
        }

        public void setActivity(Class<?> act) {
            this.activity = act;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public AAction getAction() {
            return action;
        }

        public void setAction(AAction action) {
            this.action = action;
        }

        public void setIntent(Intent intent) {
            this.intent = intent;
        }

        public Intent getIntent() {
            return intent;
        }

        public int getLevel() {
            return level;
        }

    }

}

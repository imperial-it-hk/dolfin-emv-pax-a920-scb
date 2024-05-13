/*
 * ============================================================================
 * COPYRIGHT
 *              Pax CORPORATION PROPRIETARY INFORMATION
 *   This software is supplied under the terms of a license agreement or
 *   nondisclosure agreement with Pax Corporation and may not be copied
 *   or disclosed except in accordance with the terms in that agreement.
 *      Copyright (C) 2016 - ? Pax Corporation. All rights reserved.
 * Module Date: 2016-11-30
 * Module Author: Steven.W
 * Description:
 *
 * ============================================================================
 */
package com.pax.settings;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.pax.edc.R;
import com.pax.edc.expandablerecyclerview.BaseViewHolder;

import java.util.List;

public class SettingMainMenuListAdapter extends BaseAdapter {
    private List<Item> mListItems;
    private LayoutInflater mListContainer;
    private int selectItem = -1;

    public static final class Item {
        int redId;
        Class<?> cls;

        public Item(int redId, Class<?> cls) {
            this.redId = redId;
            this.cls = cls;
        }
    }

    public SettingMainMenuListAdapter(Context context, List<Item> listItems) {
        mListContainer = LayoutInflater.from(context);
        this.mListItems = listItems;
    }

    @Override
    public int getCount() {
        return mListItems.size();
    }

    @Override
    public Object getItem(int position) {
        return mListItems.get(position);
    }

    @Override
    public long getItemId(int position) {
        return mListItems.get(position).redId;
    }

    public void setSelectItem(int selectItem) {
        this.selectItem = selectItem;
    }

    /**
     * ListView Item
     */
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view;
        if (convertView == null) {
            view = mListContainer.inflate(R.layout.setting_main_menu_list_item, parent, false);
        } else {
            view = convertView;
        }

        TextView paraName = BaseViewHolder.get(view, R.id.para_name);

        String text = view.getResources().getString(mListItems.get(position).redId);
        paraName.setText(text);

        if (position != selectItem) {
            view.setBackgroundColor(view.getResources().getColor(R.color.primary));
            paraName.setTextColor(Color.WHITE);
        } else {
            view.setBackgroundColor(Color.WHITE);
            paraName.setTextColor(Color.BLACK);
        }

        return view;
    }
}

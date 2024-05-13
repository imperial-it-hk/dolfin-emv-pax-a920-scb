/*
 * ============================================================================
 * COPYRIGHT
 *              Pax CORPORATION PROPRIETARY INFORMATION
 *   This software is supplied under the terms of a license agreement or
 *   nondisclosure agreement with Pax Corporation and may not be copied
 *   or disclosed except in accordance with the terms in that agreement.
 *      Copyright (C) 2017 - ? Pax Corporation. All rights reserved.
 * Module Date: 2017-2-8
 * Module Author: Kim.L
 * Description:
 *
 * ============================================================================
 */
package com.pax.settings;

import android.content.Context;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.pax.edc.R;
import com.pax.edc.expandablerecyclerview.BaseViewHolder;

import java.util.ArrayList;
import java.util.List;

public class NewSpinnerAdapter<T> extends BaseAdapter {
    private Context mContext;
    private List<T> list = new ArrayList<>();
    private OnTextUpdateListener listener;

    public NewSpinnerAdapter(Context context) {
        super();
        mContext = context;
    }

    @Override
    public int getCount() {
        return list.size();
    }

    @Override
    public Object getItem(int position) {
        return list.get(position);
    }

    @Override
    public long getItemId(int arg0) {
        return arg0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view;
        if (null == convertView) {
            view = LayoutInflater.from(mContext).inflate(R.layout.new_list_item, parent, false);
        } else {
            view = convertView;
        }

        TextView name = BaseViewHolder.get(view, R.id.new_item_name);
        name.setGravity(Gravity.CENTER | Gravity.START);

        if (listener != null) {
            String str = listener.onTextUpdate(list, position);
            if (str != null) {
                name.setText(str);
            }
        }

        return view;
    }

    public List<T> getListInfo() {
        return list;
    }

    public T getListInfo(int pos) {
        return list.get(pos);
    }

    public void setListInfo(List<T> infos) {
        if (infos != null) {
            list.clear();
            list.addAll(infos);
            notifyDataSetChanged();
        }
    }

    public void updateItem(int pos, T element) {
        if (element != null) {
            try {
                list.set(pos, element);
            } catch (Exception e) {
                // do nothing
            }
        }
    }

    public void setOnTextUpdateListener(OnTextUpdateListener listener) {
        this.listener = listener;
    }

    public interface OnTextUpdateListener {
        String onTextUpdate(final List<?> list, int position);
    }
}

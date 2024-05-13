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

import th.co.bkkps.utils.Log;
import android.view.View;
import android.view.ViewGroup;
import androidx.viewpager.widget.PagerAdapter;

import java.util.List;

class ViewPagerAdapter extends PagerAdapter {

    private static final String TAG = "ViewPagerAdapter";
    private List<View> lists;

    ViewPagerAdapter(List<View> data) {
        lists = data;
    }

    @Override
    public int getCount() {
        return lists.size();
    }

    @Override
    public boolean isViewFromObject(View arg0, Object arg1) {
        return arg0 == arg1;
    }

    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        try {
            // 解决View只能滑动两屏的方法
            ViewGroup parent = (ViewGroup) lists.get(position).getParent();
            if (parent != null)
                parent.removeView(lists.get(position));

            if (container != null) {
                container.addView(lists.get(position), 0);
            }
        } catch (Exception e) {
            Log.e(TAG, "", e);
        }

        return lists.get(position);
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        try {
            container.removeView(lists.get(position));
        } catch (Exception e) {
            Log.e(TAG, "", e);
        }
    }

}

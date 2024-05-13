/*
 * ============================================================================
 * COPYRIGHT
 *              Pax CORPORATION PROPRIETARY INFORMATION
 *   This software is supplied under the terms of a license agreement or
 *   nondisclosure agreement with Pax Corporation and may not be copied
 *   or disclosed except in accordance with the terms in that agreement.
 *      Copyright (C) 2016 - ? Pax Corporation. All rights reserved.
 * Module Date: 2016-11-27
 * Module Author: Steven.W
 * Description:
 *
 * ============================================================================
 */
package com.pax.pay.utils;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Typeface;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;

import com.pax.edc.R;

import static android.util.TypedValue.COMPLEX_UNIT_PX;

public class ViewUtils {

    private ViewUtils() {
        //do nothing
    }

    /**
     * 生成每一行记录
     *
     * @param title
     * @param value
     * @return
     */
    public static LinearLayout genSingleLineLayout(Context context, String title, Object value) {
        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.HORIZONTAL);
        TextView titleTv = new TextView(context);
        titleTv.setText(title);
        titleTv.setTextSize(COMPLEX_UNIT_PX, context.getResources().getDimension(R.dimen.font_size_prompt));
        titleTv.setTextColor(context.getResources().getColor(android.R.color.primary_text_light));

        layout.addView(titleTv, new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT));

        TextView valueTv = new TextView(context);
        valueTv.setText(String.valueOf(value));
        valueTv.setGravity(Gravity.END);
        valueTv.setTextSize(COMPLEX_UNIT_PX, context.getResources().getDimension(R.dimen.font_size_value));

        layout.addView(valueTv, new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT));

        return layout;
    }

    /**
     * 生成每一行记录
     *
     * @param title
     * @param value
     * @return
     */
    public static LinearLayout genSingleLineLayout(Context context, String title, String value) {
        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.HORIZONTAL);
        TextView titleTv = new TextView(context);
        titleTv.setText(title);
        titleTv.setTextSize(COMPLEX_UNIT_PX, context.getResources().getDimension(R.dimen.font_size_prompt));
        titleTv.setTextColor(context.getResources().getColor(android.R.color.primary_text_light));

        layout.addView(titleTv, new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT));

        TextView valueTv = new TextView(context);
        valueTv.setText(value);
        valueTv.setGravity(Gravity.END);
        valueTv.setTextSize(COMPLEX_UNIT_PX, context.getResources().getDimension(R.dimen.font_size_value));

        layout.addView(valueTv, new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT));

        return layout;
    }

    /**
     * 生成每一行记录
     *
     * @param title
     * @return
     */
    public static LinearLayout genSingleLineLayoutHeader(Context context, String title) {
        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.HORIZONTAL);
        TextView titleTv = new TextView(context);
        titleTv.setText(title);
        titleTv.setText(title);
        titleTv.setTextSize(COMPLEX_UNIT_PX, context.getResources().getDimension(R.dimen.font_size_prompt));
        titleTv.setTextColor(context.getResources().getColor(android.R.color.primary_text_light));
        titleTv.setTypeface(titleTv.getTypeface(), Typeface.BOLD);

        layout.addView(titleTv, new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT));

        return layout;
    }

    /**
     * 得到设备屏幕的宽度
     */
    public static int getScreenWidth(Context context) {
        return context.getResources().getDisplayMetrics().widthPixels;
    }

    /**
     * 得到设备屏幕的高度
     */
    public static int getScreenHeight(Context context) {
        return context.getResources().getDisplayMetrics().heightPixels;
    }

    public static boolean isScreenOrientationPortrait(Context context) {
        return context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT;
    }

    /**
     * 密度转换为像素值
     */
    public static float dp2Px(Context context, int dp) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dp * scale / 160);
    }

    public static float px2Dp(Context context, int px) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (px / (scale / 160));
    }
}

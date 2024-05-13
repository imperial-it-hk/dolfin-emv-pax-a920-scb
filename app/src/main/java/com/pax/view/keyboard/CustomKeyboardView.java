/*
 * ============================================================================
 * COPYRIGHT
 *              Pax CORPORATION PROPRIETARY INFORMATION
 *   This software is supplied under the terms of a license agreement or
 *   nondisclosure agreement with Pax Corporation and may not be copied
 *   or disclosed except in accordance with the terms in that agreement.
 *      Copyright (C) 2017 - ? Pax Corporation. All rights reserved.
 * Module Date: 2017-2-21
 * Module Author: Kim.L
 * Description:
 *
 * ============================================================================
 */
package com.pax.view.keyboard;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.inputmethodservice.Keyboard;
import android.inputmethodservice.KeyboardView;
import android.util.AttributeSet;
import th.co.bkkps.utils.Log;
import android.view.MotionEvent;
import androidx.core.content.ContextCompat;
import com.pax.edc.R;

import java.lang.reflect.Method;
import java.util.List;

public class CustomKeyboardView extends KeyboardView {

    private Drawable mKeyBgDrawable;
    private Drawable mOpKeyBgDrawable;

    private Paint paint = new Paint();

    private Context mContext;

    public CustomKeyboardView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CustomKeyboardView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mContext = context;
        initResources();
    }

    private void initResources() {
        mKeyBgDrawable = ContextCompat.getDrawable(mContext, R.drawable.btn_bg_dark);
        mOpKeyBgDrawable = ContextCompat.getDrawable(mContext, R.drawable.btn_bg_light);
    }

    @SuppressLint({"DrawAllocation"})
    @Override
    public void onDraw(Canvas canvas) {
        List<Keyboard.Key> keys = getKeyboard().getKeys();
        for (Keyboard.Key key : keys) {
            canvas.save();

            int offsetY = 0;
            if (key.y == 0) {
                offsetY = 1;
            }
            int initDrawY = key.y + offsetY;
            Rect rect = new Rect(key.x, initDrawY, key.x + key.width, key.y + key.height);
            canvas.clipRect(rect);
            drawIcon(canvas, key, rect);
            drawText(canvas, key, initDrawY);
            canvas.restore();
        }
    }

    private void drawIcon(Canvas canvas, Keyboard.Key key, Rect rect) {
        Drawable drawable = null;
        if (key.codes != null && key.codes.length != 0) {
            drawable = key.codes[0] < 0 ? mOpKeyBgDrawable : mKeyBgDrawable;
        }

        if (drawable != null && null == key.icon) {
            int[] state = key.getCurrentDrawableState();
            drawable.setState(state);
            drawable.setBounds(rect);
            drawable.draw(canvas);
        }

        if (key.icon != null) {
            int[] state = key.getCurrentDrawableState();
            key.icon.setState(state);
            key.icon.setBounds(rect);
            key.icon.draw(canvas);
        }
    }

    private void drawText(Canvas canvas, Keyboard.Key key, int initDrawY) {
        paint.setAntiAlias(true);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTextSize(mContext.getResources().getDimension(R.dimen.font_size_key));
        paint.setColor(key.codes[0] < 0 ? Color.WHITE : Color.BLACK);

        if (key.label != null) {
            canvas.drawText(
                    key.label.toString(),
                    key.x + (key.width / 2),
                    initDrawY + (key.height + paint.getTextSize() - paint.descent()) / 2,
                    paint
            );
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent me) {
        //AET-245 246
        //just ignore all multi-touch
        return me.getPointerCount() > 1 || onModifiedTouchEvent(me, true);
    }

    private boolean onModifiedTouchEvent(MotionEvent me, boolean possiblePoly) {
        try {
            Method method = getClass().getSuperclass().getDeclaredMethod("onModifiedTouchEvent", me.getClass(), boolean.class);
            method.setAccessible(true);
            return (boolean) method.invoke(this, me, possiblePoly);
        } catch (Exception e) {
            Log.e("CKV", "", e);
        }
        return false;
    }
}

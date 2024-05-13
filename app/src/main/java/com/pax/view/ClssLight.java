/*
 * ============================================================================
 * COPYRIGHT
 *              Pax CORPORATION PROPRIETARY INFORMATION
 *   This software is supplied under the terms of a license agreement or
 *   nondisclosure agreement with Pax Corporation and may not be copied
 *   or disclosed except in accordance with the terms in that agreement.
 *      Copyright (C) 2017 - ? Pax Corporation. All rights reserved.
 * Module Date: 2017-11-23
 * Module Author: Kim.L
 * Description:
 *
 * ============================================================================
 */

package com.pax.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.animation.Animation;
import android.widget.ImageView;
import androidx.annotation.DrawableRes;
import androidx.annotation.IntDef;
import androidx.annotation.Nullable;
import com.pax.edc.R;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public class ClssLight extends ImageView {

    public final static int OFF = 0;
    public final static int ON = 1;
    public final static int BLINK = 2;

    @IntDef({OFF, ON, BLINK})
    @Retention(RetentionPolicy.SOURCE)
    public @interface STATUS {
    }

    @DrawableRes
    private int[] statusSrc = new int[]{-1, -1};

    @STATUS
    private int status = OFF;

    public ClssLight(Context context) {
        this(context, null);
    }

    public ClssLight(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ClssLight(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        clearAnimation();
    }

    private void init(Context context, @Nullable AttributeSet attrs) {
        TypedArray array = context.obtainStyledAttributes(attrs, R.styleable.ClssLight);
        statusSrc[OFF] = array.getResourceId(R.styleable.ClssLight_offSrc, -1);
        statusSrc[ON] = array.getResourceId(R.styleable.ClssLight_onSrc, -1);
        array.recycle();
    }

    public void setStatus(@STATUS int status, final Animation blinking){
        this.status = status;
        if (status == BLINK) {
            setImageResource(statusSrc[ON]);
            startAnimation(blinking);
        } else {
            clearAnimation();
            setImageResource(statusSrc[status]);
        }
    }
}

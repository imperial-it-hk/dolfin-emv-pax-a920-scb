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
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.LinearLayout;
import androidx.annotation.IntRange;
import androidx.annotation.Nullable;
import com.pax.device.Device;
import com.pax.edc.R;

public class ClssLightsView extends LinearLayout {

    private ClssLight[] lights = new ClssLight[4];

    private AlphaAnimation blinking;

    private class LedThread extends Thread{
        private int index;

        LedThread(int index){
            this.index = index;
        }

        @Override
        public void run() {
            while (lights[index].getAnimation() != null){
                Device.setPiccLed(index, ClssLight.ON);
                SystemClock.sleep(300);
                Device.setPiccLed(index, ClssLight.OFF);
                SystemClock.sleep(300);
            }
        }
    }

    private LedThread ledThread = null;

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        Device.setPiccLed(-1, ClssLight.OFF);
    }

    public ClssLightsView(Context context) {
        this(context, null);
    }

    public ClssLightsView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ClssLightsView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        LayoutInflater mInflater = LayoutInflater.from(context);
        View myView = mInflater.inflate(R.layout.clss_light_layout, null);
        LayoutParams parentParams = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        parentParams.setLayoutDirection(HORIZONTAL);
        addView(myView, parentParams);

        init();
    }

    private void init(){
        blinking = new AlphaAnimation(1, 0);
        blinking.setDuration(500);
        blinking.setRepeatCount(Animation.INFINITE);
        blinking.setRepeatMode(Animation.REVERSE);

        lights[0] = (ClssLight) findViewById(R.id.light1);
        lights[1] = (ClssLight) findViewById(R.id.light2);
        lights[2] = (ClssLight) findViewById(R.id.light3);
        lights[3] = (ClssLight) findViewById(R.id.light4);
    }

    public void setLights(final @IntRange(from = -1, to = 3) int index, @ClssLight.STATUS int status){

        if(ledThread != null && ledThread.isAlive()){
            ledThread.interrupt();
        }

        for (int i = 0; i < lights.length; ++i) {
            if (index == i) {
                lights[i].setStatus(status, blinking);
                Device.setPiccLed(index, ClssLight.ON);
                ledThread = new LedThread(index);
                ledThread.start();
            } else {
                lights[i].setStatus(ClssLight.OFF, null);
            }
        }
    }
}

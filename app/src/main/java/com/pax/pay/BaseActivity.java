/*
 * ============================================================================
 * COPYRIGHT
 *              Pax CORPORATION PROPRIETARY INFORMATION
 *   This software is supplied under the terms of a license agreement or
 *   nondisclosure agreement with Pax Corporation and may not be copied
 *   or disclosed except in accordance with the terms in that agreement.
 *      Copyright (C) 2016 - ? Pax Corporation. All rights reserved.
 * Module Date: 2016-11-25
 * Module Author: Steven.W
 * Description:
 *
 * ============================================================================
 */
package com.pax.pay;

import android.os.Bundle;
import th.co.bkkps.utils.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import com.pax.edc.R;
import com.pax.pay.app.ActivityStack;
import com.pax.pay.app.quickclick.QuickClickProtection;
import com.pax.pay.tcpsocket.SocketServer;

import java.lang.ref.WeakReference;
import java.lang.reflect.Method;

public abstract class BaseActivity extends AppCompatActivity implements View.OnClickListener {

    protected static final String TAG = "The Activity";
    protected QuickClickProtection quickClickProtection = QuickClickProtection.getInstance();
    private ActionBar mActionBar;

    protected SocketServer socketServer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE);
        setContentView(getLayoutId());
        WeakReference<BaseActivity> weakReference = new WeakReference<>(this);
        ActivityStack.getInstance().push(weakReference.get());

        loadParam(); //AET-274

        mActionBar = getSupportActionBar();
        if (mActionBar != null) {
            mActionBar.setTitle(getTitleString());
            mActionBar.setDisplayHomeAsUpEnabled(false);
            mActionBar.setDisplayShowTitleEnabled(true);
        }

        enableBackAction(true);

        initViews();

        setListeners();
    }

    @Override
    protected void onDestroy() {
        if (this.equals(ActivityStack.getInstance().top())) {
            ActivityStack.getInstance().pop();
        }
        super.onDestroy();
        if (quickClickProtection != null) {quickClickProtection.stop();}
        if(socketServer != null) {
            socketServer.onDestroy();
        }
        Log.i(TAG,"onDestroy", "");
    }

    /**
     * get layout ID
     *
     * @return
     */
    protected abstract int getLayoutId();

    /**
     * views initial
     */
    protected abstract void initViews();

    /**
     * set listeners
     */
    protected abstract void setListeners();

    /**
     * load parameter
     */
    protected abstract void loadParam();

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (menu != null && menu.getClass().getSimpleName().equals("MenuBuilder")) {
            try {
                Method m = menu.getClass().getDeclaredMethod("setOptionalIconsVisible", Boolean.TYPE);
                m.setAccessible(true);
                m.invoke(menu, true);
            } catch (Exception e) {
                Log.e(getClass().getSimpleName(), "onMenuOpened...unable to set icons for overflow menu", e);
            }
        }
        return super.onPrepareOptionsMenu(menu);
    }

    // AET-93
    @Override
    public final void onClick(View v) {
        if (quickClickProtection.isStarted()) {
            return;
        }
        quickClickProtection.start();
        onClickProtected(v);
    }

    protected void onClickProtected(View v) {
        //do nothing
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (quickClickProtection.isStarted()) { //AET-123
            return true;
        }
        quickClickProtection.start();
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            return onKeyBackDown();
        }
        return super.onKeyDown(keyCode, event);
    }

    protected boolean onKeyBackDown() {
        finish();
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (quickClickProtection.isStarted()) { //AET-123
            return true;
        }
        quickClickProtection.start();
        return onOptionsItemSelectedSub(item);
    }

    protected boolean onOptionsItemSelectedSub(MenuItem item) {
        return super.onOptionsItemSelected(item);
    }

    protected String getTitleString() {
        return getString(R.string.app_name);
    }

    public void setTitleString(String title) {
        if (mActionBar != null) {
            mActionBar.setTitle(title);
        }
    }

    protected void enableBackAction(boolean enableBack) {
        if (mActionBar != null)
            mActionBar.setDisplayHomeAsUpEnabled(enableBack);
    }

    private boolean isShowTitleBar =true;
    protected void enableDisplayTitle(boolean enableShowTitle) {
        isShowTitleBar=enableShowTitle;
        if (mActionBar != null) {
            mActionBar.setDisplayShowTitleEnabled(isShowTitleBar);
        }
    }


    /**
     * 设置是否显示ActionBar
     *
     * @param showActionBar true 显示 false 隐藏
     */
    protected void enableActionBar(boolean showActionBar) {
        if (mActionBar != null) {
            if (showActionBar) {
                mActionBar.show();
            } else {
                mActionBar.hide();
            }
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);

        if (hasFocus) {
            View decorView = getWindow().getDecorView();
            decorView.setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            );
        }
    }
}

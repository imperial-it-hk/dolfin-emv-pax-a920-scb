/*
 * ============================================================================
 * COPYRIGHT
 *              Pax CORPORATION PROPRIETARY INFORMATION
 *   This software is supplied under the terms of a license agreement or
 *   nondisclosure agreement with Pax Corporation and may not be copied
 *   or disclosed except in accordance with the terms in that agreement.
 *      Copyright (C) 2017 - ? Pax Corporation. All rights reserved.
 * Module Date: 2017-2-22
 * Module Author: Kim.L
 * Description:
 *
 * ============================================================================
 */
package com.pax.settings;

import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.Nullable;
import com.pax.pay.app.quickclick.QuickClickProtection;

public abstract class BaseFragment extends Fragment implements View.OnClickListener {

    protected static final String TAG = "The Fragment";

    protected Context context;

    private QuickClickProtection quickClickProtection;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(getLayoutId(), container, false);
        context = view.getContext();
        quickClickProtection = QuickClickProtection.getInstance();
        initData();
        initView(view);

        return view;
    }

    /**
     * get layout ID
     *
     * @return
     */
    protected abstract int getLayoutId();

    protected abstract void initData();

    protected abstract void initView(View view);

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
}

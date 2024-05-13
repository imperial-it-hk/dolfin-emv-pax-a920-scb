/*
 * ============================================================================
 * COPYRIGHT
 *              Pax CORPORATION PROPRIETARY INFORMATION
 *   This software is supplied under the terms of a license agreement or
 *   nondisclosure agreement with Pax Corporation and may not be copied
 *   or disclosed except in accordance with the terms in that agreement.
 *      Copyright (C) 2017 - ? Pax Corporation. All rights reserved.
 * Module Date: 2017-2-9
 * Module Author: Kim.L
 * Description:
 *
 * ============================================================================
 */
package com.pax.pay.utils;

import android.text.Editable;
import android.text.TextWatcher;

import com.pax.device.Device;

public class TextValueWatcher<T> implements TextWatcher {

    private final T minValue;
    private final T maxValue;
    private boolean mEditing;
    private OnCompareListener compareListener;
    private OnTextChangedListener textChangedListener;

    public TextValueWatcher(T minValue, T maxValue) {
        mEditing = false;
        this.minValue = minValue;
        this.maxValue = maxValue;
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        //do nothing
    }

    @Override
    public void afterTextChanged(Editable s) {
        if (!mEditing) {
            mEditing = true;
            String temp = s.toString();
            if (temp.isEmpty())
                temp = "0";
            if (compareListener != null && compareListener.onCompare(temp, minValue, maxValue)) {
                if (textChangedListener != null)
                    textChangedListener.afterTextChanged(temp);
            } else {
                Device.beepErr();
                s.replace(0, s.length(), temp, 0, temp.length() - 1);
            }
            mEditing = false;
        }
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        //do nothing
    }

    public void setOnCompareListener(OnCompareListener listener) {
        this.compareListener = listener;
    }

    public interface OnCompareListener {
        boolean onCompare(String value, Object min, Object max);
    }

    public void setOnTextChangedListener(OnTextChangedListener listener) {
        this.textChangedListener = listener;
    }

    public interface OnTextChangedListener {
        void afterTextChanged(String value);
    }

}

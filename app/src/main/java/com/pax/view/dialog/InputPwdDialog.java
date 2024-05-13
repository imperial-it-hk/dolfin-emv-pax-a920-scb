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
package com.pax.view.dialog;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Typeface;
import android.inputmethodservice.Keyboard;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.InputType;
import android.text.method.PasswordTransformationMethod;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.TextView;

import com.pax.edc.R;
import com.pax.pay.utils.EditorActionListener;
import com.pax.view.keyboard.CustomKeyboardView;
import com.pax.view.keyboard.KeyboardUtils;

public class InputPwdDialog extends Dialog {

    private String title; // 标题
    private String prompt; // 提示信息

    private EditText pwdEdt;
    private int maxLength;

    private OnPwdListener listener;

    public InputPwdDialog(Context context, int length, String title, String prompt) {
        this(context, R.style.PopupDialog);
        this.maxLength = length;
        this.title = title;
        this.prompt = prompt;
    }

    /**
     * 输联机密码时调用次构造方法
     *
     * @param context
     * @param title
     * @param prompt
     */
    public InputPwdDialog(Context context, String title, String prompt) {
        super(context, R.style.PopupDialog);
        this.title = title;
        this.prompt = prompt;
    }

    public InputPwdDialog(Context context, int theme) {
        super(context, theme);

    }

    public interface OnPwdListener {
        void onSucc(String data);

        void onErr();
    }

    public void setPwdListener(OnPwdListener listener) {
        this.listener = listener;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        View convertView = getLayoutInflater().inflate(R.layout.activity_inner_pwd_layout, null);
        setContentView(convertView);
        if (getWindow() == null)
            return;
        getWindow().setGravity(Gravity.BOTTOM); // 显示在底部
        getWindow().getDecorView().setPadding(0, 0, 0, 0);
        WindowManager.LayoutParams lp = getWindow().getAttributes();
        lp.width = WindowManager.LayoutParams.MATCH_PARENT;
        lp.height = WindowManager.LayoutParams.WRAP_CONTENT;//(int) (ViewUtils.getScreenHeight(this.getContext()) * 0.6);  // 屏幕高度（像素）

        getWindow().setAttributes(lp);
        initViews(convertView);
    }

    private void initViews(View view) {

        TextView titleTv = (TextView) view.findViewById(R.id.prompt_title);
        titleTv.setText(title);

        TextView subtitleTv = (TextView) view.findViewById(R.id.prompt_no_pwd);
        if (prompt != null) {
            subtitleTv.setText(prompt);
        } else {
            subtitleTv.setVisibility(View.GONE);
        }

        TextView pwdTv = (TextView) view.findViewById(R.id.pwd_input_text);
        pwdTv.setVisibility(View.GONE);
        pwdEdt = (EditText) view.findViewById(R.id.pwd_input_et);
        pwdEdt.setFilters(new InputFilter[]{new InputFilter.LengthFilter(maxLength)});

        KeyboardUtils.hideSystemKeyboard(getContext(), pwdEdt);
        pwdEdt.setInputType(InputType.TYPE_NULL);
        pwdEdt.setFocusable(true);
        pwdEdt.setTransformationMethod(PasswordTransformationMethod.getInstance());
        pwdEdt.setTypeface(Typeface.MONOSPACE, 0);

        Keyboard keyboard = new Keyboard(view.getContext(), R.xml.numeric_keyboard_confirm);

        CustomKeyboardView keyboardView = (CustomKeyboardView) view.findViewById(R.id.pwd_keyboard);
        KeyboardUtils.bind(keyboardView, new KeyboardUtils(view.getContext(), keyboard, pwdEdt));

        pwdEdt.setOnEditorActionListener(new PwdActionListener());
    }

    private class PwdActionListener extends EditorActionListener {
        @Override
        public void onKeyOk() {
            String content = pwdEdt.getText().toString().trim();
            if (listener != null) {
                listener.onSucc(content);
            }
        }

        @Override
        public void onKeyCancel() {
            if (listener != null) {
                listener.onErr();
            }
        }
    }
}

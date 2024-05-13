package com.pax.glwrapper.impl;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.AbsoluteSizeSpan;
import android.text.style.StyleSpan;
import android.text.style.UnderlineSpan;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.pax.glwrapper.page.IPage;

import java.util.List;


//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
// from PaxGLPage
//

class ViewConverter {
    private Context mContext;

    ViewConverter(Context context) {
        this.mContext = context;
    }

    Context getContext() {
        return mContext;
    }

    final View page2View(Context context, IPage page, List<IPage.ILine> lines, int pageWidth) {
        ScrollView scrollView = new ScrollView(context);
        LinearLayout.LayoutParams lpScrollView = new LinearLayout.LayoutParams(pageWidth, ViewGroup.LayoutParams.WRAP_CONTENT);
        scrollView.setLayoutParams(lpScrollView);
        scrollView.setBackgroundColor(Color.WHITE);
        LinearLayout linearLayout = new LinearLayout(context);
        linearLayout.setLayoutParams(new LinearLayout.LayoutParams(pageWidth, ViewGroup.LayoutParams.WRAP_CONTENT));
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        linearLayout.setBackgroundColor(Color.WHITE);
        int width = 0;

        for (IPage.ILine line : lines) {
            ++width;
            LinearLayout lineLayout = new LinearLayout(context);
            lineLayout.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            lineLayout.setOrientation(LinearLayout.HORIZONTAL);
            lineLayout.setGravity(android.view.Gravity.CENTER_VERTICAL);

            for (IPage.ILine.IUnit unit : line.getUnits()) {
                float weight = unit.getWeight();
                Bitmap bitmap = unit.getBitmap();
                String text = unit.getText();
                TextView textView;
                if (text != null && text.length() > 0) {
                    textView = new TextView(context);
                    textView.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, weight));
                    if (page.getTypefaceObj() != null) {
                        try {
                            textView.setTypeface(page.getTypefaceObj());
                        } catch (Exception e) {
                            Log.e("ViewConverter", "", e);
                        }
                    }

                    SpannableString ss = new SpannableString(unit.getText());
                    ss.setSpan(new AbsoluteSizeSpan(unit.getFontSize()), 0, ss.toString().length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    ss.setSpan(new StyleSpan(unit.getTextStyle() & 0x0F), 0, ss.toString().length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    if ((unit.getTextStyle() & 0xF0) == IPage.ILine.IUnit.UNDERLINE) {
                        ss.setSpan(new UnderlineSpan(), 0, ss.toString().length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    }
                    textView.setText(ss);
                    textView.setTextColor(Color.BLACK);
                    textView.setTextSize((float) unit.getFontSize());
                    textView.setGravity(unit.getGravity());

                    lineLayout.addView(textView);
                } else if (bitmap != null) {
                    ImageView imageView = new ImageView(context);
                    LinearLayout.LayoutParams lpImageView = new LinearLayout.LayoutParams(bitmap.getWidth(), bitmap.getHeight(), 0.0F);
                    lpImageView.setMargins(0, 10, 0, 10);
                    imageView.setLayoutParams(lpImageView);
                    imageView.setScaleType(ImageView.ScaleType.CENTER);
                    lineLayout.setGravity(unit.getGravity());

                    imageView.setImageBitmap(bitmap);
                    lineLayout.addView(imageView);
                }
            }

            lineLayout.setLayoutDirection(View.LAYOUT_DIRECTION_LOCALE);
            linearLayout.addView(lineLayout);
            if (width != 1) {
                int spaceAdjustment = page.getLineSpaceAdjustment();
                int topSpaceAdjustment;
                if ((topSpaceAdjustment = line.getTopSpaceAdjustment()) != '\uffff') {
                    spaceAdjustment = topSpaceAdjustment;
                }

                LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) lineLayout.getLayoutParams();
                layoutParams.topMargin = spaceAdjustment;
                lineLayout.setLayoutParams(layoutParams);
            }
        }

        scrollView.addView(linearLayout);
        return scrollView;
    }
}


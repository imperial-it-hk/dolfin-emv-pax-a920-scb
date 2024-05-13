package com.pax.glwrapper.page;

//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
// from PaxGLPage
//

import android.graphics.Bitmap;
import android.graphics.Typeface;

import java.util.List;

public interface IPage {

    ILine addLine();

    ILine addLine(ILine line);

    ILine.IUnit createUnit();

    List<ILine> getLines();

    Typeface getTypefaceObj();

    void setTypefaceObj(Typeface typeface);

    void adjustLineSpace(int spacingAdd);

    int getLineSpaceAdjustment();

    String getTypeface();

    void setTypeface(String var1);

    public static enum EAlign {
        LEFT,
        CENTER,
        RIGHT;

        private EAlign() {
        }
    }

    interface ILine {
        List<ILine.IUnit> getUnits();

        ILine addUnit(ILine.IUnit unit);

        ILine adjustTopSpace(int spacingAdd);

        int getTopSpaceAdjustment();

        interface IUnit {
            int NORMAL = Typeface.NORMAL;
            int BOLD = Typeface.BOLD;
            int ITALIC = Typeface.ITALIC;
            int BOLD_ITALIC = Typeface.BOLD_ITALIC;
            int UNDERLINE = 1 << 4;

            String getText();

            IUnit setText(String var1);

            Bitmap getBitmap();

            IUnit setBitmap(Bitmap bitmap);

            int getFontSize();

            IUnit setFontSize(int fontSize);

            int getGravity();

            IUnit setGravity(int gravity);

            IUnit setTextStyle(int textStyle);

            int getTextStyle();

            float getWeight();

            IUnit setWeight(float weight);

            IUnit setScaleX(float var1);

            float getScaleX();

            IUnit setScaleY(float var1);

            float getScaleY();

            IPage.EAlign getAlign();

            IPage.ILine.IUnit setAlign(IPage.EAlign var1);
        }
    }
}

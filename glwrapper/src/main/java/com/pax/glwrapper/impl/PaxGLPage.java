package com.pax.glwrapper.impl;

//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
// from PaxGLPage
//

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.text.Layout;
import android.text.SpannableString;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.style.AbsoluteSizeSpan;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.pax.glwrapper.page.IPage;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;

class PaxGLPage {
    private Context context;
    private static com.pax.glwrapper.impl.PaxGLPage a;
    private static Hashtable<String, Typeface> b = new Hashtable();

    PaxGLPage(Context context) {
        this.context = context;
    }

    static  com.pax.glwrapper.impl.PaxGLPage a(Context context) {
        if (a == null) {
            a = new com.pax.glwrapper.impl.PaxGLPage(context);
        }

        return a;
    }

    /*Bitmap pageToBitmap(IPage page, int pageWidth) {
        ViewConverter viewConverter = new ViewConverter(this.context);
        View view;
        view = viewConverter.page2View(viewConverter.getContext(), page, page.getLines(), pageWidth);
        view.measure(View.MeasureSpec.makeMeasureSpec(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED),
                View.MeasureSpec.makeMeasureSpec(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED));

        if(view.getMeasuredHeight() > 0){
            Bitmap bitmap = Bitmap.createBitmap(pageWidth,view.getMeasuredHeight(), Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            view.layout(0, 0, pageWidth, view.getMeasuredHeight());
            view.draw(canvas);
            return bitmap;
        }else{
            view.layout(0, 0, pageWidth, view.getMeasuredHeight());
            view.buildDrawingCache();
            return view.getDrawingCache();
        }
    }*/

    ///////new from GLPage_V1.03
    Bitmap pageToBitmap(IPage page, int pageWidth) {
        Bitmap bitmap = null;
        if(page.getLines().size() > 0){
            View view = this.a(context, page, page.getLines(), pageWidth);
            bitmap = this.a(view, pageWidth);
        }
        return bitmap;
    }

    IPage createPage() {
        return new Page();
    }

    private class Page implements IPage {
        private Typeface typefaceobj;
        private List<ILine> lines;
        private int spacingAdd;
        private String typeface;

        Page() {
            this.typeface = null;
            this.spacingAdd = 0;
            this.lines = new ArrayList<>();
        }

        @Override
        public ILine addLine() {
            Line theLastLine = new Line();
            this.lines.add(theLastLine);
            return theLastLine;
        }

        @Override
        public ILine addLine(ILine line) {
            Line theLastLine = (Line)line;
            this.lines.add(line);
            return theLastLine;
        }

        @Override
        public List<ILine> getLines() {
            return this.lines;
        }

        @Override
        public Typeface getTypefaceObj() {
            return this.typefaceobj;
        }

        @Override
        public void setTypefaceObj(Typeface typefaceobj) {
            this.typefaceobj = typefaceobj;
        }

        @Override
        public ILine.IUnit createUnit() {
            return new Unit();
        }

        @Override
        public void adjustLineSpace(int spacingAdd) {
            this.spacingAdd = spacingAdd;
        }

        @Override
        public int getLineSpaceAdjustment() {
            return this.spacingAdd;
        }

        @Override
        public String getTypeface() {
            return this.typeface;
        }

        @Override
        public void setTypeface(String typeFace) {
            this.typeface = typeFace;
            this.typefaceobj = a(typeFace);
        }

    }

    private class Line implements IPage.ILine {
        private List<IUnit> units;
        private int lineSpace;

        private Line() {
            this.lineSpace = '\uffff';
            this.units = new ArrayList<>();
        }

        @Override
        public List<IUnit> getUnits() {
            return this.units;
        }

        @Override
        public IPage.ILine addUnit(IUnit unit) {
            this.units.add(unit);
            return this;
        }

        public IPage.ILine adjustTopSpace(int spacingAdd) {
            this.lineSpace = spacingAdd;
            return this;
        }

        public int getTopSpaceAdjustment() {
            return this.lineSpace;
        }
    }

    private class Unit implements IPage.ILine.IUnit {
        private String text;
        private Bitmap bitmap;
        private int fontSize;
        private int gravity;
        private int textStyle;
        private float weight;
        private float scaleX;
        private float scaleY;
        private IPage.EAlign r;

        private Unit() {
            this.textStyle = NORMAL;
            this.weight = 1.0F;
            this.fontSize = 24;
            this.gravity = Gravity.START;
            this.text = " ";
            this.bitmap = null;
            this.scaleX = 1.0F;
            this.scaleY = 1.0F;
            this.r = IPage.EAlign.LEFT;
        }

        public String getText() {
            return this.text;
        }

        public IPage.ILine.IUnit setText(String text) {
            this.text = text;
            this.bitmap = null;
            return this;
        }

        public Bitmap getBitmap() {
            return this.bitmap;
        }

        public IPage.ILine.IUnit setBitmap(Bitmap bitmap) {
            this.bitmap = bitmap;
            this.text = "";
            return this;
        }

        public int getFontSize() {
            return this.fontSize;
        }

        public IPage.ILine.IUnit setFontSize(int fontSize) {
            this.fontSize = fontSize;
            return this;
        }

        public int getGravity() {
            return this.gravity;
        }

        public IPage.ILine.IUnit setGravity(int gravity) {
            this.gravity = gravity;
            return this;
        }

        public int getTextStyle() {
            return this.textStyle;
        }

        public IPage.ILine.IUnit setTextStyle(int textStyle) {
            this.textStyle = textStyle;
            return this;
        }

        public float getWeight() {
            return this.weight;
        }

        public IPage.ILine.IUnit setWeight(float weight) {
            this.weight = weight;
            return this;
        }

        public IPage.ILine.IUnit setScaleX(float x) {
            this.scaleX = x;
            return this;
        }

        public float getScaleX() {
            return this.scaleX;
        }

        public IPage.ILine.IUnit setScaleY(float Y) {
            this.scaleY = Y;
            return this;
        }

        public float getScaleY() {
            return this.scaleY;
        }

        public IPage.EAlign getAlign() {
            return this.r;
        }

        public IPage.ILine.IUnit setAlign(IPage.EAlign align) {
            this.r = align;
            return this;
        }
    }

    private Bitmap a(View view, int pageWidth) {
        //view.measure(View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED), View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        view.measure(View.MeasureSpec.makeMeasureSpec(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED),
                View.MeasureSpec.makeMeasureSpec(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED));

        Bitmap bitmap = Bitmap.createBitmap(pageWidth, view.getMeasuredHeight(), Bitmap.Config.ARGB_8888);
        view.layout(0, 0, pageWidth, view.getMeasuredHeight());
        view.draw(new Canvas(bitmap));
        return bitmap;
    }

    private View a(Context context, IPage page, List<IPage.ILine> list, int pageWidth) {
        ScrollView scrollView = new ScrollView(context);
        scrollView.setLayoutParams(new LinearLayout.LayoutParams(pageWidth, -2));
        scrollView.setBackgroundColor(-1);
        LinearLayout llContent = new LinearLayout(context);
        llContent.setLayoutParams(new LinearLayout.LayoutParams(pageWidth, -2));
        llContent.setOrientation(LinearLayout.VERTICAL);
        llContent.setBackgroundColor(-1);
        int index = 0;
        Iterator var9 = list.iterator();

        while(var9.hasNext()) {
            IPage.ILine item = (IPage.ILine)var9.next();
            ++index;
            LinearLayout linearLayout = new LinearLayout(context);
            linearLayout.setLayoutParams(new LinearLayout.LayoutParams(-1, -2));
            linearLayout.setOrientation(LinearLayout.HORIZONTAL);
            linearLayout.setGravity(16);
            List<IPage.ILine.IUnit> unitList = item.getUnits();
            Iterator var13 = unitList.iterator();

            while(var13.hasNext()) {
                IPage.ILine.IUnit unit = (IPage.ILine.IUnit)var13.next();
                this.a(context, page.getTypefaceObj(), unit, linearLayout);
            }

            llContent.addView(linearLayout);
            if (index != 1) {
                try {
                    IPage.class.getDeclaredMethod("getLineSpaceAdjustment");
                    IPage.ILine.class.getDeclaredMethod("getTopSpaceAdjustment");
                    int pageSpace = page.getLineSpaceAdjustment();
                    int lineSpace = item.getTopSpaceAdjustment();
                    if (lineSpace != '\uffff') {
                        pageSpace = lineSpace;
                    }
                    LinearLayout.LayoutParams params = (LinearLayout.LayoutParams)linearLayout.getLayoutParams();
                    params.topMargin = pageSpace;
                    linearLayout.setLayoutParams(params);
                } catch (NoSuchMethodException var15) {
                }
            }
        }
        scrollView.addView(llContent);
        return scrollView;
    }

    @SuppressLint({"RtlHardcoded"})
    private void a(Context context, Typeface typeface, IPage.ILine.IUnit unit, LinearLayout layout) {
        float weight = unit.getWeight();
        Bitmap bitmap = unit.getBitmap();
        String text = unit.getText();
        /*if (bitmap == null && text == null) {
            c tView = new c(context, typeface);
            tView.setTypeface(Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL));
            tView.setLayoutParams(new LinearLayout.LayoutParams(-1, -2, weight));
            layout.addView(tView);
        } else */if (text != null && text.length() > 0) {
            float scaleX = unit.getScaleX();
            float scaleY = unit.getScaleY();
            int fontsize = (int)((float)unit.getFontSize() * scaleY);
            float scaleXratio = scaleX / scaleY;
            c tView = null;
            if ((unit.getTextStyle() & 4) == 0) {
                tView = new c(context, Typeface.create(typeface, Typeface.NORMAL), scaleXratio);
            } else {
                tView = new c(context, Typeface.create(typeface, Typeface.ITALIC), scaleXratio);
            }

            tView.setLayoutParams(new LinearLayout.LayoutParams(0, -2, weight));
            SpannableString spannableString = new SpannableString(text);
            spannableString.setSpan(new AbsoluteSizeSpan(fontsize), 0, spannableString.toString().length(), 33);
            tView.setText(spannableString);
            tView.setTextSize((float)fontsize);
            switch(unit.getAlign().ordinal()) {
                case 1:
                default:
                    tView.setGravity(3);
                    break;
                case 2:
                    tView.setGravity(17);
                    break;
                case 3:
                    tView.setGravity(5);
            }
           if(unit.getGravity() > 0 ){tView.setGravity(unit.getGravity());}

            tView.getPaint().setFakeBoldText(false);
            tView.getPaint().setUnderlineText(false);
            if ((unit.getTextStyle() & 1) != 0) {
                tView.getPaint().setFakeBoldText(true);
            }

            if ((unit.getTextStyle() & 2) != 0) {
                tView.getPaint().setUnderlineText(true);
            }

            layout.setPadding(0, 0, 0, 0);
            layout.setGravity(80);
            layout.setBaselineAligned(true);
            layout.addView(tView);
        } else if (bitmap != null) {
            ImageView iGview = new ImageView(context);
            iGview.setLayoutParams(new LinearLayout.LayoutParams(bitmap.getWidth(), bitmap.getHeight(), 0.0F));
            iGview.setScaleType(ImageView.ScaleType.CENTER);
            switch(unit.getAlign().ordinal()) {
                case 1:
                default:
                    layout.setGravity(3);
                    break;
                case 2:
                    layout.setGravity(17);
                    break;
                case 3:
                    layout.setGravity(5);
            }
            if(unit.getGravity() > 0 ){layout.setGravity(unit.getGravity());}

            iGview.setImageBitmap(bitmap);
            layout.addView(iGview);
        }

    }

    private static Typeface a(String name) {
        Typeface tf = (Typeface)b.get(name);
        if (tf == null) {
            try {
                tf = Typeface.createFromFile(name);
            } catch (Exception var3) {
                var3.printStackTrace();
                return null;
            }

            b.put(name, tf);
        }

        return tf;
    }

    private class a implements IPage.ILine {
        private List<IUnit> d;
        private Unit e;
        private int f;

        private a() {
            this.f = '\uffff';
            this.d = new ArrayList();
        }

        public List<IUnit> getUnits() {
            return this.d;
        }

        public IPage.ILine addUnit() {
            this.e = new Unit();
            this.d.add(this.e);
            return this;
        }

        public IPage.ILine addUnit(IUnit unit) {
            this.d.add(unit);
            return this;
        }

        public IPage.ILine addUnit(int weight) {
            this.e = new Unit();
            this.e.setWeight((float)weight);
            this.d.add(this.e);
            return this;
        }

        public IPage.ILine addUnit(Bitmap bitmap) {
            this.e = new Unit();
            this.e.setBitmap(bitmap);
            this.e.setText("");
            this.d.add(this.e);
            return this;
        }

        public IPage.ILine addUnit(Bitmap bitmap, IPage.EAlign align) {
            this.e = new Unit();
            this.e.setBitmap(bitmap);
            this.e.setText("");
            this.e.setAlign(align);
            this.d.add(this.e);
            return this;
        }

        public IPage.ILine addUnit(String text, int fontSize) {
            this.e = new Unit();
            this.e.setText(text);
            this.e.setFontSize(fontSize);
            this.d.add(this.e);
            return this;
        }

        public IPage.ILine addUnit(String text, int fontSize, int textStyle) {
            this.e = new Unit();
            this.e.setText(text);
            this.e.setFontSize(fontSize);
            this.e.setTextStyle(textStyle);
            this.d.add(this.e);
            return this;
        }

        public IPage.ILine addUnit(String text, int fontSize, float weight) {
            this.e = new Unit();
            this.e.setText(text);
            this.e.setFontSize(fontSize);
            this.e.setWeight(weight);
            this.d.add(this.e);
            return this;
        }

        public IPage.ILine addUnit(String text, int fontSize,IPage.EAlign align) {
            this.e = new Unit();
            this.e.setText(text);
            this.e.setFontSize(fontSize);
            this.e.setAlign(align);
            this.d.add(this.e);
            return this;
        }

        public IPage.ILine addUnit(String text, int fontSize, int textStyle, float weight) {
            this.e = new Unit();
            this.e.setText(text);
            this.e.setFontSize(fontSize);
            this.e.setTextStyle(textStyle);
            this.e.setWeight(weight);
            this.d.add(this.e);
            return this;
        }

        public IPage.ILine addUnit(String text, int fontSize, IPage.EAlign align, float weight) {
            this.e = new Unit();
            this.e.setText(text);
            this.e.setFontSize(fontSize);
            this.e.setAlign(align);
            this.e.setWeight(weight);
            this.d.add(this.e);
            return this;
        }

        public IPage.ILine addUnit(String text, int fontSize, float scaleX, float scaleY, IPage.EAlign align, float weight) {
            this.e = new Unit();
            this.e.setText(text);
            this.e.setFontSize(fontSize);
            this.e.setScaleX(scaleX);
            this.e.setScaleY(scaleY);
            this.e.setAlign(align);
            this.e.setWeight(weight);
            this.d.add(this.e);
            return this;
        }

        public IPage.ILine addUnit(String text, int fontSize, IPage.EAlign align, int textStyle) {
            this.e = new Unit();
            this.e.setText(text);
            this.e.setFontSize(fontSize);
            this.e.setAlign(align);
            this.e.setTextStyle(textStyle);
            this.d.add(this.e);
            return this;
        }

        public IPage.ILine addUnit(String text, int fontSize, IPage.EAlign align, int textStyle, float weight) {
            this.e = new Unit();
            this.e.setText(text);
            this.e.setFontSize(fontSize);
            this.e.setAlign(align);
            this.e.setTextStyle(textStyle);
            this.e.setWeight(weight);
            this.d.add(this.e);
            return this;
        }

        public IPage.ILine adjustTopSpace(int spacingAdd) {
            this.f = spacingAdd;
            return this;
        }

        public int getTopSpaceAdjustment() {
            return this.f;
        }
    }

    /*private class b implements IPage {
        private String h;
        private Typeface i;
        private List<ILine> j;
        private a k;
        private int f;

        private b() {
            this.h = "";
            this.f = 0;
            this.j = new ArrayList();
        }

        public ILine addLine() {
            this.k = new a();
            this.j.add(this.k);
            return this.k;
        }

        public ILine addLine(ILine line) {
            this.j.add(line);
            return this.k;
        }

        public List<ILine> getLines() {
            return this.j;
        }

        public String getTypeface() {
            return this.h;
        }

        public void setTypeface(String typeFace) {
            this.h = typeFace;
            this.i = a(typeFace);
        }

        public Typeface getTypefaceObj() {
            return this.i;
        }

        public void setTypefaceObj(Typeface typeface) {
            this.i = typeface;
        }

        public ILine.IUnit createUnit() {
            return new Unit();
        }

        public void adjustLineSpace(int spacingAdd) {
            this.f = spacingAdd;
        }

        public int getLineSpaceAdjustment() {
            return this.f;
        }

        public Bitmap toBitmap(int width) {
            return pageToBitmapNew(this, width);
        }
    }*/


    private class c extends TextView {
        TextPaint l = new TextPaint();
        Typeface m;
        float n = 1.0F;

        public c(Context context, Typeface typeface, float scaleXratio) {
            super(context);
            this.m = typeface;
            this.n = scaleXratio;
            this.setTypeface(this.m);
            this.setTextScaleX(scaleXratio);
            //this.setIncludeFontPadding(false);
            this.setTextColor(-16777216);
        }

        public c(Context context, Typeface typeface) {
            super(context);
            this.m = typeface;
            this.setTypeface(this.m);
            this.setIncludeFontPadding(false);
            this.setTextColor(-16777216);
        }

        @SuppressLint({"DrawAllocation"})
        protected void onDraw(Canvas canvas) {
            this.l.setTypeface(this.m);
            this.l.setTextSize((float)this.a(this.getTextSize()));
            this.l.setTextScaleX(this.n);
            //this.l.setStyle(Paint.Style.FILL_AND_STROKE);
            this.l.setColor(-16777216);
            this.l.setSubpixelText(true);
            if (this.getPaint().isFakeBoldText()) {
                this.l.setFakeBoldText(true);
            }

            if (this.getPaint().isUnderlineText()) {
                this.l.setUnderlineText(true);
            }

            String txtString = this.getText().toString();
            //String[] txt = txtString.split("\n");
            //String[] var8 = txt;
            //int var7 = txt.length;

            int gravity;
            String newtext = txtString;
            //String newtext = "";
            /*for(gravity = 0; gravity < var7; ++gravity) {
                String str = var8[gravity];
                if (str.length() == 0) {
                    newtext = newtext + "\n";
                } else {
                    while(str.length() > 0) {
                        int paintSize = this.l.breakText(str, true, (float)this.getWidth(), (float[])null);
                        newtext = newtext + str.substring(0, paintSize) + "\n";
                        str = str.substring(paintSize);
                    }
                }
            }*/

            gravity = this.getGravity(); StaticLayout mTextLayout;
            if (gravity != 49 && gravity != 17 && gravity != 8388627) {
                if (gravity == 53 || gravity == 8388661 ) {
                    mTextLayout = new StaticLayout(newtext, this.l, this.getWidth(), Layout.Alignment.ALIGN_OPPOSITE, 1.0F, 0.0F, false);
                } else {
                    mTextLayout = new StaticLayout(newtext, this.l, this.getWidth(), Layout.Alignment.ALIGN_NORMAL, 1.0F, 0.0F, false);
                }
            } else {
                mTextLayout = new StaticLayout(newtext, this.l, this.getWidth(), Layout.Alignment.ALIGN_CENTER, 1.0F, 0.0F, false);
            }
            canvas.save();
            canvas.translate(0.0F, 0.0F);
            mTextLayout.draw(canvas);
            canvas.restore();
        }

        int a(float pxValue) {
            float fontScale = this.getResources().getDisplayMetrics().scaledDensity;
            return (int)(pxValue / fontScale + 0.5F);
        }
    }

}


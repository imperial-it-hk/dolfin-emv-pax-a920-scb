package com.pax.pay.trans.action;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;

import com.pax.abl.core.AAction;
import com.pax.pay.app.FinancialApplication;
import com.pax.pay.constant.EUIParamKeys;
import com.pax.pay.trans.action.activity.IccReaderActivity;

/**
 * Created by JAICHANOK N on 2/5/2018.
 */

public class ActionIccReader extends AAction {

    private Context context;
    private String content;

    private Bitmap loadedBitmap = null;

    public ActionIccReader(ActionStartListener listener) {super(listener);
    }


    public void setParam(Context context, String content) {
        this.context = context;
        this.content = content;
    }

    @Override
    protected void process() {
          FinancialApplication.getApp().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Intent intent = new Intent(context, IccReaderActivity.class);
                    intent = intent.putExtra(EUIParamKeys.CONTENT.toString(), content);
                    intent = intent.putExtra(EUIParamKeys.BITMAP.toString(), loadedBitmap);
                    context.startActivity(intent);

                }
            });
    }
}

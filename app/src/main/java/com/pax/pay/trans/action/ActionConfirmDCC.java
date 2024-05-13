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
package com.pax.pay.trans.action;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.CountDownTimer;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.pax.abl.core.AAction;
import com.pax.abl.core.ActionResult;
import com.pax.edc.R;
import com.pax.edc.opensdk.TransResult;
import com.pax.eemv.utils.Tools;
import com.pax.pay.app.FinancialApplication;
import com.pax.pay.trans.component.Component;
import com.pax.pay.trans.model.TransData;
import com.pax.pay.trans.receipt.PrintListenerImpl;
import com.pax.pay.trans.receipt.ReceiptPrintDccRate;
import com.pax.pay.uart.SP200_serialAPI;
import com.pax.pay.utils.CountryCode;
import com.pax.pay.utils.CurrencyConverter;
import com.pax.pay.utils.Utils;

import java.util.ArrayList;
import java.util.Currency;
import java.util.Locale;

import th.co.bkkps.utils.Log;

import static com.pax.pay.utils.Utils.getString;

public class ActionConfirmDCC extends AAction {
    private Context context;
    private String title;
    private int backKeyResult = TransResult.ERR_ABORTED;
    private TransData transData;
    private boolean isDCC = false;
    private ProcessRunnable processRunnable = null;
    private String localAmount;
    private String exchangeRate;
    private String dccAmount;
    private String localCurrencyCode;
    private String dccCurrencyCode;
    private String markUp;
    boolean isSP200Enable = false;
    private CountDownTimer timeout = null;

    public ActionConfirmDCC(ActionStartListener listener) {
        super(listener);
    }

    public void setParam(int backKeyResult) {
        this.backKeyResult = backKeyResult;
    }

    public void setParam(Context context, String title,TransData transData) {
        this.context = context;
        this.title = title;
        this.transData = transData;
    }

    @Override
    protected void process() {
        processRunnable = new ProcessRunnable();
        FinancialApplication.getApp().runOnUiThreadDelay(processRunnable, 100);
        timeout = new CountDownTimer(60000, 1000) {
            public void onTick(long leftTime) {
                String tick = leftTime/1000 + "s";
//                Log.d("CountDownTimer", "Timer:"+tick);
            }

            public void onFinish() {
                if (isSP200Enable)
                {
                    SP200_serialAPI.getInstance().cancelSP200();
                }
                processRunnable.dialog.dismiss();
                setResult(new ActionResult(TransResult.ERR_USER_CANCEL, null));
                Log.d("CountDownTimer", "Finish");
            }
        };
    }

    @Override
    public void setResult(ActionResult result) {
        if (processRunnable != null && result.getRet() == TransResult.ERR_TIMEOUT)
            processRunnable.dialog.dismiss();
        else
            super.setResult(result);

        timeout.cancel();
    }

    private class ProcessRunnable implements Runnable {
        AlertDialog dialog = null;

        ProcessRunnable() {

        }

        @Override
        public void run() {
            AlertDialog.Builder builder = new AlertDialog.Builder(context);

            builder.setTitle(getString(R.string.dcc_confirm_select_title));

            int dccCurrencyNum = transData.getDccCurrencyCode() != null ? Integer.parseInt(Tools.bytes2String(transData.getDccCurrencyCode())) : 0;
            CountryCode dccCountry = CountryCode.getByCurrencyCode(dccCurrencyNum);
            Locale dccLocale = dccCountry != null ? CurrencyConverter.getLocaleByCountry(dccCountry.getAlpha2()):null;
            Currency dccCurrency = dccLocale !=null ? Currency.getInstance(dccLocale):null;

            localCurrencyCode = Currency.getInstance(transData.getCurrency()).getCurrencyCode();
            dccCurrencyCode = dccCurrency !=null ? dccCurrency.getCurrencyCode() : null;

            localAmount = CurrencyConverter.convert(Utils.parseLongSafe(transData.getAmount(), 0), transData.getCurrency());
            dccAmount = dccLocale != null ? CurrencyConverter.convert(Utils.parseLongSafe(transData.getDccAmount(), 0), dccLocale):null;
            Double exRate = transData.getDccConversionRate() != null ? Double.parseDouble(transData.getDccConversionRate()) / 10000 : 0;
            exchangeRate =  String.format("%.4f",exRate);

            markUp =  Component.unpackField63Dcc(transData);

            //Create list of currency
            ArrayList<String> currencylist = new ArrayList<String>();
//            if(dccCurrencyCode != null){currencylist.add(getString(R.string.dcc_confirm_select_msg)+" "+ dccCurrencyCode +"\u2003"+ dccAmount);};
            if(dccCurrencyCode != null){currencylist.add(getString(R.string.dcc_confirm_select_msg)+"\u2003"+ dccAmount);};
//            currencylist.add(getString(R.string.dcc_confirm_select_msg)+" "+ localCurrencyCode +"\u2003"+ localAmount);
            currencylist.add(getString(R.string.dcc_confirm_select_msg)+"\u2003"+ localAmount);

            //final ArrayAdapter<String> arrayAdapterItems = new ArrayAdapter<String>(context,android.R.layout.simple_expandable_list_item_1, currencylist){
            final ArrayAdapter<String> arrayAdapterItems = new ArrayAdapter<String>(context, android.R.layout.simple_expandable_list_item_1 , currencylist){
                @Override
                public View getView(int position, View convertView, ViewGroup parent){
                    // Get the current item from ListView
                    View view = super.getView(position,convertView,parent);
                    if (view instanceof TextView) {
                        TextView txView = (TextView) view;
                        txView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 24);
//                        txView.setGravity(Gravity.CENTER_HORIZONTAL);
//                        txView.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
//                        txView.setPadding(0, 0, 10, 0);
                    }
                    /*if(position == 0){
                        // Set a background color for ListView item
                        //view.setBackgroundColor(Color.parseColor("#FF7FFF7E"));//Bright green
                        //view.setBackgroundColor(Color.parseColor("#FF9AD799"));//Light deep Green
                        view.setBackgroundColor(Color.parseColor("#FFBFBFBF"));//Gray
                    }*/
                    return view;
                }
            };
            LayoutInflater inflater = LayoutInflater.from(context);
            View content = inflater.inflate(R.layout.list_msg_dialog_layout, null);


            //set TextView
            TextView localAmountTv = (TextView)content.findViewById(R.id.local_amount);
            TextView dccRateTv = (TextView)content.findViewById(R.id.dcc_rate);
            TextView dccAmountTv = (TextView)content.findViewById(R.id.dcc_amount);
            TextView dccMarginTv = (TextView)content.findViewById(R.id.dcc_margin);
            LinearLayout dccMarginLayout = (LinearLayout) content.findViewById(R.id.dcc_margin_layout);
            dccMarginLayout.setVisibility(View.GONE);

//            localAmountTv.setText(localCurrencyCode + "  " + localAmount);
            localAmountTv.setText(localAmount);
            dccRateTv.setText(String.format("%.4f",exRate));
//            dccAmountTv.setText(dccCurrencyCode + "  " + dccAmount);
            dccAmountTv.setText(dccAmount);
            if(markUp != null){
                dccMarginLayout.setVisibility(View.VISIBLE);
                dccMarginTv.setText(markUp + "%");
            }

            //ListView for rate selection
            final ListView lvItems = (ListView)content.findViewById(R.id.lv_items);
            lvItems.setAdapter(arrayAdapterItems);
            lvItems.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
            lvItems.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    if(position == parent.getLastVisiblePosition()){
                        isDCC = false;
                    }else{
                        isDCC = true;
                    }
                    setResult(new ActionResult(TransResult.SUCC, isDCC));
                    close(dialog);
                }
            });
            builder.setView(content);
            builder.setPositiveButton(context.getString(R.string.dialog_cancel),
                    new DialogInterface.OnClickListener() {

                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            setResult(new ActionResult(TransResult.ERR_USER_CANCEL, null));
                            close(dialog);
                            return;
                        }
                    });

            builder.setCancelable(false);
            dialog = builder.show();
            Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            positiveButton.setBackgroundColor(Color.parseColor("#FFFCB9B7"));

            timeout.start();

            transData.setDccCurrencyName(dccCurrencyCode);
            FinancialApplication.getApp().runInBackground(new Runnable() {

                @Override
                public void run() {

                    isSP200Enable = SP200_serialAPI.getInstance().isSp200Enable();
                    if (isSP200Enable) {
                        int iRet = SP200_serialAPI.getInstance().checkStatusSP200();
                        if (iRet == 0) {
                            SP200_serialAPI sp200API = SP200_serialAPI.getInstance();
                            sp200API.ShowDccRate(localAmount, dccAmount, exchangeRate, markUp);
                        }
                        else
                        {
                            isSP200Enable = false;
                        }
                    }

                    PrintListenerImpl listener = new PrintListenerImpl(context);
                    new ReceiptPrintDccRate().print(localAmount,exchangeRate,dccAmount,localCurrencyCode,transData, listener,markUp);
                }
            });
        }

        private void close(DialogInterface dialog) {

            if (isSP200Enable)
            {
                SP200_serialAPI.getInstance().cancelSP200();
            }

            dialog.dismiss();
        }
    }

}


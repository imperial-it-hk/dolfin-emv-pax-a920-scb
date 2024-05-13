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

import android.content.Context;
import android.content.Intent;

import com.pax.abl.core.AAction;
import com.pax.pay.base.Issuer;
import com.pax.pay.constant.EUIParamKeys;
import com.pax.pay.emv.EmvSP200;
import com.pax.pay.trans.action.activity.SearchCardActivity;
import com.pax.pay.trans.component.Component;
import com.pax.pay.trans.model.TransData;

public class ActionSearchCard extends AAction {
    protected Context context;
    protected byte mode;
    protected String title;
    protected String amount;
    protected String date;
    protected String searchCardPrompt;
    protected boolean supportDualCard = false;
    protected int timeOut = 60;

    public ActionSearchCard(ActionStartListener listener) {
        super(listener);
    }

    /**
     * 寻卡类型定义
     *
     * @author Steven.W
     */
    public static class SearchMode {
        /**
         * 刷卡
         */
        public static final byte SWIPE = 0x01;
        /**
         * 插卡
         */
        public static final byte INSERT = 0x02;
        /**
         * 挥卡
         */
        public static final byte WAVE = 0x04;
        /**
         * 支持手输
         */
        public static final byte KEYIN = 0x08;

        /**
         * 支持手输
         */
        public static final byte QR = 0x10;

        public static final byte RABBIT = 0x20;

        public static final byte SP200 = 0x40;

        public static final byte ALL_DEVICE = (byte)0xff;

        private SearchMode() {

        }
    }

    public static class CardInformation {
        private byte searchMode;
        private String track1;
        private String track2;
        private String track3;
        private String pan;
        private String expDate;
        private Issuer issuer;
        private String QRData;
        private EmvSP200 emvSP200;


        public CardInformation(byte mode, String track1, String track2, String track3, String pan, Issuer issuer) {
            this.searchMode = mode;
            this.track1 = track1;
            this.track2 = track2;
            this.track3 = track3;
            this.pan = pan;
            this.issuer = issuer;
        }

        public CardInformation(byte mode, String QRData) {
            this.searchMode = mode;
            this.QRData = QRData;
        }

        public CardInformation(byte mode, EmvSP200 emvSP200) {
            this.searchMode = mode;
            this.emvSP200 = emvSP200;
        }

        public CardInformation(byte mode) {
            this.searchMode = mode;
        }

        public CardInformation(byte mode, String pan, String expDate, Issuer issuer) {
            this.searchMode = mode;
            this.pan = pan;
            this.expDate = expDate;
            this.issuer = issuer;
        }

        public CardInformation(String pan) {
            this.pan = pan;
        }

        public byte getSearchMode() {
            return searchMode;
        }

        public String getTrack1() {
            return track1;
        }

        public String getTrack2() {
            return track2;
        }

        public String getTrack3() {
            return track3;
        }

        public String getPan() {
            return pan;
        }

        public String getExpDate() {
            return expDate;
        }

        public Issuer getIssuer() {
            return issuer;
        }

        public String getQRData() {return QRData; }

        public EmvSP200 getEmvSP200() {return emvSP200; }
    }

    /**
     * 设置参数
     *
     *
     * @param context ：上下文
     * @param mode    ：读卡模式
     * @param amount  ：交易模式
     */
    public void setParam(Context context, String title, byte mode, String amount, String date, String searchCardPrompt, TransData transData, int timeOut) {
        this.setParam(context, title, mode, amount, date, searchCardPrompt, transData);
        this.timeOut = timeOut;
    }

    public void setParam(Context context, String title, byte mode, String amount, String date, String searchCardPrompt, TransData transData) {
        this.context = context;
        this.title = title;
        this.mode = mode;
        this.amount = amount;
        this.date = date;
        this.searchCardPrompt = searchCardPrompt;
        Component.setTransDataInstance(transData);
    }

    public void setParam(Context context, String title, byte mode, String amount, String date, String searchCardPrompt, TransData transData, boolean supportDualCard) {
        this.context = context;
        this.title = title;
        this.mode = mode;
        this.amount = amount;
        this.date = date;
        this.searchCardPrompt = searchCardPrompt;
        this.supportDualCard = supportDualCard;
        Component.setTransDataInstance(transData);
    }

    @Override
    protected void process() {
        Intent intent = new Intent(context, SearchCardActivity.class);
        intent.putExtra(EUIParamKeys.NAV_TITLE.toString(), title);
        intent.putExtra(EUIParamKeys.NAV_BACK.toString(), true);
        intent.putExtra(EUIParamKeys.TRANS_AMOUNT.toString(), amount);
        intent.putExtra(EUIParamKeys.CARD_SEARCH_MODE.toString(), mode);
        intent.putExtra(EUIParamKeys.TRANS_DATE.toString(), date);
        intent.putExtra(EUIParamKeys.SEARCH_CARD_PROMPT.toString(), searchCardPrompt);
        intent.putExtra(EUIParamKeys.SUPP_DUAL_CARD.toString(), supportDualCard);
        intent.putExtra("activity_timeout", this.timeOut);
        context.startActivity(intent);
    }

}

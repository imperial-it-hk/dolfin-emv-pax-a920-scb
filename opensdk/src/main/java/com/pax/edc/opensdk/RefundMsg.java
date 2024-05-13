/*
 * ============================================================================
 * COPYRIGHT
 *              Pax CORPORATION PROPRIETARY INFORMATION
 *   This software is supplied under the terms of a license agreement or
 *   nondisclosure agreement with Pax Corporation and may not be copied
 *   or disclosed except in accordance with the terms in that agreement.
 *      Copyright (C) 2017 - ? Pax Corporation. All rights reserved.
 * Module Date: 2017-9-18
 * Module Author: Kim.L
 * Description:
 *
 * ============================================================================
 */
package com.pax.edc.opensdk;

import android.os.Bundle;
import androidx.annotation.IntRange;
import androidx.annotation.NonNull;

/**
 * Refund message struct
 */
public class RefundMsg {

    private RefundMsg(){
        //do nothing
    }

    public static class Request extends BaseRequest{
        @IntRange(from=0L,to=9999999999L) private long amount;
        private String orgRefNo;
        private String orgDate;

        public Request() {
        }

        Request(Bundle bundle) {
            this.fromBundle(bundle);
        }

        @Override
        int getType() {
            return Constants.REFUND;
        }

        @Override
        void fromBundle(Bundle bundle) {
            super.fromBundle(bundle);
            this.amount = IntentUtil.getLongExtra(bundle, Constants.Req.REQ_AMOUNT);
            this.orgRefNo = IntentUtil.getStringExtra(bundle, Constants.Req.REQ_ORIGINAL_REF_NO);
            this.orgDate = IntentUtil.getStringExtra(bundle, Constants.Req.REQ_ORIGINAL_DATE);
        }

        @Override
        @NonNull
        Bundle toBundle(@NonNull Bundle bundle) {
            super.toBundle(bundle);
            bundle.putLong(Constants.Req.REQ_AMOUNT, this.amount);
            bundle.putString(Constants.Req.REQ_ORIGINAL_REF_NO, this.orgRefNo);
            bundle.putString(Constants.Req.REQ_ORIGINAL_DATE, this.orgDate);
            return bundle;
        }

        @Override
        boolean checkArgs() {
            return true;
        }

        public long getAmount() {
            return amount;
        }

        public void setAmount(@IntRange(from=0L,to=9999999999L) long amount) {
            this.amount = amount;
        }

        /**
         * optional
         * @return original ref no
         */
        public String getOrgRefNo() {
            return orgRefNo;
        }

        public void setOrgRefNo(String orgRefNo) {
            this.orgRefNo = orgRefNo;
        }

        /**
         * optional
         * format: MMDD
         * @return date
         */
        public String getOrgDate() {
            return orgDate;
        }

        public void setOrgDate(String orgDate) {
            this.orgDate = orgDate;
        }

        @Override
        public String toString() {
            return super.toString() + " " + amount + " " + orgRefNo + " " + orgDate;
        }
    }

    public static class Response extends TransResponse{
        public Response() {
        }

        Response(Bundle bundle) {
            this.fromBundle(bundle);
        }

        @Override
        int getType() {
            return Constants.REFUND;
        }

        @Override
        boolean checkArgs() {
            return true;
        }
    }



}

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
 * Void message struct
 */
public class VoidMsg {

    private VoidMsg(){
        //do nothing
    }

    public static class Request extends BaseRequest{
        @IntRange(from=0L,to=999999L) private long voucherNo;

        public Request() {
        }

        Request(Bundle bundle) {
            this.fromBundle(bundle);
        }

        @Override
        int getType() {
            return Constants.VOID;
        }

        @Override
        void fromBundle(Bundle bundle) {
            super.fromBundle(bundle);
            this.voucherNo = IntentUtil.getLongExtra(bundle, Constants.Req.REQ_VOUCHER_NO);
        }

        @Override
        @NonNull
        Bundle toBundle(@NonNull Bundle bundle) {
            super.toBundle(bundle);
            bundle.putLong(Constants.Req.REQ_VOUCHER_NO, this.voucherNo);
            return bundle;
        }

        @Override
        boolean checkArgs() {
            return true;
        }

        /**
         * @return voucher no, from 0 to 999999, 0 means the last last transaction
         */
        public long getVoucherNo() {
            return voucherNo;
        }

        /**
         *
         * @param voucherNo from 0 to 999999, 0 means the last last transaction
         */
        public void setVoucherNo(@IntRange(from=0L,to=999999L) long voucherNo) {
            this.voucherNo = voucherNo;
        }

        @Override
        public String toString() {
            return super.toString() + " " + voucherNo;
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
            return Constants.VOID;
        }

        @Override
        boolean checkArgs() {
            return true;
        }
    }


}

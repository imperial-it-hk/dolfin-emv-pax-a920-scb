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
 * PreAuth message struct
 */
public class PreAuthMsg {
    private PreAuthMsg(){
        //do nothing
    }

    public static class Request extends BaseRequest{
        @IntRange(from=0L,to=9999999999L) private long amount;

        public Request() {
        }

        Request(Bundle bundle) {
            this.fromBundle(bundle);
        }

        @Override
        int getType() {
            return Constants.PRE_AUTH;
        }

        @Override
        void fromBundle(Bundle bundle) {
            super.fromBundle(bundle);
            this.amount = IntentUtil.getLongExtra(bundle, Constants.Req.REQ_AMOUNT);
        }

        @Override
        @NonNull
        Bundle toBundle(@NonNull Bundle bundle) {
            super.toBundle(bundle);
            bundle.putLong(Constants.Req.REQ_AMOUNT, this.amount);
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

        @Override
        public String toString() {
            return super.toString() + " " + amount;
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
            return Constants.PRE_AUTH;
        }

        @Override
        boolean checkArgs() {
            return true;
        }
    }

}

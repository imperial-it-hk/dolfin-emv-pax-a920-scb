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
import androidx.annotation.IntDef;
import androidx.annotation.NonNull;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Reprint total message struct
 */
public class ReprintTotalMsg {
    private ReprintTotalMsg(){
        //do nothing
    }

    public static class Request extends BaseRequest{
        /**
         * reprint type
         * @see Request#SUMMARY
         * @see Request#DETAIL
         * @see Request#LAST_SETTLE
         */
        @IntDef({SUMMARY, DETAIL, LAST_SETTLE})
        @Retention(RetentionPolicy.SOURCE)
        public @interface Type {
        }

        /**
         * reprint transaction summary of the current batch, default
         */
        public static final int SUMMARY = 1;
        /**
         * reprint transaction detail of the current batch.
         */
        public static final int DETAIL = 2;
        /**
         * reprint the last settlement.
         */
        public static final int LAST_SETTLE = 3;

        private int reprintType = SUMMARY;

        public Request() {
        }

        Request(Bundle bundle) {
            this.fromBundle(bundle);
        }

        @Override
        int getType() {
            return Constants.REPRINT_TOTAL;
        }

        @Override
        void fromBundle(Bundle bundle) {
            super.fromBundle(bundle);
            this.reprintType = IntentUtil.getIntExtra(bundle, Constants.Req.REQ_REPRINT_TYPE);
        }

        @Override
        @NonNull
        Bundle toBundle(@NonNull Bundle bundle) {
            super.toBundle(bundle);
            bundle.putInt(Constants.Req.REQ_REPRINT_TYPE, this.reprintType);
            return bundle;
        }

        @Override
        boolean checkArgs() {
            return true;
        }

        /**
         * @return reprint type {@link Type}
         */
        public int getReprintType() {
            return reprintType;
        }

        /**
         * @param type {@link Type}
         */
        public void setReprintType(@Type int type) {
            this.reprintType = type;
        }

        @Override
        public String toString() {
            return super.toString() + " " + reprintType;
        }
    }

    public static class Response extends BaseResponse{
        public Response() {
        }

        Response(Bundle bundle) {
            this.fromBundle(bundle);
        }

        @Override
        int getType() {
            return Constants.REPRINT_TOTAL;
        }

        @Override
        boolean checkArgs() {
            return true;
        }
    }
}

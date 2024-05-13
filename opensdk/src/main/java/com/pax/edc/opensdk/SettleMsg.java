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

/**
 * Settlement message struct
 */
public class SettleMsg {

    private SettleMsg(){
        //do nothing
    }

    public static class Request extends BaseRequest{
        public Request() {
        }

        Request(Bundle bundle) {
            this.fromBundle(bundle);
        }

        @Override
        int getType() {
            return Constants.SETTLE;
        }

        @Override
        boolean checkArgs() {
            return true;
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
            return Constants.SETTLE;
        }

        @Override
        boolean checkArgs() {
            return true;
        }
    }
}

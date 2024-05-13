/*
 * ============================================================================
 * COPYRIGHT
 *              Pax CORPORATION PROPRIETARY INFORMATION
 *   This software is supplied under the terms of a license agreement or
 *   nondisclosure agreement with Pax Corporation and may not be copied
 *   or disclosed except in accordance with the terms in that agreement.
 *      Copyright (C) 2017 - ? Pax Corporation. All rights reserved.
 * Module Date: 2017-3-24
 * Module Author: huangmuhua
 * Description:
 *
 * ============================================================================
 */
package com.pax.eventbus;

/**
 * emv callback event
 */
public class EmvCallbackEvent extends Event {
    public enum Status {
        OFFLINE_PIN_ENTER_READY,
        CARD_NUM_CONFIRM_SUCCESS,
        CARD_NUM_CONFIRM_ERROR,
    }

    public EmvCallbackEvent(Status status) {
        super(status);
    }

    public EmvCallbackEvent(Status status, Object data) {
        super(status, data);
    }
}

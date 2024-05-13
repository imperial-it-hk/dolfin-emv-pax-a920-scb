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
 * card searching event
 */
public class SearchCardEvent extends Event {
    public enum Status {
        ICC_UPDATE_CARD_INFO,
        ICC_CONFIRM_CARD_NUM,
        CLSS_UPDATE_CARD_INFO,
        CLSS_LIGHT_STATUS_NOT_READY,
        CLSS_LIGHT_STATUS_IDLE,
        CLSS_LIGHT_STATUS_READY_FOR_TXN,
        CLSS_LIGHT_STATUS_PROCESSING,
        CLSS_LIGHT_STATUS_REMOVE_CARD,
        CLSS_LIGHT_STATUS_COMPLETE,
        CLSS_LIGHT_STATUS_ERROR,
    }

    public SearchCardEvent(Status status) {
        super(status);
    }

    public SearchCardEvent(Status status, Object data) {
        super(status, data);
    }
}

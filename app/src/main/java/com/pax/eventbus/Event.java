/*
 * ============================================================================
 * COPYRIGHT
 *              Pax CORPORATION PROPRIETARY INFORMATION
 *   This software is supplied under the terms of a license agreement or
 *   nondisclosure agreement with Pax Corporation and may not be copied
 *   or disclosed except in accordance with the terms in that agreement.
 *      Copyright (C) 2017 - ? Pax Corporation. All rights reserved.
 * Module Date: 2017-5-12
 * Module Author: Kim.L
 * Description:
 *
 * ============================================================================
 */
package com.pax.eventbus;

/**
 * abstract event for event bus
 */
public abstract class Event {

    private Object status;
    private Object data = null;

    public Event(Object status) {
        this.setStatus(status);
    }

    public Event(Object status, Object data) {
        this.setStatus(status);
        this.setData(data);
    }

    public Object getStatus() {
        return status;
    }

    public void setStatus(Object status) {
        this.status = status;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }
}

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
package com.pax.abl.core;

/**
 * action result
 *
 * @author Steven.W
 */
public class ActionResult {
    /**
     * return code
     */
    private int ret;
    /**
     * return data
     */
    private Object data;
    private Object data1;

    public ActionResult(int ret, Object data) {
        this.ret = ret;
        this.data = data;
    }

    public ActionResult(int ret, Object data, Object data1) {
        this.ret = ret;
        this.data = data;
        this.data1 = data1;
    }

    public int getRet() {
        return ret;
    }

    public void setRet(int ret) {
        this.ret = ret;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }

    public Object getData1() {
        return data1;
    }

    public void setData1(Object data1) {
        this.data1 = data1;
    }
}

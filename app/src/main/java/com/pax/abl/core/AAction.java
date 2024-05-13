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

import com.pax.eemv.entity.TagsTable;
import com.pax.pay.trans.TransContext;

/**
 * Action abstract class
 *
 * @author Steven.W
 */
public abstract class AAction {

    protected static final String TAG = "AAction";

    private boolean isFinished = false; //AET156 161 162

    private TransContext transContext = TransContext.getInstance();

    /**
     * action start callback
     */
    //@FunctionalInterface un-comment it for JAVA8, but now is JAVA7, so ignore it and same cases on Sonar.
    public interface ActionStartListener {
        void onStart(AAction action);
    }

    /**
     * action end listener
     */
    public interface ActionEndListener {
        void onEnd(AAction action, ActionResult result);
    }

    private ActionStartListener startListener;
    private ActionEndListener endListener;

    /**
     * derived classes must call super(listener) to set
     *
     * @param listener {@link ActionStartListener}
     */
    public AAction(ActionStartListener listener) {
        this.startListener = listener;
    }

    /**
     * set{@link ActionEndListener} before run action.
     * in this method {@link ActionStartListener#onStart(AAction)} will be called.
     * and then {@link AAction#process} will be called right after that.
     */
    public void execute() {
        transContext.setCurrentAction(this);
        if (startListener != null) {
            startListener.onStart(this);
        }
        process();
    }

    /**
     * end listener setter
     *
     * @param listener {@link ActionEndListener}
     */
    public void setEndListener(ActionEndListener listener) {
        this.endListener = listener;
    }

    /**
     * action process
     */
    protected abstract void process();

    /**
     * set action result, {@link ActionEndListener#onEnd(AAction, ActionResult)} will be called
     *
     * @param result {@link ActionResult}
     */
    public void setResult(ActionResult result) {
        if (endListener != null) {
            endListener.onEnd(this, result);
        }
    }

    /**
     * check if the action is finished
     *
     * @return true/false
     */
    public boolean isFinished() {
        return isFinished;
    }

    /**
     * reset action status
     *
     * @param finished true/false
     */
    public void setFinished(boolean finished) {
        isFinished = finished;
    }
}
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

import th.co.bkkps.utils.Log;

import java.util.HashMap;
import java.util.Map;

/**
 * finite state machine for actions
 *
 * @author Steven.W
 */
public abstract class ATransaction {
    protected static final String TAG = "ATransaction";
    /**
     * state and action binding table
     */
    private Map<String, AAction> actionMap;

    /**
     * state and transaction binding table
     */
    private Map<String, ATransaction> transactionMap;

    /**
     * transaction end listener
     *
     * @author Steven.W
     */
    public interface TransEndListener {
        void onEnd(ActionResult result);
    }

    /**
     * single state bind transaction
     *
     * @param state       state
     * @param transaction cannot be this
     */
    protected void bind(String state, ATransaction transaction) {
        if (transactionMap == null) {
            transactionMap = new HashMap<>();
        }
        if (!this.equals(transaction))
            transactionMap.put(state, transaction);
    }

    /**
     * single state bind action
     *
     * @param state  state
     * @param action target action
     */
    protected void bind(String state, AAction action) {
        if (actionMap == null) {
            actionMap = new HashMap<>();
        }
        actionMap.put(state, action);
    }

    /**
     * clear the action map
     */
    protected void clear() {
        if (actionMap != null) {
            actionMap.clear();
        }
        if (transactionMap != null) {
            transactionMap.clear();
        }
    }

    /**
     * execute action bound by state
     *
     * @param state state
     */
    public void gotoState(String state) {
        AAction action = actionMap.get(state);
        if (action != null) {
            action.setFinished(false); //AET-191
            action.execute();
        } else {
            ATransaction transaction = transactionMap.get(state);
            if (transaction != null) {
                transaction.execute();
            } else {
                Log.e(TAG, "Invalid State:" + state);
            }
        }
    }

    /**
     * execute transaction
     */
    public void execute() {
        bindStateOnAction();
    }

    /**
     * call {@link #bind(String, AAction)} in this method to bind all states to actions,
     * and call {@link #gotoState(String)} to run the first action in the end of this method.
     */
    protected abstract void bindStateOnAction();

}

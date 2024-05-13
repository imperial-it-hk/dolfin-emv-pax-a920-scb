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
package com.pax.pay.app;

import android.app.Activity;
import th.co.bkkps.utils.Log;

import java.util.Deque;
import java.util.LinkedList;

/**
 * workaround of activity stack, for action used
 * singleton
 */
public class ActivityStack {
    private static final String TAG = "ActivityStack";

    private Deque<Activity> activities;
    private static ActivityStack instance;

    private ActivityStack() {
        activities = new LinkedList<>();
    }

    public static ActivityStack getInstance() {
        if (instance == null)
            instance = new ActivityStack();

        return instance;
    }

    public boolean exists(Class clz) {
        boolean isExist = false;

        Log.d("ActivityStack", String.format("_class = %1$s", clz.getClass().getSimpleName()));
        Activity[] _activities = activities.toArray(new Activity[] {});
        for (Activity _activity: _activities) {
            Log.d("ActivityStack", String.format("_activity = %1$s", _activity.getClass().getSimpleName()));
            if (_activity.getClass().getSimpleName().compareToIgnoreCase(clz.getSimpleName()) == 0) {
                isExist = true;
            }
        }

        return isExist;
    }

    /**
     * pop the top activity and finish it
     */
    public void pop() {
        try {
            Activity activity = top();
            if (activity != null) {
                remove(activity);
            }
        } catch (Exception e) {
            Log.w(TAG, e);
        }
    }

    /**
     * pop to the specific activity and finish it
     *
     * @param activity the target activity
     */
    public void popTo(Activity activity) {
        if (activity != null && activities.size() > 1) {
            while (true) {
                Activity lastCurrent = top();
                if (lastCurrent == null || activity == lastCurrent) {
                    return;
                }
                remove(lastCurrent);
            }
        }
    }

    /**
     * pop to the specific activity and finish it
     *
     * @param clz the class of the target activity
     */
    public void popTo(Class clz) {
        if (clz != null) {
            while (true) {
                Activity lastCurrent = top();
                if (lastCurrent == null || clz == lastCurrent.getClass()) {
                    return;
                }
                remove(lastCurrent);
            }
        }
    }

    /**
     * get the top activity of the stack
     *
     * @return the top activity
     */
    public Activity top() {
        try {
            if (!activities.isEmpty())
                return activities.getLast();
        } catch (Exception e) {
            Log.w(TAG, e);
        }
        return null;
    }

    /**
     * push an activity the the top
     *
     * @param activity the target activity
     */
    public void push(Activity activity) {
        activities.addLast(activity);
    }

    /**
     * pop all activities from the stack and finish them
     */
    public void popAll() {
        if (activities == null) {
            return;
        }
        while (true) {
            Activity activity = top();
            if (activity == null) {
                break;
            }
            remove(activity);
        }
    }

    /**
     * get the bottom activity of the stack
     *
     * @return the bottom activity
     */
    public Activity bottom() {
        return activities.getFirst();
    }

    /**
     * remove an activity from the stack and finish it
     *
     * @param activity the target activity
     */
    private void remove(Activity activity) {
        activity.finish();
        activities.remove(activity);
    }
}

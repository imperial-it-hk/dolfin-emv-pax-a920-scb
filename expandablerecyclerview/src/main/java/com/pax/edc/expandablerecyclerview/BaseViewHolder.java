/*
 * ============================================================================
 * COPYRIGHT
 *              Pax CORPORATION PROPRIETARY INFORMATION
 *   This software is supplied under the terms of a license agreement or
 *   nondisclosure agreement with Pax Corporation and may not be copied
 *   or disclosed except in accordance with the terms in that agreement.
 *      Copyright (C) 2017 - ? Pax Corporation. All rights reserved.
 * Module Date: 2017-8-9
 * Module Author: Kim.L
 * Description:
 * ============================================================================
 */
package com.pax.edc.expandablerecyclerview;

import android.util.SparseArray;
import android.view.View;
import androidx.annotation.IdRes;
import androidx.recyclerview.widget.RecyclerView;

public abstract class BaseViewHolder<T> extends RecyclerView.ViewHolder {

    private
    @IdRes
    int toggleId;
    private
    @IdRes
    int expandViewId;

    public BaseViewHolder(View itemView) {
        this(itemView, R.id.expandable_toggle_button, R.id.expandable);
    }

    public BaseViewHolder(View itemView, @IdRes int toggleId, @IdRes int expandViewId) {
        super(itemView);
        this.toggleId = toggleId;
        this.expandViewId = expandViewId;
        initView();
        setListener();
    }

    protected abstract void initView();

    protected abstract void setListener();

    public View getToggle() {
        return itemView.findViewById(toggleId);
    }

    public View getExpandView() {
        return itemView.findViewById(expandViewId);
    }

    final void bindBaseView(final T dataBean, final BaseViewHolder viewHolder, final int pos, final ItemClickListener listener) {
        View toggleView = getToggle();
        if (toggleView != null) {
            toggleView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (listener != null) {
                        listener.onExpand(BaseViewHolder.this);
                    }
                }
            });
        } else {
            viewHolder.itemView.setClickable(true);
        }
        bindView(dataBean, viewHolder, pos);
    }

    public void bindView(final T dataBean, final BaseViewHolder viewHolder, final int pos) {
        //do nothing
    }

    public interface ItemClickListener {
        void onExpand(BaseViewHolder viewHolder);
    }

    @SuppressWarnings("unchecked")
    public static <T extends View> T get(View view, int id) {

        SparseArray<View> viewHolder = (SparseArray<View>) view.getTag();

        if (viewHolder == null) {
            viewHolder = new SparseArray<>();
            view.setTag(viewHolder);
        }

        View childView = viewHolder.get(id);
        if (childView == null) {
            childView = view.findViewById(id);
            viewHolder.put(id, childView);
        }

        return (T) childView;
    }
}

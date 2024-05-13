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

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;


public class ExpandableRecyclerAdapter<T> extends RecyclerView.Adapter<BaseViewHolder<T>> {

    private Context context;
    private List<T> dataBeanList;
    private LayoutInflater mInflater;
    private int mExpandedPosition = -1;

    @LayoutRes
    private int itemId = -1;
    @NonNull
    private ItemViewListener<T> listener;

    public ExpandableRecyclerAdapter(Context context, @LayoutRes int id, @NonNull ItemViewListener<T> listener) {
        this.context = context;
        this.mInflater = LayoutInflater.from(context);
        this.itemId = id;
        this.listener = listener;
    }

    @Override
    public BaseViewHolder<T> onCreateViewHolder(ViewGroup parent, int viewType) {
        return genViewHolder(parent);
    }

    private BaseViewHolder<T> genViewHolder(ViewGroup parent) {
        View view = mInflater.inflate(itemId, parent, false);
        return listener.generate(view);
    }

    /**
     * 根据不同的类型绑定View
     *
     * @param holder
     * @param position
     */
    @Override
    public void onBindViewHolder(final BaseViewHolder<T> holder, int position) {
        final boolean isExpanded = position == mExpandedPosition;
        if (holder.getToggle() != null)
            holder.getToggle().setBackground(context.getResources().getDrawable(R.drawable.touch_bg));
        else
            holder.itemView.setBackground(context.getResources().getDrawable(R.drawable.touch_bg));

        if (holder.getExpandView() != null) {
            holder.getExpandView().setVisibility(isExpanded ? View.VISIBLE : View.GONE);
        }
        holder.bindBaseView(dataBeanList.get(position), holder, position, new BaseViewHolder.ItemClickListener() {
            @Override
            public void onExpand(BaseViewHolder viewHolder) {
                int old = mExpandedPosition;
                if (mExpandedPosition != -1)
                    notifyItemChanged(mExpandedPosition);
                mExpandedPosition = isExpanded ? -1 : viewHolder.getAdapterPosition();
                notifyItemChanged(viewHolder.getAdapterPosition());
                if (old != viewHolder.getAdapterPosition())
                    ((RecyclerView) viewHolder.itemView.getParent()).scrollToPosition(viewHolder.getAdapterPosition());
            }
        });
    }

    @Override
    public int getItemCount() {
        return dataBeanList.size();
    }

    public List<T> getDataBeanList() {
        return dataBeanList;
    }

    public ExpandableRecyclerAdapter<T> setDataBeanList(List<T> dataBeanList) {
        this.dataBeanList = dataBeanList;
        return this;
    }

    public interface ItemViewListener<T> {
        BaseViewHolder<T> generate(View view);
    }
}

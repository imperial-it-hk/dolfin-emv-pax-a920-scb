package com.pax.pay.trans.action.activity

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.pax.pay.trans.model.MerchantProfileManager.getAllMerchant

import com.pax.pay.trans.model.MerchantProfileManager.applyProfileAndSave
import com.pax.pay.trans.model.MerchantProfileManager.applyProfile
import com.pax.pay.BaseActivityWithTickForAction

import com.pax.pay.base.MerchantProfile

import com.pax.abl.core.ActionResult
import com.pax.edc.R
import com.pax.edc.opensdk.TransResult
import com.pax.pay.trans.action.activity.SelectMerchantActivity.Companion.IS_SAVE_CURRENT_MERCHANT
import com.pax.pay.trans.action.activity.SelectMerchantActivity.Companion.SELECT_ALL_MERCHANT_ENABLE

class SelectReportActivity : BaseActivityWithTickForAction() {
    private var recyclerView: RecyclerView? = null
    private var layoutManager: RecyclerView.LayoutManager? = null
    private var adapter: RecyclerAdapter? = null
    private var title: String? = null
    private val listOfReport: MutableList<String> = mutableListOf()

    override fun getLayoutId(): Int {
        return R.layout.activity_select_report
    }

    override fun getTitleString(): String {
        return title!!
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
    }

    override fun initViews() {
        recyclerView = findViewById(R.id.merch_rec_view)
        layoutManager = LinearLayoutManager(this)
        recyclerView?.layoutManager = layoutManager


        listOfReport.add(getString(R.string.history_menu_print_trans_detail))
        listOfReport.add(getString(R.string.history_menu_print_trans_total))
        listOfReport.add(getString(R.string.history_menu_print_last_total))

        adapter = RecyclerAdapter(listOfReport)
        recyclerView?.setHasFixedSize(true)
        recyclerView?.adapter = adapter
    }

    override fun setListeners() {}
    override fun loadParam() {
        title = "Select Merchant"
    }

    override fun onKeyBackDown(): Boolean {
        finish(ActionResult(TransResult.ERR_USER_CANCEL, null), true)
        return true
    }

    private inner class RecyclerAdapter(private val listOfReport: List<String>?) :
        RecyclerView.Adapter<RecyclerAdapter.ViewHolder>() {
        override fun onCreateViewHolder(
            parent: ViewGroup,
            i: Int
        ): ViewHolder {
            val context = parent.context
            val inflater = LayoutInflater.from(context)
            val view =
                inflater.inflate(R.layout.select_merch_layout, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.tvSelectReport.text = listOfReport!![position]
        }

        override fun getItemCount(): Int {
            return listOfReport!!.size
        }

        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView),
            View.OnClickListener {
            var tvSelectReport: TextView
            override fun onClick(view: View) {
                finish(
                    ActionResult(
                        TransResult.SUCC,
                        listOfReport!![bindingAdapterPosition],
                        bindingAdapterPosition
                    ), true
                )
            }

            init {
                tvSelectReport = itemView.findViewById<View>(R.id.merchant_text) as TextView
                itemView.setOnClickListener(this)
            }
        }
    }
}
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

class SelectMerchantActivity : BaseActivityWithTickForAction() {
    private var recyclerView: RecyclerView? = null
    private var layoutManager: RecyclerView.LayoutManager? = null
    private var adapter: RecyclerAdapter? = null
    private var title: String? = null
    private var merchantList: MutableList<String>? = null
    private var merchants: List<MerchantProfile>? = null
    private var isSaveCurrentMerchant = false
    private var isSelectAllMerchant = false

    companion object {
        const val IS_SAVE_CURRENT_MERCHANT = "IS_SAVE_CURRENT_MERCHANT"
        const val SELECT_ALL_MERCHANT_ENABLE = "SELECT_ALL_MERCHANT_ENABLE"
    }

    override fun getLayoutId(): Int {
        return R.layout.activity_select_merchant
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
        merchantList = merchants?.map { it.merchantLabelName }?.toList()?.toMutableList()
        if(isSelectAllMerchant) {
            // Add All Merchant
            merchantList?.add(0, getString(R.string.multi_merchant_all_merchant))
        }
        adapter = RecyclerAdapter(merchantList)
        recyclerView?.setHasFixedSize(true)
        recyclerView?.adapter = adapter
    }

    override fun setListeners() {}
    override fun loadParam() {
        title = "Select Merchant"
        merchants = getAllMerchant()
        isSaveCurrentMerchant = intent.getBooleanExtra(IS_SAVE_CURRENT_MERCHANT, true)
        isSelectAllMerchant = intent.getBooleanExtra(SELECT_ALL_MERCHANT_ENABLE, false)
    }

    override fun onKeyBackDown(): Boolean {
        finish(ActionResult(TransResult.ERR_USER_CANCEL, null), true)
        return true
    }

    private inner class RecyclerAdapter(private val merchantList: List<String>?) :
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
            holder.tvSelectMerchant.text = merchantList!![position]
        }

        override fun getItemCount(): Int {
            return merchantList!!.size
        }

        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView),
            View.OnClickListener {
            var tvSelectMerchant: TextView
            override fun onClick(view: View) {
                //Check if select All then start with 1

                val selectedMerchant = if(isSelectAllMerchant && bindingAdapterPosition == 0){
                    merchantList!![1]
                } else {
                    merchantList!![bindingAdapterPosition]
                }

                if (isSaveCurrentMerchant) {
                    applyProfileAndSave(selectedMerchant)
                } else {
                    applyProfile(selectedMerchant)
                }

                //Toast.makeText(getApplicationContext(),String.format("Select : %s", merchantList.get(getAdapterPosition()).getMerchantName()),Toast.LENGTH_SHORT).show();
                Log.d("Merchant", "Merchant onItemClick: index = $bindingAdapterPosition")
                view.isSelected = true
                finish(ActionResult(TransResult.SUCC,merchantList[bindingAdapterPosition],bindingAdapterPosition), true)
            }

            init {
                tvSelectMerchant = itemView.findViewById<View>(R.id.merchant_text) as TextView
                itemView.setOnClickListener(this)
            }
        }
    }
}
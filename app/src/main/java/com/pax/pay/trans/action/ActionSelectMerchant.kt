package com.pax.pay.trans.action

import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.pax.abl.core.AAction
import com.pax.pay.constant.EUIParamKeys
import com.pax.pay.trans.action.activity.SelectMerchantActivity
import com.pax.pay.trans.action.activity.SelectMerchantActivity.Companion.IS_SAVE_CURRENT_MERCHANT
import com.pax.pay.trans.action.activity.SelectMerchantActivity.Companion.SELECT_ALL_MERCHANT_ENABLE


class ActionSelectMerchant(listener: ActionStartListener?) : AAction(listener) {

    private var context: Context? = null
    private var selectAllMerchantEnable = false
    fun setParam(context: Context?) {
        this.context = context
    }
    fun setParam(context: Context?, selectAllMerchantEnable: Boolean) {
        this.context = context
        this.selectAllMerchantEnable = selectAllMerchantEnable
    }

    override fun process() {
        val intent = Intent(
            context,
            SelectMerchantActivity::class.java
        )
        val bundle = Bundle()
        bundle.putString(EUIParamKeys.NAV_TITLE.toString(), "Select Merchant")
        bundle.putBoolean(IS_SAVE_CURRENT_MERCHANT, false)
        bundle.putBoolean(SELECT_ALL_MERCHANT_ENABLE, selectAllMerchantEnable)
        intent.putExtras(bundle)
        context!!.startActivity(intent)
    }
}
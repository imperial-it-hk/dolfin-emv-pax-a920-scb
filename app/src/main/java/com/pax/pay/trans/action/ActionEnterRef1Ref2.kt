package com.pax.pay.trans.action

import android.content.Context
import android.content.Intent
import com.pax.abl.core.AAction
import com.pax.abl.core.ActionResult
import com.pax.edc.opensdk.TransResult
import com.pax.pay.constant.EUIParamKeys
import com.pax.pay.trans.action.activity.EnterRef1Ref2Activity

class ActionEnterRef1Ref2(listener: ActionStartListener?) : AAction(listener) {
    private var mContext: Context? = null
    private var title: String? = null

    fun setParam(mContext: Context?, title: String?) {
        this.mContext = mContext
        this.title = title
    }

    override fun process() {
        val intent = Intent(mContext, EnterRef1Ref2Activity::class.java)
        intent.putExtra(EUIParamKeys.NAV_TITLE.toString(), title)
        mContext?.let {
            it.startActivity(intent)
        } ?: run {
            setResult(ActionResult(TransResult.SUCC, null))
        }
    }
}
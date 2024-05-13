package com.pax.pay.trans.action

import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.pax.abl.core.AAction
import com.pax.pay.constant.EUIParamKeys
import com.pax.pay.record.TransQueryActivity


class ActionPreviewHistory(listener: ActionStartListener?) : AAction(listener) {

    private var context: Context? = null
    fun setParam(context: Context?) {
        this.context = context
    }

    override fun process() {
        val intent = Intent(
            context,
            TransQueryActivity::class.java
        )
        val bundle = Bundle()
        bundle.putString(EUIParamKeys.NAV_TITLE.toString(), "History")
        intent.putExtras(bundle)
        context!!.startActivity(intent)
    }
}
package com.pax.pay.trans.action

import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.pax.abl.core.AAction
import com.pax.pay.constant.EUIParamKeys
import com.pax.pay.trans.action.activity.SelectReportActivity


class ActionSelectReportType(listener: ActionStartListener?) : AAction(listener) {

    private lateinit var context: Context

    fun setParam(context: Context) {
        this.context = context
    }

    override fun process() {
        val intent = Intent(
            context,
            SelectReportActivity::class.java
        )
        val bundle = Bundle()
        bundle.putString(EUIParamKeys.NAV_TITLE.toString(), "Select Report Type")

        intent.putExtras(bundle)
        context.startActivity(intent)
    }

}
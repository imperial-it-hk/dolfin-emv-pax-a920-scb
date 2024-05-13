package com.pax.pay.trans.action

import android.content.Context
import android.content.Intent
import com.pax.abl.core.AAction
import com.pax.pay.trans.action.activity.EnterPhoneNumberActivity
import com.pax.pay.trans.model.TransData

class ActionEnterPhoneNumber(listener: ActionStartListener?) : AAction(listener) {

    lateinit var context : Context
    lateinit var transData : TransData

    fun setParam (context: Context, transData: TransData) {
        this.context = context
        this.transData = transData
    }


    override fun process() {
        val intent = Intent(context, EnterPhoneNumberActivity::class.java)
        context.startActivity(intent)
    }
}
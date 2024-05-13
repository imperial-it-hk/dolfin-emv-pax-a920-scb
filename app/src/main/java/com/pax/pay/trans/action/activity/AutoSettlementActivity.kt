package com.pax.pay.trans.action.activity

import android.content.Intent
import android.os.Bundle
import android.os.PersistableBundle
import androidx.appcompat.app.AppCompatActivity
import com.pax.abl.core.ATransaction
import com.pax.abl.core.ActionResult
import com.pax.edc.R
import com.pax.pay.BaseActivity
import com.pax.pay.MainActivity
import com.pax.pay.trans.SettleTrans
import org.apache.log4j.chainsaw.Main


class AutoSettlementActivity : BaseActivity() {
    companion object {
        const val PARAM_NAME_WAKEUPLIST = "WAKEUP_LIST"
    }

    var wakeupList : ArrayList<String>? = null
    lateinit var transEndListener : ATransaction.TransEndListener

    override fun getLayoutId(): Int {return R.layout.activity_null}
    override fun initViews() {}
    override fun loadParam() {
        wakeupList = intent!!.getStringArrayListExtra(PARAM_NAME_WAKEUPLIST)!!
    }
    override fun setListeners() {
        transEndListener = object : ATransaction.TransEndListener {
            override fun onEnd(result: ActionResult?) {
                finish()
                val intent  = Intent(this@AutoSettlementActivity, MainActivity::class.java)
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK + Intent.FLAG_ACTIVITY_NEW_TASK )
                this@AutoSettlementActivity.startActivity(intent)
            }
        }
    }

    override fun onStart() {
        super.onStart()

        val settleTrans = SettleTrans(this, false, true,false, wakeupList, null, true)
        settleTrans.setBackToMain(true).execute()
    }
}
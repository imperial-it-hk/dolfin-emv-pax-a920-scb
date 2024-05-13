package com.pax.pay.trans.action.activity

import android.os.Bundle
import com.pax.abl.core.AAction
import com.pax.abl.core.ATransaction
import com.pax.abl.core.ActionResult
import com.pax.edc.R
import com.pax.edc.opensdk.TransResult
import com.pax.pay.BaseActivity
import com.pax.pay.trans.SettleTrans
import com.pax.pay.trans.TransContext
import com.pax.pay.trans.model.MerchantProfileManager

class DoSettlementActivity : BaseActivity() {
    private var title: String? = null
    private var currentAction: AAction? = null
    private lateinit var currentTrans: TransContext
    var hostNameList: ArrayList<String?>? = null
    var isAllMerchantSettle : Boolean = false


    override fun getLayoutId(): Int {
        return R.layout.activity_null
    }

    override fun getTitleString(): String {
        return title!!
    }

    override fun initViews() {
        this.currentAction = TransContext.getInstance().currentAction
        this.currentTrans = TransContext.getInstance()
    }

    override fun setListeners() {

    }
    override fun loadParam() {
        isAllMerchantSettle = intent.extras!!.getBoolean("isAllMerchantSettle")
        hostNameList = intent.extras!!.getStringArrayList("SettleListAcquirer")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        doSettle()
    }

    fun finish(result: ActionResult?, needFinish: Boolean) {
        TransContext.getInstance().currentAction = currentAction
        val action = currentAction
        if (action != null) {
            quickClickProtection.start() // AET-93
            action.setResult(result)
            action.isFinished = true
            currentAction = null
            if (needFinish) {
                finish()
            }
        } else {
            finish()
        }
    }

    /**
     * settle
     */
    private fun doSettle() {
        if (isAllMerchantSettle && hostNameList?.size!!>0) {
            SettleTrans(this, false, true, false, hostNameList, settleEndListener).execute()
        } else {
            SettleTrans(this, false, true, true, arrayListOf(), settleEndListener).execute()
        }
    }

    private val settleEndListener =
        ATransaction.TransEndListener { result ->
//            if (result.ret != TransResult.SUCC) {
            if (isAllMerchantSettle) {
                var currentMerchantId = MerchantProfileManager.getCurrentMerchantId()
                val merchantProfile = MerchantProfileManager.getMerchantProfile(++currentMerchantId)
                merchantProfile?.let {
                    finish(ActionResult(TransResult.SUCC, null), false)
                } ?: run {
                    finish(ActionResult(TransResult.SUCC, null), true)
                }
            } else {
                finish(ActionResult(TransResult.SUCC, null), false)
            }
            return@TransEndListener
//            }
        }
}
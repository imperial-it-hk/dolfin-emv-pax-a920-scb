package com.pax.pay.trans.action.activity

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.widget.Button
import android.widget.TextView
import com.pax.abl.core.AAction
import com.pax.abl.core.ActionResult
import com.pax.edc.R
import com.pax.edc.opensdk.TransResult
import com.pax.pay.BaseActivity
import com.pax.pay.BaseActivityWithTickForAction
import com.pax.pay.ECR.LawsonHyperCommClass
import com.pax.pay.MainActivity
import com.pax.pay.app.ActivityStack
import com.pax.pay.app.FinancialApplication
import com.pax.pay.constant.Constants
import com.pax.pay.constant.EUIParamKeys
import com.pax.pay.trans.TransContext
import com.pax.pay.trans.model.TransData
import com.pax.pay.utils.EditorActionListener
import java.math.BigDecimal
import java.text.NumberFormat

class VerifyTransActivity : BaseActivity() {
    private var btnConfirm: Button? = null
    private var btnCancel: Button? = null
    private var btnVerify: Button? = null
    private var tvAmount: TextView? = null
    private var tvTransID: TextView? = null

    var navTitle: String? = null
    private var navBack = false
    var isVerifyState = false
    var amount: String? = null
    var transID: String? = null
    var isCancel = false


    override fun loadParam() {
        navTitle = intent.getStringExtra(EUIParamKeys.NAV_TITLE.toString())
        navBack = intent.getBooleanExtra(EUIParamKeys.NAV_BACK.toString(), false)
        isVerifyState = intent.getBooleanExtra(EUIParamKeys.VERIFY_STATE.toString(), false)
        amount = intent.getStringExtra(EUIParamKeys.TRANS_AMOUNT.toString())
        transID = intent.getStringExtra(EUIParamKeys.TRANS_ID.toString())
    }

    override fun getLayoutId(): Int {
        return R.layout.activity_verify_trans
    }

    override fun getTitleString(): String? {
        return navTitle
    }

    @SuppressLint("SetTextI18n")
    override fun initViews() {
        isCancel = false
        btnConfirm = findViewById<View>(R.id.btnConfirm) as Button?
        btnCancel = findViewById<View>(R.id.btnCancel) as Button?
        btnVerify = findViewById<View>(R.id.btnVerify) as Button?
        tvAmount = findViewById(R.id.tvAmountLabel)
        tvTransID = findViewById(R.id.tvTransLabel)

        val amt = convertAmount(amount ?: "0")
        tvAmount?.text = "AMOUNT: THB$amt"
        tvTransID?.text = transID

        btnVerify?.visibility = when (isVerifyState) {
            false -> View.GONE
            true -> View.VISIBLE
        }
    }

    override fun setListeners() {
        btnConfirm!!.setOnClickListener(this)
        btnCancel!!.setOnClickListener(this)
        btnVerify!!.setOnClickListener(this)
    }

    override fun onClickProtected(v: View) {
        when (v.id) {
            R.id.btnConfirm -> finish(ActionResult(TransResult.SUCC, "Inquiry"))
            R.id.btnCancel -> {
                isCancel = true
                finish(ActionResult(TransResult.SUCC, "Cancel"))
            }
            R.id.btnVerify -> finish(ActionResult(TransResult.SUCC, "Verify"))
        }
    }

    private fun convertAmount(amount: String): String {
        val decimalFormatter: NumberFormat = NumberFormat.getInstance()
        decimalFormatter.isGroupingUsed = true
        decimalFormatter.minimumFractionDigits = 2
        decimalFormatter.maximumFractionDigits = 2
        return decimalFormatter.format(BigDecimal(amount).divide(BigDecimal(100)))
    }

    fun finish(result: ActionResult?) {
        val action = TransContext.getInstance().currentAction
        if (action != null) {
            if (action.isFinished) return
            action.isFinished = true
            quickClickProtection.start() // AET-93
            action.setResult(result)
        } else {
            ActivityStack.getInstance().popTo(MainActivity::class.java)
            finish()
        }
    }
    override fun onKeyBackDown(): Boolean {
        finish(ActionResult(TransResult.SUCC, "Cancel"))
        return true
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        super.onKeyDown(keyCode, event)
        when(keyCode) {
            KeyEvent.KEYCODE_BACK -> {
                isCancel = true
                finish(ActionResult(TransResult.SUCC, "Cancel"))
            }
        }
        return true
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action != KeyEvent.ACTION_UP) return false

        return when {
            event.keyCode == KeyEvent.KEYCODE_BACK && isCancel -> false
            event.keyCode == KeyEvent.KEYCODE_BACK -> {
                isCancel = true
                finish(ActionResult(TransResult.SUCC, "Cancel"))
                false
            }
            event.keyCode == KeyEvent.KEYCODE_ENTER && !isCancel -> {
                finish(ActionResult(TransResult.SUCC, "Inquiry"))
                false
            }
            else -> false
        }
    }
}

package com.pax.pay.trans.action.activity

import android.os.Build
import android.view.KeyEvent
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import com.pax.abl.core.ActionResult
import com.pax.edc.R
import com.pax.edc.opensdk.TransResult
import com.pax.pay.BaseActivityWithTickForAction
import com.pax.pay.utils.Convert
import com.pax.pay.utils.Utils
import com.pax.view.dialog.CustomAlertDialog
import com.pax.view.dialog.DialogUtils
import th.co.bkkps.utils.Log

class EnterPhoneNumberActivity : BaseActivityWithTickForAction() {
    override fun getLayoutId(): Int {
        return R.layout.activity_enter_phone_number
    }


    lateinit var etx_phone_number_input : EditText
    lateinit var btn_cancel : Button
    lateinit var btn_ok: Button
    lateinit var txv_guide : TextView
    override fun initViews() {
        etx_phone_number_input = findViewById(R.id.etx_phone_number)
        btn_cancel = findViewById(R.id.btn_phone_num_cancel)
        btn_ok = findViewById(R.id.btn_phone_num_ok)
        txv_guide = findViewById(R.id.txv_guide)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            etx_phone_number_input.focusable = View.FOCUSABLE
        }
    }

    lateinit var btnCancelClickListener : View.OnClickListener
    lateinit var btnOkClickListener : View.OnClickListener

    override fun setListeners() {
        btnCancelClickListener = View.OnClickListener {
            DialogUtils.showConfirmDialog(this, "Do you want to cancel transaction?", null,
                CustomAlertDialog.OnCustomClickListener {
                    it.dismiss()
                    finish(ActionResult(TransResult.ERR_USER_CANCEL,null))
            })
        }
        btnOkClickListener = View.OnClickListener {
            var inputPhoneNum : String = etx_phone_number_input.text.toString()
            if (!inputPhoneNum.trim().equals("")) {
                DialogUtils.showConfirmDialog(this, "Phone No. $inputPhoneNum is correct?", null,
                    CustomAlertDialog.OnCustomClickListener {
                        it.dismiss()
                        finish(ActionResult(TransResult.SUCC, inputPhoneNum))
                    })
            } else {
                DialogUtils.showConfirmDialog(this, "Skip input phone no.?", null,
                    CustomAlertDialog.OnCustomClickListener {
                        inputPhoneNum = Utils.getStringPadding("",10," ", Convert.EPaddingPosition.PADDING_LEFT)
                        it.dismiss()
                        finish(ActionResult(TransResult.SUCC, inputPhoneNum))
                    })
            }
        }

        btn_cancel.setOnClickListener(btnCancelClickListener)
        btn_ok.setOnClickListener(btnOkClickListener)
    }

    var disableInputState : Boolean = false
    fun disableAllInput() {
        btn_ok.isEnabled = false
        btn_cancel.isEnabled = false
        etx_phone_number_input.isEnabled = false
        disableInputState = true
    }

    override fun loadParam() { }


    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (disableInputState) {
            return false
        } else {
            if (keyCode == 4) {
                btnCancelClickListener.onClick(btn_cancel)
                return false
            } else {
                return super.onKeyDown(keyCode, event)
            }
        }
    }
}
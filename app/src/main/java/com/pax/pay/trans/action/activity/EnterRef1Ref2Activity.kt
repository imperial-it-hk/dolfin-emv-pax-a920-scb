package com.pax.pay.trans.action.activity

import android.content.Context
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.isVisible
import com.pax.abl.core.ActionResult
import com.pax.appstore.DownloadManager
import com.pax.edc.R
import com.pax.edc.opensdk.TransResult
import com.pax.pay.BaseActivityWithTickForAction
import com.pax.pay.app.FinancialApplication
import com.pax.pay.constant.EUIParamKeys
import com.pax.settings.SysParam
import com.pax.view.dialog.CustomAlertDialog
import com.pax.view.dialog.DialogUtils

class EnterRef1Ref2Activity: BaseActivityWithTickForAction() {
    private lateinit var ref1Layout: LinearLayout
    private lateinit var ref2Layout: LinearLayout
    private lateinit var ref1Title: TextView
    private lateinit var ref2Title: TextView
    private lateinit var ref1InputText: EditText
    private lateinit var ref2InputText: EditText
    private var title: String? = null
    private var ref1: String? = null
    private var ref2: String? = null
    private var isAlreadyClick: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ref1InputText.requestFocus()
        ref1InputText.postDelayed({
            val inputMethodManager: InputMethodManager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            inputMethodManager.showSoftInput(ref1InputText, InputMethodManager.SHOW_IMPLICIT)
        }, 200)
    }

    override fun getLayoutId(): Int {
        return R.layout.activity_enter_ref1_ref2
    }

    override fun getTitleString(): String {
        return title!!
    }

    override fun loadParam() {
        title = intent.getStringExtra(EUIParamKeys.NAV_TITLE.toString())
    }

    override fun initViews() {
        ref1Layout = findViewById(R.id.ref1_layout)
        ref2Layout = findViewById(R.id.ref2_layout)
        ref1Title = findViewById(R.id.ref1_title)
        ref2Title = findViewById(R.id.ref2_title)
        ref1InputText = findViewById(R.id.ref1_input_text)
        ref2InputText = findViewById(R.id.ref2_input_text)

        isAlreadyClick = false

        val iMode = FinancialApplication.getSysParam()[SysParam.NumberParam.EDC_SUPPORT_REF1_2_MODE]
        when (DownloadManager.EdcRef1Ref2Mode.getByMode(iMode)) {
            DownloadManager.EdcRef1Ref2Mode.REF_1_MODE -> {
                ref2Layout.visibility = View.GONE
                ref1Title.text = FinancialApplication.getSysParam()[SysParam.StringParam.EDC_DISP_TEXT_REF1]
            }
            DownloadManager.EdcRef1Ref2Mode.REF_1_2_MODE -> {
                ref2Layout.visibility = View.VISIBLE
                ref1Title.text = FinancialApplication.getSysParam()[SysParam.StringParam.EDC_DISP_TEXT_REF1]
                ref2Title.text = FinancialApplication.getSysParam()[SysParam.StringParam.EDC_DISP_TEXT_REF2]
            }
            else -> {
                finish(ActionResult(TransResult.SUCC, null))//skip ref1 & 2 and do normal sale trans.
            }
        }
    }

    override fun setListeners() {
        if (ref2Layout.isVisible) {
            ref2InputText.setOnEditorActionListener(object: TextView.OnEditorActionListener {
                override fun onEditorAction(v: TextView?, actionId: Int, event: KeyEvent?): Boolean {
                    if (!isAlreadyClick) {
                        isAlreadyClick = true
                        when (actionId) {
                            EditorInfo.IME_ACTION_UNSPECIFIED -> {
                                if (event != null && event.action == KeyEvent.ACTION_DOWN) {
                                    onKeyOk(true)
                                    return true
                                }
                            }
                            EditorInfo.IME_ACTION_DONE -> {
                                onKeyOk(true)
                                return true
                            }
                            EditorInfo.IME_ACTION_NONE -> {
                                isAlreadyClick = false
                                onKeyCancel(true)
                                return true
                            }
                        }
                        return false
                    }
                    return false
                }
            })
        } else {
            ref1InputText.setOnEditorActionListener(object: TextView.OnEditorActionListener {
                override fun onEditorAction(v: TextView?, actionId: Int, event: KeyEvent?): Boolean {
                    if (!isAlreadyClick) {
                        isAlreadyClick = true
                        when (actionId) {
                            EditorInfo.IME_ACTION_UNSPECIFIED -> {
                                if (event != null && event.action == KeyEvent.ACTION_DOWN) {
                                    onKeyOk(false)
                                    return true
                                }
                            }
                            EditorInfo.IME_ACTION_DONE -> {
                                onKeyOk(false)
                                return true
                            }
                            EditorInfo.IME_ACTION_NONE -> {
                                isAlreadyClick = false
                                onKeyCancel(false)
                                return true
                            }
                        }
                        return false
                    }
                    return false
                }
            })
        }
    }

    override fun onKeyBackDown(): Boolean {
        finish(ActionResult(TransResult.ERR_USER_CANCEL, null))
        return true
    }

    private fun onKeyOk(ref2Visible: Boolean) {
        if (ref2Visible) { //Enable Ref1 + Ref2
            ref1 = ref1InputText.text.toString()
            ref2 = ref2InputText.text.toString()

            if ((ref1 != null && ref1!!.isNotEmpty()) || (ref2 != null && ref2!!.isNotEmpty())) {
                finish(ActionResult(TransResult.SUCC, ref1, ref2))
            } else {
                DialogUtils.showConfirmDialog(
                    this@EnterRef1Ref2Activity,
                    getString(R.string.prompt_skip_reference),
                    { cancel ->
                        isAlreadyClick = false
                        cancel.dismiss()
                        ref2InputText.requestFocus()
                    },
                    { confirm ->
                        confirm.dismiss()
                        ref2InputText.setText("")
                        finish(ActionResult(TransResult.SUCC, ref1, ref2))
                    },
                    CustomAlertDialog.eCustomAlertDialogButton.POSITIVE
                )
            }
        } else { //Enable Only Ref1
            ref1 = ref1InputText.text.toString()

            if (ref1 != null && ref1!!.isNotEmpty()) {
                finish(ActionResult(TransResult.SUCC, ref1, null))
            } else {
                DialogUtils.showConfirmDialog(this@EnterRef1Ref2Activity,
                    getString(R.string.prompt_skip_reference),
                    { cancel ->
                        isAlreadyClick = false
                        cancel.dismiss()
                        ref1InputText.requestFocus()
                    },
                    { confirm ->
                        confirm.dismiss()
                        ref1InputText.setText("")
                        finish(ActionResult(TransResult.SUCC, ref1, null))
                    },
                    CustomAlertDialog.eCustomAlertDialogButton.POSITIVE
                )
            }
        }
    }

    private fun onKeyCancel(ref2Visible: Boolean) {
        if (ref2Visible) { //Enable Ref1 + Ref2
            ref2InputText.setText("")
        } else { //Enable Only Ref1
            ref1InputText.setText("")
        }
        finish(ActionResult(TransResult.ERR_USER_CANCEL, null))
    }
}
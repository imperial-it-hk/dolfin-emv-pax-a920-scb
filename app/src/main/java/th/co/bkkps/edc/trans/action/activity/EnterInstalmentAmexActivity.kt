package th.co.bkkps.edc.trans.action.activity

import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.View
import android.widget.AdapterView
import android.widget.EditText
import com.pax.abl.core.ActionResult
import com.pax.edc.opensdk.TransResult
import com.pax.pay.BaseActivityWithTickForAction
import com.pax.pay.app.FinancialApplication
import com.pax.pay.constant.Constants
import com.pax.view.dialog.DialogUtils
import kotlinx.android.synthetic.main.activity_instalment_amex.*
import kotlinx.android.synthetic.main.include_button_cancel_ok.*
import th.co.bkkps.edc.trans.action.ActionEnterInstalmentAMEX.*
import java.math.RoundingMode
import java.text.DecimalFormat
import th.co.bkkps.edc.util.InputFilterDecimal
import android.text.InputFilter
import android.util.Log
import android.view.KeyEvent
import com.pax.pay.constant.EUIParamKeys
import kotlinx.android.synthetic.main.activity_instalment_amex.instalmentAmount
import kotlinx.android.synthetic.main.activity_instalment_amex.instalmentInterest
import kotlinx.android.synthetic.main.activity_instalment_amex.instalmentTerms
import kotlinx.android.synthetic.main.activity_instalment_amex.monthDue
import kotlinx.android.synthetic.main.activity_instalment_bay.*
import th.co.bkkps.edc.util.TextWatcherDecimal
import android.widget.ArrayAdapter
import android.R




class EnterInstalmentAmexActivity : BaseActivityWithTickForAction() {

    private var navTitle: String? = null
    private var navBack: Boolean = false
    private val formatter = DecimalFormat("#0.00")
    private var currentKey: Int = 0

    override fun loadParam() {
        navTitle = getIntent().getStringExtra(EUIParamKeys.NAV_TITLE.toString());
        navBack = getIntent().getBooleanExtra(EUIParamKeys.NAV_BACK.toString(), false);
    }

    override fun setListeners() {
        enableBackAction(navBack)
    }

    override fun getTitleString(): String {
        return navTitle.toString()
    }

    override fun getLayoutId(): Int {
        return com.pax.edc.R.layout.activity_instalment_amex
    }

    override fun initViews() {
        tickTimer.stop()
        super.enableActionBar(true)
        enableDisplayTitle(true)
        enableBackAction(true)

        instalmentInterest.setReadOnly(true)
        monthDue.setReadOnly(true)
        val acq = FinancialApplication.getAcqManager().findAcquirer(Constants.ACQ_AMEX_EPP)
        val minAmt = acq.instalmentMinAmt
        btn_half_ok.setOnClickListener{
            var amount = if(instalmentAmount.text != null && instalmentAmount.text.toString() != "" && instalmentAmount.text.toString() != ".")
                instalmentAmount.text.toString().replace(",","")  else "0"
            amount = formatter.format(amount.toDouble())

            //Log.e("menu","amount = " + amount + " , Terms =  " + getTerms().toString() + " , monthDue = " + monthDue.text.toString());

            if(amount.toDouble() < minAmt.toInt()){
                DialogUtils.showErrMessage(
                    this,
                    getString(com.pax.edc.R.string.menu_instalment_amex),
                    getString(com.pax.edc.R.string.err_instalment_amex_amount_min,minAmt),
                    null,
                    Constants.FAILED_DIALOG_SHOW_TIME
                )
            }else{instalmentTerms.selectedItem
                finish(ActionResult(TransResult.SUCC,
                    InstalmentAmexInfo(
                        amount,
                        getTerms().toString(),
                        monthDue.text.toString()
                    )
                ))
            }
        }
        btn_half_cancel.setOnClickListener{
            finish(ActionResult(TransResult.ERR_USER_CANCEL, null,null))
        }

        //instalmentAmount.setFilters(arrayOf<InputFilter>(InputFilterDecimal(20, 2)))
        instalmentAmount.addTextChangedListener(textWatcherCalMonthDue)
        instalmentAmount.setOnKeyListener(object : View.OnKeyListener {
            override fun onKey(v: View, keyCode: Int, event: KeyEvent): Boolean {
                currentKey = TextWatcherDecimal.setCurrentKey(keyCode)
                return false
            }
        })

        instalmentTerms.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {
            }

            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                updateMonthDue()
            }
        }

        val splitedTerms = if( !acq.instalmentTerms.isNullOrEmpty() ) acq.instalmentTerms.split(",") else null
        if(splitedTerms != null){
            val spinnerArrayAdapter = ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, splitedTerms)
            instalmentTerms.setAdapter(spinnerArrayAdapter)
        }
        instalmentAmount.setHint(getString(com.pax.edc.R.string.instalment_amount_min_hint) + minAmt)
    }



    private val textWatcherCalMonthDue = object : TextWatcher {
        private var valueAsString: String = ""
        override fun afterTextChanged(s: Editable) {}
        override fun beforeTextChanged(s: CharSequence, start: Int,
                                       count: Int, after: Int) {
            valueAsString = s.toString()
        }
        override fun onTextChanged(s: CharSequence, start: Int,
                                   before: Int, count: Int) {
            TextWatcherDecimal.calDecimal(instalmentAmount,valueAsString,currentKey.toString(),12,this,s,count)

            updateMonthDue()

        }

    }

    private fun updateMonthDue(){
        var amount = if (instalmentAmount.text != null && instalmentAmount.text.toString() != "" && instalmentAmount.text.toString() != ".") instalmentAmount.text.toString().replace(",","") else "0"
        var terms = getTerms();
        var monthDueAmount : Any = if(amount != "0" && terms != 0) amount.toDouble().div(terms) else 0

        formatter.roundingMode = RoundingMode.DOWN
        monthDueAmount = if (!monthDueAmount.equals(0)) formatter.format(monthDueAmount) else 0

        monthDue.setText(monthDueAmount.toString())
    }

    private fun getTerms(): Int{

        //val termsList = FinancialApplication.getApp().getResources().getStringArray(com.pax.edc.R.array.edc_instalment_amex_value_entries)
        //var terms = termsList[instalmentTerms.selectedItemPosition].toInt()
        var terms =  instalmentTerms.selectedItem.toString()
        return terms.toInt()
    }

    fun EditText.setReadOnly(value: Boolean, inputType: Int = InputType.TYPE_NULL) {
        isFocusable = !value
        isFocusableInTouchMode = !value
        this.inputType = inputType
    }

    override fun onKeyBackDown(): Boolean {
        finish(
            ActionResult(
                TransResult.ERR_USER_CANCEL, null, null)
        )
        return true
    }

}

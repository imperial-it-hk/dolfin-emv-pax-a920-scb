package th.co.bkkps.edc.trans.action.activity

import android.graphics.PorterDuff
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.util.Log
import android.util.TypedValue
import android.view.KeyEvent
import android.view.View
import android.widget.EditText
import com.pax.abl.core.ActionResult
import com.pax.dal.IScanner
import com.pax.dal.entity.EScannerType
import com.pax.device.Device
import com.pax.edc.R
import com.pax.edc.opensdk.TransResult
import com.pax.pay.BaseActivityWithTickForAction
import com.pax.pay.app.FinancialApplication
import com.pax.pay.constant.Constants
import com.pax.pay.constant.EUIParamKeys
import com.pax.pay.utils.CurrencyConverter
import com.pax.pay.utils.TickTimer
import com.pax.pay.utils.Utils
import com.pax.settings.SysParam
import com.pax.view.dialog.DialogUtils
import kotlinx.android.synthetic.main.activity_instalment_bay.instalmentAmount
import kotlinx.android.synthetic.main.activity_instalment_bay.instalmentSerialNum
import kotlinx.android.synthetic.main.activity_instalment_bay.instalmentTerms
import kotlinx.android.synthetic.main.activity_instalment_bay_sp_msc.*
import kotlinx.android.synthetic.main.include_button_cancel_ok.*
import th.co.bkkps.edc.trans.action.ActionEnterInstalmentBay.InstalmentBayInfo
import th.co.bkkps.edc.util.TextWatcherDecimal
import java.text.DecimalFormat


class EnterInstalmentBaySpMscActivity : BaseActivityWithTickForAction() {

    private var navTitle: String? = null
    private var navBack: Boolean = false
    private val formatter = DecimalFormat("#0.00")
    private var currentKey: Int = 0
    private var qrCode: String? = null
    private var scanner: IScanner? = null
    private var iScanListener: IScanner.IScanListener? = null
    //var textWatcherInterest: TextWatcher? = null
    override fun initViews() {
    }

    override fun loadParam() {
        navTitle = getIntent().getStringExtra(EUIParamKeys.NAV_TITLE.toString());
        navBack = getIntent().getBooleanExtra(EUIParamKeys.NAV_BACK.toString(), false);
    }

    override fun getTitleString(): String {
        return navTitle.toString()
    }

    override fun getLayoutId(): Int {
        return R.layout.activity_instalment_bay_sp_msc
    }

    override fun setListeners() {
        enableBackAction(navBack)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_instalment_bay_sp_msc)
        tickTimer.stop()

        btn_half_ok.isEnabled = true
        btn_half_ok.setOnClickListener{onConfirmResult()}
        btn_half_ok.setOnFocusChangeListener { view, hasFocus -> if(hasFocus && btn_half_ok.isEnabled){onConfirmResult()} }

        btn_half_cancel.isFocusable = false
        btn_half_cancel.isFocusable = false
        btn_half_cancel.setOnClickListener{
            finish(ActionResult(TransResult.ERR_USER_CANCEL, null,null))
        }

        instalmentAmount.background.setColorFilter(resources.getColor(R.color.primary),
            PorterDuff.Mode.SRC_ATOP)
        instalmentTerms.background.setColorFilter(resources.getColor(R.color.primary),
            PorterDuff.Mode.SRC_ATOP)
        sku.background.setColorFilter(resources.getColor(R.color.primary),
            PorterDuff.Mode.SRC_ATOP)
        mktCode.background.setColorFilter(resources.getColor(R.color.primary),
            PorterDuff.Mode.SRC_ATOP)
        instalmentSerialNum.background.setColorFilter(resources.getColor(R.color.primary),
            PorterDuff.Mode.SRC_ATOP)

        instalmentAmount.addTextChangedListener(textWatcherAmount)
        instalmentAmount.setOnKeyListener(object : View.OnKeyListener {
            override fun onKey(v: View, keyCode: Int, event: KeyEvent): Boolean {
                currentKey = TextWatcherDecimal.setCurrentKey(keyCode)
                return false
            }
        })

        imageButtonScanMktCode.isFocusable = false
        imageButtonScanSerialNum.isFocusable = false
        imageButtonScanSku.isFocusable = false
        imageButtonScanMktCode.setOnClickListener{setScannerListener(mktCode)}
        imageButtonScanSerialNum.setOnClickListener{setScannerListener(instalmentSerialNum)}
        imageButtonScanSku.setOnClickListener{setScannerListener(sku)}

        /*textViewNumpadCurrency.text = getString(
            R.string.include_numpad_currency,
            CurrencyConverter.formatter(CurrencyConverter.getDefCurrency(), applicationContext)
        )*/

        instalmentTerms.addTextChangedListener(textWatcherTerms)
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

    private val textWatcherAmount = object : TextWatcher {
        private var valueAsString: String = ""
        override fun afterTextChanged(s: Editable) {}
        override fun beforeTextChanged(s: CharSequence, start: Int,
                                       count: Int, after: Int) {
            valueAsString = s.toString()
        }
        override fun onTextChanged(s: CharSequence, start: Int,
                                   before: Int, count: Int) {
            TextWatcherDecimal.calDecimal(instalmentAmount,valueAsString,currentKey.toString(),12,this,s,count)
        }
    }

    private val textWatcherTerms = object : TextWatcher {
        private var valueAsString: String = ""
        override fun afterTextChanged(s: Editable) {}
        override fun beforeTextChanged(s: CharSequence, start: Int,
                                       count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence, start: Int,
                                   before: Int, count: Int) {
            instalmentTerms.setTextSize(TypedValue.COMPLEX_UNIT_SP, 30F)
        }
    }

    private fun setScannerListener(editText: EditText){
        iScanListener = object : IScanner.IScanListener {
            override fun onCancel() {
                // DO NOT call setResult here since it will be can in onFinish
            }

            override fun onFinish() {
                scanner?.close()
                if (qrCode != null && qrCode!!.length > 0) {
                    //setResult(ActionResult(TransResult.SUCC, qrCode))
                    editText.setText(qrCode.toString())
                } else { //FIXME press key back on A920C while scanning should return to onCancel firstly
                    //setResult(ActionResult(TransResult.ERR_USER_CANCEL, null))
                }
            }

            override fun onRead(content: String) {
                qrCode = content
                //Log.e("menu", "  onRead qrCode = " + qrCode.toString())
            }
        };

        FinancialApplication.getApp().runOnUiThread {
            var scannerRunning: IScanner
            if (Utils.getString(R.string.back_camera) == FinancialApplication.getSysParam().get(SysParam.StringParam.EDC_DEFAULT_CAMERA)) {
                scannerRunning = Device.getScanner(EScannerType.REAR)
            } else {
                scannerRunning = Device.getScanner(EScannerType.FRONT)
            }
            //scanner.close(); // 系统扫码崩溃之后，再调用掉不起来

            scannerRunning.open()
            scannerRunning.setTimeOut(TickTimer.DEFAULT_TIMEOUT * 1000)
            scannerRunning.setContinuousTimes(1)
            scannerRunning.setContinuousInterval(1000)
            scannerRunning.start(iScanListener)
            scanner = scannerRunning
        }

    }

    private fun onConfirmResult(){
        var amount = if(instalmentAmount.text != null && instalmentAmount.text.toString() != "" && instalmentAmount.text.toString() != ".") instalmentAmount.text.toString().replace(",","") else "0"
        amount = formatter.format(amount.toDouble())

        if(amount.toDouble() <= 0){
            DialogUtils.showErrMessage(
                this,
                getString(R.string.menu_instalment_bay),
                getString(R.string.err_bay_amount),
                null,
                Constants.FAILED_DIALOG_SHOW_TIME
            )
            instalmentAmount.requestFocus()
        }else if(instalmentTerms.text.isNullOrBlank()){
            DialogUtils.showErrMessage(
                this,
                getString(R.string.menu_instalment_bay),
                getString(R.string.err_bay_terms),
                null,
                Constants.FAILED_DIALOG_SHOW_TIME
            )
            instalmentTerms.requestFocus()
        }else if(mktCode.text.isNullOrBlank()){
            DialogUtils.showErrMessage(
                this,
                getString(R.string.menu_instalment_bay),
                getString(R.string.err_bay_mkt),
                null,
                Constants.FAILED_DIALOG_SHOW_TIME
            )
            mktCode.requestFocus()
        }else{
            btn_half_ok.isEnabled = false
            finish(ActionResult(TransResult.SUCC,
                InstalmentBayInfo(
                    amount,
                    instalmentTerms.text.toString(),
                    instalmentSerialNum.text.toString(),
                    mktCode.text.toString(),
                    sku.text.toString()
                )
            ))
        }
    }

}

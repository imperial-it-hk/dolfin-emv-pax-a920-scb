package th.co.bkkps.edc.trans.action.activity

import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.widget.AdapterView
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
import com.pax.pay.trans.model.ETransType
import com.pax.pay.utils.CurrencyConverter
import com.pax.pay.utils.TickTimer
import com.pax.pay.utils.Utils
import com.pax.settings.SysParam
import com.pax.view.dialog.DialogUtils
import kotlinx.android.synthetic.main.activity_redeem_bay_full.*
import kotlinx.android.synthetic.main.include_button_cancel_ok.*
import th.co.bkkps.edc.trans.action.ActionEnterRedeemBay
import th.co.bkkps.edc.util.TextWatcherDecimal
import java.text.DecimalFormat


class EnterRedeemBayActivity : BaseActivityWithTickForAction() {

    private var navTitle: String? = null
    private var navBack: Boolean = false
    private val formatter = DecimalFormat("#0.00")
    private var currentKey: Int = 0
    private var qrCode: String? = null
    private var scanner: IScanner? = null
    private var iScanListener: IScanner.IScanListener? = null
    private var redeemPoints = "";
    private var transType: ETransType? = null
    private var type = 0 //RedeemType 1=Full,2=Partial,3=Catalogue
    //var textWatcherInterest: TextWatcher? = null
    override fun initViews() {
    }


    override fun getLayoutId(): Int {
        return R.layout.activity_redeem_bay_full
    }

    override fun getTitleString(): String {
        return navTitle.toString()
    }

    override fun setListeners() {
        enableBackAction(navBack)
    }

    override fun loadParam() {
        navTitle = getIntent().getStringExtra(EUIParamKeys.NAV_TITLE.toString());
        navBack = getIntent().getBooleanExtra(EUIParamKeys.NAV_BACK.toString(), false);
        transType = intent.extras!!.get(EUIParamKeys.TRANS_TYPE.toString()) as ETransType
        type = intent.getIntExtra(getString(R.string.redeem_type),0)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_redeem_bay_full)
        tickTimer.stop()

        btn_half_ok.setOnClickListener{
            var amount = if(redeemAmount.text != null && redeemAmount.text.toString() != "" && redeemAmount.text.toString() != ".") redeemAmount.text.toString().replace(",","") else "0"
            amount = formatter.format(amount.toDouble())
            var totalPoints = if(redeemPoints != "") redeemPoints else points.text.toString()
            var mktCode = if(promoCode.text != null) promoCode.text.toString() else ""
            var prdCode = if(productCode.text != null) productCode.text.toString() else ""
            var catalogType = if(catalogList.isShown) catalogList.selectedItem.toString() else ""

            if(amount.toDouble() <= 0){
                DialogUtils.showErrMessage(
                    this,
                    getString(R.string.menu_redeem_bay),
                    "Please Insert Amount",
                    null,
                    Constants.FAILED_DIALOG_SHOW_TIME
                )
            }else if( totalPoints.toInt() == 0){
                DialogUtils.showErrMessage(
                    this,
                    getString(R.string.menu_redeem_bay),
                    "Please Insert Points",
                    null,
                    Constants.FAILED_DIALOG_SHOW_TIME
                )
            }else{
                finish(ActionResult(TransResult.SUCC,
                    ActionEnterRedeemBay.RedeemBayInfo(
                        amount,
                        mktCode,
                        prdCode,
                        totalPoints,
                        catalogType
                    )
                ))
            }
        }

        btn_half_cancel.setOnClickListener{
            finish(ActionResult(TransResult.ERR_USER_CANCEL, null,null))
        }

        redeemAmount.addTextChangedListener(textWatcherAmount)
        redeemAmount.setOnKeyListener(object : View.OnKeyListener {
            override fun onKey(v: View, keyCode: Int, event: KeyEvent): Boolean {
                currentKey = TextWatcherDecimal.setCurrentKey(keyCode)
                return false
            }
        })

        imageButtonScanPromoCode.setOnClickListener{setScannerListener(promoCode)}

        imageButtonScanProductCode.setOnClickListener{setScannerListener(productCode)}

        //listener for Point selection box
        pointList.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {
            }

            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                val pointsValList = FinancialApplication.getApp().getResources().getStringArray(R.array.edc_redeem_bay_points_value_entries)
                var pointsVal = pointsValList[position].toString()
                if(pointsVal.equals("other")){
                    layout_pointText.visibility = View.VISIBLE
                    redeemPoints = ""
                }else if(pointsVal.equals("code")){
                    layout_pointText.visibility = View.VISIBLE
                    layout_promo_code.visibility = View.VISIBLE
                    redeemPoints = ""
                }else{
                    redeemPoints = pointList.selectedItem.toString()
                    layout_pointText.visibility = View.GONE
                    layout_promo_code.visibility = View.GONE
                    points.text.clear()
                    promoCode.text.clear()
                }
            }
        }

        layout_pointText.visibility = View.GONE
        layout_promo_code.visibility = View.GONE

        //Config UI for each Redeem type
        if(type == 1){//RedeemType 1=Full Freedom
            layout_amount.visibility = View.GONE
            layout_product_code.visibility = View.GONE
            layout_catalog_type.visibility = View.GONE
        }else if(type == 2){//RedeemType 2=Partial Freedom
            layout_pointText.visibility = View.VISIBLE
            layout_point_list.visibility = View.GONE
            layout_product_code.visibility = View.GONE
            layout_catalog_type.visibility = View.GONE
        }else if(type == 3){//RedeemType 3=Catalogue
            layout_promo_code.visibility = View.VISIBLE
            layout_amount.visibility = View.GONE
            layout_point_list.visibility = View.GONE
        }

        /*textViewNumpadCurrency.text = getString(
            R.string.include_numpad_currency,
            CurrencyConverter.formatter(CurrencyConverter.getDefCurrency(), applicationContext)
        )*/
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
            TextWatcherDecimal.calDecimal(redeemAmount,valueAsString,currentKey.toString(),12,this,s,count)
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
                Log.e("menu", "  onRead qrCode = " + qrCode.toString())
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

}

/*
 * ============================================================================
 * COPYRIGHT
 *              Pax CORPORATION PROPRIETARY INFORMATION
 *   This software is supplied under the terms of a license agreement or
 *   nondisclosure agreement with Pax Corporation and may not be copied
 *   or disclosed except in accordance with the terms in that agreement.
 *      Copyright (C) 2016 - ? Pax Corporation. All rights reserved.
 * Module Date: 2016-11-25
 * Module Author: Sim.G
 * Description:
 *
 * ============================================================================
 */
package com.pax.pay.trans.action.activity;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.ConditionVariable;
import android.text.Editable;
import android.text.InputFilter;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextWatcher;
import android.text.style.ForegroundColorSpan;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.pax.abl.core.AAction;
import com.pax.abl.core.ActionResult;
import com.pax.abl.utils.EncUtils;
import com.pax.abl.utils.PanUtils;
import com.pax.abl.utils.TrackUtils;
import com.pax.dal.ICardReaderHelper;
import com.pax.dal.entity.EReaderType;
import com.pax.dal.entity.PollingResult;
import com.pax.dal.exceptions.IccDevException;
import com.pax.dal.exceptions.MagDevException;
import com.pax.dal.exceptions.PiccDevException;
import com.pax.device.Device;
import com.pax.edc.R;
import com.pax.edc.opensdk.TransResult;
import com.pax.eventbus.EmvCallbackEvent;
import com.pax.eventbus.SearchCardEvent;
import com.pax.pay.BaseActivityWithTickForAction;
import com.pax.pay.ECR.EcrData;
import com.pax.pay.app.FinancialApplication;
import com.pax.pay.base.Acquirer;
import com.pax.pay.base.Issuer;
import com.pax.pay.constant.Constants;
import com.pax.pay.constant.EUIParamKeys;
import com.pax.pay.emv.EmvListenerImpl;
import com.pax.pay.emv.EmvSP200;
import com.pax.pay.emv.clss.ClssListenerImpl;
import com.pax.pay.trans.TransContext;
import com.pax.pay.trans.action.ActionInputPassword;
import com.pax.pay.trans.action.ActionSearchCard.CardInformation;
import com.pax.pay.trans.action.ActionSearchCard.SearchMode;
import com.pax.pay.trans.component.Component;
import com.pax.pay.uart.SP200_serialAPI;
import com.pax.pay.utils.CurrencyConverter;
import com.pax.pay.utils.EditorActionListener;
import com.pax.pay.utils.TickTimer;
import com.pax.pay.utils.ToastUtils;
import com.pax.pay.utils.Utils;
import com.pax.settings.SysParam;
import com.pax.view.ClssLight;
import com.pax.view.ClssLightsView;
import com.pax.view.dialog.DialogUtils;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

import th.co.bkkps.amexapi.AmexTransAPI;
import th.co.bkkps.utils.DynamicOffline;
import th.co.bkkps.utils.Log;

/**
 * SearchCardAction中跳转至SearchCardActivity,Intent传递两个参数;
 * <p>
 * {@link com.pax.pay.constant.EUIParamKeys#TRANS_AMOUNT},
 * {@link com.pax.pay.constant.EUIParamKeys #CARD_SEARCH_MODE}
 *
 * @author Sim.G
 */

public class SearchCardActivity extends BaseActivityWithTickForAction {
    public static final int REQ_ADJUST_TIP = 0;
    private final AAction currentAction = TransContext.getInstance().getCurrentAction();
    public String QRData;
    boolean isS200run;
    private ClssLightsView llClssLight;
    private ActionInputPassword inputPasswordAction = null;
    private TextView tvPromptTh; // 输入方式提示
    private TextView tvPromptEn; // 输入方式提示
    private EditText edtCardNo; // 输入框
    private EditText expDate; //卡有效期
    private TextView holderName; //显示持卡人姓名
    private TextView cardIssuer;
    private ImageButton qrBtn;
    private Button btnConfirm; // 确认按钮
    private Button btnCancel; // 确认按钮
    private ImageView ivSwipe; // 刷卡图标
    private ImageView ivInsert; // 插卡图标
    private ImageView ivTap; // 非接图标
    private SurfaceView sfCameraView;
    private LinearLayout llSupportedCard;
    private LinearLayout layoutCardInfo;
    private SurfaceView mCamSurfView;
    private String navTitle;
    private String amount; // 交易金额
    private String strAmount;
    private String cardNo; // 卡号
    private String searchCardPromptTh; // 寻卡提示
    private String searchCardPromptEn; // 寻卡提示
    private boolean supportManual = false; // 是否支持手输
    private boolean isManualMode = false;
    private boolean isQRMode = false;
    private boolean supportQR = false;
    private boolean supportRabbit = false;
    private boolean supportSP200 = false;
    private int retSP200 = 0;
    private EReaderType readerType = null; // 读卡类型
    private Issuer matchedIssuer = null;
    private List<Issuer> matchedIssuerList;
    private float iccAdjustPercent = 0;
    private int timeout;
    // 寻卡成功时，此界面还保留， 在后续界面切换时，还有机会跑到前台，此时按返回键，此activity finish，同时会有两个分支同时进行
    // 如果寻卡成功时， 此标志为true
    private boolean isSuccLeave = false;
    private int retryTime = 3;
    /**
     * 支持的寻卡类型{@link com.pax.pay.trans.action.ActionSearchCard.SearchMode}
     */
    private byte mode; // 寻卡模式
    private PollingResult pollingResult = null;
    private SearchCardThread searchCardThread = null;
    private SearchSP200Thread mSearchSP200Thread = null;
    private boolean isTimeOut = false;
    private ConditionVariable cv;
    private boolean isFinish = false;
    private boolean supportDualCard = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        enableBackAction(true);

        FinancialApplication.getApp().register(this);
        runSearchCardThread();

        if ((mSearchSP200Thread != null) && (mSearchSP200Thread.getState() == Thread.State.TERMINATED)) {
            mSearchSP200Thread.interrupt();
        }

        if (supportSP200) {

            String amountSP200 = FinancialApplication.getConvert().stringPadding(strAmount, '0', 12, com.pax.glwrapper.convert.IConvert.EPaddingPosition.PADDING_LEFT);
            SP200_serialAPI.getInstance().DoContactless(amountSP200);
            mSearchSP200Thread = new SearchSP200Thread();
            isS200run = true;
            mSearchSP200Thread.start();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        tickTimer.start(FinancialApplication.getSysParam().get(SysParam.NumberParam.SCREEN_TIME_OUT_SEARCH_CARD, TickTimer.DEFAULT_TIMEOUT));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        FinancialApplication.getApp().unregister(this);

        if (searchCardThread != null && searchCardThread.getState() == Thread.State.TERMINATED) {
            FinancialApplication.getDal().getCardReaderHelper().stopPolling();
            searchCardThread.interrupt();
        }
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onSearchCardEvent(SearchCardEvent event) {
        switch ((SearchCardEvent.Status) event.getStatus()) {
            case ICC_UPDATE_CARD_INFO:
                onUpdateCardInfo((EmvListenerImpl.CardInfo) event.getData());
                break;
            case ICC_CONFIRM_CARD_NUM:
                onCardNumConfirm();
                break;
            case CLSS_UPDATE_CARD_INFO:
                onUpdateCardInfo((ClssListenerImpl.CardInfo) event.getData());
                break;
            case CLSS_LIGHT_STATUS_NOT_READY:
                llClssLight.setLights(-1, ClssLight.OFF);
                break;
            case CLSS_LIGHT_STATUS_IDLE:
            case CLSS_LIGHT_STATUS_READY_FOR_TXN:
                llClssLight.setLights(0, ClssLight.BLINK);
                break;
            case CLSS_LIGHT_STATUS_PROCESSING:
                llClssLight.setLights(1, ClssLight.ON);
                break;
            case CLSS_LIGHT_STATUS_REMOVE_CARD:
                llClssLight.setLights(2, ClssLight.ON);
                break;
            case CLSS_LIGHT_STATUS_COMPLETE:
                llClssLight.setLights(2, ClssLight.BLINK);
                break;
            case CLSS_LIGHT_STATUS_ERROR:
                llClssLight.setLights(3, ClssLight.BLINK);
                break;
            default:
                break;
        }
    }

    @Override
    protected void loadParam() {
        Bundle bundle = getIntent().getExtras();

        navTitle = getIntent().getStringExtra(EUIParamKeys.NAV_TITLE.toString());
        // 显示金额
        try {
            strAmount = bundle.getString(EUIParamKeys.TRANS_AMOUNT.toString());
            if (strAmount != null && !strAmount.isEmpty() && Utils.parseLongSafe(strAmount, 0) > 0) {
                amount = CurrencyConverter.convert(Utils.parseLongSafe(strAmount, 0));
            }
        } catch (Exception e) {
            Log.w(TAG, "", e);
            amount = null;
        }

        // 寻卡方式
        try {
            mode = bundle.getByte(EUIParamKeys.CARD_SEARCH_MODE.toString(), SearchMode.SWIPE);
            supportDualCard = getIntent().getBooleanExtra(EUIParamKeys.SUPP_DUAL_CARD.toString(), false);

            // 是否支持手输卡号
            if (FinancialApplication.getSysParam().get(SysParam.BooleanParam.EDC_ENABLE_KEYIN)
                    && !DynamicOffline.getInstance().isDynamicOfflineActiveStatus()) {
                supportManual = (mode & SearchMode.KEYIN) == SearchMode.KEYIN;
            } else {
                supportManual = false;
            }

            supportQR = (mode & SearchMode.QR) == SearchMode.QR;
            supportRabbit = (mode & SearchMode.RABBIT) == SearchMode.RABBIT;
            supportSP200 = SP200_serialAPI.getInstance().isSp200Enable() & (mode & SearchMode.WAVE) == SearchMode.WAVE;

            readerType = toReaderType(mode);
        } catch (Exception e) {
            Log.w(TAG, "", e);
        }

        // 获取寻卡提醒
        if ((mode & SearchMode.WAVE) != SearchMode.WAVE && ((mode & SearchMode.KEYIN) == SearchMode.KEYIN || supportManual)) {
            searchCardPromptTh = Utils.getThString(R.string.prompt_insert_swipe_keyin);
            searchCardPromptEn = Utils.getEnString(R.string.prompt_insert_swipe_keyin);
        } else if ((mode & SearchMode.WAVE) != SearchMode.WAVE && ((mode & SearchMode.KEYIN) != SearchMode.KEYIN || !supportManual)) {
            searchCardPromptTh = Utils.getThString(R.string.prompt_insert_swipe_card);
            searchCardPromptEn = Utils.getEnString(R.string.prompt_insert_swipe_card);
        } else if ((mode & SearchMode.WAVE) == SearchMode.WAVE && ((mode & SearchMode.KEYIN) != SearchMode.KEYIN || !supportManual)) {
            searchCardPromptTh = Utils.getThString(R.string.prompt_insert_swipe_wave_card);
            searchCardPromptEn = Utils.getEnString(R.string.prompt_insert_swipe_wave_card);
        } else {
            searchCardPromptTh = Utils.getThString(R.string.prompt_default_searchCard_prompt);
            searchCardPromptEn = Utils.getEnString(R.string.prompt_default_searchCard_prompt);
        }

        try {
            String prompt = bundle.getString(EUIParamKeys.SEARCH_CARD_PROMPT.toString());
            if (prompt != null && !prompt.isEmpty()) {
                searchCardPromptTh = prompt;
                searchCardPromptEn = "";
            }
        } catch (Exception e) {
            Log.w(TAG, "", e);
        }

        timeout = bundle.getInt("activity_timeout", 60);
        tickTimer.start(timeout);

    }

    /**
     * 获取ReaderType
     *
     * @param mode {@link SearchMode}
     * @return {@link EReaderType}
     */
    private EReaderType toReaderType(byte mode) {
        byte newMode = (byte) (mode & (~SearchMode.KEYIN) & (~SearchMode.QR) & (~SearchMode.RABBIT) & (~SearchMode.SP200));
        EReaderType[] types = EReaderType.values();
        for (EReaderType type : types) {
            if (type.getEReaderType() == newMode)
                return type;
        }
        return null;
    }

    @Override
    protected int getLayoutId() {
        return R.layout.activity_bankcard_pay;
    }

    @Override
    protected String getTitleString() {
        return navTitle;
    }

    @Override
    protected void initViews() {
        initDefaultViews();
    }

    // 默认寻卡界面初始化
    private void initDefaultViews() {
        llClssLight = (ClssLightsView) findViewById(R.id.clssLight);
        llClssLight.setVisibility(View.GONE);
        if ((mode & SearchMode.WAVE) == SearchMode.WAVE) {
            llClssLight.setVisibility(View.VISIBLE);
            llClssLight.setLights(0, ClssLight.BLINK);
        }

        LinearLayout llAmount = (LinearLayout) findViewById(R.id.amount_layout);
        if (amount == null || amount.isEmpty()) { // 余额查询不显示金额
            llAmount.setVisibility(View.INVISIBLE);
        } else {
            TextView tvAmount = (TextView) findViewById(R.id.amount_txt); // 只显示交易金额
            tvAmount.setText(amount);
        }

        edtCardNo = (EditText) findViewById(R.id.bank_card_number);// 初始为卡号输入框
        expDate = (EditText) findViewById(R.id.bank_card_expdate);//卡有效期输入框

        holderName = (TextView) findViewById(R.id.bank_card_holder_name);
        holderName.setVisibility(View.GONE);

        cardIssuer = (TextView) findViewById(R.id.card_issuer);
        cardIssuer.setVisibility(View.GONE);


        btnConfirm = (Button) findViewById(R.id.ok_btn);
        btnConfirm.setEnabled(false);

        btnCancel = (Button) findViewById(R.id.cancel_btn);
        if (btnCancel != null && EcrData.instance.isOnProcessing) {
            btnCancel.setVisibility(View.VISIBLE);
        } else {
            btnCancel.setVisibility(View.GONE);
        }

        tvPromptTh = (TextView) findViewById(R.id.tv_prompt_readcard_th);
        tvPromptEn = (TextView) findViewById(R.id.tv_prompt_readcard_en);

        ivSwipe = (ImageView) findViewById(R.id.iv_swipe);
        ivInsert = (ImageView) findViewById(R.id.iv_insert);
        ivTap = (ImageView) findViewById(R.id.iv_tap);
        sfCameraView = (SurfaceView) findViewById(R.id.camera_view);

        tvPromptTh.setText(searchCardPromptTh);
        tvPromptEn.setText(searchCardPromptEn);


        if (supportManual) {
            edtCardNo.setHint(R.string.prompt_card_num_manual);
            edtCardNo.setHintTextColor(getResources().getColor(R.color.secondary_text_light));
            edtCardNo.setClickable(true);// 支持手输卡号
            edtCardNo.addTextChangedListener(new CardNoWatcher());
            edtCardNo.setFilters(new InputFilter[]{new InputFilter.LengthFilter(19 + 4)});// 4为卡号分隔符个数
            edtCardNo.setCursorVisible(true);

            expDate.setVisibility(View.GONE);
            expDate.setClickable(true);// 支持手输卡号
            expDate.addTextChangedListener(new ExpDateWatcher());
            expDate.setFilters(new InputFilter[]{new InputFilter.LengthFilter(4 + 1)});// 4为卡号分隔符个数
            expDate.setCursorVisible(true);

            edtCardNo.setOnEditorActionListener(new EditorActionListener() {
                @Override
                protected void onKeyOk() {
                    processManualCardNo();
                }

                @Override
                protected void onKeyCancel() {
                    //do nothing
                }
            });
            expDate.setOnEditorActionListener(new EditorActionListener() {
                @Override
                protected void onKeyOk() {
                    processManualExpDate();
                }

                @Override
                protected void onKeyCancel() {
                    //do nothing
                }
            });
        } else {
            edtCardNo.setEnabled(false);// 不支持手输入卡号
            expDate.setEnabled(false);// 不支持手输入卡号
        }

        qrBtn = (ImageButton) findViewById(R.id.qr_scanner);
        if (supportQR) {
            qrBtn.setOnClickListener(this);
            qrBtn.setFocusable(false);
        } else {
            qrBtn.setVisibility(View.GONE);
        }

        btnConfirm.setVisibility(View.INVISIBLE);
        llSupportedCard = (LinearLayout) findViewById(R.id.supported_card_prompt);
        layoutCardInfo = (LinearLayout) findViewById(R.id.card_info);
        setSearchCardImage((mode & SearchMode.SWIPE) == SearchMode.SWIPE,
                (mode & SearchMode.INSERT) == SearchMode.INSERT, (mode & SearchMode.WAVE) == SearchMode.WAVE);


    }

    @Override
    protected void setListeners() {
        btnConfirm.setOnClickListener(this);
        btnCancel.setOnClickListener(this);
    }

    @Override
    public void onClickProtected(View v) {
        switch (v.getId()) { // manual input case: get click event from IME_ACTION_DONE, the button is always hidden.
            case R.id.ok_btn:
                onOkClicked();
                break;
            case R.id.qr_scanner:
                isQRMode = true;
                llClssLight.setLights(-1, ClssLight.OFF);
                FinancialApplication.getDal().getCardReaderHelper().stopPolling();
                finish(new ActionResult(TransResult.SUCC, new CardInformation(SearchMode.QR)));
                break;
            case R.id.cancel_btn:
                finish(new ActionResult(TransResult.ERR_USER_CANCEL, null));
                break;
        }
    }

    @Override
    protected boolean onOptionsItemSelectedSub(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onHeaderBackClicked();
            return true;
        }
        return super.onOptionsItemSelectedSub(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQ_ADJUST_TIP && data != null) { //AET-82
            updateAmount(data);
        }
    }

    private void updateAmount(@NonNull Intent data) {
        amount = data.getStringExtra(EUIParamKeys.TRANS_AMOUNT.toString());
        String tipAmount = data.getStringExtra(EUIParamKeys.TIP_AMOUNT.toString());
        TextView tvAmount = (TextView) findViewById(R.id.amount_txt);
        tvAmount.setText(amount);
        FinancialApplication.getApp().doEvent(new EmvCallbackEvent(EmvCallbackEvent.Status.CARD_NUM_CONFIRM_SUCCESS,
                new String[]{amount, tipAmount}));
        isSuccLeave = true; //AET-106
    }

    private void runSearchCardThread() {
        if (searchCardThread != null && searchCardThread.getState() == Thread.State.TERMINATED) {
            FinancialApplication.getDal().getCardReaderHelper().stopPolling();
            searchCardThread.interrupt();
        }
        isManualMode = false;
        isQRMode = false;
        searchCardThread = new SearchCardThread();
        searchCardThread.start();

        if (supportQR) {
            isQRMode = true;

            ivTap.setVisibility(View.GONE);
            sfCameraView.setVisibility(View.VISIBLE);
        }
    }

    private void onReadCardCancel() {
        if (isManualMode) return; //EDCBBLAND-651: Fix issue manual key-in
        if (!isManualMode && !isQRMode) { // AET-179
            Log.i(TAG, "SEARCH CARD CANCEL");
            FinancialApplication.getDal().getCardReaderHelper().stopPolling();
            if (supportSP200) {
                SP200_serialAPI.getInstance().BreakReceiveThread();
            }
        }
        finish(new ActionResult(TransResult.ERR_USER_CANCEL, null));
    }

    private void onReadCardError() {
        ToastUtils.showMessage(R.string.prompt_please_retry);
        runSearchCardThread();
    }

    private void onEditCardNo() {
        isManualMode = true;
        FinancialApplication.getDal().getCardReaderHelper().stopPolling();

        llClssLight.setLights(-1, ClssLight.OFF);
        quickClickProtection.stop();
        cardNo = edtCardNo.getText().toString().replace(" ", "");
        expDate.setText("");
        expDate.setVisibility(View.VISIBLE);
        expDate.requestFocus();
    }

    private void onEditCardNoError() {
        ToastUtils.showMessage(R.string.prompt_card_num_err);
        edtCardNo.setText("");
        edtCardNo.requestFocus();
    }

    private void onEditDate() {
        // Fixed EDCBBLAND-380: Remove password popup for manual key in.
        onVerifyManualIssuer();
//        runInputMerchantPwdAction();
    }

    private void onEditDateError() {
        ToastUtils.showMessage(R.string.prompt_card_date_err);
        expDate.setText("");
        expDate.requestFocus();
    }

    private void onVerifyManualIssuer() {
        matchedIssuerList = FinancialApplication.getAcqManager().findAllIssuerByPan(cardNo, null);
        if (matchedIssuerList == null || matchedIssuerList.isEmpty()) {
            Log.i(TAG, "onVerifyManualIssuer[EDC_No_Issuer]");
            Acquirer acqAmex = FinancialApplication.getAcqManager().findActiveAcquirer(Constants.ACQ_AMEX);
            if (acqAmex != null && AmexTransAPI.getInstance().getProcess().isAppInstalled(this)) {
                String date = expDate.getText().toString().replace("/", "");
                if (!date.isEmpty()) {
                    date = date.substring(2) + date.substring(0, 2);// 将MMyy转换成yyMM
                }
                CardInformation cardInfo = new CardInformation(SearchMode.KEYIN, cardNo, date, null);
                stopSp200Thread();
                finish(new ActionResult(TransResult.ERR_NEED_FORWARD_TO_AMEX_API, cardInfo));
            } else {
                finish(new ActionResult(TransResult.ERR_CARD_UNSUPPORTED, null));
            }
            return;
        }

        if (matchedIssuerList.size() > 1) {
            FinancialApplication.getApp().runOnUiThread(new SelectIssuerRunnable(true, matchedIssuerList));
        } else {
            // Fixed EDCBBLAND-365 handle in case no matching card range.
            matchedIssuer = matchedIssuerList.get(0);
            Log.i(TAG, "onVerifyManualIssuer[IssuerName=" + matchedIssuer.getIssuerName() + ", IssuerBrand=" + matchedIssuer.getIssuerBrand() + "]");
            onVerifyManualPan();
        }
    }

    private void onVerifyManualPan() {
        String date = expDate.getText().toString().replace("/", "");
        if (!date.isEmpty()) {
            date = date.substring(2) + date.substring(0, 2);// 将MMyy转换成yyMM
        }
        if (matchedIssuer == null || !FinancialApplication.getAcqManager().isIssuerSupported(matchedIssuer)) {
            Log.i(TAG, matchedIssuer != null ? "onVerifyManualIssuer[EDC_Not_Support, IssuerName=" + matchedIssuer.getIssuerName() + ", IssuerBrand=" + matchedIssuer.getIssuerBrand() + "]" : "onVerifyManualIssuer[EDC_No_Issuer]");
            Acquirer acqAmex = FinancialApplication.getAcqManager().findActiveAcquirer(Constants.ACQ_AMEX);
            if (acqAmex != null && AmexTransAPI.getInstance().getProcess().isAppInstalled(this)) {
                CardInformation cardInfo = new CardInformation(SearchMode.KEYIN, cardNo, date, null);
                stopSp200Thread();
                finish(new ActionResult(TransResult.ERR_NEED_FORWARD_TO_AMEX_API, cardInfo));
            } else {
                finish(new ActionResult(TransResult.ERR_CARD_UNSUPPORTED, null));
            }
            return;
        }

        if (onChkForceSettlement(matchedIssuer)) {
            finish(new ActionResult(TransResult.ERR_SETTLE_NOT_COMPLETED, null));
            return;
        }

        if (!matchedIssuer.isAllowManualPan()) {
            finish(new ActionResult(TransResult.ERR_UNSUPPORTED_FUNC, null));
            return;
        }

        if (!Issuer.validPan(matchedIssuer, cardNo)) {
            finish(new ActionResult(TransResult.ERR_CARD_INVALID, null));
            return;
        }

        if (!Issuer.validCardExpiry(matchedIssuer, date)) {
            finish(new ActionResult(TransResult.ERR_CARD_EXPIRED, null));
            return;
        }

        CardInformation cardInfo = new CardInformation(SearchMode.KEYIN, cardNo, date, matchedIssuer);

        stopSp200Thread();

        finish(new ActionResult(TransResult.SUCC, cardInfo));
    }

    private void onUpdateCardInfo(EmvListenerImpl.CardInfo cardInfo) {
        edtCardNo.setVisibility(View.INVISIBLE);

        if (cardInfo.getIssuer() != null) {
            cardIssuer.setVisibility(View.VISIBLE);
            cardIssuer.setText(cardInfo.getIssuer().trim());
        }
        edtCardNo.setVisibility(View.GONE);
        expDate.setVisibility(View.GONE);
        iccAdjustPercent = cardInfo.getAdjustPercent();
    }

    private void onUpdateCardInfo(ClssListenerImpl.CardInfo cardInfo) {
        edtCardNo.setVisibility(View.INVISIBLE);

        if (cardInfo.getIssuer() != null) {
            cardIssuer.setVisibility(View.VISIBLE);
            cardIssuer.setText(cardInfo.getIssuer().trim());
        }
        edtCardNo.setVisibility(View.GONE);
        expDate.setVisibility(View.GONE);
        iccAdjustPercent = cardInfo.getAdjustPercent();
    }

    private void onCardNumConfirm() {
        isSuccLeave = false;
        edtCardNo.setClickable(false);
        //AET-120
        tickTimer.start();
        //AET-135
        Utils.wakeupScreen(TickTimer.DEFAULT_TIMEOUT);
        confirmBtnChange();
        onOkClicked();
    }

    private void onClickBack() {
        //AET-147
        edtCardNo.setText("");
        edtCardNo.clearFocus();
        if (cardNo != null) {
            cardNo = null;
            expDate.setText("");
            expDate.setVisibility(View.GONE);
            expDate.clearFocus();
            //AET-155
            llClssLight.setLights(0, ClssLight.BLINK);
            runSearchCardThread();
            tickTimer.stop();
            tickTimer.start();
        }
    }

    /**
     * 设置图标显示
     *
     * @param mag  enable mag
     * @param icc  enable icc
     * @param picc enable picc
     */
    private void setSearchCardImage(boolean mag, boolean icc, boolean picc) {
        ivSwipe.setImageResource(mag ? R.drawable.swipe_card : R.drawable.no_swipe_card);
        ivInsert.setImageResource(icc ? R.drawable.insert_card : R.drawable.no_insert_card);
        ivTap.setImageResource(picc ? R.drawable.tap_card : R.drawable.no_tap_card);
    }

    @Override
    protected boolean onKeyBackDown() {
        if (pollingResult != null && pollingResult.getReaderType() == EReaderType.ICC) {
            FinancialApplication.getApp().doEvent(new EmvCallbackEvent(EmvCallbackEvent.Status.CARD_NUM_CONFIRM_ERROR));
        } else {
            if (isManualMode)
                onClickBack();
            else
                FinancialApplication.getApp().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        onReadCardCancel();
                    }
                });
        }
        return true;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        return isSuccLeave || super.onKeyDown(keyCode, event);
    }

    @Override
    public void finish(ActionResult result) {
        //if (searchCardThread != null && searchCardThread.getState() == Thread.State.TERMINATED) {

        //    searchCardThread.interrupt();
        //}
        if (isFinish) return;
        isFinish = true;

        FinancialApplication.getDal().getCardReaderHelper().stopPolling();
        CardInformation cardInfo = (CardInformation) result.getData();

        if (cardInfo != null) {
            if (supportSP200
                    && cardInfo.getSearchMode() != SearchMode.SP200) {
                SP200_serialAPI.getInstance().BreakReceiveThread();
            }
        }

        if (result.getRet() == TransResult.SUCC) {
            isSuccLeave = true;
        }

        //mSearchBBSThread
        super.finish(result);
    }

    // 填写信息校验
    private void processMag() {
        String content = edtCardNo.getText().toString().replace(" ", "");
        if (content == null || content.isEmpty()) {
            FinancialApplication.getApp().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    onReadCardError();
                }
            });
            return;
        }
    }

    private void processMagCon() {

        if (pollingResult != null && pollingResult.getReaderType() == EReaderType.MAG) {
            String pan = TrackUtils.getPan(pollingResult.getTrack2());

            if (pan == null) {
                finish(new ActionResult(TransResult.ERR_CARD_INVALID, null));
                return;
            }

            matchedIssuerList = FinancialApplication.getAcqManager().findAllIssuerByPan(pan, null);
            if (matchedIssuerList == null || matchedIssuerList.isEmpty()) {
                Log.i(TAG, "processMagCon[EDC_No_Issuer]");
                Acquirer acqAmex = FinancialApplication.getAcqManager().findActiveAcquirer(Constants.ACQ_AMEX);
                if (acqAmex != null && AmexTransAPI.getInstance().getProcess().isAppInstalled(this)) {
                    CardInformation cardInfo = new CardInformation(SearchMode.SWIPE, pollingResult.getTrack1(), pollingResult.getTrack2(),
                            pollingResult.getTrack3(), TrackUtils.getPan(pollingResult.getTrack2()), matchedIssuer);
                    stopSp200Thread();
                    finish(new ActionResult(TransResult.ERR_NEED_FORWARD_TO_AMEX_API, cardInfo));
                } else {
                    finish(new ActionResult(TransResult.ERR_CARD_UNSUPPORTED, null));
                }
                return;
            }

            if (matchedIssuerList.size() > 1) {
                cv = new ConditionVariable();
                FinancialApplication.getApp().runOnUiThread(new SelectIssuerRunnable(true, matchedIssuerList));
                cv.block();
            } else { // Fixed EDCBBLAND-365 handle in case no matching card range.
                matchedIssuer = matchedIssuerList.get(0);
                Log.i(TAG, "processMagCon[IssuerName=" + matchedIssuer.getIssuerName() + ", IssuerBrand=" + matchedIssuer.getIssuerBrand() + "]");
            }

            if (matchedIssuer == null || !FinancialApplication.getAcqManager().isIssuerSupported(matchedIssuer)) {
                Log.i(TAG, matchedIssuer != null ? "processMagCon[EDC_Not_Support, IssuerName=" + matchedIssuer.getIssuerName() + ", IssuerBrand=" + matchedIssuer.getIssuerBrand() + "]" : "processMagCon[EDC_No_Issuer]");
                Acquirer acqAmex = FinancialApplication.getAcqManager().findActiveAcquirer(Constants.ACQ_AMEX);
                if (acqAmex != null && AmexTransAPI.getInstance().getProcess().isAppInstalled(this)) {
                    CardInformation cardInfo = new CardInformation(SearchMode.SWIPE, pollingResult.getTrack1(), pollingResult.getTrack2(),
                            pollingResult.getTrack3(), TrackUtils.getPan(pollingResult.getTrack2()), matchedIssuer);
                    stopSp200Thread();
                    finish(new ActionResult(TransResult.ERR_NEED_FORWARD_TO_AMEX_API, cardInfo));
                } else {
                    finish(new ActionResult(TransResult.ERR_CARD_UNSUPPORTED, null));
                }
                return;
            }

            if (onChkForceSettlement(matchedIssuer)) {
                finish(new ActionResult(TransResult.ERR_SETTLE_NOT_COMPLETED, null));
                return;
            }

            if (!Issuer.validPan(matchedIssuer, pan)) {
                finish(new ActionResult(TransResult.ERR_CARD_INVALID, null));
                return;
            }

            if (!Issuer.validCardExpiry(matchedIssuer, TrackUtils.getExpDate(pollingResult.getTrack2()))) {
                finish(new ActionResult(TransResult.ERR_CARD_EXPIRED, null));
                return;
            }
        } else {
            finish(new ActionResult(TransResult.ERR_CARD_INVALID, null));
            return;
        }

        CardInformation cardInfo = new CardInformation(SearchMode.SWIPE, pollingResult.getTrack1(), pollingResult.getTrack2(),
                pollingResult.getTrack3(), TrackUtils.getPan(pollingResult.getTrack2()), matchedIssuer);

        stopSp200Thread();

        finish(new ActionResult(TransResult.SUCC, cardInfo));

    }

    private void stopSp200Thread() {
        isS200run = false;
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private boolean onChkForceSettlement(Issuer issuer) {
        Acquirer acquirer = FinancialApplication.getAcqManager().mapAcquirerByIssuer(issuer);
        if (acquirer != null) {
            return Component.chkSettlementStatus(acquirer.getName());
        }
        return false;
    }

    private void onHeaderBackClicked() {
        if (isSuccLeave) {
            return;
        }
        if (pollingResult != null && pollingResult.getReaderType() == EReaderType.ICC) {
            FinancialApplication.getApp().doEvent(new EmvCallbackEvent(EmvCallbackEvent.Status.CARD_NUM_CONFIRM_ERROR));
        } else {
            FinancialApplication.getApp().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    onReadCardCancel();
                }
            });
        }
    }

    private void onOkClicked() {
        tickTimer.stop();
        btnConfirm.setEnabled(false);
        if (pollingResult != null && pollingResult.getReaderType() == EReaderType.ICC) {
            boolean enableTip = FinancialApplication.getSysParam().get(SysParam.BooleanParam.EDC_SUPPORT_TIP);
            if (enableTip && iccAdjustPercent > 0) {
                long baseAmountLong = CurrencyConverter.parse(amount);
                Intent intent = new Intent(SearchCardActivity.this, AdjustTipActivity.class);
                intent.putExtra(EUIParamKeys.NAV_TITLE.toString(), navTitle);
                intent.putExtra(EUIParamKeys.TRANS_AMOUNT.toString(), String.valueOf(baseAmountLong));
                intent.putExtra(EUIParamKeys.TIP_PERCENT.toString(), iccAdjustPercent);
                intent.putExtra(EUIParamKeys.CARD_MODE.toString(), EReaderType.ICC.toString());
                startActivityForResult(intent, REQ_ADJUST_TIP);
            } else {
                FinancialApplication.getApp().doEvent(new EmvCallbackEvent(EmvCallbackEvent.Status.CARD_NUM_CONFIRM_SUCCESS));
            }
        } else if (pollingResult != null && pollingResult.getReaderType() == EReaderType.MAG) {
            processMag();
        }
    }

    private void processManualCardNo() {
        final String content = edtCardNo.getText().toString().replace(" ", "");
        if (content.length() < 13) {
            onEditCardNoError();
        } else {
            onEditCardNo();
        }
    }

    private void processManualExpDate() {
        final String content = expDate.getText().toString().replace(" ", "");
        if (content == null || content.isEmpty()) {
            expDate.setText("");
            expDate.requestFocus();
        } else {
            if (dateProcess(content)) {
                onEditDate();
            } else {
                onEditDateError();
            }
        }
    }

    private boolean dateProcess(String content) {
        final String mmYY = "MM/yy";
        if (content.length() != mmYY.length()) {
            return false;
        }

        SimpleDateFormat dateFormat = new SimpleDateFormat(mmYY, Locale.US);
        dateFormat.setLenient(false);
        try {
            dateFormat.parse(content);
        } catch (ParseException e) {
            Log.w(TAG, "", e);
            return false;
        }

        return true;
    }

    private void confirmBtnChange() {
        String content = edtCardNo.getText().toString();
        if (!content.isEmpty()) {
            btnConfirm.setEnabled(true);
            btnConfirm.setVisibility(View.VISIBLE);
        } else {
            btnConfirm.setEnabled(false);
            btnConfirm.setVisibility(View.INVISIBLE);
        }
    }

    private void runInputMerchantPwdAction() {
        inputPasswordAction = new ActionInputPassword(new AAction.ActionStartListener() {

            @Override
            public void onStart(AAction action) {
                ((ActionInputPassword) action).setParam(SearchCardActivity.this, 6,
                        getString(R.string.prompt_merchant_pwd), null, false);
                ((ActionInputPassword) action).setParam(TransResult.ERR_USER_CANCEL);
            }
        });

        inputPasswordAction.setEndListener(new AAction.ActionEndListener() {

            @Override
            public void onEnd(AAction action, ActionResult result) {
                TransContext.getInstance().setCurrentAction(currentAction);

                if (result.getRet() != TransResult.SUCC) {
                    //AET-156
                    finish(new ActionResult(result.getRet(), null));
                    return;
                }

                String data = EncUtils.sha1((String) result.getData());
                if (!data.equals(FinancialApplication.getSysParam().get(SysParam.StringParam.SEC_MERCHANT_PWD))) {
                    //retry three times
                    retryTime--;
                    if (retryTime > 0) {
                        // AET-110, AET-157
                        DialogUtils.showErrMessage(SearchCardActivity.this, getString(R.string.trans_password),
                                getString(R.string.err_password), new DialogInterface.OnDismissListener() {
                                    @Override
                                    public void onDismiss(DialogInterface dialog) {
                                        FinancialApplication.getApp().runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                //AET-158
                                                if (!isTimeOut) {
                                                    onEditDate();
                                                }
                                            }
                                        });
                                    }
                                }, Constants.FAILED_DIALOG_SHOW_TIME);
                    } else {
                        finish(new ActionResult(TransResult.ERR_PASSWORD, null));
                    }
                    return;
                }
                onVerifyManualIssuer();
            }
        });

        inputPasswordAction.execute();
    }

    //AET-158
    @Override
    protected void onTimerFinish() {
        TransContext.getInstance().setCurrentAction(currentAction);
        currentAction.setFinished(false); //AET-253
        if (inputPasswordAction != null) {
            inputPasswordAction.setResult(new ActionResult(TransResult.ERR_TIMEOUT, null));
        }

        isTimeOut = true;
        super.onTimerFinish();
    }

    @Override
    protected void onTimerTick(long timeleft) {
        btnCancel.setText(String.format(getString(R.string.title_erc_payment_select_cancel_button_with_timer).toString(), timeleft));
        super.onTimerTick(timeleft);
    }

    // 寻卡线程
    private class SearchCardThread extends Thread {

        @Override
        public void run() {
            try {
                ICardReaderHelper cardReaderHelper = FinancialApplication.getDal().getCardReaderHelper();
                if (readerType == null) {
                    return;
                }
                pollingResult = null;
                pollingResult = cardReaderHelper.polling(readerType, 60 * 1000);

                if (supportSP200) {
                    SP200_serialAPI.getInstance().BreakReceiveThread();
                }

                cardReaderHelper.stopPolling();
                if (isFinish) return;
                if (pollingResult.getOperationType() == PollingResult.EOperationType.CANCEL
                        || pollingResult.getOperationType() == PollingResult.EOperationType.TIMEOUT) {
                    FinancialApplication.getApp().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            onReadCardCancel();
                        }
                    });
                } else if (pollingResult.getOperationType() == PollingResult.EOperationType.OK) {
                    FinancialApplication.getApp().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            onReadCardOk();
                        }
                    });

                    // Fixed EDCBBLAND-366 Not allow Chip card to do transaction by swipe.
                    // supportDualCard for transactioin that support to use magnetic on chip card
                    if (pollingResult.getReaderType() == EReaderType.MAG &&
                            (supportDualCard || (mode & SearchMode.INSERT) != SearchMode.INSERT || !TrackUtils.isIcCard(pollingResult.getTrack2()))) {
                        processMagCon();
                    }
                }

            } catch (MagDevException | IccDevException | PiccDevException e) {
                Log.e(TAG, "SearchCardThread", e);
                // 读卡失败处理
                FinancialApplication.getApp().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        onReadCardError();
                    }
                });
            }
        }

        private void onReadCardOk() {
            //case of allowing Fallback
            if (pollingResult.getReaderType() == EReaderType.MAG) {
                if (!supportDualCard && ((mode & SearchMode.INSERT) == SearchMode.INSERT && TrackUtils.isIcCard(pollingResult.getTrack2()))) {
                    Device.beepErr();
                    ToastUtils.showMessage(R.string.prompt_ic_card_input);
                    mode &= ~SearchMode.SWIPE;
                    readerType = toReaderType(mode);
                    setSearchCardImage((mode & SearchMode.SWIPE) == SearchMode.SWIPE,
                            (mode & SearchMode.INSERT) == SearchMode.INSERT,
                            (mode & SearchMode.WAVE) == SearchMode.WAVE);
                    tvPromptTh.setText(searchCardPromptTh);
                    tvPromptEn.setText(searchCardPromptEn);
                    runSearchCardThread();
                    return;
                }

                Device.beepPrompt();
                // 有时刷卡成功，单没有磁道II，做一下防护
                String track2 = pollingResult.getTrack2();
                String track1 = pollingResult.getTrack1();
                String pan = TrackUtils.getPan(track2);
                String exp = TrackUtils.getExpDate(track2);
                String cardholder = TrackUtils.getHolderName(track1);

                //日期显示格式为（MMYY）
                if (exp == null || exp.length() != 4) {
                    Device.beepErr();
                    setSearchCardImage((mode & SearchMode.SWIPE) == SearchMode.SWIPE,
                            (mode & SearchMode.INSERT) == SearchMode.INSERT,
                            (mode & SearchMode.WAVE) == SearchMode.WAVE);
                    tvPromptTh.setText(searchCardPromptTh);
                    tvPromptEn.setText(searchCardPromptEn);
                    runSearchCardThread();
                    return;
                }

                edtCardNo.setEnabled(false);
                edtCardNo.setText(PanUtils.separateWithSpace(pan));
                expDate.setVisibility(View.VISIBLE);
                expDate.setEnabled(false);

                //MM/YY
                exp = exp.substring(2, 4) + "/" + exp.substring(0, 2);
                expDate.setText(exp);

                //持卡人姓名为非空时才可见
                if (cardholder != null) {
                    holderName.setVisibility(View.VISIBLE);
                    holderName.setText(cardholder.trim());
                }

                llSupportedCard.setVisibility(View.INVISIBLE);
                layoutCardInfo.setVisibility(View.INVISIBLE);
                qrBtn.setEnabled(false);
                qrBtn.setVisibility(View.INVISIBLE);
                llClssLight.setLights(-1, ClssLight.OFF);
                //AET-136
                tickTimer.start();
                Utils.wakeupScreen(TickTimer.DEFAULT_TIMEOUT);
                confirmBtnChange();
                btnConfirm.setVisibility(View.INVISIBLE);
                onOkClicked();

            } else if (pollingResult.getReaderType() == EReaderType.ICC) {
                //需要通过EMV才能获取到卡号等信息,所以先在EMV里面获取到信息，再到case CARD_NUM_CONFIRM中显示
                disableNonCardView();
                llClssLight.setLights(-1, ClssLight.OFF);
                stopSp200Thread();
                finish(new ActionResult(TransResult.SUCC, new CardInformation(SearchMode.INSERT)));
            } else if (pollingResult.getReaderType() == EReaderType.PICC) {
                disableNonCardView();
                llClssLight.setVisibility(View.VISIBLE);
                stopSp200Thread();
                finish(new ActionResult(TransResult.SUCC, new CardInformation(SearchMode.WAVE)));
            }
        }

        private void disableNonCardView() {
            llSupportedCard.setVisibility(View.INVISIBLE);
            edtCardNo.setEnabled(false);
            expDate.setEnabled(false);
            qrBtn.setEnabled(false);
            qrBtn.setVisibility(View.INVISIBLE);
        }

    }

    // 卡号分割及输入长度检查
    private class CardNoWatcher implements TextWatcher {

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            if (s.length() == 0)
                return;

            if (before < count) {
                String card = s.toString().replace(" ", "");
                card = card.replaceAll("(\\d{4}(?!$))", "$1 ");
                if (!card.equals(s.toString())) {
                    edtCardNo.setText(card);
                    edtCardNo.setSelection(card.length());
                }
            }
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            //do nothing
        }

        @Override
        public void afterTextChanged(Editable s) {
            //do nothing
        }
    }

    private class ExpDateWatcher implements TextWatcher {

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            if (s.length() == 0)
                return;
            String exp = s.toString().replace("/", "");
            exp = exp.replaceAll("(\\d{2}(?!$))", "$1/");
            if (!exp.equals(s.toString())) {
                expDate.setText(exp);
                expDate.setSelection(exp.length());
            }
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            //do nothing
        }

        @Override
        public void afterTextChanged(Editable s) {
            //do nothing
        }
    }

    private class SearchSP200Thread extends Thread {
        SP200_serialAPI sp200API = SP200_serialAPI.getInstance();

        @Override
        public void run() {
            sp200API.StartReceiveThread(60); // timeout 60
            FinancialApplication.getApp().runOnUiThreadDelay(new Runnable() {
                @Override
                public void run() {
                    Log.d("SP200_Search:", "Callback onReadSP200OK()");
                    if (pollingResult == null && isS200run) {
                        Log.d("SP200_Search:", "Call onReadSP200Result");
                        onReadSP200Result(retSP200);
                    } else {
                        Log.d("SP200_Search:", "pollingResult[" + (pollingResult == null ? "YES" : "NO") + "],isS200run[" + isS200run + "]");
                        sp200API.BreakReceiveThread();
                        SP200_serialAPI.getInstance().cancelSP200();
                    }
                }
            }, 500);
        }

        private void onReadSP200Result(int result) {
            Log.i(TAG, "SP200 Result");
            int iRet;
            EmvSP200 emv;
            byte[] szMsgResult = new byte[1024];

            iRet = sp200API.GetSp200Result(1, szMsgResult);
            if (iRet == TransResult.SUCC) {
                emv = sp200API.GetContactlessResult(szMsgResult);
                if (emv != null) {
                    FinancialApplication.getDal().getCardReaderHelper().stopPolling();
                    finish(new ActionResult(result, new CardInformation(SearchMode.SP200, emv)));
                }
            }
        }
    }

    public class SelectIssuerRunnable implements Runnable {
        private final boolean isFirstSelect;
        private final List<Issuer> issuersList;


        SelectIssuerRunnable(final boolean isFirstSelect, final List<Issuer> issuersList) {
            this.isFirstSelect = isFirstSelect;
            this.issuersList = issuersList;
        }

        @Override
        public void run() {
            Context context = SearchCardActivity.this;
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            if (isFirstSelect) {
                builder.setTitle(context.getString(R.string.emv_application_choose));
            } else {
                SpannableString sstr = new SpannableString(context.getString(R.string.emv_application_choose_again));
                sstr.setSpan(new ForegroundColorSpan(Color.RED), 5, 9, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                builder.setTitle(sstr);
            }
            String[] issuers = new String[issuersList.size()];
            for (int i = 0; i < issuers.length; i++) {
                issuers[i] = issuersList.get(i).getName();
                Log.i(TAG, "SelectIssuerRunnable[issuers=" + issuers[i] + "]");
            }
            builder.setSingleChoiceItems(issuers, -1, new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface dialog, int which) {
                    matchedIssuer = issuersList.get(which);
                    Log.i(TAG, "SelectIssuerRunnable[matchedIssuer=" + matchedIssuer + "]");
                    close(dialog);
                }
            });

            builder.setPositiveButton(context.getString(R.string.dialog_cancel),
                    new DialogInterface.OnClickListener() {

                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Log.i(TAG, "SelectIssuerRunnable[User not select]");
                            finish(new ActionResult(TransResult.ERR_USER_CANCEL, null));
                            close(dialog);
                            return;
                        }
                    });
            builder.setCancelable(false);
            builder.create().show();
        }

        private void close(DialogInterface dialog) {
            dialog.dismiss();
            if (isManualMode) {
                onVerifyManualPan();
            } else {
                cv.open();
            }
        }
    }
}

/*
 * ============================================================================
 * COPYRIGHT
 *              Pax CORPORATION PROPRIETARY INFORMATION
 *   This software is supplied under the terms of a license agreement or
 *   nondisclosure agreement with Pax Corporation and may not be copied
 *   or disclosed except in accordance with the terms in that agreement.
 *      Copyright (C) 2016 - ? Pax Corporation. All rights reserved.
 * Module Date: 2016-11-25
 * Module Author: Steven.W
 * Description:
 *
 * ============================================================================
 */
package com.pax.pay.service;

import com.pax.abl.core.ActionResult;
import com.pax.abl.utils.PanUtils;
import com.pax.edc.opensdk.BaseResponse;
import com.pax.edc.opensdk.Constants;
import com.pax.edc.opensdk.PreAuthMsg;
import com.pax.edc.opensdk.RefundMsg;
import com.pax.edc.opensdk.ReprintTotalMsg;
import com.pax.edc.opensdk.ReprintTransMsg;
import com.pax.edc.opensdk.SaleMsg;
import com.pax.edc.opensdk.SettleMsg;
import com.pax.edc.opensdk.TransResponse;
import com.pax.edc.opensdk.TransResult;
import com.pax.edc.opensdk.VoidMsg;
import com.pax.pay.app.FinancialApplication;
import com.pax.pay.trans.model.TransData;
import com.pax.pay.utils.TransResultUtils;
import com.pax.settings.SysParam;

public class ParseResp {

    private ParseResp() {
        //do nothing
    }

    public static BaseResponse generate(int commandType, final ActionResult result) {
        switch (commandType) {
            case Constants.PRE_AUTH:
                return updatePreAuth(new PreAuthMsg.Response(), result);
            case Constants.SALE:
                return updateSale(new SaleMsg.Response(), result);
            case Constants.VOID:
                return updateVoid(new VoidMsg.Response(), result);
            case Constants.REFUND:
                return updateRefund(new RefundMsg.Response(), result);
            case Constants.SETTLE:
                return updateSettle(new SettleMsg.Response(), result);
            case Constants.REPRINT_TRANS:
                return updateReprintTrans(new ReprintTransMsg.Response(), result);
            case Constants.REPRINT_TOTAL:
                return updateReprintTotal(new ReprintTotalMsg.Response(), result);
            default:
                return null;
        }
    }

    private static boolean updateBase(BaseResponse response, final ActionResult result) {
        response.setAppId(FinancialApplication.getApp().getPackageName());
        if (result == null) {
            response.setRspCode(TransResult.ERR_HOST_REJECT);
            response.setRspMsg(TransResultUtils.getMessage(TransResult.ERR_HOST_REJECT));
            return false;
        }

        if (result.getRet() != TransResult.SUCC) {
            response.setRspCode(result.getRet());
            response.setRspMsg(TransResultUtils.getMessage(result.getRet()));
            return false;
        }

        response.setRspMsg(TransResultUtils.getMessage(TransResult.SUCC));
        return result.getData() != null;
    }

    private static void updateTrans(TransResponse response, final ActionResult result) {
        if (!updateBase(response, result))
            return;
        response.setMerchantName(FinancialApplication.getSysParam().get(SysParam.StringParam.EDC_MERCHANT_NAME_EN));
        response.setMerchantId(FinancialApplication.getAcqManager().getCurAcq().getMerchantId());
        response.setTerminalId(FinancialApplication.getAcqManager().getCurAcq().getTerminalId());

        TransData transData = (TransData) result.getData();

        response.setCardNo(PanUtils.maskCardNo(transData.getPan(), transData.getIssuer().getPanMaskPattern()));
        response.setVoucherNo(transData.getStanNo());
        response.setBatchNo(transData.getBatchNo());
        response.setIssuerName(transData.getIssuer().getName());
        response.setAcquirerName(transData.getAcquirer().getName());
        response.setRefNo(transData.getRefNo());
        response.setTransTime(transData.getDateTime());
        response.setAmount(transData.getAmount());
        response.setAuthCode(transData.getAuthCode());
        response.setCardType(enterMode2CardType(transData.getEnterMode()));
        response.setCardholderSignature(transData.getSignData());
        response.setSignaturePath(transData.getSignPath());
    }

    @TransResponse.CardType
    private static int enterMode2CardType(TransData.EnterMode enterMode) {
        switch (enterMode) {
            case MANUAL:
                return TransResponse.MANUAL;
            case SWIPE:
                return TransResponse.MAG;
            case INSERT:
                return TransResponse.ICC;
            case CLSS:
                return TransResponse.PICC;
            case FALLBACK:
                return TransResponse.FALLBACK;
            default:
                return TransResponse.NO_CARD;
        }
    }

    private static PreAuthMsg.Response updatePreAuth(PreAuthMsg.Response response, final ActionResult result) {
        updateTrans(response, result);
        return response;
    }

    private static SaleMsg.Response updateSale(SaleMsg.Response response, final ActionResult result) {
        updateTrans(response, result);
        return response;
    }

    private static VoidMsg.Response updateVoid(VoidMsg.Response response, final ActionResult result) {
        updateTrans(response, result);
        return response;
    }

    private static RefundMsg.Response updateRefund(RefundMsg.Response response, final ActionResult result) {
        updateTrans(response, result);
        return response;
    }

    private static SettleMsg.Response updateSettle(SettleMsg.Response response, final ActionResult result) {
        updateTrans(response, result);
        return response;
    }

    private static ReprintTransMsg.Response updateReprintTrans(ReprintTransMsg.Response response, final ActionResult result) {
        updateBase(response, result);
        return response;
    }

    private static ReprintTotalMsg.Response updateReprintTotal(ReprintTotalMsg.Response response, final ActionResult result) {
        updateBase(response, result);
        return response;
    }
}

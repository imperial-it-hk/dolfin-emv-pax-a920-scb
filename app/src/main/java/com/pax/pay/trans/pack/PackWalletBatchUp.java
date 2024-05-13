package com.pax.pay.trans.pack;

import th.co.bkkps.utils.Log;
import androidx.annotation.NonNull;
import com.pax.abl.core.ipacker.PackListener;
import com.pax.gl.pack.exception.Iso8583Exception;
import com.pax.glwrapper.convert.IConvert;
import com.pax.pay.app.FinancialApplication;
import com.pax.pay.trans.model.ETransType;
import com.pax.pay.trans.model.TransData;

/**
 * Created by SORAYA S on 17-May-18.
 */

public class PackWalletBatchUp extends PackIso8583 {

    public PackWalletBatchUp(PackListener listener) {
        super(listener);
    }

    @NonNull
    @Override
    public byte[] pack(@NonNull TransData transData) {
        try {
            setMandatoryData(transData);

            return pack(false, transData);

        } catch (Exception e) {
            Log.e(TAG, "", e);
        }
        return "".getBytes();
    }

    @Override
    protected void setMandatoryData(@NonNull TransData transData) throws Iso8583Exception {
        // h
        String pHeader = transData.getTpdu() + transData.getHeader();
        entity.setFieldValue("h", pHeader);
        // m
        ETransType transType = transData.getTransType();
        if (transData.getReversalStatus() == TransData.ReversalStatus.REVERSAL) {
            entity.setFieldValue("m", transType.getDupMsgType());
        } else if (transData.getAdviceStatus() == TransData.AdviceStatus.ADVICE) {
            entity.setFieldValue("m", transType.getRetryChkMsgType());
        } else {
            entity.setFieldValue("m", transType.getMsgType());
        }

        // field 3
        setBitData3(transData);

        setBitData4(transData);
        // field 11
        setBitData11(transData);
        // field 12-13-17
        setBitData12(transData);

        // field 24 NII
        transData.setNii(transData.getAcquirer().getNii());
        setBitData24(transData);

        setBitData37(transData);
        setBitData38(transData);

        setBitData41(transData);
        setBitData42(transData);

        setBitData62(transData);
    }

    @Override
    protected void setBitData3(@NonNull TransData transData) throws Iso8583Exception {
        String procCode = transData.getTransType().getProcCode();
        if(transData.getOrigTransType() == ETransType.REFUND_WALLET){
            procCode = "200000";
        }
        setBitData("3", procCode);
    }

    @Override
    protected void setBitData12(@NonNull TransData transData) throws Iso8583Exception {
        String temp = transData.getOrigDateTime();
        if (temp != null && !temp.isEmpty()) {
            String year = temp.substring(0, 4);
            String date = temp.substring(4, 8);
            String time = temp.substring(8, temp.length());
            setBitData("12", time);
            setBitData("13", date);
            setBitData("17", year);
        }
    }

    @Override
    protected void setBitData62(@NonNull TransData transData) throws Iso8583Exception {
        setBitData("62", FinancialApplication.getConvert().stringPadding(String.valueOf(transData.getOrigTransNo()), '0', 6, IConvert.EPaddingPosition.PADDING_LEFT));
    }
}

package com.pax.pay.trans.pack;

import androidx.annotation.NonNull;

import com.pax.abl.core.ipacker.PackListener;
import com.pax.device.UserParam;
import com.pax.gl.pack.exception.Iso8583Exception;
import com.pax.pay.app.FinancialApplication;
import com.pax.pay.constant.Constants;
import com.pax.pay.trans.model.ETransType;
import com.pax.pay.trans.model.TransData;

import th.co.bkkps.utils.Log;

public class PackSaleCompletionV2 extends PackIsoBase{
    public PackSaleCompletionV2(PackListener listener) {
        super(listener);
    }


    @NonNull
    @Override
    public byte[] pack(@NonNull TransData transData) {
        try {
            setMandatoryData(transData);

            if (IsTransTLE(transData)) {
                transData.setTpdu("600" + UserParam.TLENI01 + "8000");
                setBitHeader(transData);
                return packWithTLE(transData);
            } else {
                return pack(false,transData);
            }
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
        } else {
            entity.setFieldValue("m", transType.getMsgType());
        }

        setBitData3(transData);
        setBitData4(transData);


        setBitData11(transData);
        setBitData14(transData);

        setBitData22(transData);
        setBitData23(transData);

        if (transData.getNii()==null && transData.getAcquirer()!=null) {
            transData.setNii(transData.getAcquirer().getNii());
        }
        setBitData24(transData);
        setBitData25(transData);

        setBitData35(transData);

        setBitData37(transData);


        transData.setAcquirer(FinancialApplication.getAcqManager().findAcquirer(Constants.ACQ_UP));
        setBitData41(transData);
        setBitData42(transData);

        if (!transData.isPinFree()) {
            setBitData52(transData);
        }

        setBitData55(transData);

        setBitData62(transData);
    }
}

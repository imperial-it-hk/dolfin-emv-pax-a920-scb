package com.pax.pay.trans.pack;

import th.co.bkkps.utils.Log;
import androidx.annotation.NonNull;
import com.pax.abl.core.ipacker.PackListener;
import com.pax.eemv.utils.Tools;
import com.pax.gl.pack.IIso8583;
import com.pax.gl.pack.exception.Iso8583Exception;
import com.pax.pay.trans.component.Component;
import com.pax.pay.trans.model.ETransType;
import com.pax.pay.trans.model.TransData;

/**
 * Created by SORAYA S on 31-Jan-18.
 */

public class PackQRSale extends PackIso8583 {

    private static final String TABLE_ID = "PI";
    private static final String TABLE_ID_INQID = "RF";
    private static final String TABLE_VERSION = "\001";//hex-01(Start of Heading:SOH)

    public PackQRSale(PackListener listener) {
        super(listener);
    }

    @NonNull
    @Override
    public byte[] pack(@NonNull TransData transData) {
        try {
            setMandatoryData(transData);

            return pack(false,transData);

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

        // field 3
        setBitData3(transData);
        setBitData4(transData);
        //field 11
        setBitData11(transData);

        //field 12-13
        setBitData12(transData);

        // field 22
        setBitData22(transData);

        // field 24 NII
        transData.setNii(transData.getAcquirer().getNii());
        setBitData24(transData);

        setBitData25(transData);

        setBitData37(transData);
        setBitData38(transData);

        setBitData41(transData);
        setBitData42(transData);

        setBitData62(transData);

        setBitData63(transData);
    }

    @Override
    protected void setBitData22(@NonNull TransData transData) throws Iso8583Exception {
        IIso8583.IIso8583Entity.IFieldAttrs iFieldAttrs22 = entity.createFieldAttrs().setPaddingPosition(IIso8583.IIso8583Entity.IFieldAttrs.EPaddingPosition.PADDING_LEFT);
        //entity.setFieldValue("22", "000").setFieldAttrs("22", iFieldAttrs22);//for PromptPay default 0000
        setBitData("22", "000", iFieldAttrs22);
    }

    @Override
    protected void setBitData38(@NonNull TransData transData) throws Iso8583Exception {
        setBitData("38", transData.getAuthCode());
    }

    @Override
    protected void setBitData63(@NonNull TransData transData) throws Iso8583Exception {
        if (transData.isTransInqID()) {
            String paymentInqData = TABLE_ID_INQID + TABLE_VERSION ;
            String lengthPaymentAscii = Tools.hexToAscii(String.valueOf(Component.getPaddedNumber(Tools.string2Bytes(paymentInqData).length, 4)));
            String promptPayData = lengthPaymentAscii + paymentInqData;
            //entity.setFieldValue("63", promptPayData);
            setBitData("63", promptPayData);
        }
        else  {
            String paymentInqData = TABLE_ID + TABLE_VERSION + transData.getField63();
            String lengthPaymentAscii = Tools.hexToAscii(String.valueOf(Component.getPaddedNumber(Tools.string2Bytes(paymentInqData).length, 4)));
            String promptPayData = lengthPaymentAscii + paymentInqData;
            //entity.setFieldValue("63", promptPayData);
            setBitData("63", promptPayData);
        }

    }
}

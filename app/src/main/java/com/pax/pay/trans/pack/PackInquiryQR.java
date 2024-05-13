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
 * Created by NANNAPHAT S on 15/11/2018.
 */

public class PackInquiryQR extends PackIso8583 {

    private static final String TABLE_ID_BC = "BC";
    private static final String TABLE_VERSION_BC = "\01";//hex-01(Start of Heading:SOH)

    public PackInquiryQR(PackListener listener) {
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

        // field 22
        setBitData22(transData);

        // field 24 NII
        transData.setNii(transData.getAcquirer().getNii());
        setBitData24(transData);

        setBitData41(transData);
        setBitData42(transData);
        //setBitData60(transData);
        //setBitData62(transData); // KBANK no have this bit
        setBitData63(transData);
    }

    @Override
    protected void setBitData22(@NonNull TransData transData) throws Iso8583Exception {
        IIso8583.IIso8583Entity.IFieldAttrs iFieldAttrs22 = entity.createFieldAttrs().setPaddingPosition(IIso8583.IIso8583Entity.IFieldAttrs.EPaddingPosition.PADDING_LEFT);
        //entity.setFieldValue("22", "030").setFieldAttrs("22", iFieldAttrs22);
        setBitData("22","030",iFieldAttrs22);
    }

    @Override
    protected void setBitData63(@NonNull TransData transData) throws Iso8583Exception {
        //entity.setFieldValue("63", initBuyerCode(transData) + Component.initPrintText() + Component.initTerminalInformation());
        setBitData("63", initBuyerCode(transData) + Component.initPrintText() + Component.initTerminalInformation());
    }

    private String initBuyerCode(TransData transData){
        // buyer code.
        String qrBuyerCode = transData.getQrBuyerCode();
        String bcData = TABLE_ID_BC + TABLE_VERSION_BC + qrBuyerCode;
        String lengthbcDataAscii = Tools.hexToAscii(String.valueOf(Component.getPaddedNumber(Tools.string2Bytes(bcData).length, 4)));
        return lengthbcDataAscii + bcData;
    }
}

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
 * Created by WITSUTA A on 11/23/2018.
 */

public class PackQRSaleAllInOne extends PackIso8583 {

    public PackQRSaleAllInOne(PackListener listener) {
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

        // Inquiry trans only.
        setBitData37(transData);

        setBitData41(transData);
        setBitData42(transData);

        setBitData62(transData);

        setBitData63(transData);
    }

    @Override
    protected void setBitData22(@NonNull TransData transData) throws Iso8583Exception {
        IIso8583.IIso8583Entity.IFieldAttrs iFieldAttrs22 = entity.createFieldAttrs().setPaddingPosition(IIso8583.IIso8583Entity.IFieldAttrs.EPaddingPosition.PADDING_LEFT);
        setBitData("22", "000", iFieldAttrs22);
    }

    @Override
    protected void setBitData37(@NonNull TransData transData) throws Iso8583Exception {
        if (transData.getTransType() == ETransType.STATUS_INQUIRY_ALL_IN_ONE) {
            setBitData("37", transData.getRefNo());
        }
    }

    @Override
    protected void setBitData63(@NonNull TransData transData) throws Iso8583Exception {
        if (transData.getTransType() == ETransType.STATUS_INQUIRY_ALL_IN_ONE) {
            setBitData("63",initPaymentInquiryTranID(transData) + initPrintText());
        }else {
            setBitData("63", initPaymentInquiryGenQr(transData) + initPrintText());
        }

    }

    private String initPaymentInquiryGenQr (TransData transData) {
        String tableID = "PI";
        String tableVer = Tools.hexToAscii("04");

        //  if EDC gen = 1;  , Server gen = 2;  Transaction ID = 3;
        String qrGen = Tools.hexToAscii("02");
        String refID = transData.getQrID();
        String totalMSG = tableID + tableVer + qrGen + refID;
        String lenMsg = Tools.hexToAscii(String.valueOf(Component.getPaddedNumber(Tools.string2Bytes(totalMSG).length, 4)));

        return lenMsg + totalMSG;
    }

    private String initPaymentInquiryTranID (TransData transData) {
        String tableID = "PI";
        String tableVer = Tools.hexToAscii("04");

        //  if EDC gen = 1;  , Server gen = 2;  Transaction ID = 3;
        String qrGen = Tools.hexToAscii("03");
        String refID = Component.getPaddedString(transData.getRefNo(), 12, '0');
        String totalMSG = tableID + tableVer + qrGen + refID;
        String lenMsg = Tools.hexToAscii(String.valueOf(Component.getPaddedNumber(Tools.string2Bytes(totalMSG).length, 4)));

        return lenMsg + totalMSG;
    }

    private String initPrintText() {
        String tableID = "TX";
        String tableVer = Tools.hexToAscii("03");
        String charPerLine = Tools.hexToAscii("34");
        String maxPrintDataSize = Tools.hexToAscii("0500");
        String totalMSG = tableID + tableVer + charPerLine + maxPrintDataSize;
        String lenMsg = Tools.hexToAscii(String.valueOf(Component.getPaddedNumber(Tools.string2Bytes(totalMSG).length, 4)));
        return lenMsg + totalMSG;
    }

}

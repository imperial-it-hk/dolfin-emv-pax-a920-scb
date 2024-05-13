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
 * Created by NANNAPHAT S on 29-Nov-18.
 */

public class PackPromptpayVoid extends PackIso8583 {

    public PackPromptpayVoid(PackListener listener) {
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
        } else if (transData.getAdviceStatus() == TransData.AdviceStatus.ADVICE) {
            entity.setFieldValue("m", transType.getRetryChkMsgType());
        } else {
            entity.setFieldValue("m", transType.getMsgType());
        }

        // field 3 Processing code
        setBitData3(transData);
        // field 4 Txn Amount
        setBitData4(transData);
        // field 11 STAN
        setBitData11(transData);

        // field 12-13 Date/Time
        setBitData12(transData);

        // field 24 NII
        transData.setNii(transData.getAcquirer().getNii());
        setBitData24(transData);
        // field 37 Ref No.
        setBitData37(transData);
        // field 41 TID
        setBitData41(transData);
        // field 42 MID
        setBitData42(transData);
        // field 62 TracePackQrVoidPackQrVoid
        setBitData62(transData);

//        if(transData.getAdviceStatus() == TransData.AdviceStatus.NORMAL) {
//            // field 63 Table TX
//            setBitData63(transData);
//        }
    }

//    @Override
//    protected void setBitData12(@NonNull TransData transData) throws Iso8583Exception {
//        String temp = transData.getOrigDateTime();
//        if (temp != null && !temp.isEmpty()) {
//            String year = temp.substring(0, 4);
//            String date = temp.substring(4, 8);
//            String time = temp.substring(8, temp.length());
//            setBitData("12", time);
//            setBitData("13", date);
//            setBitData("17", year);
//        }
//    }

//    @Override
//    protected void setBitData22(@NonNull TransData transData) throws Iso8583Exception {
//        IIso8583.IIso8583Entity.IFieldAttrs iFieldAttrs22 = entity.createFieldAttrs().setPaddingPosition(IIso8583.IIso8583Entity.IFieldAttrs.EPaddingPosition.PADDING_LEFT);
//        //entity.setFieldValue("22", "030").setFieldAttrs("22", iFieldAttrs22);
//        setBitData("22", "030", iFieldAttrs22);
//    }

    @Override
    protected void setBitData37(@NonNull TransData transData) throws Iso8583Exception {
        setBitData("37", transData.getRefNo());
    }

    @Override
    protected void setBitData62(@NonNull TransData transData) throws Iso8583Exception {
        setBitData("62", FinancialApplication.getConvert().stringPadding(String.valueOf(transData.getOrigTransNo()), '0', 6, IConvert.EPaddingPosition.PADDING_LEFT) );
    }

//    @Override
//    protected void setBitData63(@NonNull TransData transData) throws Iso8583Exception {
//        setBitData("63", initPrintText());
//    }
//
//    private String initPrintText() {
//        String tableID = "TX";
//        String tableVer = Tools.hexToAscii("03");
//        String charPerLine = Tools.hexToAscii("34");
//        String maxPrintDataSize = Tools.hexToAscii("0500");
//        String totalMSG = tableID + tableVer + charPerLine + maxPrintDataSize;
//        String lenMsg = Tools.hexToAscii(String.valueOf(Component.getPaddedNumber(Tools.string2Bytes(totalMSG).length, 4)));
//        return lenMsg + totalMSG;
//    }
}

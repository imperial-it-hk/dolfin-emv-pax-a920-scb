package com.pax.pay.trans.pack;

import th.co.bkkps.utils.Log;
import androidx.annotation.NonNull;
import com.pax.abl.core.ipacker.PackListener;
import com.pax.device.UserParam;
import com.pax.gl.pack.IIso8583;
import com.pax.gl.pack.exception.Iso8583Exception;
import com.pax.pay.constant.Constants;
import com.pax.pay.trans.component.Component;
import com.pax.pay.trans.model.TransData;

public class PackInstalmentBatchUpKbank extends PackIso8583 {
    public PackInstalmentBatchUpKbank(PackListener listener) {
        super(listener);
    }

    @NonNull
    @Override
    public byte[] pack(@NonNull TransData transData) {
        try {
            setFinancialData(transData);

            return pack(false, transData);

        } catch (Exception e) {
            Log.e(TAG, "", e);
        }
        return "".getBytes();
    }

    @Override
    protected void setFinancialData(@NonNull TransData transData) throws Iso8583Exception {
        boolean isAmex = Constants.ISSUER_AMEX.equals(transData.getIssuer().getName());

        if (IsTransTLE(transData)) {
            transData.setTpdu("600" + UserParam.TLENI01 + "8000");
        }
        // h
        String pHeader = transData.getTpdu() + transData.getHeader();
        entity.setFieldValue("h", pHeader);
        // m
        entity.setFieldValue("m", transData.getTransType().getMsgType());

        TransData.EnterMode enterMode = transData.getEnterMode();
        if (enterMode == TransData.EnterMode.INSERT) {
            if (!isAmex) {
                setBitData23(transData);
                setBitData55(transData);
            }
        }

        setBitData2(transData);
        setBitData3(transData);
        setBitData4(transData);
        setBitData11(transData);
        setBitData12(transData);// field 12, 13
        setBitData14(transData);
        setBitData22(transData);
        setBitData24(transData);
        setBitData25(transData);
        setBitData37(transData);
        setBitData38(transData);
        if (!isAmex) setBitData39(transData);
        setBitData41(transData);
        setBitData42(transData);
        setBitData60(transData);
        setBitData61Byte(transData);
        setBitData62(transData);
        setBitData63Byte(transData);
    }

    @Override
    protected void setBitData3(@NonNull TransData transData) throws Iso8583Exception {
        setBitData("3", transData.getProcCode());
    }

    @Override
    protected void setBitData22(@NonNull TransData transData) throws Iso8583Exception {
        String value = getInputMethodByIssuer(transData);
        if (value != null && !value.isEmpty()) {
            IIso8583.IIso8583Entity.IFieldAttrs iFieldAttrs22 = entity.createFieldAttrs().setPaddingPosition(IIso8583.IIso8583Entity.IFieldAttrs.EPaddingPosition.PADDING_LEFT);
            setBitData("22", value, iFieldAttrs22);
        }
    }

    @Override
    protected void setBitData25(@NonNull TransData transData) throws Iso8583Exception {
        String serviceCode = transData.getOrigTransType().getServiceCode();
        setBitData("25", serviceCode);
    }

    @Override
    protected void setBitData39(@NonNull TransData transData) throws Iso8583Exception {
        setBitData("39", transData.getResponseCode().getCode());
    }

    @Override
    protected void setBitData60(@NonNull TransData transData) throws Iso8583Exception {
        String msgType = transData.getOrigTransType().getMsgType();
        String f60 = msgType + Component.getPaddedNumber(transData.getOrigTransNo(), 6) + Component.getPaddedString(transData.getRefNo(), 12, '0');
        setBitData("60", f60);
    }
}

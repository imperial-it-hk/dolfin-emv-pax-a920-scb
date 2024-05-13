package com.pax.pay.trans.pack;

import th.co.bkkps.utils.Log;
import androidx.annotation.NonNull;
import com.pax.abl.core.ipacker.PackListener;
import com.pax.device.UserParam;
import com.pax.eemv.utils.Tools;
import com.pax.gl.pack.IIso8583;
import com.pax.gl.pack.exception.Iso8583Exception;
import com.pax.pay.constant.Constants;
import com.pax.pay.trans.component.Component;
import com.pax.pay.trans.model.TransData;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class PackDccGetRateKbank extends PackIso8583 {
    public PackDccGetRateKbank(PackListener listener) {
        super(listener);
    }

    @NonNull
    @Override
    public byte[] pack(@NonNull TransData transData) {
        try {
            setMandatoryData(transData);
            setFinancialData(transData);

            // for DCC Markup
            setBitData63(transData);

            if (IsTransTLE(transData)) {
                transData.setTpdu("600" + UserParam.TLENI01 + "8000");
                setBitHeader(transData);
                return packWithTLE(transData);
            } else {
                return pack(false, transData);
            }

        } catch (Exception e) {
            Log.e(TAG, "", e);
        }
        return "".getBytes();
    }

    @Override
    protected void setFinancialData(@NonNull TransData transData) throws Iso8583Exception {
        // Header, MsgType, field 3, 25, 41, 42 are set in Mandatory data.

        boolean isAmex = Constants.ISSUER_AMEX.equals(transData.getIssuer().getName());

        TransData.EnterMode enterMode = transData.getEnterMode();

        switch (enterMode) {
            case MANUAL:
                setBitData2(transData);
                setBitData14(transData);
                break;
            case SWIPE:
            case FALLBACK:
                setBitData35(transData);
                break;
            case CLSS:
            case INSERT:
                setBitData35(transData);
                if (!isAmex) setBitData23(transData);
                setBitData55(transData);
                break;
        }

        setBitData4(transData);
        setBitData11(transData);
        setBitData22(transData);
        transData.setNii(transData.getAcquirer().getNii());
        setBitData24(transData);
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
    protected void setBitData63(@NonNull TransData transData) throws Iso8583Exception {
        final byte[] tableLen = {0x00, 0x05};
        final byte[] tableCode = {0x44, 0x49};//default DI (DCC Information)
        final byte[] version = {0x30, 0x30, 0x31};//default version "001"

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        try {
            outputStream.write(tableLen);
            outputStream.write(tableCode);
            outputStream.write(version);
        } catch (IOException e) {
            e.printStackTrace();
        }

        transData.setField63Byte(outputStream.toByteArray());;
        setBitData63Byte(transData);
    }
}

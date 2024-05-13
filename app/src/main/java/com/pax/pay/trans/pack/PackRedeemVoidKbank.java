package com.pax.pay.trans.pack;

import th.co.bkkps.utils.Log;
import androidx.annotation.NonNull;
import com.pax.abl.core.ipacker.PackListener;
import com.pax.device.UserParam;
import com.pax.gl.pack.IIso8583;
import com.pax.gl.pack.exception.Iso8583Exception;
import com.pax.pay.trans.model.TransData;

public class PackRedeemVoidKbank extends PackIso8583 {

    public PackRedeemVoidKbank(PackListener listener) {
        super(listener);
    }

    @NonNull
    @Override
    public byte[] pack(@NonNull TransData transData) {
        try {
            setMandatoryData(transData);
            setFinancialData(transData);

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
        // Header, MsgType, field 3, 24, 25, 41, 42 are set in Mandatory data.

        setBitData2(transData);
        setBitData4(transData);
        setBitData11(transData);
        setBitData12(transData);
        setBitData14(transData);
        setBitData22(transData);
        setBitData37(transData);
        setBitData62(transData);
        setBitData63Byte(transData);
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
    protected void setBitData37(@NonNull TransData transData) throws Iso8583Exception {
        setBitData("37", transData.getOrigRefNo());
    }
}

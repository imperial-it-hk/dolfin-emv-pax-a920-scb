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

import static com.pax.pay.trans.model.TransData.EnterMode.MANUAL;

public class PackInstalmentKbank extends PackIso8583 {

    public PackInstalmentKbank(PackListener listener) {
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
        boolean isAmex = Constants.ISSUER_AMEX.equals(transData.getIssuer().getName());
        TransData.EnterMode enterMode = transData.getEnterMode();

        if (transData.getReversalStatus() == TransData.ReversalStatus.REVERSAL) {
            setBitData2(transData);
            setBitData14(transData);
            if (enterMode == TransData.EnterMode.INSERT) {
                if (!isAmex) setBitData23(transData);
                setBitData55(transData);
            }
        } else {
            switch (enterMode) {
                case MANUAL:
                    setBitData2(transData);
                    setBitData14(transData);
                    break;
                case SWIPE:
                case FALLBACK:
                    setBitData35(transData);
                    break;
                case INSERT:
                    setBitData35(transData);
                    if (!isAmex) setBitData23(transData);
                    setBitData55(transData);
                    break;
            }
        }

        setBitData4(transData);
        setBitData11(transData);
        setBitData22(transData);
        setBitData61(transData);
        setBitData62(transData);
        setBitData63(transData);
    }

    @Override
    protected void setBitData22(@NonNull TransData transData) throws Iso8583Exception {
        String value;

        String issuer = transData.getIssuer().getName();
        if (issuer.compareTo(Constants.ISSUER_UP) == 0){
            value = "802";
        } else {
            value = getInputMethodByIssuer(transData);
        }

        if (value != null && !value.isEmpty()) {
            IIso8583.IIso8583Entity.IFieldAttrs iFieldAttrs22 = entity.createFieldAttrs().setPaddingPosition(IIso8583.IIso8583Entity.IFieldAttrs.EPaddingPosition.PADDING_LEFT);
            setBitData("22", value, iFieldAttrs22);
        }
    }

    @Override
    protected void setBitData61(@NonNull TransData transData) throws Iso8583Exception {
        if (transData.getReversalStatus() == TransData.ReversalStatus.REVERSAL) {
            setBitData("61", transData.getField61Byte());
        } else {
            byte[] f61 = new byte[0];
            try {
                f61 = packBit61(transData);
            } catch (IOException e) {
                Log.e(TAG, "", e);
            }

            transData.setField61Byte(f61);
            setBitData("61", f61);
        }
    }

    @Override
    protected void setBitData63(@NonNull TransData transData) throws Iso8583Exception {
        if (transData.getReversalStatus() == TransData.ReversalStatus.REVERSAL) {
            setBitData("63", transData.getField63Byte());
        } else {
            byte[] f63 = new byte[0];
            try {
                f63 = packBit63(transData);
            } catch (IOException e) {
                Log.e(TAG, "", e);
            }

            transData.setField63Byte(f63);
            setBitData("63", f63);
        }
    }

    private byte[] packBit61(TransData transData) throws IOException {
        final byte[] planId = {0x39, 0x39, 0x39};//default 999
        byte[] paymentTerm = Tools.string2Bytes(Component.getPaddedNumber(transData.getInstalmentPaymentTerm(), 2));
        byte[] iPlanMode = Tools.string2Bytes(transData.getInstalmentIPlanMode());
        byte[] promotionKey = Tools.string2Bytes(transData.getInstalmentPromotionKey());

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        outputStream.write(planId);
        outputStream.write(paymentTerm);
        outputStream.write(iPlanMode);
        outputStream.write(promotionKey);

        return outputStream.toByteArray();
    }

    private byte[] packBit63(TransData transData) throws IOException {
        final byte[] tableCode = {0x30, 0x31};//default 01
        byte[] serialNo = transData.isInstalmentPromoProduct() || ("03".equals(transData.getInstalmentIPlanMode()) || "04".equals(transData.getInstalmentIPlanMode()))
                ? Tools.string2Bytes(Component.getPaddedStringRight(transData.getInstalmentSerialNo(), 13, ' '))
                : Tools.string2Bytes(Component.getPaddedString("9", 13, '9'));

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        outputStream.write(tableCode);
        outputStream.write(serialNo);

        return outputStream.toByteArray();
    }
}

package com.pax.pay.trans.pack;

import th.co.bkkps.utils.Log;
import androidx.annotation.NonNull;
import com.pax.abl.core.ipacker.PackListener;
import com.pax.device.UserParam;
import com.pax.eemv.utils.Tools;
import com.pax.gl.pack.IIso8583;
import com.pax.gl.pack.exception.Iso8583Exception;
import com.pax.pay.trans.component.Component;
import com.pax.pay.trans.model.TransData;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class PackRedeemSaleKbank extends PackIso8583 {

    public PackRedeemSaleKbank(PackListener listener) {
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

        TransData.EnterMode enterMode = transData.getEnterMode();

        if (transData.getReversalStatus() == TransData.ReversalStatus.REVERSAL) {
            setBitData2(transData);
            setBitData12(transData);
            setBitData13(transData);
            setBitData14(transData);
        } else {
            if (enterMode == TransData.EnterMode.MANUAL) {
                setBitData2(transData);
                setBitData14(transData);
            } else if (enterMode == TransData.EnterMode.SWIPE
                       || enterMode == TransData.EnterMode.INSERT) {
                setBitData35(transData);
            }
        }

        setBitData4(transData);
        setBitData11(transData);
        setBitData22(transData);
        setBitData62(transData);
        setBitData63(transData);
    }

    @Override
    protected void setBitData4(@NonNull TransData transData) throws Iso8583Exception {
        transData.setAmount("0");// default 0 for all redeemed transaction
        setBitData("4", transData.getAmount());
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
        byte[] redemption = new byte[0];

        try {
            if (transData.getReversalStatus() == TransData.ReversalStatus.REVERSAL) {
                redemption = transData.getField63Byte();
            } else {
                switch (transData.getTransType()) {
                    case KBANK_REDEEM_PRODUCT:
                        redemption = packRedeemProduct(transData);
                        break;
                    case KBANK_REDEEM_PRODUCT_CREDIT:
                        redemption = packRedeemProduct(transData);
                        break;
                    case KBANK_REDEEM_VOUCHER:
                        redemption = packRedeemVoucher(transData);
                        break;
                    case KBANK_REDEEM_VOUCHER_CREDIT:
                        redemption = packRedeemVoucherCredit(transData);
                        break;
                    case KBANK_REDEEM_DISCOUNT:
                        redemption = packRedeemDiscount(transData);
                        break;
                    case KBANK_REDEEM_INQUIRY:
                        redemption = packRedeemInquiry(transData);
                        break;
                }
            }
        } catch (IOException e) {
            Log.e(TAG, "", e);
        }

        transData.setField63Byte(redemption);
        setBitData("63", redemption);
    }

    private byte[] packRedeemProduct(TransData transData) throws IOException {
        byte[] serviceCode = {0x30, 0x36};//default 06
        byte[] productCode = Tools.string2Bytes(Component.getPaddedString(transData.getProductCode(), 5, '0'));
        byte[] prodQty = Tools.string2Bytes(Component.getPaddedNumber(transData.getProductQty(), 2));

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        outputStream.write(serviceCode);
        outputStream.write(productCode);
        outputStream.write(prodQty);

        return outputStream.toByteArray();
    }

    private byte[] packRedeemVoucher(TransData transData) throws IOException {
        byte[] serviceCode = {0x30, 0x36};//default 06
        byte[] productCode = {0x30, 0x30, 0x30, 0x30, 0x31};//default 00001
        byte[] saleAmt = {0x30, 0x30, 0x30, 0x30, 0x30, 0x30, 0x30, 0x30, 0x30, 0x30, 0x30, 0x30};//default 0 (12 digits)
        byte[] redeemedPt = Tools.string2Bytes(Component.getPaddedNumber(transData.getRedeemedPoint(), 9));// manually enter

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        outputStream.write(serviceCode);
        outputStream.write(productCode);
        outputStream.write(saleAmt);
        outputStream.write(redeemedPt);

        return outputStream.toByteArray();
    }

    private byte[] packRedeemVoucherCredit(TransData transData) throws IOException {
        byte[] serviceCode = {0x30, 0x36};//default 06
        byte[] productCode = {0x30, 0x30, 0x30, 0x30, 0x31};//default 00001
        byte[] saleAmt = Tools.string2Bytes(Component.getPaddedString(transData.getRedeemedAmount(), 12, '0'));// manually enter
        byte[] redeemedPt = Tools.string2Bytes(Component.getPaddedNumber(transData.getRedeemedPoint(), 9));// manually enter

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        outputStream.write(serviceCode);
        outputStream.write(productCode);
        outputStream.write(saleAmt);
        outputStream.write(redeemedPt);

        return outputStream.toByteArray();
    }

    private byte[] packRedeemDiscount(TransData transData) throws IOException {
        byte[] serviceCode = {0x30, 0x36};//default 06
        byte[] productCode = Tools.string2Bytes(Component.getPaddedString(transData.getProductCode(), 5, '0'));// %Fix - manually enter, %Var - default 89999 in trans
        byte[] saleAmt = Tools.string2Bytes(Component.getPaddedString(transData.getRedeemedAmount(), 12, '0'));// manually enter

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        outputStream.write(serviceCode);
        outputStream.write(productCode);
        outputStream.write(saleAmt);

        return outputStream.toByteArray();
    }

    private byte[] packRedeemInquiry(TransData transData) throws IOException {
        byte[] serviceCode = {0x30, 0x36};//default 06
        byte[] productCode = {0x30, 0x30, 0x30, 0x30, 0x30};// default 00000

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        outputStream.write(serviceCode);
        outputStream.write(productCode);

        return outputStream.toByteArray();
    }
}

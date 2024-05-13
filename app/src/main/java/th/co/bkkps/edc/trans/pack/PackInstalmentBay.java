/*
 * ============================================================================
 * COPYRIGHT
 *              Pax CORPORATION PROPRIETARY INFORMATION
 *   This software is supplied under the terms of a license agreement or
 *   nondisclosure agreement with Pax Corporation and may not be copied
 *   or disclosed except in accordance with the terms in that agreement.
 *      Copyright (C) 2016 - ? Pax Corporation. All rights reserved.
 * Module Date: 2016-11-25
 * Module Author: Steven.W
 * Description:
 *
 * ============================================================================
 */
package th.co.bkkps.edc.trans.pack;

import android.util.Log;
import androidx.annotation.NonNull;
import com.pax.abl.core.ipacker.PackListener;
import com.pax.device.UserParam;
import com.pax.eemv.utils.Tools;
import com.pax.gl.pack.exception.Iso8583Exception;
import com.pax.pay.app.FinancialApplication;
import com.pax.pay.constant.Constants;
import com.pax.pay.trans.component.Component;
import com.pax.pay.trans.model.ETransType;
import com.pax.pay.trans.model.TransData;
import com.pax.pay.trans.pack.PackIso8583;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class PackInstalmentBay extends PackIso8583 {

    public PackInstalmentBay(PackListener listener) {
        super(listener);
    }

    @Override
    @NonNull
    public byte[] pack(@NonNull TransData transData) {
        try {
            setFinancialData(transData);
//            setBitData60(transData);

            if (IsTransTLE(transData)) {
                transData.setTpdu("600" + UserParam.TLENI02 + "0000");
                setBitHeader(transData);
                return packWithTLE(transData);
            }
            else {
                transData.setTpdu("600" + "150" + "0000");
                setBitHeader(transData);
                return pack(false, transData);
            }

        } catch (Exception e) {
            Log.e(TAG, "", e);
        }
        return "".getBytes();
    }

    @Override
    protected void setFinancialData(@NonNull TransData transData) throws Iso8583Exception {
        setMandatoryData(transData);
    }

    @Override
    protected void setMandatoryData(@NonNull TransData transData) throws Iso8583Exception {
        boolean isReferral = false;
        boolean isAmex = Constants.ACQ_AMEX.equals(transData.getAcquirer().getName());

        // h
        String pHeader = transData.getTpdu() + transData.getHeader();
        entity.setFieldValue("h", pHeader);
        // m
        ETransType transType = transData.getTransType();
        if (transData.getReversalStatus() == TransData.ReversalStatus.REVERSAL) {
            entity.setFieldValue("m", transType.getDupMsgType());
        } else if (transData.getReferralStatus() == TransData.ReferralStatus.REFERRED) {
            entity.setFieldValue("m", "0220");
            isReferral = true;
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
        transData.setNii(FinancialApplication.getAcqManager().getCurAcq().getNii());
        setBitData24(transData);

        setBitData25(transData);

        TransData.EnterMode enterMode = transData.getEnterMode();

        if (enterMode == TransData.EnterMode.MANUAL) {
            setBitData2(transData);

            setBitData14(transData);

        } else if (enterMode == TransData.EnterMode.SWIPE || enterMode == TransData.EnterMode.FALLBACK) {
            if (!isReferral) {
                setBitData35(transData);
            }

            if(enterMode == TransData.EnterMode.FALLBACK && !isAmex) {
                setBitData36(transData);
            }

            //[54]tip amount  by lixc
//            setBitData54(transData);

        } else if (enterMode == TransData.EnterMode.INSERT || enterMode == TransData.EnterMode.CLSS) {
            // [2]主账号
//            setBitData2(transData);

            // [14]有效期
//            setBitData14(transData);

            if (!isAmex) {//AMEX, not present
                setBitData23(transData);
            }

            if (!isReferral) {
                setBitData35(transData);

                // field 55 ICC
                setBitData55(transData);
            }
        }

        // field 41
        setBitData41(transData);

        // field 42
        setBitData42(transData);

        // [52]PIN
        setBitData52(transData);

        setBitData62(transData);

        if (isAmex && isReferral) {
            setBitData2(transData);
            setBitData12(transData);
            setBitData14(transData);
            setBitData38(transData);
        }

        setBitData63(transData);
    }

    @Override
    protected void setBitData38(@NonNull TransData transData) throws Iso8583Exception {
        setBitData("38", transData.getOrigAuthCode());
    }

    @Override
    protected void setBitData63(@NonNull TransData transData) throws Iso8583Exception {
        try {
            transData.setField63Byte(initGetBit63Data(transData));
            setBitData("63", transData.getField63Byte());
        }catch (IOException e) {
            Log.e(TAG, "", e);
        }
    }

    private byte[] initGetBit63Data(TransData transData) throws IOException {
        Log.e("menu", "-- setBitData63 getInstalmentTerms = " + transData.getInstalmentTerms());
        Log.e("menu", "-- setBitData63 getInstalmentInterest = " + transData.getInstalmentInterest());
        Log.e("menu", "-- setBitData63 getInstalmentSerialNum = " + transData.getSerialNum());
        String merCode = transData.getAcquirer().getMerchantId();
        String serialNum = transData.getSerialNum();
        String sku = transData.getSkuCode();


        String mktCode = transData.getMktCode() != null && !transData.getMktCode().trim().isEmpty() ? transData.getMktCode() : transData.getInstalmentInterest().replace(".","") ;
        mktCode = transData.isBayInstalmentSpecific() ? Component.getPaddedStringRight(mktCode, 10, ' ') : Component.getPaddedStringRight(Component.getPaddedString(mktCode + "1",5,'0'), 10, ' ');
        Log.e("menu", "-- setBitData63 mktCode = " + mktCode);

        /*if(!mktCode.isEmpty()){
            if(mktCode.length() < 4){
                mktCode = Component.getPaddedString(mktCode,4,'0') + "1";
                Log.e("menu", "-- setBitData63 mktCode = " + mktCode);
            }
        }*/
        merCode = !merCode.isEmpty() && merCode.length() > 10 ? merCode.substring(merCode.length() - 10) : Component.getPaddedString(merCode, 10, '0');
        Log.e("menu", "-- setBitData63 merCode = " + merCode);

        byte[] byteTransType = Tools.str2Bcd("9122");//fix value
        byte[] byteNonValue =  Component.getPaddedStringRight("", 12, ' ').getBytes();//fix value
        byte[] byteMerCode = Tools.string2Bytes(merCode);
        byte[] byteMktCode = Tools.string2Bytes(mktCode);

        byte[] byteSku = sku != null ? Tools.string2Bytes(Component.getPaddedStringRight(sku,20,' ')) : Tools.string2Bytes("00000000000000000000");
//        byte[] byteSku = sku != null ? Tools.string2Bytes(Component.getPaddedStringRight(sku,20,' ')) : Tools.string2Bytes(Component.getPaddedStringRight("",20,' '));

        byte[] byteTerms = Tools.string2Bytes(Component.getPaddedString(transData.getInstalmentTerms(), 2, '0'));
        byte[] byteTerm2 = Tools.string2Bytes("00");// Default value
        byte[] byteSerialNum = serialNum != null && !serialNum.isEmpty()  ? Tools.string2Bytes(Component.getPaddedStringRight(serialNum, 20, ' ')) : Component.getPaddedStringRight("", 20, ' ').getBytes();
        byte[] byteSaleCode = Tools.string2Bytes(Component.getPaddedStringRight("1000", 9, ' '));// Default value

        ByteArrayOutputStream outputMessage = new ByteArrayOutputStream( );
        outputMessage.write(new byte[]{0x37, 0x30});
        outputMessage.write(byteNonValue);
        outputMessage.write(byteMerCode);
        outputMessage.write(byteMktCode);
        outputMessage.write(byteSku);
        outputMessage.write(byteTerms);
        outputMessage.write(byteTerm2);
        outputMessage.write(byteSerialNum);
        outputMessage.write(byteSaleCode);
        byte[] byteMSG = outputMessage.toByteArray();

        //Cal length of data
        //String lengthMSG = String.valueOf(outputMessage.size());
        String lengthMSG = String.valueOf(Component.getPaddedNumber(outputMessage.size(),4));
        byte[] bytesLen = Tools.str2Bcd(lengthMSG);

        //Data plus length of data
        ByteArrayOutputStream totalMsgStream = new ByteArrayOutputStream();
        totalMsgStream.write(bytesLen);
        totalMsgStream.write(byteMSG);
//        byte[] byteTotalMsgandLen = totalMsgStream.toByteArray();

        //Cal total length of data(include data's length)
        //String lengthMSGwithLen = String.valueOf(totalMsgStream.size());
//        String lengthMSGwithLen = String.valueOf(Component.getPaddedNumber(totalMsgStream.size(),4));
//        byte[] bytesLengthMsgWithLen = Tools.str2Bcd(lengthMSGwithLen);
//
//        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
//        outputStream.write(bytesLengthMsgWithLen);
//        outputStream.write(byteTotalMsgandLen);

        return totalMsgStream.toByteArray( );
    }

}

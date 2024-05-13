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
package com.pax.pay.trans.pack;

import androidx.annotation.NonNull;

import com.pax.abl.core.ipacker.PackListener;
import com.pax.gl.pack.exception.Iso8583Exception;
import com.pax.pay.trans.model.ETransType;
import com.pax.pay.trans.model.TransData;
import com.pax.pay.utils.EReceiptUtils;

import th.co.bkkps.utils.ERMUtil;
import th.co.bkkps.utils.Log;

public class PackEReceiptTerminalRegistration extends PackIsoBase {

    public PackEReceiptTerminalRegistration(PackListener listener) {
        super(listener);
        super.setIsErcmEnable(true);
    }
    private int INTI_ERM_TERMINAL_VERSION = 3 ;

    @Override
    @NonNull
    public byte[] pack(@NonNull TransData transData) {
        try {
            if(transData.getERCMTerminalSerialNumber() != null
                && transData.getERCMHeaderImagePath() != null
                && transData.getERCMLogoImagePath() != null
                && transData.getERCMBankCode() != null
                && transData.getERCMMerchantCode() != null
                && transData.getERCMStoreCode() != null)
            {

                setMandatoryData(transData);
                return pack(false,transData);
            }
            else
            {
                return null;
            }
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

        // field 24 NII
        //    >> use external NII follow by each acquirer
        //    >> dont set ant NII within this function
        transData.setNii(transData.getInitAcquirerIndex());
        setBitData24(transData);

//        setBitData46(transData);            // field 46 Terminal Serial Number  : [LEN 2 bytes] + [TSN]
//        setBitData55(transData);            // field 55 Bank code               : [LEN 2 bytes] + [BANK CODE]
//        setBitData56(transData);            // field 56 Merchant code           : [LEN 2 bytes] + [MERCHANT CODE]
//        setBitData57(transData);            // field 57 Store code              : [LEN 2 bytes] + [STORE CODE]
//        setBitData61(transData);            // field 61 Header Image            : [LEN 2 bytes] + [SESSION KEY BLOCK TABLE]
//        //setBitData62(transData);            // field 62 Footer Image            : [LEN 2 bytes] + [HEADER IMAGE]
//        setBitData63(transData);            // field 63 Logo Image              : [LEN 2 bytes] + [BANK CODE]

        setBitData46(transData);            // field 46 Terminal Serial Number  : [LEN 2 bytes] + [TSN]
        setBitData55(transData);            // field 55 Bank code               : [LEN 2 bytes] + [BANK CODE]
        setBitData56(transData);            // field 56 Merchant code           : [LEN 2 bytes] + [MERCHANT CODE]
        setBitData57(transData);            // field 57 Store code              : [LEN 2 bytes] + [STORE CODE]
        setBitData61(transData);            // field 61 Header Image            : [LEN 2 bytes] + [SESSION KEY BLOCK TABLE]
        setBitData62(transData);            // field 62 Footer Image            : [LEN 2 bytes] + [HEADER IMAGE]
        setBitData63(transData);            // field 63 Logo Image              : [LEN 2 bytes] + [BANK CODE]
    }



    @Override
    protected  void setBitData46(@NonNull TransData transData) throws Iso8583Exception {
        String serialNo = transData.getERCMTerminalSerialNumber();
        int max_len = 15 ;
        if (serialNo.length() < max_len) {
            serialNo = EReceiptUtils.getInstance().GetSerialNumber(transData.getERCMTerminalSerialNumber());
//            if (EReceiptUtils.PACK_TESTMODE ==true ){
//                serialNo = EReceiptUtils.getInstance().GetSerialNumber(transData.getERCMTerminalSerialNumber());
//            } else {
//                serialNo = EReceiptUtils.StringPadding(serialNo, max_len, "0", Convert.EPaddingPosition.PADDING_RIGHT);
//            }

        }
        setBitData("46", EReceiptUtils.getInstance().getSize(serialNo.getBytes()) );
    }

    @Override
    protected  void setBitData55(@NonNull TransData transData) throws Iso8583Exception {
        String BankCode = transData.getERCMBankCode();
        if (BankCode != null) {
            setBitData("55", BankCode.getBytes());
            Log.i(EReceiptUtils.TAG, "ERCM-BankCode has been set."  );
        } else {
            Log.i(EReceiptUtils.TAG, "getERCMBankCode in transdata was missing."  );
        }

    }

    private  void setBitData56(@NonNull TransData transData) throws Iso8583Exception {
        String mercCode = transData.getERCMMerchantCode();
        if (mercCode != null) {
            setBitData("56", mercCode.getBytes());
            Log.i(EReceiptUtils.TAG, "ERCM-MerchantCode has been set."  );
        } else {
            Log.i(EReceiptUtils.TAG, "getERCMMerchantCode in transdata was missing."  );
        }
    }

    private  void setBitData57(@NonNull TransData transData) throws Iso8583Exception {
        //String storeCode = transData.getERCMStoreCode();
        String storeCode = ERMUtil.INSTANCE.getErmStoreCode(transData);
        if (storeCode != null) {
            setBitData("57", storeCode.getBytes());
            Log.i(EReceiptUtils.TAG, "ERCM-StoreCode has been set."  );
        } else {
            Log.i(EReceiptUtils.TAG, "getERCMStoreCode in transdata was missing."  );
        }

    }

    @Override
    protected  void setBitData61(@NonNull TransData transData) throws Iso8583Exception  {
        byte[] DE61Data= new  byte[0] ;
        DE61Data =transData.getSessionKeyBlock();
//        if (EReceiptUtils.PACK_TESTMODE ==  true ){
//
//        }
//        else {
//            DE61Data = EReceiptUtils.getInstance().CreateSessionKeyBlock(transData);
//        }

        if (DE61Data != null) {
            Log.i(EReceiptUtils.TAG, "SessionKeyBlock was created."  );
            //transData.setField61Byte(EReceiptUtils.getInstance().getSize(DE61Data));
            setBitData("61", DE61Data );
        } else {
            Log.i(EReceiptUtils.TAG, "SessionKeyBlock is missing."  );
        }


    }

    @Override
    protected  void setBitData62(@NonNull TransData transData) throws Iso8583Exception  {
        byte[] DE62Data = new byte[0];
        DE62Data = new byte[] {0x1f, (byte)0x8B ,0x08, 0x08} ;
//        if (EReceiptUtils.PACK_TESTMODE ==  true ){
//            DE62Data = new byte[] {0x1f, (byte)0x8B ,0x08, 0x08} ;
//        }
//        else {
//            DE62Data = transData.getERCMFooterImagePath();
//        }

        if (DE62Data != null ) {setBitData("62", DE62Data);
            Log.i(EReceiptUtils.TAG, "ERCMFooterImageData in transdata has been set : size=" + transData.getERCMFooterImagePath() + " bytes." );
        }
        else {
            Log.i(EReceiptUtils.TAG, "ERCMFooterImageData in transdata is nothing.");
        }
    }

    @Override
    protected  void setBitData63(@NonNull TransData transData) throws Iso8583Exception  {
        //byte[] DE63Data = new byte[] {0x00, (byte)0xFF};
        byte[] DE63Data = new byte[0];
        DE63Data = transData.getERCMLogoImagePath();
//        if (EReceiptUtils.PACK_TESTMODE ==  true ){
//            DE63Data = transData.getERCMLogoImagePath();
//        }
//        else {
//            DE63Data = transData.getERCMLogoImagePath();
//        }
        if (transData.getERCMLogoImagePath() != null ) {
            setBitData("63",DE63Data);
            Log.i(EReceiptUtils.TAG, "getERCMLogoImagePath in transdata has been set : size=" + transData.getERCMLogoImagePath() + " bytes." );
        } else {
            Log.i(EReceiptUtils.TAG, "getERCMLogoImagePath in transdata is nothing.");
        }


    }

//    private void readAndSetFromImagePath (String BitNumber,byte[] ImgBytes) throws Iso8583Exception{
//        setBitData(BitNumber, ImgBytes);
//    }
//
//    private void readAndSetFromImagePath (String BitNumber,String imagePath) throws Iso8583Exception  {
//        if (imagePath != null) {
//            File pImg = new File(imagePath) ;
//            byte[] ImgBytes ;
//
//            if (pImg.isFile()) {
//                if (pImg.length() > 0) {
//                    int ImgSize =  Integer.valueOf(String.valueOf(pImg.length())) ;
//                    ImgBytes = new byte[ImgSize-1];
//
//                    try {
//                        FileInputStream fileReader = new FileInputStream(imagePath);
//                        fileReader.read(ImgBytes,0,ImgSize-1);
//
//                        setBitData(BitNumber, ImgBytes);
//                    } catch (FileNotFoundException e) {
//                        e.printStackTrace();
//                    } catch (IOException e) {
//                        e.printStackTrace();
//                    }
//                }
//            }
//        }
//    }

}

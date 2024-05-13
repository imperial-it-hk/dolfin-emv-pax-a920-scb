package com.pax.pay.trans.pack;
import androidx.annotation.NonNull;

import com.pax.abl.core.ipacker.PackListener;
import com.pax.gl.pack.exception.Iso8583Exception;
import com.pax.pay.app.FinancialApplication;
import com.pax.pay.base.Acquirer;
import com.pax.pay.constant.Constants;
import com.pax.pay.trans.action.ActionEReceiptInfoUpload;
import com.pax.pay.trans.component.Component;
import com.pax.pay.trans.model.ETransType;
import com.pax.pay.trans.model.TransData;
import com.pax.pay.utils.Convert;
import com.pax.pay.utils.EReceiptUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.GZIPOutputStream;

import th.co.bkkps.utils.Log;

public class PackEReceiptUploadForMultiApp extends PackIsoBase {
    private TransData origTransData;

    public PackEReceiptUploadForMultiApp(PackListener listener) {
        super(listener);
        super.setIsErcmEnable(true);  // Extend size Field63 can contain data over 999 bytes
    }

    @NonNull
    @Override
    public byte[] pack(@NonNull TransData transData) {
        try {
            setMandatoryData(transData);

            return packERMTLEDData(transData);
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
        entity.setFieldValue("m", transData.getTransType().getMsgType());

        String L4Dgs = getLast4Digits(transData).replace("X", "0").replace("x", "0");
        transData.setPan(L4Dgs);
        setBitData2(transData);
        setBitData4(transData);
        setBitData11(transData);

        setBitData12(transData);//12-13
        if(transData.getOrigTransType() !=null && (transData.getOrigTransType()==ETransType.QR_INQUIRY
                || transData.getOrigTransType()==ETransType.QR_VOID_KPLUS
                || transData.getOrigTransType()==ETransType.QR_INQUIRY_ALIPAY
                || transData.getOrigTransType()==ETransType.QR_VOID_ALIPAY
                || transData.getOrigTransType()==ETransType.QR_INQUIRY_WECHAT
                || transData.getOrigTransType()==ETransType.QR_VOID_WECHAT
                || transData.getOrigTransType()==ETransType.QR_INQUIRY_CREDIT
                || transData.getOrigTransType()==ETransType.QR_VOID_CREDIT)) {
            transData.setExpDate("0000");
        }
        setBitData14(transData);
//
//        transData.setNii(origTransData.getAcquirer().getNii());//todo
        transData.setNii(transData.getInitAcquirerIndex());
        setBitData24(transData);

        setBitData37(transData);
        setBitData38(transData);

        Acquirer currentAcquirer = transData.getAcquirer();
        Acquirer swapAcuirer = FinancialApplication.getAcqManager().findActiveAcquirer(transData.getInitAcquirerName());
        transData.setAcquirer(swapAcuirer);
        setBitData41(transData);
        setBitData42(transData);

        setBitData46(transData);
//        setBitData54(transData);
        setBitData58(transData);//todo Payment type??? SALE,VOID SALE,...
        setBitData59(transData);//todo Payment media??? VISA,MASTER,...
        setBitData60(transData);
        setBitData61Byte(transData);//todo Receipt image format
        setBitData62(transData);
        setBitData63Byte(transData);//todo Receipt image table ???

        transData.setAcquirer(currentAcquirer);
    }



    private String getLast4Digits (TransData transData) {
        String FullPAN =null ;
        if (transData.getPan() != null) {
            FullPAN = transData.getPan();
        } else if (transData.getPan() == null
                    && (origTransData.getTransType() == ETransType.QR_INQUIRY_ALIPAY
                            || origTransData.getTransType() == ETransType.QR_VOID_ALIPAY
                            || origTransData.getTransType() == ETransType.QR_INQUIRY_WECHAT
                            || origTransData.getTransType() == ETransType.QR_VOID_WECHAT
                            || origTransData.getTransType() == ETransType.QR_INQUIRY
                            || origTransData.getTransType() == ETransType.QR_VOID_KPLUS)) {
            FullPAN = origTransData.getTxnID();
        } else if (origTransData.getTransType() == ETransType.QR_INQUIRY_CREDIT
                || origTransData.getTransType() == ETransType.QR_VOID_CREDIT) {
            FullPAN = origTransData.getBuyerLoginID() != null ? origTransData.getBuyerLoginID().trim() : null;
        }
        if (FullPAN != null) {
            FullPAN= FullPAN.trim();
            String PanLast4Digits = null;
            PanLast4Digits= FullPAN.substring(FullPAN.length()-4 ,FullPAN.length());
            return PanLast4Digits;
        } else {
            return null;
        }
    }

    @Override
    protected void setBitData4(@NonNull TransData transData) throws Iso8583Exception {
        setBitData("4", transData.getAmount());
    }

    @Override
    protected void setBitData37(@NonNull TransData transData) throws Iso8583Exception {
        String RefNo = "";
        if(transData.getRefNo()!=null) {
            if(transData.getRefNo().isEmpty()) {
                RefNo = Component.getPaddedNumber(transData.getAcquirer().getCurrBatchNo(), 6) + Component.getPaddedNumber(origTransData.getStanNo(), 6);
            } else {
                RefNo = transData.getRefNo();
            }
        } else {
            RefNo = Component.getPaddedNumber(transData.getAcquirer().getCurrBatchNo(), 6) + Component.getPaddedNumber(origTransData.getStanNo(), 6);
        }
        setBitData("37", RefNo);
        //setBitData("37", origTransData.getRefNo());
    }

    @Override
    protected void setBitData38(@NonNull TransData transData) throws Iso8583Exception {
        setBitData("38", transData.getAuthCode());
    }

    @Override
    protected void setBitData46(@NonNull TransData transData) throws Iso8583Exception {
        String TSN = EReceiptUtils.getInstance().GetSerialNumber(FinancialApplication.getDownloadManager().getSn());
        setBitData("46", EReceiptUtils.getInstance().getSize(TSN.getBytes()));
    }

    @Override
    protected void setBitData58(@NonNull TransData transData) throws Iso8583Exception {
        ActionEReceiptInfoUpload actionEReceiptInfoUpload = new ActionEReceiptInfoUpload(null);
        String[] Reformat = actionEReceiptInfoUpload.ReformatCardSchemeMultiApp(transData);
        Log.d(EReceiptUtils.TAG,"         Payment Type = " + Reformat[0]);
        setBitData("58", Reformat[0]);                                      //todo Payment type  -- SALE,VOID, REDEEM, INSTALLMENT
    }

    @Override
    protected void setBitData59(@NonNull TransData transData) throws Iso8583Exception {
        ActionEReceiptInfoUpload actionEReceiptInfoUpload = new ActionEReceiptInfoUpload(null);
        String[] Reformat = actionEReceiptInfoUpload.ReformatCardSchemeMultiApp(transData);
        Log.d(EReceiptUtils.TAG,"         Payment Method = " + Reformat[1]);
        setBitData("59",  Reformat[1]);                                     //todo Payment method -- Card Scheme VISA, MC, JCB, AMEX
    }

    @Override
    protected void setBitData60(@NonNull TransData transData) throws Iso8583Exception {
        setBitData("60", Component.getPaddedNumber(transData.getAcquirer().getCurrBatchNo(), 6));
    }

    @Override
    protected void setBitData61Byte(@NonNull TransData transData) throws Iso8583Exception {
        //byte[] tmp_compress_de61 = Tools.str2Bcd("1f8b08080000000002ff44415441008c8eb18eda401086fb798a91d25c2c1dccc01a8ca514c37a0c2b836ded2edc5e952a2f90228fe9765f27c501c7118a7cd234ff3f33ff4fc419111171f48a5e0f2a41e12aed5d0cd29f0e430719e38ccaf27551f2aadae0cbd6cfcad27c87fcfa1cc8d135dff09315f3a632eb25e4e3c530c444546dcc9a888821472f56ef6f8888cc12f256a2dd3f1a5c410e51fa7b1d89889715e41fcf816ca824e4554a98524a58111bb490cf2ec8ed89a6b1c694e629412633e7c59cab8bc59bda94756920cb387ab443a39fd19b922ac85e5bec4fc75b4dbe769271bc459c5d10b45e1b176112d7608d4217964c4c30458b35b642cbb53165b960a956db16a678f6582351451f03d96bcbf80f09b2d776f1d4087250c8728c75dc6fef2c5ecc9821c3045370bbbefef9bfc0f435a3f3e23ad1b7b917bb975efa08d3e34e5114d80fe8b53df50d1645715d7028b6eb87b783363bc520d185566c1cfc3b7ab5eac678ff6a68d1eb41a23b2bee86a109f3a0feecac86c740ecb6d277f8fbd79f9a673ce3af958aa2280a3caab77be923da617cffd0e0ef002ae5c5da24030000");
        //unzip format :: 303031D72020202020205052452052454C454153450AD72020202020205048495453414E554C4F4B0AD720542E3035352D323531363839202842522E353534290AD72D2D2D2D2D2D2D2D2D2D2D2D2D2D2D2D2D2D2D2D2D2D2D0AD754494423202020202020202020202036313139383437330AD74D494423202020203430313030303839343730303030310AD754524143452320202020202020202020203030303034330AD742415443482320202020202020202020203030303031380AD75354414E232020202020202020202020203030303133380AD73D3D3D3D3D3D3D3D3D3D3D3D3D3D3D3D3D3D3D3D3D3D3D0AD73430353020313658582058585858203830313420430AD7564953412020202020202020204558503A2058582F58580AD730342F31322F31382020202020202031393A34353A35340AD74150505220434F444520202020202020203030393530380AD7524546204E554D202020203030303031313030303133380AD741505020202020202020202056495341204352454449540ACE414944203A2041303030303030303033313031300ACE5443203A20464130333734343535323141383642460ACE545652203A20303038303030383030300AD752454631202020202020202020202020202020202020580AD752454632202020202020202020202020202020202020580AD753414C450AD7414D543A544842202020202020202020202031322E31310AD70ACE0ACE5349474E3A5F5F5F5F5F5F5F5F5F5F5F5F5F5F5F5F5F5F5F5F5F5F5F5F5F5F5F5F5F5F5F5F5F5F5F5F5F5F5F5F5F0ACE20202020202020202020202020204B5241494B4145572F52414348414E414E540ACE0ACE20202020202020202020202020202A2A2A204E4F20524546554E44202A2A2A0ACE202020202020492041434B4E4F574C45444745205341544953464143544F525920524543454950540ACE202020202020202020204F462052454C415449564520474F4F44532F53455256494345530ACE2020202020202020202020202020204B42414E4B207265763A312E312E310ACE0ACE202020202020202020202A2A2A2A2A204D45524348414E5420434F5059202A2A2A2A2A0A
        byte[] compress_de61 = gZipCompress(transData.geteSlipFormat());
        setBitData("61", compress_de61);
    }

    @Override
    protected void setBitData63Byte(@NonNull TransData transData) throws Iso8583Exception {
        setBitData("63", generateERTable(transData));
    }

    public byte[] generateERTable(TransData transData) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try {
            outputStream.write(new byte[]{0x45, 0x52});//Table ID: ER
            outputStream.write(new byte[]{0x30, 0x30, 0x30, 0x32});//version
            outputStream.write(new byte[]{0x31});//Compression: 1 - GZIP
            outputStream.write(new byte[]{0x32});//Terminal receipt format : 2 - VX
            outputStream.write(getEReceiptType(transData)); //getEReceiptType(transData));//Signature capture code
            outputStream.write(new byte[]{0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0,
                                            0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0});//Receipt Hash - reserved
            outputStream.write(new byte[]{0x0, 0x0});//Number of receipt - reserved
            outputStream.write(new byte[]{0x0, 0x0});//Block number - reserved
            return outputStream.toByteArray();
        } catch (IOException e) {
            Log.e(TAG, "", e);
        } finally {
            try {
                outputStream.close();
            } catch (IOException e) {
                Log.e(TAG, "", e);
            }
        }
        return "".getBytes();
    }

    protected byte[] gZipCompress(byte[] input) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try {
            GZIPOutputStream gzip = new GZIPOutputStream(outputStream);
            gzip.write(input);
            gzip.close();

            return outputStream.toByteArray();
        } catch (IOException e) {
            Log.e(TAG, "", e);
        } finally {
            try {
                outputStream.close();
            } catch (IOException e) {
                Log.e(TAG, "", e);
            }
        }
        return input;
    }

    private byte[] getEReceiptType(TransData transData) {
        //todo case sign pad fail send '01'
        if (transData.getTransType() != ETransType.ERCEIPT_SETTLE_UPLOAD) {
            if (transData.getSignData() != null) {
                return "00".getBytes();
            } else if (transData.isPinVerifyMsg() || transData.isTxnSmallAmt()) {
                return "02".getBytes();
            }
        }
        return "03".getBytes();//Customer did not sign
    }
}

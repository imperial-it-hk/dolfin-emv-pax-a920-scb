package com.pax.pay.trans.pack;

import th.co.bkkps.utils.Log;
import androidx.annotation.NonNull;
import com.pax.abl.core.ipacker.PackListener;
import com.pax.eemv.utils.Tools;
import com.pax.gl.pack.exception.Iso8583Exception;
import com.pax.pay.app.FinancialApplication;
import com.pax.pay.trans.component.Component;
import com.pax.pay.trans.model.ETransType;
import com.pax.pay.trans.model.TransData;
import com.pax.settings.SysParam;

/**
 * Created by WITSUTA A on 11/22/2018.
 */

public class PackGetQrInfo extends PackIso8583 {

    public PackGetQrInfo(PackListener listener) {
        super(listener);
    }

    @Override
    @NonNull
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
        entity.setFieldValue("m", transType.getMsgType());

        setBitData3(transData);

        setBitData4(transData);

        setBitData11(transData);

        setBitData12(transData);

        // field 24 Nii
        transData.setNii(transData.getAcquirer().getNii());
        setBitData24(transData);

        setBitData17(transData);

        setBitData41(transData);

        setBitData42(transData);

        setBitData63(transData);
    }

    @Override
    protected void setBitData17(@NonNull TransData transData) throws Iso8583Exception {
        String temp = transData.getDateTime();
        if (temp != null && !temp.isEmpty()) {
            String year = temp.substring(0, 4);
            setBitData("17", year);
        }
    }

    @Override
    protected void setBitData63(@NonNull TransData transData) throws Iso8583Exception {

        String tableID = "QR";
        String tableVersion = Tools.hexToAscii("03");

        String supCard = Tools.hexToAscii(transData.getQrType());
        String billerID = transData.getAcquirer().getBillerIdPromptPay();

        String merCountry = "TH";

        String merName = FinancialApplication.getSysParam().get(SysParam.StringParam.EDC_MERCHANT_NAME_EN);
        String merNameLength = Tools.hexToAscii(Component.getPaddedNumber(merName.length(), 2));

        String totalMSG = tableID + tableVersion + supCard + billerID + merCountry + merNameLength + merName;

        String length63 = Tools.hexToAscii(String.valueOf(Component.getPaddedNumber(Tools.string2Bytes(totalMSG).length, 4)));

        String field63 = length63 + totalMSG;

        setBitData("63",field63);
    }
}

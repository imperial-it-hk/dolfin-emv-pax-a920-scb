package com.pax.pay.trans.pack;

import th.co.bkkps.utils.Log;
import androidx.annotation.NonNull;
import com.pax.abl.core.ipacker.PackListener;
import com.pax.abl.utils.EncUtils;
import com.pax.device.UserParam;
import com.pax.eemv.utils.Tools;
import com.pax.gl.pack.exception.Iso8583Exception;
import com.pax.pay.app.FinancialApplication;
import com.pax.pay.trans.model.TransData;
import com.pax.pay.utils.Utils;
import com.pax.settings.SysParam;

public class PackLoadTMK extends PackIso8583 {

    public PackLoadTMK(PackListener listener) {
        super(listener);
    }

    @Override
    @NonNull
    public byte[] pack(@NonNull TransData transData) {
        try {
            // nii 006 to load TLE Key
            if (FinancialApplication.getAcqManager().getCurAcq().isEnableTle())
            {
                transData.setTpdu("600" + "006" + "0000");
            }
            else
            {
                return "".getBytes();
            }

            long stanNo = FinancialApplication.getSysParam().get(SysParam.NumberParam.EDC_STAN_NO);
            transData.setTraceNo(stanNo);
            FinancialApplication.getSysParam().set(SysParam.NumberParam.EDC_TRACE_NO, (int) stanNo, true);

            setMandatoryData(transData);

            // field 11
            setBitData11(transData);

            // field 62
            setBitData62(transData);

            if (IsTransTLE(transData))
                return packWithTLE(transData);
            else
                return pack(false, transData);
        } catch (Iso8583Exception e) {
            Log.e(TAG, "", e);
        }
        return "".getBytes();
    }

    @Override
    protected void setBitData62(@NonNull TransData transData) throws Iso8583Exception {
        String teid = FinancialApplication.getUserParam().getTE_ID();
               teid = FinancialApplication.getConvert().stringPadding(teid, '0', 8, com.pax.glwrapper.convert.IConvert.EPaddingPosition.PADDING_LEFT);
        String tepin = FinancialApplication.getUserParam().getTE_PIN();
        String tle_h = "HTLE";
        String tle_ver = "04";
        String downType = "4";
        String reqType = "1";
        //First 3 digits of KMSIF INI Parameter 001
        String nii = UserParam.KMSIF01.substring(0,3);
        String tid = FinancialApplication.getAcqManager().getCurAcq().getTerminalId();
        //Last 8 digits of KMSIF INI Parameter
        String vendor = UserParam.KMSIF01.substring(UserParam.KMSIF01.length()-8,UserParam.KMSIF01.length());
        String trace = FinancialApplication.getConvert().stringPadding(String.valueOf(transData.getTraceNo()), '0', 6, com.pax.glwrapper.convert.IConvert.EPaddingPosition.PADDING_LEFT);
        //00000012123456781234      TEID + TEPIN
        String pin_string = teid + tepin + "1234";
        String pin_hash = EncUtils.sha1(pin_string).substring(0, 8).toUpperCase();
        String txt_string = pin_hash + tid + trace.substring(trace.length()-4);
        String txt_hash = EncUtils.sha1(txt_string).substring(0, 8).toUpperCase();

        String rsa = FinancialApplication.getUserParam().getRSAKey();

        byte[] exp = new byte[]{(byte) 0x01, 0x00, 0x01};
        byte[] mod = Tools.str2Bcd(rsa);
        byte[] rsaReq = Utils.concat(exp,mod);

        String head = tle_h + tle_ver + downType + reqType + nii + tid + vendor + teid + txt_hash;
        byte[] field62 = Utils.concat(head.getBytes(), rsaReq);
        setBitData("62", field62);
    }
}


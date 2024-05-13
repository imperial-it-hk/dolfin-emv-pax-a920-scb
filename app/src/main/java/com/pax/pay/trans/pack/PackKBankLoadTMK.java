package com.pax.pay.trans.pack;

import th.co.bkkps.utils.Log;
import androidx.annotation.NonNull;
import com.pax.abl.core.ipacker.PackListener;
import com.pax.abl.utils.EncUtils;
import com.pax.device.TerminalEncryptionParam;
import com.pax.device.UserParam;
import com.pax.eemv.utils.Tools;
import com.pax.gl.pack.exception.Iso8583Exception;
import com.pax.pay.app.FinancialApplication;
import com.pax.pay.base.Acquirer;
import com.pax.pay.constant.Bank;
import com.pax.pay.trans.model.TransData;
import com.pax.pay.utils.Utils;
import com.pax.settings.SysParam;

public class PackKBankLoadTMK extends PackIso8583 {

    public PackKBankLoadTMK(PackListener listener) {
        super(listener);
    }

    @Override
    @NonNull
    public byte[] pack(@NonNull TransData transData) {
        try {
            // nii 126 to load TLE Key
            Acquirer acq = FinancialApplication.getAcqManager().getCurAcq();
            if (acq.isEnableTle())
            {
                transData.setTpdu("600" + UserParam.KMSNI01 + "8000");
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
        TerminalEncryptionParam param = FinancialApplication.getUserParam().getTEParam(Bank.KBANK);
        String teid = param.getId();
        teid = FinancialApplication.getConvert().stringPadding(teid, '0', 8, com.pax.glwrapper.convert.IConvert.EPaddingPosition.PADDING_LEFT);
        String tepin = param.getPin();
        String tle_h = "HTLE";
        String tle_ver = "04";
        String downType = "4";
        String reqType = "1";

        String nii = UserParam.KMSAQ01;
        String tid = FinancialApplication.getAcqManager().getCurAcq().getTerminalId();
        String vendor = UserParam.KMSVR01;
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


package com.pax.pay.trans.pack;

import th.co.bkkps.utils.Log;
import androidx.annotation.NonNull;
import com.pax.abl.core.ipacker.PackListener;
import com.pax.device.DeviceImplNeptune;
import com.pax.gl.pack.exception.Iso8583Exception;
import com.pax.jemv.device.DeviceManager;
import com.pax.pay.trans.model.TransData;
import com.pax.pay.utils.Utils;

import java.util.Arrays;

public class PackUPIActTMK extends PackIso8583 {

    private static String NETWORK_MGMT_INF_CODE_TMK_ACTIVATE = "300";

    public PackUPIActTMK(PackListener listener) {
        super(listener);
    }

    @Override
    @NonNull
    public byte[] pack(@NonNull TransData transData) {
        try {

            setMandatoryData(transData);

            // field 11
            setBitData11(transData);

            // field 60
            setBitData60(transData);

            return pack(false,transData);
        } catch (Iso8583Exception e) {
            Log.e(TAG, "", e);
        }
        return "".getBytes();
    }

    @Override
    protected void setBitData60(@NonNull TransData transData) throws Iso8583Exception {

        byte[] sn = new byte[10];
        DeviceManager.getInstance().setIDevice(DeviceImplNeptune.getInstance());
        DeviceManager.getInstance().readSN(sn);

        byte[] hardwareSN = new byte[30];
        Arrays.fill(hardwareSN, (byte) 0x20);
        System.arraycopy(sn,0,hardwareSN,0, 10);

        byte[] field60 = Utils.concat(hardwareSN,NETWORK_MGMT_INF_CODE_TMK_ACTIVATE.getBytes());

        setBitData("60", field60);
    }
}


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
package com.pax.abl.mac;

import androidx.annotation.NonNull;
import com.pax.dal.IPed;
import com.pax.device.Device;
import com.pax.pay.utils.Utils;

/**
 * MAC method enum
 */
public enum EMac {
    /**
     * EDC standard method
     */
    EDC {
        @Override
        @NonNull
        public byte[] getMac(IPed ped, byte keyIndex, byte[] data) {
            String beforeCalcMacData = Utils.bcd2Str(data);

            byte[] mac = Device.calcMac(beforeCalcMacData);
            if (mac.length > 0)
                return Utils.bcd2Str(mac).substring(0, 8).getBytes();
            return "".getBytes();
        }
    },;

    @NonNull
    public abstract byte[] getMac(IPed ped, byte keyIndex, byte[] data);
}

package com.pax.pay.utils.models;

import java.util.Date;

public class SP200FirmwareInfos {
    public enum enumSourceFirmware {NONE, SOFTFILE_FIRMWARE, DEVICE_FIRMWARE}
    public String FirmwareName = null;
    public enumSourceFirmware FirmwareSource = enumSourceFirmware.NONE;
    public String FirmwareVersion = null;
}

package com.pax.eemv.enums;

public enum EKernelType {
    DEF(0),
    JCB(1),
    MC(2),
    VIS(3),
    PBOC(4),
    AE(5),
    ZIP(6),
    FLASH(7),
    EFT(8);

    private byte kernelType;

    EKernelType(int kernelType) {
        this.kernelType = (byte) kernelType;
    }

    public byte getKernelType() {
        return this.kernelType;
    }
}

/* Location:           E:\Linhb\projects\Android\PaxEEmv_V1.00.00_20170401\lib\PaxEEmv_V1.00.00_20170401.jar
 * Qualified Name:     com.pax.eemv.enums.EKernelType
 * JD-Core Version:    0.6.0
 */
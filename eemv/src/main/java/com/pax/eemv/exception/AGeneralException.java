package com.pax.eemv.exception;

public abstract class AGeneralException extends Exception {
    private static final long serialVersionUID = 1L;
    private final String errModule;
    private final int errCode;
    private final String errMsg;

    AGeneralException(String module, int errCode, String errMsg) {
        super(module + "#" + errCode + "(" + errMsg + ")");
        this.errModule = module;
        this.errCode = errCode;
        this.errMsg = errMsg;
    }


    AGeneralException(String module, int errCode, String errMsg, Throwable throwable) {
        super(module + "#" + errCode + "(" + errMsg + ")", throwable);
        this.errModule = module;
        this.errCode = errCode;
        this.errMsg = errMsg;
    }

    public String getErrModule() {
        return this.errModule;
    }

    public int getErrCode() {
        return this.errCode;
    }

    public String getErrMsg() {
        return this.errMsg;
    }
}

/* Location:           E:\Linhb\projects\Android\PaxEEmv_V1.00.00_20170401\lib\PaxEEmv_V1.00.00_20170401.jar
 * Qualified Name:     com.pax.eemv.exception.AGeneralException
 * JD-Core Version:    0.6.0
 */
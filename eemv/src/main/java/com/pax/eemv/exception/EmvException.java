package com.pax.eemv.exception;


public class EmvException extends AGeneralException {
    private static final long serialVersionUID = 1L;
    private static final String MODULE = "EMV";

    public EmvException(String errCode) {
        this(Integer.parseInt(errCode));
    }

    public EmvException(int errCode) {
        this(EEmvExceptions.values()[getErrIndex(errCode)]);
    }

    public EmvException(EEmvExceptions errCode) {
        super(MODULE, errCode.getErrCodeFromBasement(), errCode.getErrMsg());
    }

    public EmvException(EEmvExceptions errCode, Throwable throwable) {
        super(MODULE, errCode.getErrCodeFromBasement(), errCode.getErrMsg(), throwable);
    }

    private static int getErrIndex(int errCode) {
        int result = 0;
        for (EEmvExceptions e : EEmvExceptions.values()) {
            if (e.getErrCodeFromBasement() == errCode) {
                return result;
            }
            result++;
        }
        return EEmvExceptions.EMV_ERR_UNKNOWN.ordinal();
    }

}

/* Location:           E:\Linhb\projects\Android\PaxEEmv_V1.00.00_20170401\lib\PaxEEmv_V1.00.00_20170401.jar
 * Qualified Name:     com.pax.eemv.exception.EmvException
 * JD-Core Version:    0.6.0
 */
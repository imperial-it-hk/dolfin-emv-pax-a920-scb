package com.pax.eemv;

import com.pax.eemv.exception.EEmvExceptions;
import com.pax.eemv.exception.EmvException;
import com.pax.eemv.utils.Tools;

class Amount {
    byte[] baseAmount;
    byte[] cashBackAmt;

    public Amount() {
        this.baseAmount = new byte[0];
        this.cashBackAmt = new byte[0];
    }

    public String getAmount() {
        return Tools.bytes2String(this.baseAmount);
    }

    public void setAmount(String amount) throws EmvException {
        if (amount.length() > 12) {
            throw new EmvException(EEmvExceptions.EMV_ERR_AMOUNT_FORMAT);
        }

        this.baseAmount = Tools.string2Bytes(amount);
    }

    public String getCashBackAmt() {
        return Tools.bytes2String(this.cashBackAmt);
    }

    public void setCashBackAmt(String cashBackAmt) throws EmvException {
        if (cashBackAmt.length() > 12) {
            throw new EmvException(EEmvExceptions.EMV_ERR_AMOUNT_FORMAT);
        }

        this.cashBackAmt = Tools.string2Bytes(cashBackAmt);
    }
}

/* Location:           E:\Linhb\projects\Android\PaxEEmv_V1.00.00_20170401\lib\PaxEEmv_V1.00.00_20170401.jar
 * Qualified Name:     com.pax.eemv.Amount
 * JD-Core Version:    0.6.0
 */
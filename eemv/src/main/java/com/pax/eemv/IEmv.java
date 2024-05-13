package com.pax.eemv;

import com.pax.eemv.entity.CTransResult;
import com.pax.eemv.entity.InputParam;
import com.pax.eemv.exception.EmvException;

public interface IEmv extends IEmvBase {
    IEmv getEmv();

    void setListener(IEmvListener listener);

    void readCardProcess(InputParam inputParam) throws EmvException;

    CTransResult afterReadCardProcess(InputParam inputParam) throws EmvException;
}

/* Location:           E:\Linhb\projects\Android\PaxEEmv_V1.00.00_20170401\lib\PaxEEmv_V1.00.00_20170401.jar
 * Qualified Name:     com.pax.eemv.IEmv
 * JD-Core Version:    0.6.0
 */
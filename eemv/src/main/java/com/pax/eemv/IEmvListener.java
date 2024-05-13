package com.pax.eemv;

import com.pax.eemv.entity.Amounts;
import com.pax.eemv.entity.CandList;
import com.pax.eemv.enums.EOnlineResult;
import com.pax.eemv.exception.EmvException;

import java.util.List;

public interface IEmvListener {
    Amounts onGetAmounts();

    int onWaitAppSelect(boolean isFirstSelect, List<CandList> candList);

    int onConfirmCardNo(String cardNo);

    int onCardHolderPwd(boolean bOnlinePin, int leftTimes, byte[] pinData);

    EOnlineResult onOnlineProc();

    boolean onChkExceptionFile();

    int setDe55ForReversal() throws EmvException;

    EOnlineResult onUpdateScriptResult();

    boolean onChkForceSettlement();

    int onChkIsOfflineTransSend();

    int onProcessEnterAuthCode();

    void onUpdateOfflineTransDataFromKernel();

    int onChkIsNotAllowRefundFullEmv();

    boolean onChkIsDynamicOffline();

    int onProcessEnterRefNo();
}

/* Location:           E:\Linhb\projects\Android\PaxEEmv_V1.00.00_20170401\lib\PaxEEmv_V1.00.00_20170401.jar
 * Qualified Name:     com.pax.eemv.IEmvListener
 * JD-Core Version:    0.6.0
 */
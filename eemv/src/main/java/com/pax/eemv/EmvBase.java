package com.pax.eemv;

import android.util.SparseArray;

import com.pax.eemv.entity.AidParam;
import com.pax.eemv.entity.CTransResult;
import com.pax.eemv.entity.Capk;
import com.pax.eemv.entity.Config;
import com.pax.eemv.entity.InputParam;
import com.pax.eemv.exception.EEmvExceptions;
import com.pax.eemv.exception.EmvException;

import java.util.ArrayList;
import java.util.List;

public abstract class EmvBase implements IEmv {
    protected List<Capk> capkList;
    protected List<AidParam> aidParamList;
    protected Config cfg = new Config();
    private SparseArray<byte[]> tags = new SparseArray<>();
    protected String acquirerName;
    protected boolean isAmexErrorGotoSecondGen = false;
    protected boolean isReversalFail = false;
    protected boolean isTleFail = false;
    protected boolean isOfflineApprNeedChkReverse = false;// Fixed EDCBBLAND-235 Modify auto reversal to support offline trans

    static {
        System.loadLibrary("F_DEVICE_LIB_Android");
        System.loadLibrary("F_PUBLIC_LIB_Android");
    }

    protected EmvBase() {
        capkList = new ArrayList<>();
        aidParamList = new ArrayList<>();
    }

    @Override
    public IEmv getEmv() {
        return this;
    }

    @Override
    public void init() throws EmvException {
        tags.clear();
    }

    @Override
    public final byte[] getTlv(int tag) {
        if (tag == 0x71 || tag == 0x72) {
            return tags.get(tag);
        }
        return getTlvSub(tag);
    }

    @Override
    public final void setTlv(int tag, byte[] value) throws EmvException {
        if (tag == 0x71 || tag == 0x72) {
            tags.put(tag, value);
            return;
        }
        setTlvSub(tag, value);
    }

    @Override
    public void setConfig(Config emvCfg) {
        cfg = emvCfg;
    }

    @Override
    public Config getConfig() {
        return cfg;
    }

    // Run callback
    // Parameter settings, loading aid,
    // select the application, application initialization,read application data, offline data authentication,
    // terminal risk management,cardholder authentication, terminal behavior analysis,
    // issuing bank data authentication, execution script
    @Override
    public CTransResult process(InputParam inputParam) throws EmvException {
        throw new EmvException(EEmvExceptions.EMV_ERR_NO_KERNEL);
    }

    @Override
    public void setListener(IEmvListener listener) {
        //do nothing
    }

    @Override
    public void setAidParamList(List<AidParam> aidParamList) {
        this.aidParamList = aidParamList == null ? new ArrayList<AidParam>() : aidParamList;
    }

    @Override
    public List<AidParam> getAidParamList() {
        return aidParamList == null ? new ArrayList<AidParam>() : aidParamList;
    }

    @Override
    public void setCapkList(List<Capk> capkList) {
        this.capkList = capkList == null ? new ArrayList<Capk>() : capkList;
    }

    @Override
    public String getAcquirerName() {
        return acquirerName;
    }

    @Override
    public void setAcquirerName(String acquirerName) {
        this.acquirerName = acquirerName;
    }

    @Override
    public String getVersion() {
        return "";
    }

    protected abstract byte[] getTlvSub(int tag);

    protected abstract void setTlvSub(int tag, byte[] value) throws EmvException;
}
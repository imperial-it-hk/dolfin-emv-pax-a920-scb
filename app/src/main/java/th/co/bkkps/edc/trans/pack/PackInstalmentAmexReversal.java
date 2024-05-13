package th.co.bkkps.edc.trans.pack;

import androidx.annotation.NonNull;

import com.pax.abl.core.ipacker.PackListener;
import com.pax.device.UserParam;
import com.pax.gl.pack.exception.Iso8583Exception;
import com.pax.glwrapper.convert.IConvert;
import com.pax.pay.app.FinancialApplication;
import com.pax.pay.trans.model.TransData;
import com.pax.pay.trans.pack.PackReversal;

import th.co.bkkps.utils.Log;

public class PackInstalmentAmexReversal extends PackReversal {
    public PackInstalmentAmexReversal(PackListener listener) {
        super(listener);
    }

    @Override
    @NonNull
    public byte[] pack(@NonNull TransData transData) {
        try {
            setCommonData(transData);
            setBitData48(transData);

            if (IsTransTLE(transData)) {
                transData.setTpdu("600" + UserParam.TLENI01 + "8000");
                setBitHeader(transData);
            }
            return pack(false, transData);

        } catch (Exception e) {
            Log.e(TAG, "", e);
        }
        return "".getBytes();
    }

    @Override
    protected void setBitData48(@NonNull TransData transData) throws Iso8583Exception {
        byte[] terms = FinancialApplication.getConvert().strToBcd(transData.getInstalmentTerms(), IConvert.EPaddingPosition.PADDING_LEFT);
        setBitData("48", new byte[]{0x05, terms[0]});
    }
}

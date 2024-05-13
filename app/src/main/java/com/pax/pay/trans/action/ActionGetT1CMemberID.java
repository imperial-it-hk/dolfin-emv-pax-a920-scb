package com.pax.pay.trans.action;

import android.content.Context;

import com.pax.abl.core.AAction;
import com.pax.abl.core.ActionResult;
import com.pax.edc.opensdk.TransResult;
import com.pax.eemv.utils.Tools;
import com.pax.pay.ECR.EcrData;
import com.pax.pay.app.FinancialApplication;
import com.pax.pay.base.Acquirer;
import com.pax.pay.constant.Constants;
import com.pax.pay.trans.model.ETransType;
import com.pax.pay.trans.model.TransData;
import com.pax.pay.trans.transmit.Online;
import com.pax.pay.trans.transmit.TransProcessListener;
import com.pax.pay.trans.transmit.TransProcessListenerImpl;
import com.pax.settings.SysParam;

import java.io.UnsupportedEncodingException;

import th.co.bkkps.utils.Log;

public class ActionGetT1CMemberID extends AAction {

    /**
     * derived classes must call super(listener) to set
     *
     * @param listener {@link ActionStartListener}
     */
    public ActionGetT1CMemberID(ActionStartListener listener) { super(listener); }

    private final String TAG = "T1C";
    private Context context = null ;
    private TransData transData = null;

    public void setParam(Context context, TransData transData) {
        this.context = context;
        this.transData = transData;
    }



    @Override
    protected void process() {
        FinancialApplication.getApp().runInBackground(new Runnable() {
            @Override
            public void run() {
                TransProcessListener transProcessListenerImpl = new TransProcessListenerImpl(context);
                transProcessListenerImpl.onUpdateProgressTitle("T1C member inquiry, please wait...");

                int ret = FinancialApplication.getAcqManager().findAycapAcquirer(Constants.ACQ_AYCAP_T1C_HOST);
                if (ret != TransResult.SUCC) {
                    setResult(new ActionResult(ret,null));
                    return;
                }

                // begin prepare transaction data
                if(ret==TransResult.SUCC) {
                    InitGetThe1CardMemberId();
                    if (transData == null) {
                        setResult(new ActionResult(TransResult.ERR_PACK,null));
                        return;
                    }

                    Online transSender = new Online();
                    ret = transSender.online(transData,transProcessListenerImpl);
                    if (ret==TransResult.SUCC) {
                        Log.d(TAG, "\tOnlineResult = " + ret + " (success)");
                        if (transData.getResponseCode().getCode().equals("00")) {
                            Log.d(TAG, "\t\tresp.code = " + transData.getResponseCode().getCode().equals("00"));
                            if (extractT1C_fromField63(transData)) {
                                Log.d(TAG, "\t\t\t\tT1C get member ID success");
                                setResult(new ActionResult(TransResult.SUCC,null));
                            } else {
                                Log.d(TAG, "\t\t\t\tCard isn't T1C card");
                                setResult(new ActionResult(TransResult.ERR_CARD_INVALID,null));
                            }
                        }  else {
                            if (transData.getResponseCode().getCode().equals("56")) {
                                Log.d(TAG, "\t\t\t Error '56' : T1C - No Card record");
                                setResult(new ActionResult(TransResult.T1C_INQUIRY_MEMBER_NO_CARD_RECORD,null));
                            } else if (transData.getResponseCode().getCode().equals("96")) {
                                Log.d(TAG, "\t\t\t Error '96' : T1C - System Malfunction");
                                setResult(new ActionResult(TransResult.T1C_INQUIRY_MEMBER_SYSTEM_MALFUNCTION,null));
                            } else {
                                Log.d(TAG, "\t\t\t Server Response '" + transData.getResponseCode()+ "' : " + transData.getResponseCode().getMessage());
                                transProcessListenerImpl.onShowErrMessage(transData.getResponseCode().getMessage(), Constants.FAILED_DIALOG_SHOW_TIME,false);
                                setResult(new ActionResult(TransResult.ERR_ABORTED,null));
                            }
                        }
                    } else {
                        Log.d(TAG, "\tOnlineResult = " + ret + " (failed)");
                        setResult(new ActionResult(ret,null));
                    }
                }

                transProcessListenerImpl.onHideProgress();
            }
        });
    }

    private TransData InitGetThe1CardMemberId() {
        // temporary acquirer & issuer
        Acquirer acquirer = FinancialApplication.getAcqManager().findAcquirer(Constants.ACQ_AYCAP_T1C_HOST);
        transData.setTransType(ETransType.GET_T1C_MEMBER_ID);
        transData.setStanNo(FinancialApplication.getSysParam().get(SysParam.NumberParam.EDC_STAN_NO));
        transData.setTraceNo(FinancialApplication.getSysParam().get(SysParam.NumberParam.EDC_TRACE_NO));
        transData.setAcquirer(acquirer);
        transData.setNii(acquirer.getNii());
        transData.setTpdu("600"  + acquirer.getNii() + "832F");

        return transData;
    }

    private byte[] renewArraySize(int size) {
        return new byte[size];
    }

    private boolean extractT1C_fromField63(TransData transData) {
        if (transData.getField63RecByte() == null) {
            Log.d(TAG,"\t\t\tField63 contain : null");
            return false;
        }
        Log.d(TAG,"\t\t\tField63 contain : " + Tools.bcd2Str(transData.getField63RecByte()) );

        boolean returnBool = false;
        byte[] f63 = transData.getField63RecByte();
        int curPoS = 0;
        boolean isT1CRecTable =false;

        byte[] table__len = new byte[2] ;
        byte[] table___id = new byte[2] ;
        byte[] non__value = new byte[12] ;
        byte[] exttxntype = new byte[4] ;
        byte[] t1c_cardno = new byte[16] ;
        byte[] cardrefflg = new byte[1] ;
        int tot_len = table___id.length +  non__value.length+  exttxntype.length+  t1c_cardno.length+  cardrefflg.length ;

        if (f63.length >= 4){
            System.arraycopy(f63, curPoS , table__len, 0, table__len.length);  curPoS+=table__len.length;
            System.arraycopy(f63, curPoS , table___id, 0, table___id.length); curPoS+=table___id.length;
            String tabelId = safeConvertByteArrayToString(table___id) ;
            if (tabelId.equals("91")) {
                Log.d(TAG,"\t\t\t >> Invalid table-length");
                isT1CRecTable=true;
            } else {
                if (! (Tools.bytes2Int(table__len) == tot_len))         {Log.d(TAG,"\t\t\t >> Invalid table-length");}
                if (! table___id.equals(new byte[] {(byte)0x91, 0x23})) {Log.d(TAG,"\t\t\t >> Table-Id wasn't match");}
            }
        }
        if (isT1CRecTable) {
            System.arraycopy(f63, curPoS , non__value, 0, non__value.length); curPoS+=non__value.length;
            System.arraycopy(f63, curPoS , exttxntype, 0, exttxntype.length); curPoS+=exttxntype.length;
            System.arraycopy(f63, curPoS , t1c_cardno, 0, t1c_cardno.length); curPoS+=t1c_cardno.length;
            System.arraycopy(f63, curPoS , cardrefflg, 0, cardrefflg.length); curPoS+=cardrefflg.length;
            String extTransType = safeConvertByteArrayToString(exttxntype);
            if ( ! extTransType.equals("9123")) {Log.e(TAG,"\t\t\t >> Extended transaction type wasn't match"); return false;}

            try {
                boolean  isT1CCard = false ;
                if ( ! (new String(t1c_cardno,"UTF-8").equals("0000000000000000"))) {
                    isT1CCard=true;
                    EcrData.instance.T1C_MemberID = t1c_cardno;
                    Log.e(TAG,"\t\t\t >> T1C get memberID OK");
                } else {
                    isT1CCard=false;
                    EcrData.instance.T1C_MemberID = null;
                    Log.e(TAG,"\t\t\t  >> this card isn't T1C card");
                }
                if (isT1CCard) {
                    EcrData.instance.T1C_CardRefFlag = cardrefflg;
                    returnBool=true;
                    Log.e(TAG,"\t\t\t  >> get CardReferenceFlag");
                }
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }
        return returnBool;
    }

    private String safeConvertByteArrayToString (byte[] srcArr) {
        if (srcArr == null) { return  null ;}
        if (srcArr.length == 0) {return null;}
        try {
            String returnStr = new String(srcArr,"UTF8");
            return returnStr;
        } catch (Exception ex) {
            Log.e("GT1C","ERROR on convert byte-array to string") ;
        }
        return null;
    }

}

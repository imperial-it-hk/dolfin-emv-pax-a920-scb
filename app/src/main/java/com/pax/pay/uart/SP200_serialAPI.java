package com.pax.pay.uart;

import android.content.res.AssetManager;
import android.os.SystemClock;

import androidx.annotation.NonNull;

import com.pax.abl.core.ActionResult;
import com.pax.abl.utils.PanUtils;
import com.pax.abl.utils.TrackUtils;
import com.pax.dal.IComm;
import com.pax.dal.entity.ECheckMode;
import com.pax.dal.entity.EPedKeyType;
import com.pax.dal.entity.EUartPort;
import com.pax.dal.entity.UartParam;
import com.pax.dal.exceptions.CommException;
import com.pax.device.DeviceImplNeptune;
import com.pax.device.UserParam;
import com.pax.edc.opensdk.TransResult;
import com.pax.eemv.utils.Tools;
import com.pax.pay.app.FinancialApplication;
import com.pax.pay.base.Acquirer;
import com.pax.pay.constant.Constants;
import com.pax.pay.emv.EmvSP200;
import com.pax.pay.ped.PedManager;
import com.pax.pay.utils.Checksum;
import com.pax.pay.utils.Convert;
import com.pax.pay.utils.Utils;
import com.pax.pay.utils.models.SP200FirmwareInfos;
import com.pax.settings.SysParam;

import java.io.InputStream;
import java.util.Arrays;

import th.co.bkkps.utils.Log;

public class SP200_serialAPI {
    private static final int aeslen = 16;
    private static final int deslen = 8;

    private static final int ENCRYPT_ECB = 3;
    private static final int DECRYPT_ECB = 2;
    private static final int ENCRYPT = 1;
    private static final int DECRYPT = 0;

    private static final String TAG = "SP200_serialAPI";
    private static final EUartPort PORT = EUartPort.PINPAD;

    private static final int MAX_STRING = 9999;
    private static final int TYPE_LEN = 2;
    private static final int FIELD_LEN = 2;
    private static final int LEN_ICC_DATA = 260;

    private static boolean isSp200Enable = false;

    private static boolean runInit = false;
    private byte[] recSP200;
    private static ReceiveThread receiveThread;

    private static SP200_serialAPI instance;

    private static final byte STX = 0x02;  // Start of text
    private static final byte ETX = 0x03;  // End of text
    private static final byte FS  = 0x1C;  // File seperator
    private static final byte ACK = 0x06;  // Acknowledgement
    private static final byte NAK = 0x15;  // Negative acknowledge

    private static int iSp200Mode = 0;
    private static String iSp200Version = null;

    public static final int SP200TIMEOUT = 30;

    // Command
    public static final String CMD_SCAN_QR          = "31";
    public static final String CMD_QR_DISP          = "32";
    public static final String CMD_CANCEL           = "33";
    public static final String CMD_QR_DISP_NO_LOGO  = "34";
    public static final String CMD_DO_CTLS          = "35";
    public static final String CMD_SP200_TRNS       = "36";
    public static final String CMD_GET_SIGNATURE    = "37";
    public static final String CMD_PINPAD           = "38";
    public static final String CMD_GET_PAN          = "39";

    public static final String CMD_INIT             = "40";
    public static final String CMD_SEND_FILE        = "41";
    public static final String CMD_RATE_DCC         = "42";
    public static final String CMD_PINPAD_AES       = "43";

    // Param Type
    public static final String TYPE_SP200_MODE      = "01";
    public static final String TYPE_DATE            = "02";
    public static final String TYPE_TIME            = "03";
    public static final String TYPE_CURRENT_VER     = "04";
    public static final String TYPE_PAN             = "10";
    public static final String TYPE_KEY             = "11";
    public static final String TYPE_PINBLOCK        = "12";
    public static final String TYPE_AMOUNT          = "20";
    public static final String TYPE_AMOUNT_LOCAL    = "21";
    public static final String TYPE_AMOUNT_DCC      = "22";
    public static final String TYPE_EXCHANGE_RATE   = "23";
    public static final String TYPE_MARKUP_DCC      = "24";
    public static final String TYPE_QR              = "30";
    public static final String TYPE_TIMEOUT         = "40";
    public static final String TYPE_FILENAME        = "41";
    public static final String TYPE_FILESIZE        = "42";
    public static final String TYPE_FILECRC         = "43";
    public static final String TYPE_TOTALPACKAGE    = "44";
    public static final String TYPE_PACKAGENUM      = "45";
    public static final String TYPE_PACKAGEDATA     = "46";
    public static final String TYPE_PACKAGESIZE     = "47";

    public static final String TYPE_CTLS_TYPE       = "50"; // 1 = visa paywave
    public static final String TYPE_TRACK2          = "51";
    public static final String TYPE_PANSEQNO        = "52";
    public static final String TYPE_ICCDATA         = "53";
    public static final String TYPE_ICCDATALEN      = "54";
    public static final String TYPE_APPLABEL        = "55";
    public static final String TYPE_APPPREFERNAME   = "56";
    public static final String TYPE_AID             = "57";
    public static final String TYPE_AIDLEN          = "58";
    public static final String TYPE_TVR             = "59";
    public static final String TYPE_APPCRYPTO       = "5A";
    public static final String TYPE_HOLDERNAME      = "5B";
    public static final String TYPE_EMVPAN          = "5C";
    public static final String TYPE_SIGNATURE       = "60";
    public static final String TYPE_FREE_SIGN       = "61";
    public static final String TYPE_FREE_PIN        = "62";
    public static final String TYPE_CTLS_MODE       = "63";   // PATHTYPE
    public static final String TYPE_PINBLOCK_CRC    = "64";
    public static final String TYPE_CTLS_LIMIT      = "65";
    public static final String TYPE_RESULT          = "98";
    public static final String TYPE_ERROR           = "99";

    private static IComm pinPadComm = null;

    public synchronized static SP200_serialAPI getInstance() {
        if (instance == null) {
            UartParam uartParam = new UartParam();
            uartParam.setPort(PORT);
            uartParam.setAttr("115200,8,n,1");
            pinPadComm = FinancialApplication.getDal().getCommManager().getUartComm(uartParam);

            instance = new SP200_serialAPI();
        }

        return instance;
    }

    public void disconnet() {
        if (pinPadComm != null)
        {
            pinPadComm.cancelRecv();
        }
    }

    public SP200_serialAPI() {
        recSP200 = null;
    }

    private byte GetCheckSum(byte[] str, int len)
    {
        int i;

        byte crc = str[0];
        for (i = 1; i < len; i++)
        {
            crc ^= str[i];
        }
        return crc;
    }

    public void SetMsgHeader(byte[] msg, String cmd)
    {
        int i=0;
        msg[i++] = 0x02;
        msg[i++] = 0;
        msg[i++] = 3;
        msg[i++] = cmd.getBytes()[0];
        msg[i++] = cmd.getBytes()[1];
        msg[i++] = FS;
        msg[i++] = ETX;
        msg[i++] = GetCheckSum(msg,7);
    }

    public int GetMsgLength(byte[] msg, int iMsgSize)
    {
        int i;
        for (i = iMsgSize-1; i > 0; -- i)
        {
            if (msg[i] == FS && msg[i-1] != ETX)
            {
                break;
            }
        }

        return (i < 5) ? -1 : i+1; // return length or failure
    }

    public void FinishedMsg(byte[] msg)
    {
        int msgLength = 0;
        msgLength = GetMsgLength(msg, MAX_STRING);

        if (msgLength != -1)
        {
            int len = msgLength - 3;
            byte[] lenByte = Tools.str2Bcd(Integer.toString(len));
            if (len < 100) {
                lenByte = Utils.concat(Tools.str2Bcd(Integer.toString(0)), lenByte);
            }
            System.arraycopy( lenByte, 0 , msg,1, FIELD_LEN );

            msg[msgLength++] = ETX;
            msg[msgLength] = GetCheckSum(msg,msgLength);
        }
    }

    public void AddMsgField(byte[] msg, String fieldType, byte[] data, int len)
    {
        int startField = GetMsgLength(msg, MAX_STRING);

        System.arraycopy( fieldType.getBytes(), 0, msg, startField, TYPE_LEN );

        byte[] lenByte = Tools.str2Bcd(Integer.toString(len));
        if (len < 100) {
            lenByte = Utils.concat(Tools.str2Bcd(Integer.toString(0)), lenByte);
        }

        System.arraycopy( lenByte, 0 , msg,startField + TYPE_LEN, TYPE_LEN );

        System.arraycopy( data, 0, msg, startField + TYPE_LEN + FIELD_LEN, len);

        startField = startField + TYPE_LEN + FIELD_LEN + len;

        msg[startField++] = FS;

        FinishedMsg(msg);
    }

    public int mesg_data(byte[] input, String type, byte[] output)
    {
        int i, len = 0;
        byte[] lenByte = new byte[2];

        if (input==null || input.length < 2)
        {
            return 0;
        }

        for (i = 0; i < input.length-2 ; i++)
        {
            if (input[i] == FS && input[i + 1] == type.getBytes()[0] && input[i + 2] == type.getBytes()[1])
            {
                System.arraycopy( input, i + 3, lenByte, 0,FIELD_LEN);
                len = Integer.parseInt(Tools.bcd2Str(lenByte));
                System.arraycopy( input, i + 5, output, 0, len);
                break;
            }
        }
        return len;
    }

    class ReceiveThread extends Thread {
        @Override
        public void run() {
            super.run();

            byte[] data = null;

            int recvState = 0;
            int len = 0;
            byte[] lenByte = new byte[2];

            recSP200 = null;

            while (!Thread.interrupted()) {

                if (pinPadComm != null) {
                    try {
                        data = pinPadComm.recv(1);
                    } catch (Exception e) {
                        data = null;
                    }

                    if (data != null && !(Tools.bcd2Str(data).equals("")))
                    {
                        recSP200 = Utils.concat(recSP200, data);

                        if (data[0] == FS)
                        {
                            recvState = 1;
                        }
                        else if (recvState == 1)
                        {
                            if(Tools.bcd2Str(data).equals("03"))
                            {
                                System.arraycopy(recSP200, 1, lenByte, 0, FIELD_LEN);
                                len = Integer.parseInt(Tools.bcd2Str(lenByte));

                                if (len != recSP200.length - 4)     // 02 + length1 + length2 + 03
                                {
                                    Arrays.fill(data, (byte)0x00);
                                    recSP200 = null;
                                    break;
                                }
                                recvState = 2;
                            }
                            else
                            {
                                recvState = 0;
                            }
                        }
                        else if (recvState == 2)
                        {
                            recvState = 0;
                            break;
                        }
                    }
                }
            }

            pinPadComm.cancelRecv();
            try {
                pinPadComm.disconnect();
                Log.e(TAG, "pinPadComm.disconnect");
            } catch (CommException e) {
                e.printStackTrace();
            }

        }

    };

    public void BreakReceiveThread(){

        Log.e(TAG, "BreakReceiveThread");

        if (receiveThread != null) {
            receiveThread.interrupt();
            receiveThread = null;
        }
    }

    public int StartReceiveThread(int timeout){

        Log.d(TAG, "StartReceiveThread");
        pinPadComm.setRecvTimeout(10);
        if (receiveThread == null) {
            receiveThread = new ReceiveThread();
            receiveThread.start();
        }

        try {
            if (receiveThread != null) {
                receiveThread.join(timeout * 1000);
                if (receiveThread != null) {
                    receiveThread.interrupt();
                    receiveThread = null;
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            return TransResult.ERR_SP200_FAIL;
        }

        return 0;
    }

    //
    // Get Response Immediately
    //
    public int SendCommandSP200(int timeout, byte[] sendByte,int recvTimeout){

        int iRet = SendCommand(sendByte);

        if(iRet != TransResult.SUCC) {
            return iRet;
        }

        pinPadComm.setRecvTimeout(recvTimeout);
        if (receiveThread == null) {
            receiveThread = new ReceiveThread();
            receiveThread.start();
        }

        try {
            receiveThread.join(timeout * 1000);
            if (receiveThread != null) {
                receiveThread.interrupt();
                receiveThread = null;
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            return TransResult.ERR_SP200_FAIL;
        }

        return 0;
    }

    public int SendCommand(byte[] sendByte){

        recSP200 = null;

        int size = GetMsgLength(sendByte, sendByte.length) + 2 ;
        byte[] rawMsg = new byte[size];
        Utils.SaveArrayCopy(sendByte, 0, rawMsg, 0, size);

        if (pinPadComm.getConnectStatus() == IComm.EConnectStatus.CONNECTED) {
            try {
                pinPadComm.disconnect();
            } catch (CommException e) {
                e.printStackTrace();
            }
        }

        //Reset before send
        //pinPadComm.reset();


        byte[] data = null;

        pinPadComm.setConnectTimeout(1000);

        try {
            pinPadComm.connect();
        } catch (CommException e) {
            e.printStackTrace();
        }

        if (pinPadComm.getConnectStatus() == IComm.EConnectStatus.CONNECTED) {
            pinPadComm.setSendTimeout(10000);
            pinPadComm.setRecvTimeout(100);
            try {

                pinPadComm.send(rawMsg);

                SystemClock.sleep(500);

                data = pinPadComm.recv(1);

            } catch (Exception e) {
                //e.printStackTrace();
                data = null;
            }

            if (sendByte != null) {
                Log.d(TAG, "write_blocked(): " + Convert.getInstance().bcdToStr(rawMsg));
            }
        } else {
            Log.d(TAG, "connect(): Fail....T__T");
        }

        if (data == null || !Tools.bcd2Str(data).equals("06"))
        {
            iSp200Mode = 0;
            runInit = false;
            return -1;
        }else{
            //TODO: set real mode
            iSp200Mode = 1;

        }

        return 0;
    }

    public int SendCommand2(byte[] sendByte){

        recSP200 = null;

        int size = GetMsgLength(sendByte, sendByte.length) + 2 ;
        byte[] rawMsg = new byte[size];
        Utils.SaveArrayCopy(sendByte, 0, rawMsg, 0, size);

        if (pinPadComm.getConnectStatus() == IComm.EConnectStatus.CONNECTED) {
            try {
                pinPadComm.disconnect();
            } catch (CommException e) {
                e.printStackTrace();
            }
        }

        //Reset before send
        //pinPadComm.reset();


        byte[] data = null;

        pinPadComm.setConnectTimeout(1000);

        try {
            pinPadComm.connect();
        } catch (CommException e) {
            e.printStackTrace();
        }

        if (pinPadComm.getConnectStatus() == IComm.EConnectStatus.CONNECTED) {
            pinPadComm.setSendTimeout(10000);
            pinPadComm.setRecvTimeout(100);
            try {

                pinPadComm.send(rawMsg);

                //SystemClock.sleep(500);

                data = pinPadComm.recv(1);

            } catch (CommException e) {
                //e.printStackTrace();
                data = null;
            }

            if (sendByte != null) {
                Log.d(TAG, "write_blocked(): " + Convert.getInstance().bcdToStr(rawMsg));
            }
        } else {
            Log.d(TAG, "connect(): Fail....T__T");
        }

        if (data == null || !Tools.bcd2Str(data).equals("06"))
        {
            iSp200Mode = 0;
            runInit = false;
            return -1;
        }else{
            //TODO: set real mode
            iSp200Mode = 1;

        }

        return 0;
    }


    public int GetSp200Result(int timeout, byte[] szResult)
    {
        byte[] errResult = new byte[2];
        int errCode = 0;
        int i;

        for (i = 0; i < timeout*10; i++)  // timeout in *100ms
        {
            if (recSP200 != null)
            {
                Log.d(TAG, "read(): " + Convert.getInstance().bcdToStr(recSP200));
                mesg_data( recSP200, TYPE_ERROR, errResult);
                errCode = Integer.parseInt(Tools.bcd2Str(errResult));
                System.arraycopy( recSP200, 0, szResult, 0, recSP200.length);
                return errCode;
            }
            SystemClock.sleep(100);
        }

        return -1;
    }

    public EmvSP200 GetContactlessResult(byte[] szMsgResult)
    {
        byte[]  ucClssType   = new byte[2];
        byte[]  ucClssMode   = new byte[2];
        byte[]  ucPanSeqNo   = new byte[2];                       // 23.PAN seq #
        byte[]  ucTrack2     = new byte[100];                     // 35.track
        byte[]  ucTrack2Temp = new byte[100];                     // 35.track
        byte[]  ucICCData    = new byte[LEN_ICC_DATA * 2 + 2];    // 55.ICC data,or AMEX non-EMV transaction 4DBC,DCC Exchange Rate
        byte[]  ucICCDataLen = new byte[4];
        byte[]  ucAppPreferName = new byte[50];
        byte[]  ucAppLabel   = new byte[50];
        byte[]  ucAID        = new byte[30];
        byte[]  ucAIDLEN     = new byte[2];
        byte[]  ucAppCrypto  = new byte[16 + 1];
        byte[]  ucTVR        = new byte[10 + 1];
        byte[]  ucHolderName = new byte[30];
        byte[]  ucEmvPan     = new byte[20];
        byte[]  ucPan        = new byte[20];
        byte[]  ucExpr       = new byte[5];
        byte[]  ucResult     = new byte[2];
        byte[]  ucFreeSign   = new byte[1];
        byte[]  ucFreePin    = new byte[1];
        byte[]  ucCRC        = new byte[4];

        String  sICCData;
        String  sTVR;
        String  sAppCrypto;

        int iClssType   = 0;
        int iPanSeqNo   = 0;
        int iIccDataLen = 0;
        int iAidLen     = 0;
        int iSignFree   = 0;
        int iPinFree    = 0;

        EmvSP200 emvsp200 = new EmvSP200();
        int len = 0;

        if (szMsgResult != null)
        {
            len = mesg_data(szMsgResult, TYPE_CTLS_TYPE, ucClssType);
            if (len>0) {
                emvsp200.setClssType(Integer.parseInt(Tools.bcd2Str(ucClssType)));
            }

            len = mesg_data(szMsgResult, TYPE_CTLS_MODE, ucClssMode);
            if (len>0) {
                emvsp200.setClssMode(Integer.parseInt(Tools.bcd2Str(ucClssMode)));
            }

            len = mesg_data(szMsgResult, TYPE_TRACK2, ucTrack2);
            if (len>0) {
                if (ucTrack2[len - 1] == 'F') {
                    ucTrack2Temp = Arrays.copyOfRange(ucTrack2, 0, len - 1);
                    emvsp200.setTrackData(Tools.bytes2String(ucTrack2Temp).trim());
                } else {
                    emvsp200.setTrackData(Tools.bytes2String(ucTrack2).trim());
                }
            }

            len = mesg_data(szMsgResult, TYPE_PANSEQNO, ucPanSeqNo);
            if (len>0) {
                iPanSeqNo = Integer.parseInt(Tools.bcd2Str(ucPanSeqNo));
                emvsp200.setPanSeqNo(String.format("%03d", iPanSeqNo));
            }

            len = mesg_data(szMsgResult, TYPE_ICCDATA, ucICCData);
            if (len>0) {
                emvsp200.setIccData(Tools.bytes2String(ucICCData).trim());
            }

            len = mesg_data(szMsgResult, TYPE_ICCDATALEN, ucICCDataLen);
            if (len>0) {
                emvsp200.setIccDataLen(Integer.parseInt(Tools.bcd2Str(ucICCDataLen)));
            }

            len = mesg_data(szMsgResult, TYPE_APPLABEL, ucAppLabel);
            if (len>0) {
                emvsp200.setAppLabel(Tools.bytes2String(ucAppLabel).trim());
            }

            len = mesg_data(szMsgResult, TYPE_APPPREFERNAME, ucAppPreferName);
            if (len>0) {
                emvsp200.setAppPreferName(Tools.bytes2String(ucAppPreferName).trim());
            }

            len = mesg_data(szMsgResult, TYPE_AID, ucAID);
            if (len>0) {
                emvsp200.setAid(Tools.bytes2String(ucAID).trim());
            }

            len = mesg_data(szMsgResult, TYPE_AIDLEN, ucAIDLEN);
            if (len>0) {
                emvsp200.setAidLen(Integer.parseInt(Tools.bcd2Str(ucAIDLEN)));
            }

            len = mesg_data(szMsgResult, TYPE_TVR, ucTVR);
            if (len>0) {
                emvsp200.setTvr(Tools.bytes2String(ucTVR));
            }

            len = mesg_data(szMsgResult, TYPE_APPCRYPTO, ucAppCrypto);
            if (len>0) {
                emvsp200.setAppCrypto(Tools.bytes2String(ucAppCrypto).trim());
            }

            len = mesg_data(szMsgResult, TYPE_HOLDERNAME, ucHolderName);
            if (len>0) {
                emvsp200.setHolderName(Tools.bytes2String(ucHolderName).trim());
            }

            mesg_data(szMsgResult, TYPE_EMVPAN, ucEmvPan);
            emvsp200.setEmvPan(Tools.bytes2String(ucEmvPan).trim());

            emvsp200.setPan(TrackUtils.getPan(emvsp200.getTrackData()));
            emvsp200.setExpDate(TrackUtils.getExpDate(emvsp200.getTrackData()));

            len = mesg_data(szMsgResult, TYPE_FREE_SIGN, ucFreeSign);
            if (len>0) {
                iSignFree = Integer.parseInt(Tools.bcd2Str(ucFreeSign));
                emvsp200.setSignFree(iSignFree == 1);
            }

            len = mesg_data(szMsgResult, TYPE_FREE_PIN, ucFreePin);
            if (len>0) {
                iPinFree = Integer.parseInt(Tools.bcd2Str(ucFreePin));
                emvsp200.setPinFree(iPinFree == 1);
            }

            len = mesg_data(szMsgResult, TYPE_PINBLOCK_CRC, ucCRC);
            if (len>0) {
                emvsp200.setCRC(Tools.bytes2String(ucCRC));
            }

            len = mesg_data(szMsgResult, TYPE_RESULT, ucResult);
            if (len>0) {
                emvsp200.setiResult(Integer.parseInt(Tools.bcd2Str(ucResult)));
            }

            return emvsp200;
        }
        return null;
    }

    public EmvSP200 GetPanResult(byte[] szMsgResult)
    {
        byte[]  ucPan        = new byte[20];


        EmvSP200 emvsp200 = new EmvSP200();
        int len = 0;

        if (szMsgResult != null)
        {
            len = mesg_data(szMsgResult, TYPE_PAN, ucPan);
            emvsp200.setPan(Tools.bytes2String(ucPan).trim());

            return emvsp200;
        }
        return null;
    }

    public SP200FirmwareInfos getFirmwareFromSP200Device() {
        runInit = false;
        int ret = initSP200();
        if (ret == TransResult.SUCC) {
            if (iSp200Version != null) {
                SP200FirmwareInfos info = new SP200FirmwareInfos();
                info.FirmwareName = "SP200Device";
                info.FirmwareSource = SP200FirmwareInfos.enumSourceFirmware.DEVICE_FIRMWARE;
                info.FirmwareVersion = iSp200Version;
                return info;
            }
        }
        return null;
    }

    public int initSP200(){
        isSp200Enable = FinancialApplication.getSysParam().get(SysParam.BooleanParam.EDC_SUPPORT_SP200);

        if(!isSp200Enable) {
            iSp200Mode = 0;
            runInit = false;
            return 0;
        }

        if ( runInit)
            return 0;


        byte[] msg = new byte[MAX_STRING];

        SetMsgHeader(msg,CMD_INIT);

        int timeout = SP200TIMEOUT;

        byte[] timeoutByte = Tools.str2Bcd(Integer.toString(timeout));
        if (timeout < 100) {
            timeoutByte = Utils.concat(Tools.str2Bcd(Integer.toString(0)), timeoutByte);
        }

        AddMsgField(msg, TYPE_TIMEOUT, timeoutByte, timeoutByte.length);

        String szDateTime = Utils.getCurentDateTime2();

        AddMsgField(msg, TYPE_DATE, szDateTime.substring(2).getBytes(), 6);
        AddMsgField(msg, TYPE_TIME, szDateTime.substring(8).getBytes(), 6);

        String szCtlsTransLimit = FinancialApplication.getSysParam().getCtlsTransLimitAsString();
        AddMsgField(msg, TYPE_CTLS_LIMIT, szCtlsTransLimit.getBytes(), szCtlsTransLimit.length());

        int iRet = SendCommandSP200(10, msg, 1000); // timeout 10 sec
        if (iRet != 0) return iRet;

        iRet = GetSp200Result(2, recSP200);

        if (iRet == TransResult.SUCC) {
            byte[] ucSp200mode = new byte[4];
            mesg_data(recSP200, TYPE_SP200_MODE, ucSp200mode);

            int iSp200mode = Integer.parseInt(Tools.bcd2Str(ucSp200mode));

            if (iSp200mode > 0) {
                this.iSp200Mode = iSp200mode;
            }

            try {
                byte[] ucSp200version = new byte[64];
                mesg_data(recSP200, TYPE_CURRENT_VER, ucSp200version);
                String respSp200version = (new String(ucSp200version,"UTF-8")).trim();
                if (respSp200version != null && respSp200version.length() != 0) {
                    this.iSp200Version = respSp200version;
                    Log.d("SP200", String.format("Current version of SP200 is = %1$s" , this.iSp200Version));
                } else {
                    this.iSp200Version = null;
                    Log.d("SP200", "Cannot obtain SP200-Version after initialized.");
                }
            } catch (Exception ex) {
                Log.e("SP200",String.format("Error on detecting SP200-Version : %1$s", ex.getMessage() ));
            }
        }

        if (iRet == 0){
            runInit = true;
        }

        return iRet;
    }

    public int checkStatusSP200(){

        isSp200Enable = FinancialApplication.getSysParam().get(SysParam.BooleanParam.EDC_SUPPORT_SP200);
        if(!isSp200Enable) {
            iSp200Mode = 0;
            runInit = false;
            return 0;
        }

        byte[] msg = new byte[MAX_STRING];

        SetMsgHeader(msg,CMD_INIT);

        int timeout = SP200TIMEOUT;

        byte[] timeoutByte = Tools.str2Bcd(Integer.toString(timeout));
        if (timeout < 100) {
            timeoutByte = Utils.concat(Tools.str2Bcd(Integer.toString(0)), timeoutByte);
        }

        AddMsgField(msg, TYPE_TIMEOUT, timeoutByte, timeoutByte.length);

        String szDateTime = Utils.getCurentDateTime2();

        AddMsgField(msg, TYPE_DATE, szDateTime.substring(2).getBytes(), 6);
        AddMsgField(msg, TYPE_TIME, szDateTime.substring(8).getBytes(), 6);

        int iRet = SendCommandSP200(10, msg, 1000); // timeout 10 sec
        if (iRet != 0) return iRet;

        iRet = GetSp200Result(2, recSP200);
        if (iRet == TransResult.SUCC) {
            byte[] ucSp200mode = new byte[4];
            mesg_data(recSP200, TYPE_SP200_MODE, ucSp200mode);

            int iSp200mode = Integer.parseInt(Tools.bcd2Str(ucSp200mode));

            if (iSp200mode > 0) {
                this.iSp200Mode = iSp200mode;
            }
        }

        if (iRet == 0){
            runInit = true;
        }
        return iRet;
    }

    public int cancelSP200(){

        byte[] msg = new byte[MAX_STRING];

        SetMsgHeader(msg,CMD_CANCEL);

        SendCommand(msg);

        int iRet = GetSp200Result(2, recSP200);

        return 0;
    }

    public int cancelQR(){

        byte[] msg = new byte[MAX_STRING];

        SetMsgHeader(msg,CMD_CANCEL);

        SendCommand(msg);

        int iRet = GetSp200Result(2, recSP200);

        isQrRunning = false;

        return 0;
    }


    public byte[] enterPin(byte[] panBlock)
    {
        byte[] ucEncPinBlock = new byte[aeslen];
        byte[] ucDecPinBlock = new byte[aeslen];
        byte[] pinData       = new byte[aeslen];
        byte[] desPinBlock   = new byte[deslen];
        byte[] szCRC         = new byte[4];
        byte[] msg = new byte[MAX_STRING];
        int retry = 3;

        SetMsgHeader(msg, SP200_serialAPI.CMD_PINPAD_AES);
        AddMsgField(msg, SP200_serialAPI.TYPE_PAN, panBlock, panBlock.length);
        String aeskey = Utils.randomString(aeslen);
        AddMsgField(msg, SP200_serialAPI.TYPE_KEY, aeskey.getBytes(), aeskey.length());

        int iRet = -1;
        for (int i = 0; i<retry; i++){
            iRet = SendCommandSP200(SP200TIMEOUT, msg, 1000);
            if (iRet != TransResult.SUCC){
                SystemClock.sleep(100);
                continue;
            } else {
                break;
            }
        }

        if (iRet != TransResult.SUCC){
            return null;
        }

        iRet = GetSp200Result(SP200TIMEOUT,recSP200);

        PedManager ped = FinancialApplication.getPedInstance();

        if (iRet == TransResult.SUCC) {

            int len = mesg_data(recSP200, SP200_serialAPI.TYPE_PINBLOCK, ucEncPinBlock);
            if (len > 0) {
                ucDecPinBlock = DeviceImplNeptune.getInstance().aes(ucEncPinBlock, aeskey.getBytes(), DECRYPT_ECB );
                if (ucDecPinBlock[0] != 0x00) {
                    System.arraycopy(ucDecPinBlock, 0, desPinBlock, 0, deslen);
                    long crc16 = Checksum.getCRC16(desPinBlock);
                    len = mesg_data(recSP200, SP200_serialAPI.TYPE_PINBLOCK_CRC, szCRC);
                    long crcSP200 = Utils.parseLongSafe(Utils.bcd2Str(szCRC),0);

                    if (crc16 == crcSP200) {
                        Acquirer acq = FinancialApplication.getAcqManager().getCurAcq();
                        pinData = ped.calcDes(PedManager.ONE_ENCRYPT, (byte)(UserParam.getTPK_TDKID(acq)), desPinBlock);
                    }
                    else {
                        pinData = null;
                    }
                } else {
                    pinData = null;
                }
            }
            else
            {
                pinData = null;
            }
        }
        else
        {
            pinData = null;
        }
        return pinData;
    }

    public byte[] enterUpiPin(byte[] panBlock, boolean exPinBypass)
    {
        byte[] ucEncPinBlock = new byte[aeslen];
        byte[] ucDecPinBlock = new byte[aeslen];
        byte[] pinData       = new byte[aeslen];
        byte[] desPinBlock   = new byte[deslen];
        byte[] szCRC         = new byte[4];
        byte[] msg = new byte[MAX_STRING];
        int retry = 3;

        SetMsgHeader(msg, SP200_serialAPI.CMD_PINPAD_AES);
        AddMsgField(msg, SP200_serialAPI.TYPE_PAN, panBlock, panBlock.length);
        String aeskey = Utils.randomString(aeslen);
        AddMsgField(msg, SP200_serialAPI.TYPE_KEY, aeskey.getBytes(), aeskey.length());

        int iRet = -1;
        for (int i = 0; i<retry; i++){
            iRet = SendCommandSP200(SP200TIMEOUT, msg, 1000);
            if (iRet != TransResult.SUCC){
                SystemClock.sleep(100);
            } else {
                break;
            }
        }

        if (iRet != TransResult.SUCC){
            pinData = new byte[]{-1};
            return pinData;
        }

        iRet = GetSp200Result(SP200TIMEOUT,recSP200);

        PedManager ped = FinancialApplication.getPedInstance();

        if (iRet == TransResult.SUCC) {

            int len = mesg_data(recSP200, SP200_serialAPI.TYPE_PINBLOCK, ucEncPinBlock);
            if (len > 0) {
                ucDecPinBlock = DeviceImplNeptune.getInstance().aes(ucEncPinBlock, aeskey.getBytes(), DECRYPT_ECB );
                if (ucDecPinBlock[0] != 0x00) {
                    System.arraycopy(ucDecPinBlock, 0, desPinBlock, 0, deslen);
                    long crc16 = Checksum.getCRC16(desPinBlock);
                    len = mesg_data(recSP200, SP200_serialAPI.TYPE_PINBLOCK_CRC, szCRC);
                    long crcSP200 = Utils.parseLongSafe(Utils.bcd2Str(szCRC),0);

                    if (crc16 == crcSP200) {
                        Acquirer acq = FinancialApplication.getAcqManager().getCurAcq();
                        pinData = ped.calcDes(PedManager.ONE_ENCRYPT, (byte)(UserParam.getTPK_TDKID(acq)), desPinBlock);
                    }
                    else {
                        pinData = null;
                    }
                } else {
                    pinData = null;
                }
            }
            else
            {
                pinData = null;
            }
        }
        else
        {
            pinData =  null;
        }
        return pinData;
    }

    public byte[] enterOfflinePin() {
        byte[] ucEncPinBlock = new byte[aeslen];
        byte[] ucDecPinBlock = new byte[aeslen];
        byte[] pinData       = new byte[aeslen];
        byte[] desPinBlock   = new byte[deslen];
        byte[] szCRC         = new byte[4];
        byte[] msg = new byte[MAX_STRING];
        int retry = 3;

        byte[] panBlock = PanUtils.getPanBlock("00000000000000", PanUtils.X9_8_NO_PAN).getBytes();
        SetMsgHeader(msg, SP200_serialAPI.CMD_PINPAD_AES);
        AddMsgField(msg, SP200_serialAPI.TYPE_PAN, panBlock, panBlock.length);
        String aeskey = Utils.randomString(aeslen);
        AddMsgField(msg, SP200_serialAPI.TYPE_KEY, aeskey.getBytes(), aeskey.length());

        int iRet = -1;
        for (int i = 0; i<retry; i++){
            iRet = SendCommandSP200(SP200TIMEOUT, msg, 1000);
            if (iRet != TransResult.SUCC){
                SystemClock.sleep(100);
            } else {
                break;
            }
        }

        if (iRet != TransResult.SUCC){
            return new byte[]{-1};
        }

        iRet = GetSp200Result(SP200TIMEOUT,recSP200);

        PedManager ped = FinancialApplication.getPedInstance();

        if (iRet == TransResult.SUCC) {

            int len = mesg_data(recSP200, SP200_serialAPI.TYPE_PINBLOCK, ucEncPinBlock);
            if (len > 0) {
                ucDecPinBlock = DeviceImplNeptune.getInstance().aes(ucEncPinBlock, aeskey.getBytes(), DECRYPT_ECB );
                if (ucDecPinBlock[0] != 0x00) {
                    System.arraycopy(ucDecPinBlock, 0, desPinBlock, 0, deslen);
                    long crc16 = Checksum.getCRC16(desPinBlock);
                    len = mesg_data(recSP200, SP200_serialAPI.TYPE_PINBLOCK_CRC, szCRC);
                    long crcSP200 = Utils.parseLongSafe(Utils.bcd2Str(szCRC),0);

                    if (crc16 == crcSP200) {
                        Acquirer acq = FinancialApplication.getAcqManager().getCurAcq();
                        pinData = ped.calcDes(PedManager.ONE_ENCRYPT, (byte)(UserParam.getTPK_TDKID(acq)), desPinBlock);
                    }
                    else {
                        pinData = null;
                    }
                } else {
                    pinData = null;
                }
            }
            else
            {
                pinData = null;
                return pinData;
            }

            pinData = ped.calcDes(PedManager.ONE_DECRYPT, (byte) 99, ucEncPinBlock);
            pinData[0] |= 0x20;

        } else {
            pinData = null;
        }

        return pinData;
    }

    private byte[] iccResp;

    public void setIccRespOut(byte[] iccResp){
       this.iccResp = iccResp.clone();
    }

    private boolean isOfflinePin;

    public void setOfflinePin(boolean offlinePin) {
        isOfflinePin = offlinePin;
    }

    public boolean isOfflinePin() {
        return isOfflinePin;
    }

    private boolean isNopinInput;

    public void setNopinInput(boolean noPinInput) {
        isNopinInput = noPinInput;
    }

    public boolean isNopinInput() {
        return isNopinInput;
    }

    private boolean isSp200Cancel;

    public boolean isSp200Cancel() {
        return isSp200Cancel;
    }

    public void setSp200Cancel(boolean sp200Cancel) {
        isSp200Cancel = sp200Cancel;
    }

    public byte[] getIccResp() {
        return iccResp;
    }

    public int DoContactless(String amount){

        byte[] msg = new byte[MAX_STRING];

        SetMsgHeader(msg,CMD_DO_CTLS);

        AddMsgField(msg, TYPE_AMOUNT, amount.getBytes(), amount.length());
        recSP200 = null;
        return SendCommand(msg);
    }

    public void GetContactlessPan(String amount){

        byte[] msg = new byte[MAX_STRING];

        SetMsgHeader(msg,CMD_GET_PAN);

        AddMsgField(msg, TYPE_AMOUNT, amount.getBytes(), amount.length());

        SendCommand(msg);
    }

    public int GetSignature(String amount) {

        byte[] msg = new byte[MAX_STRING];
        int retry = 3;

        SetMsgHeader(msg, SP200_serialAPI.CMD_GET_SIGNATURE);
        AddMsgField(msg, SP200_serialAPI.TYPE_AMOUNT, amount.getBytes(), amount.length());

        int iRet = -1;
        for (int i = 0; i<retry; i++){
            iRet = SendCommand(msg);
            if (iRet != TransResult.SUCC){
                SystemClock.sleep(100);
                continue;
            } else {
                break;
            }
        }
        return iRet;
    }

    public void ShowDccRate(String amountLocal, String amountDcc, String exchangeRate, String markupDcc){

        byte[] msg = new byte[MAX_STRING];

        SetMsgHeader(msg,CMD_RATE_DCC);

        int timeout = 60; // set time out to 60 sec

        byte[] timeoutByte = Tools.str2Bcd(Integer.toString(timeout));

        timeoutByte = Utils.concat(Tools.str2Bcd(Integer.toString(0)), timeoutByte);

        AddMsgField(msg, TYPE_TIMEOUT, timeoutByte, timeoutByte.length);

        AddMsgField(msg, TYPE_AMOUNT_LOCAL, amountLocal.getBytes(), amountLocal.length());
        AddMsgField(msg, TYPE_AMOUNT_DCC, amountDcc.getBytes(), amountDcc.length());
        AddMsgField(msg, TYPE_EXCHANGE_RATE, exchangeRate.getBytes(), exchangeRate.length());
        AddMsgField(msg, TYPE_MARKUP_DCC, markupDcc.getBytes(), markupDcc.length());

        SendCommand(msg);
    }

    private boolean isQrRunning;

    public void setQrRunning(boolean qrRunning) {
        isQrRunning = qrRunning;
    }

    public boolean isQrRunning() {
        return isQrRunning;
    }

    public int ShowQR(int timeout, String QRData, boolean showlogo) {

        byte[] msg = new byte[MAX_STRING];
        int retry = 3;

        if (showlogo)
        {
            SetMsgHeader(msg, SP200_serialAPI.CMD_QR_DISP);
        }
        else
        {
            SetMsgHeader(msg, SP200_serialAPI.CMD_QR_DISP_NO_LOGO);
        }

        byte[] timeoutByte = Tools.str2Bcd(Integer.toString(timeout));
        if (timeout < 100) {
            timeoutByte = Utils.concat(Tools.str2Bcd(Integer.toString(0)), timeoutByte);
        }

        AddMsgField(msg, SP200_serialAPI.TYPE_TIMEOUT, timeoutByte, timeoutByte.length);

        AddMsgField(msg, SP200_serialAPI.TYPE_QR, QRData.getBytes(), QRData.length());

        int iRet = -1;
        for (int i = 0; i<retry; i++){
            iRet = SendCommandSP200(timeout,msg, 10);
            if (iRet != TransResult.SUCC){
                SystemClock.sleep(100);
                continue;
            } else {
                break;
            }
        }

        if (iRet == 0){
            isQrRunning = true;
        }

        return iRet;
    }

    public String GetSp200QRResult()
    {
        byte[] qrMsg  = new byte[999];
        int iRet;

        byte[] szMsgResult = new byte[1024];

        iRet = GetSp200Result(1, szMsgResult);
        if (iRet == TransResult.SUCC) {
            int len = mesg_data(szMsgResult, TYPE_QR, qrMsg);
            if (len>0) {
                return Tools.bytes2String(qrMsg).trim();
            }
        }
        return "";
    }

    public boolean isSp200Enable() {
        return FinancialApplication.getSysParam().get(SysParam.BooleanParam.EDC_SUPPORT_SP200,false);
    }

    public int getiSp200Mode(){
        return iSp200Mode;
    }

    public int updateSp200(SP200FirmwareInfos aiptargDownloadFirmware){
        int iRet = 0;
        int packageSize = 1024*7;
        byte[] fileBytes = null;
        byte[] msg = new byte[9999];
        int fileSize;
        int totalPackage;
        int fileCrc;
        String tag = "updateSp200";

        //file path
        AssetManager am = FinancialApplication.getApp().getResources().getAssets();
        String aipFileName = aiptargDownloadFirmware.FirmwareName;
        try {
            Log.d("AssetFile", String.format("target file for update is '%1$s", aipFileName.toString()));
            if(aipFileName != null) {
                InputStream is = am.open(aipFileName);
                fileBytes = new byte[is.available()];
                is.read(fileBytes);
                is.close();
            } else {
                Log.d("AssetFile", "Cannot find any *.aip firmware file in asset directory.");
                return -1;
            }
        } catch (Exception e){
            e.printStackTrace();
        }

        if (fileBytes == null || fileBytes.length == 0){
            return -1;
        }

        SetMsgHeader(msg, CMD_SEND_FILE);

        String filename = "SP200.aip";
        AddMsgField(msg, TYPE_FILENAME, filename.getBytes(), filename.length());

        fileSize = fileBytes.length;
        byte[] fileSizeByte = Tools.str2Bcd(String.format("%08d", fileSize));
        AddMsgField(msg, TYPE_FILESIZE, fileSizeByte, fileSizeByte.length);
        Log.d(tag, "TYPE_FILESIZE = " + Convert.getInstance().bcdToStr(fileSizeByte));

        if (iSp200Version == null || iSp200Version.length() == 0){
            // use old CRC checksum
            fileCrc = Checksum.getCRC16old(fileBytes);
        } else {
            // use calculate checksum from CRC16 function
            fileCrc = Checksum.getCRC16(fileBytes);
        }

        byte[] fileCrcByte = Tools.str2Bcd(String.format("%08d", fileCrc));

        AddMsgField(msg, TYPE_FILECRC, fileCrcByte, fileCrcByte.length);
        Log.d(tag, "TYPE_FILECRC = " + Convert.getInstance().bcdToStr(fileCrcByte));

        totalPackage = (fileSize / packageSize) + ((fileSize / packageSize > 0) ? 1 : 0);
        byte[] totalPackageByte = Tools.str2Bcd(String.format("%08d", totalPackage));
        AddMsgField(msg, TYPE_TOTALPACKAGE, totalPackageByte, totalPackageByte.length);
        Log.d(tag, "TYPE_TOTALPACKAGE = " + Convert.getInstance().bcdToStr(totalPackageByte));

        byte[] packageSizeByte = Tools.str2Bcd(String.format("%08d", packageSize));
        AddMsgField(msg, TYPE_PACKAGESIZE, packageSizeByte, packageSizeByte.length);
        Log.d(tag, "TYPE_PACKAGESIZE = " + Convert.getInstance().bcdToStr(packageSizeByte));

        iRet = SendCommand2(msg);

        // send package and wait for ack

        //loop send file
        if (0 == iRet) {
            int remaining = fileSize;
            int j = 0;
            int currentPackageSize;
            while (remaining > 0) {

                msg = new byte[9999];
                byte[] dummy;

                SetMsgHeader(msg, CMD_SEND_FILE);

                dummy = Tools.str2Bcd(String.format("%08d", j));
                AddMsgField(msg, TYPE_PACKAGENUM, dummy, dummy.length);

                currentPackageSize = (remaining >= packageSize) ? packageSize : remaining;
                dummy = Tools.str2Bcd(String.format("%08d", currentPackageSize));
                AddMsgField(msg, TYPE_PACKAGESIZE, dummy, dummy.length);

                dummy = new byte[currentPackageSize];
                System.arraycopy(fileBytes, packageSize*j, dummy, 0, currentPackageSize);
                AddMsgField(msg, TYPE_PACKAGEDATA, dummy, currentPackageSize);

                iRet = SendCommand2(msg);
                if (iRet != 0) {
                    //iRet = ERR_INV_ACK;
                    break;
                }
                remaining -= packageSize;
                j++;

                //DispPercent((iFileSize - iRemaining) * 100 / iFileSize);
            }
        }
        return iRet;
    }

    public String ScanQRForPan(int timeout) {// 30
        byte[] msg = new byte[MAX_STRING];
        byte[] qrMsg  = new byte[999];

        SetMsgHeader(msg, SP200_serialAPI.CMD_SCAN_QR);

        byte[] timeoutByte = Tools.str2Bcd(Integer.toString(timeout));
        if (timeout < 100) {
            timeoutByte = Utils.concat(Tools.str2Bcd(Integer.toString(0)), timeoutByte);
        }

        AddMsgField(msg, SP200_serialAPI.TYPE_TIMEOUT, timeoutByte, timeoutByte.length);



        int iRet = SendCommandSP200(timeout, msg, 1000);
        if (iRet != 0)  {
            return null;
        }

        iRet = GetSp200Result(2, recSP200);

        if (iRet == TransResult.SUCC) {
            int len = mesg_data(recSP200, TYPE_QR, qrMsg);
            if (len>0) {
                return Tools.bytes2String(qrMsg).trim();
            }
        }

        return null;
    }

    public interface SP200ReturnListener {
        void onReturnResult(ActionResult result);
    }

    public void ScanQRForPan(int timeout, @NonNull SP200ReturnListener listener) {
        byte[] msg = new byte[MAX_STRING];
        byte[] qrMsg  = new byte[999];

        SetMsgHeader(msg, SP200_serialAPI.CMD_SCAN_QR);

        byte[] timeoutByte = Tools.str2Bcd(Integer.toString(timeout));
        if (timeout < 100) {
            timeoutByte = Utils.concat(Tools.str2Bcd(Integer.toString(0)), timeoutByte);
        }

        AddMsgField(msg, SP200_serialAPI.TYPE_TIMEOUT, timeoutByte, timeoutByte.length);


        int iRet = SendCommandSP200(timeout, msg, 1000);
        if (iRet != 0)  {
            listener.onReturnResult(new ActionResult(TransResult.ERR_SP200_SEND_COMMAND_FAILED, null));
        }

        iRet = GetSp200Result(2, recSP200);

        if (iRet == TransResult.SUCC) {
            int len = mesg_data(recSP200, TYPE_QR, qrMsg);
            if (len>0) {
                listener.onReturnResult(new ActionResult(TransResult.SUCC,Tools.bytes2String(qrMsg).trim()));
            }
        }

        listener.onReturnResult(new ActionResult(TransResult.ERR_SP200_RESULT_FAILED, null));
    }

    public void checkStatusSP200(@NonNull SP200ReturnListener listener){
        isSp200Enable = FinancialApplication.getSysParam().get(SysParam.BooleanParam.EDC_SUPPORT_SP200);
        if(!isSp200Enable) {
            iSp200Mode = 0;
            runInit = false;
            listener.onReturnResult(new ActionResult(TransResult.ERR_SP200_NOT_ENABLE,null));
        }

        byte[] msg = new byte[MAX_STRING];

        SetMsgHeader(msg,CMD_INIT);

        int timeout = SP200TIMEOUT;

        byte[] timeoutByte = Tools.str2Bcd(Integer.toString(timeout));
        if (timeout < 100) {
            timeoutByte = Utils.concat(Tools.str2Bcd(Integer.toString(0)), timeoutByte);
        }

        AddMsgField(msg, TYPE_TIMEOUT, timeoutByte, timeoutByte.length);

        String szDateTime = Utils.getCurentDateTime2();

        AddMsgField(msg, TYPE_DATE, szDateTime.substring(2).getBytes(), 6);
        AddMsgField(msg, TYPE_TIME, szDateTime.substring(8).getBytes(), 6);

        int iRet = SendCommandSP200(10, msg, 1000); // timeout 10 sec
        if (iRet != 0) {
            listener.onReturnResult(new ActionResult(TransResult.ERR_SP200_SEND_COMMAND_FAILED, null));
        }

        iRet = GetSp200Result(2, recSP200);
        if (iRet == TransResult.SUCC) {
            byte[] ucSp200mode = new byte[4];
            mesg_data(recSP200, TYPE_SP200_MODE, ucSp200mode);

            int iSp200mode = Integer.parseInt(Tools.bcd2Str(ucSp200mode));

            if (iSp200mode > 0) {
                this.iSp200Mode = iSp200mode;
            }
        }

        if (iRet == 0){
            runInit = true;
            listener.onReturnResult(new ActionResult(TransResult.SUCC, null));
        } else {
            listener.onReturnResult(new ActionResult(TransResult.ERR_SP200_RESULT_FAILED, null));
        }
    }


    public void cancelQR(@NonNull SP200ReturnListener listener){

        byte[] msg = new byte[MAX_STRING];

        SetMsgHeader(msg,CMD_CANCEL);

        SendCommand(msg);

        int iRet = GetSp200Result(2, recSP200);

        isQrRunning = false;

        listener.onReturnResult(new ActionResult(TransResult.SUCC,null));
    }
}


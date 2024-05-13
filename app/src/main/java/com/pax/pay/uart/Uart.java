package com.pax.pay.uart;

import th.co.bkkps.utils.Log;

import com.pax.dal.IComm;
import com.pax.dal.entity.EUartPort;
import com.pax.dal.entity.UartParam;
import com.pax.dal.exceptions.CommException;
import com.pax.pay.app.FinancialApplication;
import com.pax.pay.utils.Convert;

public class Uart {
    private static Uart uart;

    private IComm uartComm;

    private Uart() {
        String model = android.os.Build.MODEL.toString();
        UartParam uartParam = new UartParam();
        uartParam.setPort(EUartPort.PINPAD);
//        uartParam.setPort(model.equals("A920") ? (EUartPort.USBDEV) : (EUartPort.COM1));
        uartParam.setAttr("115200,8,n,1");
        // uartParam.setAttr("PAXDEV");

        // uartComm = getObject.getIDals().getCommManager().getUartComm(uartParam);
        uartComm = FinancialApplication.getDal().getCommManager().getUartComm(uartParam);
        //uartComm = TestSerialPortActivity.idal.getCommManager().getUartComm(uartParam);
        //logTrue("getUartComm" + uartParam.getPort().toString());
        // comm = IppUser.getInstance().getDal().getCommManager().getUartComm(uartParam);

    };

    public static synchronized Uart getInstance() {
        if (uart == null) {
            uart = new Uart();
        }
        return uart;
    }

    public void connect() {
        try {
            if (uartComm.getConnectStatus() == IComm.EConnectStatus.DISCONNECTED) {
                uartComm.connect();
                //logTrue("Connect");
            } else {
                //logTrue("have connected");
            }
        } catch (CommException e) {
            //logErr("Connect", e.getMessage());
            e.printStackTrace();
        }
    }

    public void send(byte[] data) {
        try {
            connect();
            if (uartComm.getConnectStatus() == IComm.EConnectStatus.CONNECTED) {
                uartComm.send(data);
                if(data != null) {
                    Log.d("BSS_tx", Convert.getInstance().bcdToStr(data));
                }
            } else {
                //logErr("Send", "please connect first");
            }
        } catch (CommException e) {

            e.printStackTrace();
        } finally {
            // disConnect();
        }
    }

    public byte[] recv(int len) {
        try {
            connect();
            if (uartComm.getConnectStatus() == IComm.EConnectStatus.CONNECTED) {
                byte[] result = uartComm.recv(len);
                return result;
            } else {
                return null;
            }
        } catch (CommException e) {
            e.printStackTrace();
            return null;
        }

    }

    public byte[] recvNonBlocking() {
        try {
            connect();
            if (uartComm.getConnectStatus() == IComm.EConnectStatus.CONNECTED) {
                byte[] result = uartComm.recvNonBlocking();
                if(result != null) {
                    Log.d("BSS_rx", Convert.getInstance().bcdToStr(result));
                }
                //logTrue("recvNonBlocking");
                return result;
            } else {
                Log.d("recvNonBlocking", "please connect first");
                return null;
            }
        } catch (CommException e) {
            e.printStackTrace();
            Log.d("recvNonBlocking", e.getMessage());
            return null;
        }
    }

    public void disConnect() {
        try {
            if (uartComm.getConnectStatus() == IComm.EConnectStatus.CONNECTED)
                uartComm.disconnect();
            //logTrue("DisConnect");
            uart = null;
            uartComm = null;
        } catch (CommException e) {
            e.printStackTrace();
            //logErr("DisConnect", e.getMessage());
        }

    }

    public void setConnectTimeout(int timeout) {
        uartComm.setConnectTimeout(timeout);
        //logTrue("setConnectTimeout");
    }

    public void cancelRecv() {
        uartComm.cancelRecv();
        //logTrue("cancelRecv");
        disConnect();
    }

    public void reset() {
        //uartComm.reset();
        //logTrue("reset");
    }

    public void setSendTimeout(int timeout) {
        uartComm.setSendTimeout(timeout);
        //logTrue("setSendTimeout");
    }

    public void setRecvTimeout(int timeout) {
        uartComm.setRecvTimeout(timeout);
        //logTrue("setRecvTimeout");
    }
}

package com.pax.pay.uart;

import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;

import com.pax.dal.IComm;
import com.pax.dal.IDalCommManager;
import com.pax.dal.entity.EUartPort;
import com.pax.dal.entity.UartParam;
import com.pax.dal.exceptions.CommException;
import com.pax.pay.uart.driver.UsbSerialDriver;
import com.pax.pay.uart.driver.UsbSerialPort;
import com.pax.pay.uart.driver.UsbSerialProber;
import com.pax.pay.utils.Convert;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import th.co.bkkps.utils.Log;

public class BaseL920BMCommClass {
    private String mTAG;

    /** 5 data bits. */
    public static final int DATABITS_5 = 5;

    /** 6 data bits. */
    public static final int DATABITS_6 = 6;

    /** 7 data bits. */
    public static final int DATABITS_7 = 7;

    /** 8 data bits. */
    public static final int DATABITS_8 = 8;

    /** No flow control. */
    public static final int FLOWCONTROL_NONE = 0;

    /** RTS/CTS input flow control. */
    public static final int FLOWCONTROL_RTSCTS_IN = 1;

    /** RTS/CTS output flow control. */
    public static final int FLOWCONTROL_RTSCTS_OUT = 2;

    /** XON/XOFF input flow control. */
    public static final int FLOWCONTROL_XONXOFF_IN = 4;

    /** XON/XOFF output flow control. */
    public static final int FLOWCONTROL_XONXOFF_OUT = 8;

    /** No parity. */
    public static final int PARITY_NONE = 0;

    /** Odd parity. */
    public static final int PARITY_ODD = 1;

    /** Even parity. */
    public static final int PARITY_EVEN = 2;

    /** Mark parity. */
    public static final int PARITY_MARK = 3;

    /** Space parity. */
    public static final int PARITY_SPACE = 4;

    /** 1 stop bit. */
    public static final int STOPBITS_1 = 1;

    /** 1.5 stop bits. */
    public static final int STOPBITS_1_5 = 3;

    /** 2 stop bits. */
    public static final int STOPBITS_2 = 2;

    ConnectionInterface mBaseL920BMCommCbk;
    private IComm mBaseComm = null;
    private boolean mFlagConnect = false;
    private UartParam mUartParam = null;
    private IDalCommManager mIDALCommMang = null;

    // USB
    private UsbManager mUsbManager;
    private UsbSerialPort UsbPort;
    private boolean isUsbFlag = false;
    private boolean isUsbOpenFlag= false;
    private int mUsbPortNumber = 0;
    private int mUsbBaudrate = 115200;
    private int mUsbDataBits = UsbSerialPort.DATABITS_8;
    private int mUsbParity = UsbSerialPort.PARITY_NONE;
    private int mUsbStopBits = UsbSerialPort.STOPBITS_1;

    public BaseL920BMCommClass(String Tag, IDalCommManager IDALCommMang) {
        mTAG = "BASE<--->" + Tag;
        mIDALCommMang = IDALCommMang;

        //Set default Parameters
        mUartParam = new UartParam();
        mUartParam.setPort(EUartPort.COM1);
        mUartParam.setAttr("115200,8,n,1");

        mBaseComm = mIDALCommMang.getUartComm(mUartParam);
        isUsbFlag = false;
        if (mBaseComm.getConnectStatus() == IComm.EConnectStatus.CONNECTED) {
            try {
                mBaseComm.disconnect();
            } catch (CommException e) {
                e.printStackTrace();
            }
            mFlagConnect = false;
        }
    }

    public BaseL920BMCommClass(String Tag, UsbManager usbManager) {
        mTAG = "BASE<--->" + Tag;
        mUsbManager = usbManager;

        //Set default Parameters
        mUsbPortNumber = 0;
        mUsbBaudrate = 115200;
        mUsbDataBits = DATABITS_8;
        mUsbStopBits = STOPBITS_1;
        mUsbParity   = PARITY_NONE;

        mFlagConnect = false;
        isUsbFlag = true;
        isUsbOpenFlag = false;
    }

    public void setParameters (int portNumber, int baudrate, int dataBits, int stopBits) {
        if (isUsbFlag) {
            mUsbPortNumber = portNumber;
            mUsbBaudrate = baudrate;
            mUsbDataBits = dataBits;
            mUsbStopBits = stopBits;
            mUsbParity = PARITY_NONE;
        }
        else {
            mUartParam = new UartParam();
            mUartParam.setPort(EUartPort.values()[portNumber]);
            String portParam = String.format("%d,%d,%c,%d",baudrate, dataBits, 'n', stopBits);
            Log.d(mTAG, "setParameters = " + portParam);
            mUartParam.setAttr(portParam);
        }
    }

    public boolean connect(ConnectionInterface onConnectCbk) {
        if (isUsbFlag) {
            UsbPort = usbRefreshDeviceList(mUsbManager, mUsbPortNumber);
            isUsbOpenFlag = openUsbDevice();
            if (!isUsbOpenFlag) {
                Log.d(mTAG, "usb:connect1(): Fail1....T__T");
                UsbPort = null;
            }else {
                Log.d(mTAG, "usb:connect1(): OK1....^^");
                try {
                    UsbPort.purgeHwBuffers(true,true);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            if (isUsbOpenFlag) {
                mBaseL920BMCommCbk = onConnectCbk;
                if (mBaseL920BMCommCbk != null) mBaseL920BMCommCbk.onConnect();
            }

            return isUsbOpenFlag;
        }
        else {
            mBaseComm = mIDALCommMang.getUartComm(mUartParam);

            if ((mBaseComm.getConnectStatus() != IComm.EConnectStatus.CONNECTED) || (mFlagConnect == false)) {
                if (mBaseComm.getConnectStatus() == IComm.EConnectStatus.CONNECTED) {
                    try {
                        mBaseComm.disconnect();
                    } catch (CommException e) {
                        e.printStackTrace();
                    }
                }
                mBaseL920BMCommCbk = onConnectCbk;
                mBaseComm.setConnectTimeout(3000);
                try {
                    mBaseComm.connect();
                } catch (CommException e) {
                    e.printStackTrace();
                }
                if (mBaseComm.getConnectStatus() == IComm.EConnectStatus.CONNECTED) {
                    mFlagConnect = true;
                    if (mBaseL920BMCommCbk != null) mBaseL920BMCommCbk.onConnect();
                    Log.d(mTAG, "connect(): OK....^^");

                    return true;
                } else {
                    mFlagConnect = false;
                    Log.d(mTAG, "connect(): Fail....T__T");
                    return false;
                }
            }
        }
        return true;
    }
    public boolean connect() {
        if (isUsbFlag) {
            if ((UsbPort == null) || (isUsbOpenFlag == false)) {
                UsbPort = usbRefreshDeviceList(mUsbManager, mUsbPortNumber);
                isUsbOpenFlag = openUsbDevice();
                if (!isUsbOpenFlag) {
                    //Log.d(mTAG, "usb:connect2(): Fail1....T__T");
                }else {
                    Log.d(mTAG, "usb:connect2(): OK1....^^");
                    try {
                        UsbPort.purgeHwBuffers(true,true);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            else  {
               // Log.d(mTAG, "usb:connect2(): OK2....^^");
            }

            return isUsbOpenFlag;
        }
        else {
            mBaseComm = mIDALCommMang.getUartComm(mUartParam);

            if ((mBaseComm.getConnectStatus() != IComm.EConnectStatus.CONNECTED) || (mFlagConnect == false)) {
                if (mBaseComm.getConnectStatus() == IComm.EConnectStatus.CONNECTED) {
                    try {
                        mBaseComm.disconnect();
                    } catch (CommException e) {
                        e.printStackTrace();
                    }
                }
                mBaseComm.setConnectTimeout(3000);
                try {
                    mBaseComm.connect();
                } catch (CommException e) {
                    e.printStackTrace();
                }
                if (mBaseComm.getConnectStatus() == IComm.EConnectStatus.CONNECTED) {
                    mFlagConnect = true;
                    Log.d(mTAG, "connect(): OK....^^");
                    return true;
                } else {
                    mFlagConnect = false;
                    Log.d(mTAG, "connect(): Fail....T__T");
                    return false;
                }
            }
        }
        return true;
    }

    public void write_blocked (byte[] data,  int tmout) {

        if (connect()) {
            if (isUsbFlag) {
                if (data != null) {
                    try {
                        UsbPort.write(data,tmout);

                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    Log.d(mTAG, "usb: write_blocked(): " + Convert.getInstance().bcdToStr(data));
//                    BssTransLog.bss_ecr_logging(BssTransLog.pSTATE_SEND_MSG_EDC_TO_POS,
//                                                BssTransLog.pProtocol_USB,
//                                                data);
                }
                else {
                    Log.d(mTAG, "usb: write_blocked(): please connect first");
                }
            } else {
                mBaseComm.setSendTimeout(tmout);
                try {
                    mBaseComm.send(data);
                } catch (CommException e) {
                    e.printStackTrace();
                }
                if (data != null) {
                    Log.d(mTAG, "write_blocked(): " + Convert.getInstance().bcdToStr(data));
                }
            }
        } else{
            Log.d(mTAG, "write_blocked(): please connect first");
        }
    }

    public byte[] read_blocked(int len, int tmout) {
        if (isUsbFlag) {
            if (connect()) {
                byte[] result = new byte[len];
                int rd_len = 0;

                try {
                    rd_len = UsbPort.read(result, tmout);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                if (rd_len > 0) {
                    Log.d(mTAG, "read_blocked(): " + Convert.getInstance().bcdToStr( Arrays.copyOf(result, rd_len)));
//                    BssTransLog.bss_ecr_logging(BssTransLog.pSTATE_RECV_MSG_POS_TO_EDC,
//                                                BssTransLog.pProtocol_USB,
//                                                Arrays.copyOf(result, rd_len));
                    return Arrays.copyOf(result, rd_len);
                }
                else {
                  //  Log.d(mTAG, "read_blocked(): null");
                    return null;
                }
            }
        }
        else {
            if (connect()) {
                mBaseComm.setRecvTimeout(tmout);
                byte[] result = null;
                try {
                    result = mBaseComm.recv(len);
                } catch (CommException e) {
                    e.printStackTrace();
                }
                if ((result != null) && (result.length > 0)) {
                    Log.d(mTAG, "read_blocked(): " + Convert.getInstance().bcdToStr(result));
                }
                return result;
            } else {
                Log.d(mTAG, "read_blocked(): please connect first");
                return null;
            }
        }
        return null;
    }

    public byte[] read_non_blocking() {
        if (isUsbFlag) {
            if (connect()) {
                byte[] result = new byte[1024*10];
                int rd_len = 0;

                try {
                    rd_len = UsbPort.read(result, 1);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                if (rd_len > 0) {
                    Log.d(mTAG, "usb:read_non_blocking(): " + Convert.getInstance().bcdToStr(Arrays.copyOf(result, rd_len)));
                    return Arrays.copyOf(result, rd_len);
                }
                else {
                  //  Log.d(mTAG, "usb:read_non_blocking(): null");
                    return null;
                }
            }
            Log.d(mTAG, "usb:read_non_blocking(): please connect first");
        }
        else {
            if (connect()) {
                byte[] result = null;
                try {
                    result = mBaseComm.recvNonBlocking();
                } catch (CommException e) {
                    e.printStackTrace();
                }

                if ((result != null) && (result.length > 0)) {
                    Log.d(mTAG, "read_non_blocking(): " + Convert.getInstance().bcdToStr(result));
                }
                return result;
            } else {
                Log.d(mTAG, "read_non_blocking(): please connect first");
            }
        }
        return null;
    }

    public boolean disconnect() {
        if (isUsbFlag) {
            if (isUsbOpenFlag) {
                if (mBaseL920BMCommCbk != null) mBaseL920BMCommCbk.onDisconnect();
            }
            return closeUsbDevice();
        }
        else {
            try {
                if (mFlagConnect == true) {
                    if (mBaseL920BMCommCbk != null) mBaseL920BMCommCbk.onDisconnect();
                    mBaseComm.cancelRecv();
                    mBaseComm.disconnect();
                    mFlagConnect = false;
                    Log.d(mTAG, "disconnect(): OK....^^");
                }
                mBaseComm = null;
                return  true;
            } catch (CommException e) {
                e.printStackTrace();
                Log.d(mTAG, "disconnect(): ERROR ....T__T");
                return  false;
            }
        }
    }

    public UsbSerialPort usbRefreshDeviceList(UsbManager usbManager, int usbPortNumber) {
       // Log.d(mTAG, "Refreshing device list ...");
        final List<UsbSerialDriver> drivers = UsbSerialProber.getDefaultProber().findAllDrivers(usbManager);
        final List<UsbSerialPort> result = new ArrayList<UsbSerialPort>();

        for (final UsbSerialDriver driver : drivers) {
            final List<UsbSerialPort> ports = driver.getPorts();
            Log.d(mTAG, String.format("+ %s: %s port%s", driver, Integer.valueOf(ports.size()), ports.size() == 1 ? "" : "s"));
            result.addAll(ports);
        }

        if(result.size() > 0){
            Log.d(mTAG,"Get FTDIPort info");
            if (usbPortNumber < result.size()) {
                Log.d(mTAG,"Port Number = " + usbPortNumber);
                return result.get(usbPortNumber);
            }
            else {
                Log.d(mTAG,"Can't find port number");
                return null;
            }

        }else {
            //Log.d(mTAG,"Can't find any device!");
        }
        return null;
    }

    public boolean openUsbDevice(){

        if (UsbPort == null) {
            //Log.e(mTAG, "No USB serial device.");
            return false;
        } else {

            UsbDeviceConnection connection = mUsbManager.openDevice(UsbPort.getDriver().getDevice());

            if (connection == null) {
                Log.e(mTAG, "Opening USB device failed");
                UsbPort = null;
                return false;
            }

            try {
                UsbPort.open(connection);
                UsbPort.setParameters(mUsbBaudrate, mUsbDataBits, mUsbStopBits, mUsbParity);

                Log.d(mTAG, "Open USB port successfully....^^");
            } catch (IOException e) {
                Log.e(mTAG, "Error setting up USB device: " + e.getMessage(), e);
                try {
                    UsbPort.close();
                } catch (IOException e2) {
                    // Ignore.
                }
                UsbPort = null;

                return false;
            }
            Log.d(mTAG, "USB Serial device: " + UsbPort.getClass().getSimpleName());

            return true;
        }
    }

    public boolean closeUsbDevice() {

        if (UsbPort != null) {
            try {
                UsbPort.close();
            } catch (IOException e) {
                // Ignore.
            }
            UsbPort = null;
            Log.d(mTAG, "USB Disconnect1");
            return true;
        }
        else {
           // Log.d(mTAG, "USB Disconnect2");
        }
        isUsbOpenFlag = false;
        return false;
    }

}

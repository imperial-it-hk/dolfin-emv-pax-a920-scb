package com.pax.pay.uart;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.widget.Toast;

import com.pax.pay.uart.driver.UsbSerialDriver;
import com.pax.pay.uart.driver.UsbSerialPort;
import com.pax.pay.uart.driver.UsbSerialProber;

import java.util.ArrayList;
import java.util.List;

import th.co.bkkps.utils.Log;

public class UsbSerialManagerClass extends BroadcastReceiver {
    private static final String TAG = "UsbSerialManager:";
    private static String ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION";
    private UsbSerialPort mUsbPort;
    Context mAppContext;
    Context mActContext;
    private UsbManager mUsbManager;
    public List<UsbSerialPort> mUsbPorts;
    private boolean isConnectFlag = false;
    private ArrayList<ConnectionInterface> connListener = new ArrayList<>();
    public boolean permission = false;

    public UsbSerialManagerClass(Context appContext) {
        mAppContext = appContext;

        mUsbManager = (UsbManager) mAppContext.getSystemService(Context.USB_SERVICE);

        mAppContext.registerReceiver(this, new IntentFilter(UsbManager.ACTION_USB_DEVICE_ATTACHED));
        mAppContext.registerReceiver(this, new IntentFilter(UsbManager.ACTION_USB_DEVICE_DETACHED));
        mAppContext.registerReceiver(this, new IntentFilter(ACTION_USB_PERMISSION));

    }

    public void addConnListener(ConnectionInterface listener) {
        if (listener != null) {
            connListener.add(listener);
        }
    }

    public void requestPermission() {
        mUsbPort = usbRefreshDeviceList(mUsbManager);

        if (mUsbPort != null) {
            if (mUsbManager.hasPermission(mUsbPort.getDriver().getDevice())) {
                Log.d(TAG, "FN(0):DeviceName:" + mUsbPort.getDriver().getDevice().getDeviceName() + ":" + mUsbPort.getDriver().getDevice().getProductName() + " has permission.");
                isConnectFlag = true;
                permission = true;
                if (!connListener.isEmpty()) {
                    for (ConnectionInterface obj : connListener) {
                        try {
                            obj.onConnect();
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    }
                }
            } else {
                Log.d(TAG, "FN(0):RequestPermission DeviceName:" + mUsbPort.getDriver().getDevice().getDeviceName() + ":" + mUsbPort.getDriver().getDevice().getProductName());
                PendingIntent mPermissionIntent = PendingIntent.getBroadcast(mAppContext, 0, new Intent(ACTION_USB_PERMISSION), 0);
                mUsbManager.requestPermission(mUsbPort.getDriver().getDevice(), mPermissionIntent);
            }
        } else {
            Log.d(TAG, "FN(0):No USB serial device. (T__T)");
            //Toast.makeText(mAppContext, "No USB serial device. (T__T)", Toast.LENGTH_SHORT).show();
        }
    }

    public boolean isConnect() {
        return isConnectFlag;
    }


    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (ACTION_USB_PERMISSION.equals(action)) {
            synchronized (mAppContext) {
                Log.d(TAG, "ACTION_USB_PERMISSION");
                UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                    // Log.d(TAG, "EXTRA_PERMISSION_GRANTED ===> " + device);
                    Log.d(TAG, "EXTRA_PERMISSION_GRANTED ===> DeviceName = " + device.getDeviceName() + " : " + device.getProductName());
                    isConnectFlag = true;
                    permission = true;
                    if (connListener != null) {
                        for (ConnectionInterface obj : connListener) {
                            obj.onConnect();
                        }
                    }
                } else {
                    Log.d(TAG, "Permission denied for device ===> " + device);
                }
            }
        } else if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)) {
            synchronized (mAppContext) {
                Log.d(TAG, "ACTION_USB_DEVICE_ATTACHED");

                mUsbPort = usbRefreshDeviceList(mUsbManager);
                if (mUsbPort != null) {
                    if (mUsbManager.hasPermission(mUsbPort.getDriver().getDevice())) {
                        Log.d(TAG, "FN(1):DeviceName : " + mUsbPort.getDriver().getDevice().getDeviceName() + ":" + mUsbPort.getDriver().getDevice().getProductName() + " has permission.");
                        isConnectFlag = true;
                        if (!connListener.isEmpty()) {
                            for (ConnectionInterface obj : connListener) {
                                obj.onConnect();
                            }
                        }
                    } else {
                        Log.d(TAG, "FN(1):RequestPermission DeviceName = " + mUsbPort.getDriver().getDevice().getDeviceName() + " : " + mUsbPort.getDriver().getDevice().getProductName());
                        PendingIntent mPermissionIntent = PendingIntent.getBroadcast(mAppContext, 0, new Intent(ACTION_USB_PERMISSION), 0);
                        mUsbManager.requestPermission(mUsbPort.getDriver().getDevice(), mPermissionIntent);
                    }
                }
            }
        } else if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
            synchronized (mAppContext) {
                Log.d(TAG, "ACTION_USB_DEVICE_DETACHED");
                isConnectFlag = false;
                permission = false;
                if (!connListener.isEmpty()) {
                    for (ConnectionInterface obj : connListener) {
                        obj.onDisconnect();
                    }
                }
            }
        } else {
            Log.d(TAG, "......");
        }
    }

    public UsbSerialPort usbRefreshDeviceList(UsbManager usbManager) {
        Log.d(TAG, "Refreshing device list ...");
        final List<UsbSerialDriver> drivers = UsbSerialProber.getDefaultProber().findAllDrivers(usbManager);
        final List<UsbSerialPort> result = new ArrayList<UsbSerialPort>();

        for (final UsbSerialDriver driver : drivers) {
            final List<UsbSerialPort> ports = driver.getPorts();
            //Log.d(TAG, "DeviceName = " + driver.getDevice());
            //Log.d(TAG, "DeviceName = " + driver.getDevice().getDeviceName()+":"+ driver.getDevice().getProductName()+":Port=" + ports.size());
            //Log.d(TAG, String.format("+ %s: %s port%s", driver, Integer.valueOf(ports.size()), ports.size() == 1 ? "" : "s"));
            result.addAll(ports);
        }

        if (result.size() > 0) {
            return result.get(0);
        } else {
            Log.d(TAG, "Can't find any device!");
        }
        return null;
    }

    public boolean isPermission() {
        mUsbPort = usbRefreshDeviceList(mUsbManager);
        return mUsbPort != null && mUsbManager.hasPermission(mUsbPort.getDriver().getDevice());
    }
}

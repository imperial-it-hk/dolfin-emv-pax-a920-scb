package com.pax.pay.uart;

import android.os.CountDownTimer;

import th.co.bkkps.utils.Log;

import com.pax.pay.ECR.HyperCommClass;
import com.pax.pay.utils.Convert;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Arrays;

public class CommManageClass extends Thread {
    private volatile int runFlag = 0;
    private volatile int exitFlag = 0;
    //private ArrayList<CommManageInterface> mReceiveCbk = new ArrayList<>();
    private CommManageInterface mReceiveCbk;
    public CommConnInterface MainIO;
    private CountDownTimer rcvTimeout = null;

    private boolean isRcvTimeoutON = false;
    private boolean isFinish = false;
    ByteArrayOutputStream pduBuffer;

    public CommManageClass() {

        pduBuffer = new ByteArrayOutputStream();

        rcvTimeout = new CountDownTimer(300, 10) {

            public void onTick(long millisUntilFinished) {
                if (!isRcvTimeoutON) isRcvTimeoutON = true;
                //  Log.d("CommManageClass:", "RX TIMEOUT IS ON.....");
            }

            public void onFinish() {
                isRcvTimeoutON = false;
                isFinish = true;
                Log.d("CommManageClass:", "RX TIMEOUT OCCUR.....");
            }
        };
        rcvTimeout.cancel();
    }

    public void AddConnListener(CommConnInterface mainIO) {
        synchronized (this) {
            this.MainIO = mainIO;
        }
    }

    public void RemoveConnListener() {
        synchronized (this) {
            MainIO = null;
        }
    }

    public void AddReceiveListener(CommManageInterface RcvCbk) {
        synchronized (this) {
            //mReceiveCbk.add(ConnCbk);
            mReceiveCbk = RcvCbk;
        }
    }

    public void run() {
        boolean isOpen = true;
        try {
            while (exitFlag == 0) {
                //    Log.d("CommManageClass:", "PAUSE_LOOP");

                Thread.sleep(10);

                while (runFlag > 0) {
                    Thread.sleep(10);
                    //    Log.d("CommManageClass:", "RUN_LOOP");
                    synchronized (this) {
                        if (MainIO != null) {

                            byte[] msgReadBuffer = MainIO.Read();

                            if ((msgReadBuffer != null) && (msgReadBuffer.length > 0)) {
                                //                                for (CommManageInterface obj : mReceiveCbk) {
                                //                                    obj.onReceive(Arrays.copyOf(msgReadBuffer, msgReadBuffer.length), msgReadBuffer.length);
                                //                                }

                                pduBuffer.write(msgReadBuffer, 0, msgReadBuffer.length);
                                rcvTimeout.start();
                            }
                            if (isFinish) {
                                Log.d("ProtoFilterClass:", "buf = " + Convert.getInstance().bcdToStr(pduBuffer.toByteArray()));
                                mReceiveCbk.onReceive(pduBuffer.toByteArray(), pduBuffer.toByteArray().length);
                                pduBuffer.reset();
                                isFinish = false;
                            }
                        }
                    }
                }


//                if (MainIO != null) {
//                    MainIO.Disconnect();
//                }
//                else {
//                    Log.d("CommManageClass:", "MainIO = Null");
//                }
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        runFlag = 0;

    }

    public void StopReceive() {
        runFlag = 0;
        Log.d("CommManageClass:", "StopReceive()");
    }

    public void StartReceive() {
        //ClearBuffer();
        runFlag = 1;
        exitFlag = 0;
        Log.d("CommManageClass:", "StartReceive()");
    }

    public void StartThread() {
        StartReceive();
        if (!this.isAlive()) {
            this.start();
            Log.d("CommManageClass:", "StartThread()");
        }

    }

    public void StopThread() {
        exitFlag = 1;
        runFlag = 0;
        try {
            this.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        Log.d("CommManageClass:", "StopThread()");
    }

    public void ClearBuffer() {
        synchronized (this) {
            if (MainIO != null) {
                while (true) {
                    byte[] msgReadBuffer = MainIO.Read();
                    if (msgReadBuffer == null) break;
                }
            }
        }
    }


    public boolean isStarted() {
        return runFlag > 0;
    }


}

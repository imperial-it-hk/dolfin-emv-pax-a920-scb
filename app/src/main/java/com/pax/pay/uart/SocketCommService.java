package com.pax.pay.uart;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import th.co.bkkps.utils.Log;
import android.widget.Toast;

import com.pax.device.DeviceImplNeptune;
import com.pax.eemv.utils.Tools;

import com.pax.pay.app.FinancialApplication;
import com.pax.pay.utils.Convert;
import com.pax.pay.utils.Utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;


public class SocketCommService {

    private static final String TAG = SocketCommService.class.getName();
    private Context mContext;

    private ConnectedThread mConnectedThread;
    private AcceptThread mAcceptThread;
    private byte[] receivedData;
    private Socket mSocket;

    static final int socketServerPORT = 4001;

    private int mState;

    public SocketCommService(Context context) {
        this.mState = STATE_NONE;
        this.mContext = context;
    }

    // Constants that indicate the current connection state
    private static final int STATE_NONE = 0;       // we're doing nothing
    private static final int STATE_LISTEN = 1;     // now listening for incoming connections
    private static final int STATE_CONNECTING = 2; // now initiating an outgoing connection
    private static final int STATE_CONNECTED = 3;  // now connected to a remote device


    // Defines several constants used when transmitting messages between the
    // service and the UI.
    private interface MessageConstants {
        int MESSAGE_READ = 0;
        int MESSAGE_WRITE = 1;
        int CONNECTING=2;
        int CONNECTED=3;
        int NO_SOCKET_FOUND=4;
        int MESSAGE_TOAST = 5;
        int MESSAGE_SENT_RECEIVED = 6;
    }

    @SuppressLint("HandlerLeak")
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);

            switch (msg.what){
                case SocketCommService.MessageConstants.MESSAGE_READ:
                    receivedData = java.util.Arrays.copyOf((byte[])msg.obj,  msg.arg1);
                    //byte[] readbuf=(byte[])msg.obj;
                    String string_recieved = new String(receivedData);
                    //receivedData = Tools.str2Bcd(string_recieved);
                    Log.d(TAG, String.format("buff = 0x%x",receivedData[0]));

                    Toast.makeText(mContext,"Receive: "+ string_recieved,Toast.LENGTH_SHORT).show();
//                    stopConnectedThread();
                    break;

                case SocketCommService.MessageConstants.MESSAGE_WRITE:
                    if(msg.obj!=null){
                        SocketCommService.ConnectedThread connectedThread= new SocketCommService.ConnectedThread((Socket)msg.obj);
                        connectedThread.write("Hello World!".getBytes());
                    }
                    break;

                case SocketCommService.MessageConstants.CONNECTED:
                    Toast.makeText(mContext,"Connected",Toast.LENGTH_SHORT).show();
                    break;

                case SocketCommService.MessageConstants.CONNECTING:
                    Toast.makeText(mContext,"Connecting...",Toast.LENGTH_SHORT).show();
                    break;

                case SocketCommService.MessageConstants.NO_SOCKET_FOUND:
                    Toast.makeText(mContext,"No socket found",Toast.LENGTH_SHORT).show();
                    break;
                case SocketCommService.MessageConstants.MESSAGE_TOAST:
                    Toast.makeText(mContext, msg.getData().getString("toast"), Toast.LENGTH_LONG).show();
                    break;
            }
        }
    };

    public synchronized void start() {
        // Cancel any thread currently running a connection
        stop();

        DeviceImplNeptune.getInstance().delayMs((short)500);

        Log.e(TAG, "start AcceptThread");
        mAcceptThread = new AcceptThread();
        mAcceptThread.start();

        mState = STATE_LISTEN;
    }

    /**
     * Stop all threads
     */
    public synchronized void stop() {
        Log.e(TAG, "stop.");
        if (mAcceptThread != null) {
            Log.e(TAG, "Accept. "+mAcceptThread.getState());
            mAcceptThread.cancel();
            mAcceptThread = null;
        }
        if (mConnectedThread != null) {
            Log.e(TAG, "Connected. "+mConnectedThread.getState());
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        mState = STATE_NONE;
    }

    private synchronized  void connected(Socket mmSocket) {
        Log.d(TAG, "connected: Starting.");

        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        // Start the thread to manage the connection and perform transmissions
        mConnectedThread = new SocketCommService.ConnectedThread(mmSocket);
    }


    public byte[] read(){
        receivedData = null;

        if(mState == STATE_NONE){
            Log.e(TAG, "read start");
            start();
            return new byte[]{-1};
        }

        if(mConnectedThread == null) {
            //Log.e(TAG, "mConnectedThread = null");
            return new byte[]{-1};
        }

        if( mConnectedThread.getState() == Thread.State.NEW) {
            Log.i(TAG, "mConnectedThread is not started yet. Call start.");
            mConnectedThread.start();
        } else {
            Log.i(TAG, "mConnectedThread is already started.");
        }

        mState = STATE_CONNECTED;
        try {
            mConnectedThread.join(1000);
            mConnectedThread.interrupt();
            //mConnectedThread = null;
        } catch (Exception ex) {
            ex.printStackTrace();
            return new byte[]{-1};
        }

        Utils.Delay100Ms(20);

        if (receivedData != null) {
            String string_recieved = new String(receivedData);
            Log.e(TAG, string_recieved);

            //ByteArrayOutputStream bos = new ByteArrayOutputStream();

            //for (int i = 0; i < string_recieved.length(); i++)
            //{
            //    bos.write(Byte.parseByte(string_recieved.substring(1)));
            //}

            //Log.e(TAG, string_recieved);

            //receivedData = Tools.hexStringToByteArray(string_recieved);
        } else {
            return new byte[]{-1};
        }

        return receivedData;
    }

    public boolean write(byte[] bytes) {
        Log.e(TAG, "write "+ bytes.length + "byte");
//        if(mConnectedThread == null) return false;
//
//        Log.e(TAG, "write()..start mConnectedThread");

        //connected(mSocket);
        //Utils.Delay100Ms(20);
        if (mConnectedThread != null) {
            mConnectedThread.write(bytes);
            return true;
        }
        return false;
    }

    public void clearBuffer() {
        receivedData = null;
    }

    public void startAcceptingConnection() {
        if (mAcceptThread == null) {
            mAcceptThread = new AcceptThread();
        }
        if (!mAcceptThread.isAlive()) {
            Log.e(TAG, "startAcceptingConnection");
            Log.e(TAG, "getState: " + mAcceptThread.getState());
            if (mAcceptThread.getState() != Thread.State.NEW && mAcceptThread.getState() != Thread.State.TERMINATED) {
                mAcceptThread.start();
            } else {
                mAcceptThread.cancel();
                mAcceptThread = null;

                mAcceptThread = new AcceptThread();
                mAcceptThread.start();
            }
        }
    }

    public void write_block(byte[] data) {
        Log.i(TAG, "Start write....");
        mConnectedThread.write(data);
    }

    public byte[] read_block() {
        return receivedData;
    }

    public void stopConnectedThread() {
        if (mAcceptThread != null) {
            mAcceptThread.cancel();
            mAcceptThread = null;

            mAcceptThread = new AcceptThread();
            mAcceptThread.start();
        }
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }
    }

    private class AcceptThread extends Thread {
        private final ServerSocket mmServerSocket;

        public AcceptThread() {
            ServerSocket tmp = null;
            try {
                // create ServerSocket using specified port
                tmp = new ServerSocket(socketServerPORT);
            } catch (IOException e) {
                Log.e(TAG, "Socket's listen() method failed", e);
            }

            mmServerSocket = tmp;
        }

        @Override
        public void run() {
            mSocket = null;
            try {
                while (true) {
                    // block the call until connection is created and return
                    // Socket object
                    mSocket = mmServerSocket.accept();

                    if (mSocket != null) {
                        Log.i(TAG, "Start accepting.... socket is not null start call connected.");
                        // A connection was accepted. Perform work associated with
                        // the connection in a separate thread.
                        connected(mSocket);
                        FinancialApplication.getEcrProcess().mCommManage.StartReceive();
                        Utils.Delay100Ms(50);

                        break;
                    }
                }
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        // Closes the connect socket and causes the thread to finish.
        public void cancel() {
            try {
                mmServerSocket.close();
                mState = STATE_NONE;
                Log.e(TAG, "ServerSocket close");
            } catch (IOException e) {
                Log.e(TAG, "Could not close the connect socket", e);
            }
        }
    }

    private class ConnectedThread extends Thread {
        private final Socket  mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;
        private byte[] mmBuffer; // mmBuffer store for the stream

        public ConnectedThread(Socket  socket) {
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the input and output streams; using temp objects because
            // member streams are final.
            try {
                tmpIn = socket.getInputStream();
            } catch (IOException e) {
                Log.e(TAG, "Error occurred when creating input stream", e);
            }
            try {
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Log.e(TAG, "Error occurred when creating output stream", e);
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
            mHandler.obtainMessage(MessageConstants.CONNECTED).sendToTarget();
            receivedData = null;
        }

        public void run() {
            mmBuffer = new byte[1024];
            int numBytes; // bytes returned from read()
            mState = STATE_CONNECTED;

            // Keep listening to the InputStream until an exception occurs.
            while (!Thread.interrupted()){
                try {
                    Log.i(TAG, "InputStream...waiting for data coming");
                    // Read from the InputStream.
                    numBytes = mmInStream.read(mmBuffer);
                    // Send the obtained bytes to the UI activity.
                    Message readMsg = mHandler.obtainMessage(
                            MessageConstants.MESSAGE_READ, numBytes, -1,
                            mmBuffer);
                    if (numBytes == -1) { // -1 mead socket already disconnect
                        Log.d(TAG, "Input stream was disconnected");
                        cancel();
                        break;
                    } else {
                        Log.d(TAG, "Input stream was disconnected");
                        readMsg.sendToTarget();
                    }
                } catch (IOException e) {
                    Log.d(TAG, "Input stream was disconnected", e);
                    cancel();
                    break;
                }
            }
        }

        // Call this from the main activity to send data to the remote device.
        public void write(byte[] bytes) {
            try {
                Log.i(TAG, "In OutStream....");
                mmOutStream.write(bytes);
            } catch (IOException e) {
                Log.e(TAG, "Error occurred when sending data", e);

                // Send a failure message back to the activity.
                Message writeErrorMsg =
                        mHandler.obtainMessage(MessageConstants.MESSAGE_TOAST);
                Bundle bundle = new Bundle();
                bundle.putString("toast", "Couldn't send data to the other device");
                writeErrorMsg.setData(bundle);
                mHandler.sendMessage(writeErrorMsg);
                cancel();
            }
        }

        // Call this method from the main activity to shut down the connection.
        public void cancel() {
            try {
                if (mmSocket.isConnected()) {
                    mmSocket.close();
                }
                mState = STATE_NONE;
                Log.e(TAG, "socket disconnect");
            } catch (IOException e) {
                Log.e(TAG, "Could not close the connect socket", e);
            }
        }
    }
}

package com.pax.pay.tcpsocket;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import th.co.bkkps.utils.Log;
import android.view.Gravity;
import android.widget.TextView;

import com.pax.device.Device;
import com.pax.pay.MainActivity;
import com.pax.pay.app.ActivityStack;
import com.pax.pay.app.FinancialApplication;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Enumeration;

/**
 * Created by SORAYA S on 26-Feb-18.
 */

public class SocketServer {

    private static final String TAG = "SocketServer";

    MainActivity activity;
    ServerSocket serverSocket;
    String message = "";
    static final int socketServerPORT = 8080;

    /*private ATransaction.TransEndListener listener = new ATransaction.TransEndListener() {

        @Override
        public void onEnd(ActionResult result) {
            FinancialApplication.getApp().runOnUiThread(new Runnable() {

                @Override
                public void run() {
//                    resetUI();
                }
            });

        }
    };*/

    public SocketServer(MainActivity activity) {
        this.activity = activity;
        Thread socketServerThread = new Thread(new SocketServerThread());
        socketServerThread.start();
    }

    public int getPort() {
        return socketServerPORT;
    }

    public void onDestroy() {
        if (serverSocket != null) {
            try {
                serverSocket.close();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                Log.e(TAG, "", e);
            }
        }
    }

    private class SocketServerThread extends Thread {

        int count = 0;

        @Override
        public void run() {
            try {
                serverSocket = new ServerSocket(socketServerPORT);

                while (true) {
                    Socket socket = serverSocket.accept();
                    count++;
                    message += "#" + count + " from "
                            + socket.getInetAddress() + ":"
                            + socket.getPort();

                    SocketServerReplyThread socketServerReplyThread = new SocketServerReplyThread(
                            socket, count);
                    socketServerReplyThread.run();

                }
            } catch (IOException e) {
                // TODO Auto-generated catch block
                Log.e(TAG, "", e);
            }
        }

    }

    private class SocketServerReplyThread extends Thread {

        private Socket hostThreadSocket;
        int cnt;

        SocketServerReplyThread(Socket socket, int c) {
            hostThreadSocket = socket;
            cnt = c;
        }

        @Override
        public void run() {
            OutputStream outputStream;
            String msgReply = "Hello from Server, you are #" + cnt;

            try {

                outputStream = hostThreadSocket.getOutputStream();
                PrintStream printStream = new PrintStream(outputStream);
                printStream.print(msgReply);

                BufferedReader in = new BufferedReader(new InputStreamReader(hostThreadSocket.getInputStream()));
                //receive a message
                String incomingMsg;
                while((incomingMsg=in.readLine())!=null)
                {
                    message +=  "\n" + incomingMsg;
                }

                FinancialApplication.getApp().runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        Device.beepOk();
                        TextView title = new TextView(getCurrentContext());
                        title.setText("Received Message");
                        title.setPadding(10, 10, 10, 10);
                        title.setGravity(Gravity.LEFT);
                        title.setTextColor(Color.BLACK);
                        title.setTextSize(20);

                        AlertDialog.Builder builder = new AlertDialog.Builder(getCurrentContext());
                        builder.setCustomTitle(title);
                        builder.setMessage(message);
                        builder.setCancelable(false);
                        builder.setNegativeButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.cancel();
                            }
                        });

                        AlertDialog alert = builder.create();
                        alert.show();
                    }
                });
//                new SaleTrans(activity, "70", (byte) -1, true, listener).execute();

                message += "\n";

                printStream.close();
                in.close();

            } catch (IOException e) {
                // TODO Auto-generated catch block
                message += "Something wrong! " + e.toString() + "\n";
                Log.e(TAG, message, e);
            }
        }

    }

    public String getIpAddress() {
        String ip = "";
        try {
            Enumeration<NetworkInterface> enumNetworkInterfaces = NetworkInterface
                    .getNetworkInterfaces();
            while (enumNetworkInterfaces.hasMoreElements()) {
                NetworkInterface networkInterface = enumNetworkInterfaces
                        .nextElement();
                Enumeration<InetAddress> enumInetAddress = networkInterface
                        .getInetAddresses();
                while (enumInetAddress.hasMoreElements()) {
                    InetAddress inetAddress = enumInetAddress
                            .nextElement();

                    if (inetAddress.isSiteLocalAddress()) {
                        ip += "Server running at : "
                                + inetAddress.getHostAddress();
                    }
                }
            }

        } catch (SocketException e) {
            // TODO Auto-generated catch block
            ip += "Something Wrong! " + e.toString() + "\n";
            Log.e(TAG, ip, e);
        }
        return ip;
    }

    private Context getCurrentContext() {
        return ActivityStack.getInstance().top();
    }
}

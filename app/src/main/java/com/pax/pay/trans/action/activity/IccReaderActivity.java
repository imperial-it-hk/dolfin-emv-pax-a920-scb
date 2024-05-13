package com.pax.pay.trans.action.activity;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.text.method.ScrollingMovementMethod;

import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.pax.abl.core.ActionResult;
import com.pax.abl.utils.PanUtils;
import com.pax.abl.utils.TrackUtils;
import com.pax.dal.ICardReaderHelper;
import com.pax.dal.entity.EReaderType;
import com.pax.dal.entity.PollingResult;
import com.pax.dal.exceptions.IccDevException;
import com.pax.dal.exceptions.MagDevException;
import com.pax.dal.exceptions.PiccDevException;
import com.pax.device.Device;
import com.pax.edc.opensdk.TransResult;
import com.pax.pay.BaseActivityWithTickForAction;

import com.pax.edc.R;
import com.pax.pay.app.FinancialApplication;
import com.pax.pay.service.IccTester;
import com.pax.pay.trans.action.ActionSearchCard;
import com.pax.pay.utils.Convert;
import com.pax.pay.utils.IApdu;
import com.pax.pay.utils.IApdu.IApduReq;
import com.pax.pay.utils.IApdu.IApduResp;
import com.pax.pay.utils.Packer;
import com.pax.pay.utils.TickTimer;
import com.pax.pay.utils.ToastUtils;
import com.pax.pay.utils.Utils;
import com.pax.view.ClssLight;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;

/**
 * Created by JAICHANOK N on 2/7/2018.
 */

public class IccReaderActivity  extends BaseActivityWithTickForAction {
    public static boolean b = false;
    private Button btnRead;
    private String cardID;
    private String cardName;
    private ImageView imageView;
    private TextView textView;
    private PollingResult pollingResult;
    private SearchCardThread searchCardThread = null;
    Context context;
    private Bitmap bmp;
    String cardResult = "";

    @Override
    protected int getLayoutId() {return R.layout.activity_icc_layout;}

    @Override
    protected void initViews() {
        imageView = (ImageView) findViewById(R.id.card_img);
        textView = (TextView) findViewById(R.id.card_result);
        btnRead = (Button) findViewById(R.id.readCard_btn);
        context = this;
    }

    @Override
    protected void setListeners() {
        btnRead.setOnClickListener(this);
    }

    @Override
    protected void loadParam() {
        //get data
        Intent intent = getIntent();

    }

    @Override
    protected void onClickProtected(View v) {
        Log.i("On Click", v.toString());
        switch (v.getId()) {
            case R.id.readCard_btn:
                tickTimer.start();
                textView.setMovementMethod(new ScrollingMovementMethod());
                textView.setText("\n" + "Please insert card.");
                imageView.setImageDrawable(null);
                /*runSearchCardThread();*/
                if (searchCardThread != null && searchCardThread.getState() == Thread.State.TERMINATED) {
                    FinancialApplication.getDal().getCardReaderHelper().stopPolling();
                    searchCardThread.interrupt();
                }
                searchCardThread = new IccReaderActivity.SearchCardThread();
                searchCardThread.start();
                //finish(new ActionResult(TransResult.SUCC, null));
                //break;

        }
    }
    private void runSearchCardThread() {
        if (searchCardThread != null && searchCardThread.getState() == Thread.State.TERMINATED) {
            FinancialApplication.getDal().getCardReaderHelper().stopPolling();
            searchCardThread.interrupt();
        }
        searchCardThread = new IccReaderActivity.SearchCardThread();
        searchCardThread.start();
    }

    private class SearchCardThread extends Thread {

        @Override
        public void run() {
            IccTester.getInstance().light(true);
            while (!Thread.interrupted()) {
                b = IccTester.getInstance().detect((byte) 0);
                if (b) {
                    Message message = Message.obtain();
                    message.what = 1;
                    handler.sendMessage(message);
                    //cardResult = context.getString(R.string.wait_load) ;
                    byte[] res = IccTester.getInstance().init((byte) 0);
                    if (res == null) {
                        Log.i("Test", "init ic card,but no response");
                        return;
                    }
                    //cardResult += ("\ninit response：" + Convert.getInstance().bcdToStr(res));
                    IccTester.getInstance().autoResp((byte) 0, true);

                    IApdu apdu = Packer.getInstance().getApdu();

                    byte[] thai_id_card = new byte[]{(byte) 0xA0, 0x00, 0x00, 0x00, 0x54, 0x48, 0x00, 0x01};
                    // step1

                    IApduReq apduReq = apdu.createReq((byte) 0x00, (byte) 0xa4, (byte) 0x04, (byte) 0x00, thai_id_card, (byte) 0);
                    apduReq.setLcAlwaysPresent();
                    //apduReq.setLc((byte)8);
                    byte[] req = apduReq.pack();

                    byte[] isoRes = IccTester.getInstance().isoCommand((byte) 0, req);

                    if (isoRes != null) {
                        IApduResp apduResp = apdu.unpack(isoRes);
                        String isoStr = null;
                        try {
                            isoStr = "isocommand response:" + " Data:" + new String(apduResp.getData(), "iso8859-1")
                                    + " Status:" + apduResp.getStatus() + " StatusString:" + apduResp.getStatusString();
                        } catch (UnsupportedEncodingException e) {
                            e.printStackTrace();
                        }
                        //cardResult += ("\n" + isoStr);

                        // request CID,
                        String result_1 = apduResp.getStatusString();
                        if(result_1.equals("9000")){
                            byte[] req_cid = new byte[]{ 0x00};
                            apduReq = apdu.createReq((byte) 0x80, (byte) 0xb0, (byte) 0x00, (byte) 0x04, req_cid, (byte) 0x0d);
                            apduReq.setLcAlwaysPresent();
                            //apduReq.setLc((byte)2);
                            req = apduReq.pack();
                            req[4] = 0x02;
                            byte[] isoRes_1 = IccTester.getInstance().isoCommand((byte) 0, req);
                            if(isoRes_1 != null){
                                apduResp = apdu.unpack(isoRes_1);
                                try {
                                    /*isoStr ="\n" + "isocommand response2:" + " Data:" + new String(apduResp.getData(), "iso8859-1")
                                            + " Status:" + apduResp.getStatus() + " StatusString:" + apduResp.getStatusString();*/
                                    isoStr ="Card Number: " + new String(apduResp.getData(), "iso8859-1");
                                } catch (UnsupportedEncodingException e) {
                                    e.printStackTrace();
                                }
                                cardResult = ("\n" + isoStr);
                            }

                            String result_2 = apduResp.getStatusString();
                            if(result_2.equals("9000")) {
                                // request to get CID data
                                byte[] req_name_en = new byte[]{(byte)0x80,(byte)0xB0, 0x00, 0x75, 0x02, 0x00, 0x64};

                                byte[] isoRes_2 = IccTester.getInstance().isoCommand((byte) 0, req_name_en);
                                if(isoRes_2 != null) {
                                    apduResp = apdu.unpack(isoRes_2);
                                    try {
                                        /*isoStr ="\n" + "isocommand response2:" + " Data:" + new String(apduResp.getData(), "iso8859-1")
                                                + " Status:" + apduResp.getStatus() + " StatusString:" + apduResp.getStatusString();*/
                                        String name =  new String(apduResp.getData(), "iso8859-1").replaceAll("[\r\n]+", " ");
                                        Log.i("TEST", name);
                                        isoStr ="\n" +"Name EN: " + "\n" + name.replace("#"," ");
                                    } catch (UnsupportedEncodingException e) {
                                        e.printStackTrace();
                                    }
                                    cardResult += ("\n" + isoStr);
                                }

                                String result_3 = apduResp.getStatusString();
                                if(result_3.equals("9000")) {
                                    byte[] req_name_th = new byte[]{(byte)0x80, (byte)0xB0, 0x00, 0x11, 0x02, 0x00, 0x64};

                                    byte[] isoRes_3 = IccTester.getInstance().isoCommand((byte) 0, req_name_th);
                                    if(isoRes_3 != null){
                                        apduResp = apdu.unpack(isoRes_3);
                                        try {
                                            /*isoStr ="\n" + "isocommand response2:" + " Data:" + new String(apduResp.getData(), "iso8859-1")
                                                    + " Status:" + apduResp.getStatus() + " StatusString:" + apduResp.getStatusString();*/
                                            String name_t = new String(apduResp.getData(), "iso8859-1");
                                            try {
                                                //convert a string by encoding to 'TIS620' Thai language code based on 'ISO8859_1' in java standard.
                                                name_t = new String(name_t.getBytes("ISO8859_1"),
                                                        "TIS620").replace("#"," ");
                                            } catch (Exception ex) {
                                               Log.e("ERROR","Convert To Thai is an error :");
                                                ex.printStackTrace();
                                            }
                                            isoStr = "\n" + "\n" +"NAME TH:" + name_t;
                                        } catch (UnsupportedEncodingException e) {
                                            e.printStackTrace();
                                        }
                                        cardResult += (isoStr);
                                    }

                                    String result_4 = apduResp.getStatusString();
                                    if(result_3.equals("9000")) {
                                        byte[] req_photo = new byte[]{(byte)0x80,(byte) 0xB0, 0x01, 0x7B, 0x02, 0x00, (byte)0xFF};

                                        byte[] isoRes_4 = IccTester.getInstance().isoCommand((byte) 0, req_photo);

                                        byte[] idByte1 = new byte[0];
                                        if(isoRes_4 != null){
                                            apduResp = apdu.unpack(isoRes_4);
                                            try {
                                                isoStr ="\n" + "isocommand response2:" + " Data:" + new String(apduResp.getData(), "iso8859-1")
                                                        + " Status:" + apduResp.getStatus() + " StatusString:" + apduResp.getStatusString();
                                                idByte1 = apduResp.getData();
                                            } catch (UnsupportedEncodingException e) {
                                                e.printStackTrace();
                                            }

                                            byte[] req_photo2 = new byte[]{(byte) 0x80,(byte) 0xB0, 0x02, 0x7A, 0x02, 0x00,(byte) 0xFF};
                                            byte[] req_photo3 = new byte[]{(byte) 0x80,(byte) 0xB0, 0x03, 0x79, 0x02, 0x00,(byte) 0xFF};
                                            byte[] req_photo4 = new byte[]{(byte) 0x80,(byte) 0xB0, 0x04, 0x78, 0x02, 0x00,(byte) 0xFF};
                                            byte[] req_photo5 = new byte[]{(byte) 0x80,(byte) 0xB0, 0x05, 0x77, 0x02, 0x00,(byte) 0xFF};
                                            byte[] req_photo6 = new byte[]{(byte) 0x80,(byte) 0xB0, 0x06, 0x76, 0x02, 0x00,(byte) 0xFF};
                                            byte[] req_photo7 = new byte[]{(byte) 0x80,(byte) 0xB0, 0x07, 0x75, 0x02, 0x00,(byte) 0xFF};
                                            byte[] req_photo8 = new byte[]{(byte) 0x80,(byte) 0xB0, 0x08, 0x74, 0x02, 0x00,(byte) 0xFF};
                                            byte[] req_photo9 = new byte[]{(byte) 0x80,(byte) 0xB0, 0x09, 0x73, 0x02, 0x00,(byte) 0xFF};
                                            byte[] req_photo10 = new byte[]{(byte) 0x80,(byte) 0xB0, 0x0A, 0x72, 0x02, 0x00,(byte) 0xFF};
                                            byte[] req_photo11 = new byte[]{(byte) 0x80,(byte) 0xB0, 0x0B, 0x71, 0x02, 0x00,(byte) 0xFF};
                                            byte[] req_photo12 = new byte[]{(byte) 0x80,(byte) 0xB0, 0x0C, 0x70, 0x02, 0x00,(byte) 0xFF};
                                            byte[] req_photo13 = new byte[]{(byte) 0x80,(byte) 0xB0, 0x0D, 0x6F, 0x02, 0x00,(byte) 0xFF};
                                            byte[] req_photo14 = new byte[]{(byte) 0x80,(byte) 0xB0, 0x0E, 0x6E, 0x02, 0x00,(byte) 0xFF};
                                            byte[] req_photo15 = new byte[]{(byte) 0x80,(byte) 0xB0, 0x0F, 0x6D, 0x02, 0x00,(byte) 0xFF};
                                            byte[] req_photo16 = new byte[]{(byte) 0x80,(byte) 0xB0, 0x10, 0x6C, 0x02, 0x00,(byte) 0xFF};
                                            byte[] req_photo17 = new byte[]{(byte) 0x80,(byte) 0xB0, 0x11, 0x6B, 0x02, 0x00,(byte) 0xFF};
                                            byte[] req_photo18 = new byte[]{(byte) 0x80,(byte) 0xB0, 0x12, 0x6A, 0x02, 0x00,(byte) 0xFF};
                                            byte[] req_photo19 = new byte[]{(byte) 0x80,(byte) 0xB0, 0x13, 0x69, 0x02, 0x00,(byte) 0xFF};
                                            byte[] req_photo20 = new byte[]{(byte) 0x80,(byte) 0xB0, 0x14, 0x68, 0x02, 0x00,(byte) 0xFF};


                                            byte[] isoRes_photo2 = IccTester.getInstance().isoCommand((byte) 0, req_photo2);
                                            apduResp = apdu.unpack(isoRes_photo2);
                                            byte[] idByte2 = apduResp.getData();

                                            byte[] isoRes_photo3 = IccTester.getInstance().isoCommand((byte) 0, req_photo3);
                                            apduResp = apdu.unpack(isoRes_photo3);
                                            byte[] idByte3 = apduResp.getData();

                                            byte[] isoRes_photo4 = IccTester.getInstance().isoCommand((byte) 0, req_photo4);
                                            apduResp = apdu.unpack(isoRes_photo4);
                                            byte[] idByte4 = apduResp.getData();

                                            byte[] isoRes_photo5 = IccTester.getInstance().isoCommand((byte) 0, req_photo5);
                                            apduResp = apdu.unpack(isoRes_photo5);
                                            byte[] idByte5 = apduResp.getData();

                                            byte[] isoRes_photo6 = IccTester.getInstance().isoCommand((byte) 0, req_photo6);
                                            apduResp = apdu.unpack(isoRes_photo6);
                                            byte[] idByte6 = apduResp.getData();

                                            byte[] isoRes_photo7 = IccTester.getInstance().isoCommand((byte) 0, req_photo7);
                                            apduResp = apdu.unpack(isoRes_photo7);
                                            byte[] idByte7 = apduResp.getData();

                                            byte[] isoRes_photo8 = IccTester.getInstance().isoCommand((byte) 0, req_photo8);
                                            apduResp = apdu.unpack(isoRes_photo8);
                                            byte[] idByte8 = apduResp.getData();

                                            byte[] isoRes_photo9 = IccTester.getInstance().isoCommand((byte) 0, req_photo9);
                                            apduResp = apdu.unpack(isoRes_photo9);
                                            byte[] idByte9 = apduResp.getData();

                                            byte[] isoRes_photo10 = IccTester.getInstance().isoCommand((byte) 0, req_photo10);
                                            apduResp = apdu.unpack(isoRes_photo10);
                                            byte[] idByte10 = apduResp.getData();

                                            byte[] isoRes_photo11 = IccTester.getInstance().isoCommand((byte) 0, req_photo11);
                                            apduResp = apdu.unpack(isoRes_photo11);
                                            byte[] idByte11 = apduResp.getData();

                                            byte[] isoRes_photo12 = IccTester.getInstance().isoCommand((byte) 0, req_photo12);
                                            apduResp = apdu.unpack(isoRes_photo12);
                                            byte[] idByte12 = apduResp.getData();

                                            byte[] isoRes_photo13 = IccTester.getInstance().isoCommand((byte) 0, req_photo13);
                                            apduResp = apdu.unpack(isoRes_photo13);
                                            byte[] idByte13 = apduResp.getData();

                                            byte[] isoRes_photo14 = IccTester.getInstance().isoCommand((byte) 0, req_photo14);
                                            apduResp = apdu.unpack(isoRes_photo14);
                                            byte[] idByte14 = apduResp.getData();

                                            byte[] isoRes_photo15 = IccTester.getInstance().isoCommand((byte) 0, req_photo15);
                                            apduResp = apdu.unpack(isoRes_photo15);
                                            byte[] idByte15 = apduResp.getData();

                                            byte[] isoRes_photo16 = IccTester.getInstance().isoCommand((byte) 0, req_photo16);
                                            apduResp = apdu.unpack(isoRes_photo16);
                                            byte[] idByte16 = apduResp.getData();

                                            byte[] isoRes_photo17 = IccTester.getInstance().isoCommand((byte) 0, req_photo17);
                                            apduResp = apdu.unpack(isoRes_photo17);
                                            byte[] idByte17 = apduResp.getData();

                                            byte[] isoRes_photo18 = IccTester.getInstance().isoCommand((byte) 0, req_photo18);
                                            apduResp = apdu.unpack(isoRes_photo18);
                                            byte[] idByte18 = apduResp.getData();

                                            byte[] isoRes_photo19 = IccTester.getInstance().isoCommand((byte) 0, req_photo19);
                                            apduResp = apdu.unpack(isoRes_photo19);
                                            byte[] idByte19 = apduResp.getData();

                                            byte[] isoRes_photo20 = IccTester.getInstance().isoCommand((byte) 0, req_photo20);
                                            apduResp = apdu.unpack(isoRes_photo20);
                                            byte[] idByte20 = apduResp.getData();

                                            ByteArrayOutputStream outputStream = new ByteArrayOutputStream( );
                                            try {
                                                outputStream.write(idByte1);
                                                outputStream.write(idByte2);
                                                outputStream.write(idByte3);
                                                outputStream.write(idByte4);
                                                outputStream.write(idByte5);
                                                outputStream.write(idByte6);
                                                outputStream.write(idByte7);
                                                outputStream.write(idByte8);
                                                outputStream.write(idByte9);
                                                outputStream.write(idByte10);
                                                outputStream.write(idByte11);
                                                outputStream.write(idByte12);
                                                outputStream.write(idByte13);
                                                outputStream.write(idByte14);
                                                outputStream.write(idByte15);
                                                outputStream.write(idByte16);
                                                outputStream.write(idByte17);
                                                outputStream.write(idByte18);
                                                outputStream.write(idByte19);
                                                outputStream.write(idByte20);
                                            } catch (IOException e) {
                                                e.printStackTrace();
                                            }

                                            byte img_byte[] = outputStream.toByteArray( );
                                            // Create a byte array
                                            byte[] bytes = img_byte;

                                            // Wrap a byte array into a buffer
                                            ByteBuffer buf = ByteBuffer.wrap(bytes);

                                            // Retrieve bytes between the position and limit
                                            // (see Putting Bytes into a ByteBuffer)
                                            bytes = new byte[buf.remaining()];

                                            // transfer bytes from this buffer into the given destination array
                                            buf.get(bytes, 0, bytes.length);

                                            // Retrieve all bytes in the buffer
                                            buf.clear();
                                            bytes = new byte[buf.capacity()];

                                            // transfer bytes from this buffer into the given destination array
                                            buf.get(bytes, 0, bytes.length);

                                            bmp= BitmapFactory.decodeByteArray(bytes,0,bytes.length);

                                            /*textView.setText(cardResult);

                                            imageView.setImageBitmap(bmp);*/

                                            //cardResult += ("\n" + isoStr);

                                        }
                                    }
                                }

                            }

                        }
                    }
                    IccTester.getInstance().close((byte) 0);
                    IccTester.getInstance().light(false);
                    Message message2 = Message.obtain();
                    message2.what = 0;
                    handler.sendMessage(message2);
                    tickTimer.stop();
                    SystemClock.sleep(2000);
                    break;

                } else {
                    cardResult = getString(R.string.prompt_insert_card) ;

                    SystemClock.sleep(2000);
                }
            }
        }

    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        IccTester.getInstance().close((byte) 0);
        IccTester.getInstance().light(false);
    }

    private Handler handler = new Handler() {
        public void handleMessage(android.os.Message msg) {
            switch (msg.what) {
                case 0:
                    textView.setText("");
                    textView.setText(cardResult);
                    imageView.setImageBitmap(bmp);
                    break;
                case 1:
                    textView.setText("\n" + "Reading card, please wait....");
                    imageView.setImageBitmap(bmp);
                    break;
                default:
                    break;
            }
        };
    };
}

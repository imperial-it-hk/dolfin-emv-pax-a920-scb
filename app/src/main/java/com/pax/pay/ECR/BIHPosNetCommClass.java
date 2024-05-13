package com.pax.pay.ECR;

import android.annotation.SuppressLint;

import com.pax.pay.uart.CommManageClass;
import com.pax.pay.uart.ProtoFilterClass;
import com.pax.pay.utils.Checksum;

import java.io.ByteArrayOutputStream;
import java.util.Arrays;

public class BIHPosNetCommClass extends PosNetCommClass {
    public BIHPosNetCommClass(CommManageClass CommManage, PosNetCommInterface PosNetCommCbk) {
        super(CommManage, PosNetCommCbk);
    }

    public BIHPosNetCommClass(CommManageClass CommManage, ProtoFilterClass ProtoFilter, PosNetCommInterface PosNetCommCbk) {
        super(CommManage, ProtoFilter, PosNetCommCbk);
    }

    @Override
    public int ProtocolPack(byte[] transType, ByteArrayOutputStream pduBuffer, ByteArrayOutputStream msgBuffer) {
        byte[] STXbuffer = new byte[]{0x02};
        byte[] ETXbuffer = new byte[]{0x03};
        byte[] msgLength = new byte[3];
        byte[] LRCbuffer = new byte[1];

        //Pack STX
        pduBuffer.write(STXbuffer, 0, STXbuffer.length);

        //Transaction Type
        pduBuffer.write(transType, 0, transType.length);

        //Pack data length
        int msg_length = ((msgBuffer==null) ? 0 : msgBuffer.toByteArray().length);
        Arrays.fill(msgLength, (byte)0x20);
        @SuppressLint("DefaultLocale") String strLength = String.format("%03d",  msg_length);
        System.arraycopy(strLength.getBytes(), 0, msgLength,0,  (strLength.getBytes().length >  msgLength.length )?  msgLength.length : strLength.getBytes().length );
        pduBuffer.write(msgLength,0, msgLength.length);

        //Pack Message Data
        if ((msgBuffer != null) && (msgBuffer.toByteArray().length >0)) {
            pduBuffer.write(msgBuffer.toByteArray(), 0, msgBuffer.toByteArray().length);
        }

        ByteArrayOutputStream toCalculateLRC = new ByteArrayOutputStream();
        if (pduBuffer.size() > 0) {
            try {
                toCalculateLRC.write(pduBuffer.toByteArray());
                toCalculateLRC.write(0x62);
            }
            catch (Exception ex) {

            }
        }

        //Pack LRC
        LRCbuffer[0] = Checksum.calculateLRC(Arrays.copyOfRange(toCalculateLRC.toByteArray(), 0, toCalculateLRC.toByteArray().length));
        pduBuffer.write(LRCbuffer, 0, LRCbuffer.length);

        //Pack ETX
        pduBuffer.write(ETXbuffer, 0, ETXbuffer.length);


        return 0;
    }
}

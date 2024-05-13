package com.pax.pay.utils;

import android.content.Context;

import com.pax.pay.utils.Convert.EEndian;

class PackerApdu implements IApdu {
    private static PackerApdu instance;
    private Context context;

    private static final String TAG = PackerApdu.class.getSimpleName();

    private byte[] rspData = null;
    private short status = 0;

    private PackerApdu() {
    }

    public void setContext(Context context) {
        this.context = context;
    }

    public synchronized static PackerApdu getInstance() {
        if (instance == null) {
            instance = new PackerApdu();
        }

        return instance;
    }

    @Override
    public IApduReq createReq(byte cla, byte ins) {
        return new ApduReq(cla, ins);
    }

    @Override
    public IApduReq createReq(byte cla, byte ins, byte[] data) {
        return new ApduReq(cla, ins, data);
    }

    @Override
    public IApduReq createReq(byte cla, byte ins, byte[] data, short le) {
        return new ApduReq(cla, ins, data, le);
    }

    @Override
    public IApduReq createReq(byte cla, byte ins, byte p1, byte p2) {
        return new ApduReq(cla, ins, p1, p2);
    }

    @Override
    public IApduReq createReq(byte cla, byte ins, byte p1, byte p2, byte[] data) {
        return new ApduReq(cla, ins, p1, p2, data);
    }

    @Override
    public IApduReq createReq(byte cla, byte ins, byte p1, byte p2, byte[] data, short le) {
        return new ApduReq(cla, ins, p1, p2, data, le);
    }

    @Override
    public IApduResp unpack(byte[] resp) {
        ApduResp arsp = new ApduResp();
        arsp.setRspDataAndStatus(resp);
        return arsp;
    }

    private class ApduReq implements IApduReq {

        private boolean isDoubleLcLe = false;
        private boolean isLcAlwaysPresent = false;
        private boolean isLePresent = true;

        private byte CLA = 0;
        private byte INS = 0;
        private byte P1 = 0;
        private byte P2 = 0;
        // private short Lc = 0;
        private byte[] reqdata = null;
        private short Le = 0;

        @Override
        public void setLengthOfLcLeTo2Bytes() {
            isDoubleLcLe = true;
            Le = (short) 0xFFFF;
        }

        @Override
        public void setLcAlwaysPresent() {
            isLcAlwaysPresent = true;
        }

        @Override
        public void setLeNotPresent() {
            isLePresent = false;
        }

        public ApduReq(byte cla, byte ins) {
            CLA = cla;
            INS = ins;
        }

        public ApduReq(byte cla, byte ins, byte[] data) {
            CLA = cla;
            INS = ins;
            setData(data);
        }

        public ApduReq(byte cla, byte ins, byte[] data, short le) {
            CLA = cla;
            INS = ins;
            setData(data);
            Le = le;
        }

        public ApduReq(byte cla, byte ins, byte p1, byte p2) {
            CLA = cla;
            INS = ins;
            P1 = p1;
            P2 = p2;

        }

        public ApduReq(byte cla, byte ins, byte p1, byte p2, byte[] data) {
            CLA = cla;
            INS = ins;
            P1 = p1;
            P2 = p2;
            setData(data);
        }

        public ApduReq(byte cla, byte ins, byte p1, byte p2, byte[] data, short le) {
            CLA = cla;
            INS = ins;
            P1 = p1;
            P2 = p2;
            setData(data);
            Le = le;
        }

        @Override
        public byte getCla() {
            return CLA;
        }

        @Override
        public void setCla(byte cLA) {
            CLA = cLA;
        }

        @Override
        public byte getIns() {
            return INS;
        }

        @Override
        public void setIns(byte ins) {
            INS = ins;
        }

        @Override
        public byte getP1() {
            return P1;
        }

        @Override
        public void setP1(byte p1) {
            P1 = p1;
        }

        @Override
        public byte getP2() {
            return P2;
        }

        @Override
        public void setP2(byte p2) {
            P2 = p2;
        }

        @Override
        public short getLe() {
            return Le;
        }

        @Override
        public void setLe(short le) {
            Le = le;
        }

        @Override
        public byte[] getData() {
            return reqdata;
        }

        @Override
        public void setData(byte[] data) {
            if (data != null) {
                reqdata = data;
                // Lc = (short) this.reqdata.length;
            } else {
                reqdata = null;
                // Lc = 0;
            }
        }

        @Override
        public byte[] pack() {
            //short lc = 0;
            int lc = 0;     // changed from short to int to support length > 32768 
            
            Convert convert =Convert.getInstance();

            int lengthOfLc = isDoubleLcLe ? 2 : 1;
            int lengthOfLe = isDoubleLcLe ? 2 : 1;

            int offset = 0;

            // if Le not present
            if (!isLePresent) {
                lengthOfLe = 0;
            }

            // with req data
            if (reqdata != null && (lc = reqdata.length) > 0) {
                // Lc MUST be present!
            } else { // w/o req data
                if (!isLcAlwaysPresent) {
                    lengthOfLc = 0;
                }
            }

            byte[] request = new byte[4 + lengthOfLc + lc + lengthOfLe];
            request[0] = CLA;
            request[1] = INS;
            request[2] = P1;
            request[3] = P2;

            offset = 4;
            if (lengthOfLc == 0) {
                // no Lc
            } else if (lengthOfLc == 1) {
                request[offset] = (byte) lc;
                offset++;
            } else {
                convert.shortToByteArray((short)lc, request, offset, EEndian.BIG_ENDIAN);
                offset += 2;
            }

            if (lc > 0) {
                System.arraycopy(reqdata, 0, request, offset, lc);
                offset += lc;
            }

            if (lengthOfLe == 0) {
                // no Le
            } else if (lengthOfLe == 1) {
                request[offset] = (byte) Le;
                offset++;
            } else if (lengthOfLe == 2) {
                convert.shortToByteArray(Le, request, offset, EEndian.BIG_ENDIAN);
                offset += 2;
            }
            return request;
        }

    }

    private class ApduResp implements IApduResp {
        private byte[] data;
        private short status;

        public void setRspDataAndStatus(byte[] dataAndStatus) {
            if (dataAndStatus == null || dataAndStatus.length < 2)
                return;

            data = new byte[dataAndStatus.length - 2];
            System.arraycopy(dataAndStatus, 0, data, 0, data.length);

            status = Convert.getInstance()
                    .shortFromByteArray(dataAndStatus, dataAndStatus.length - 2, EEndian.BIG_ENDIAN);

        }

        @Override
        public byte[] getData() {
            return data;
        }

        @Override
        public short getStatus() {
            return status;
        }

        @Override
        public String getStatusString() {
            byte[] statusCode = new byte[2];
           Convert.getInstance().shortToByteArray(status, statusCode, 0, EEndian.BIG_ENDIAN);
            return Convert.getInstance().bcdToStr(statusCode);
        }

    }

}

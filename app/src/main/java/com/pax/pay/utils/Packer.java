package com.pax.pay.utils;

import android.content.Context;

public class Packer implements IPacker {
    private static Packer instance;
    private Context context;

    private Packer() {
    }

    public void setContext(Context context) {
        this.context = context;
    }

    public synchronized static Packer getInstance() {
        if (instance == null) {
            instance = new Packer();
        }

        return instance;
    }

    @Override
    public IApdu getApdu() {
        PackerApdu apdu = PackerApdu.getInstance();
        apdu.setContext(context);
        return apdu;
    }

}

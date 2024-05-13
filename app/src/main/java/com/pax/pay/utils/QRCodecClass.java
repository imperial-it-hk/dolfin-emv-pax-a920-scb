package com.pax.pay.utils;

import android.app.Fragment;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.os.Bundle;
import th.co.bkkps.utils.Log;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.pax.dal.IScanCodec;
import com.pax.dal.entity.DecodeResult;
import com.pax.edc.R;
import com.pax.pay.app.FinancialApplication;
import com.pax.settings.SysParam;


import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class QRCodecClass implements Camera.PreviewCallback, SurfaceHolder.Callback{
    private String TAG = "QRCodecClass:";
    private int WIDTH = 640, HEIGHT = 480;
    private Context mContext;
    private SurfaceView mSurfaceView;
    private SurfaceHolder holder;
    private static Camera camera;
    private boolean isOpen = false;
    private byte[] data = null;
    private IScanCodec mIScanCodec;
    private QRCodecInterface QRCodecCbk;

    public QRCodecClass(Context Context, IScanCodec iScanCodec, SurfaceView surfaceview, QRCodecInterface QRCodecCbk) {
        this.mContext = Context;
        this.mSurfaceView = surfaceview;
        this.QRCodecCbk = QRCodecCbk;
        this.mIScanCodec = iScanCodec;
    }

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        if (data != null) {
            Log.i(TAG, "dataLEN:" + data.length);
            long startTime = System.currentTimeMillis();
            DecodeResult decodeResult = mIScanCodec.decode(data);
            long timeCost = System.currentTimeMillis() - startTime;
            String res = "timeCost:"
                    + timeCost
                    + " result:"
                    + ((decodeResult == null || decodeResult.getContent() == null) ? "null" : decodeResult.getContent());
            Log.i(TAG, res);

            if ((decodeResult != null) && (decodeResult.getContent() != null)) {
                Log.i(TAG, decodeResult.getContent());
                this.QRCodecCbk.onRead(decodeResult.getContent());
            }
            camera.addCallbackBuffer(data);

        }
    }

    public void Start() {
        mIScanCodec.init(mContext, WIDTH, HEIGHT);
        holder = mSurfaceView.getHolder();
        holder.addCallback(QRCodecClass.this);
        initCamera();
        camera.addCallbackBuffer(data);
        camera.setPreviewCallbackWithBuffer(QRCodecClass.this);
        camera.setPreviewCallback(QRCodecClass.this);
        try {
            camera.setPreviewDisplay(holder);
        } catch (IOException e) {
            e.printStackTrace();
        }
        camera.startPreview();
        Log.i(TAG, "Open");
        isOpen = true;
    }

    public void Stop(){
        if (isOpen) {
            releaseRes();
        }
    }

    public void initCamera() {
        if (camera != null) {
            camera.setPreviewCallbackWithBuffer(null);
            camera.stopPreview();
            camera.release();
            camera = null;
        }
        if (Utils.getString(R.string.back_camera).equals(FinancialApplication.getSysParam().get(SysParam.StringParam.EDC_DEFAULT_CAMERA))) {
            camera = Camera.open(0);
        } else {
            camera = Camera.open(1);
        }
        camera.setDisplayOrientation(90);
        Camera.Parameters parameters = camera.getParameters();
        parameters.setPreviewSize(WIDTH, HEIGHT);
        parameters.setPictureSize(WIDTH, HEIGHT);
        parameters.setZoom(parameters.getZoom());
        camera.setParameters(parameters);

        // For formats besides YV12, the size of the buffer is determined by multiplying the preview image width,
        // height, and bytes per pixel. The width and height can be read from Camera.Parameters.getPreviewSize(). Bytes
        // per pixel can be computed from android.graphics.ImageFormat.getBitsPerPixel(int) / 8, using the image format
        // from Camera.Parameters.getPreviewFormat().
        float bytesPerPixel = ImageFormat.getBitsPerPixel(parameters.getPreviewFormat()) / (float) 8;
        data = new byte[(int) (bytesPerPixel * WIDTH * HEIGHT)];

        Log.i("Test", "previewFormat:" + parameters.getPreviewFormat() + " bytesPerPixel:" + bytesPerPixel
                + " prewidth:" + parameters.getPreviewSize().width + " preheight:" + parameters.getPreviewSize().height);

    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {

    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        Log.i("Test", "format:" + format + "width:" + width + "height:" + height);
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {

    }

    private void releaseRes() {

        mIScanCodec.release();

        if (camera != null) {
            camera.stopPreview();
            camera.setPreviewCallbackWithBuffer(null);
            camera.release();
            camera = null;
            holder = null;
        }

    }

}

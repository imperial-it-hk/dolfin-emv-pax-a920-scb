package com.pax.pay;

import android.Manifest;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.camera.view.PreviewView;

import com.king.mlkit.vision.camera.AnalyzeResult;
import com.king.mlkit.vision.camera.CameraScan;
import com.king.mlkit.vision.camera.util.LogUtils;
import com.king.mlkit.vision.camera.util.PermissionUtils;
import com.king.wechat.qrcode.scanning.analyze.WeChatScanningAnalyzer;
import com.pax.abl.core.ActionResult;
import com.pax.edc.R;
import com.pax.edc.opensdk.TransResult;
import com.pax.pay.utils.CurrencyConverter;
import com.pax.pay.utils.Utils;

import java.util.List;


public class ScanKitActivity extends BaseActivityWithTickForAction implements CameraScan.OnScanResultCallback<List<String>> {

    public static final String SCAN_TITLE = "SCAN_TITLE";
    public static final String TRANS_AMOUNT = "TRANS_AMOUNT";
    private static final int CAMERA_PERMISSION_REQUEST_CODE = 0X86;
    protected PreviewView previewView;
    private CameraScan<List<String>> mCameraScan;
    private String title = "";
    private String transAmount = "";
    private TextView tv_amount;
    private Button cancel_btn;
    private Button ok_btn;

    @Override
    public int getLayoutId() {
        return R.layout.activity_scan_kit;
    }

    @Override
    protected String getTitleString() {
        return title;
    }

    @Override
    protected void initViews() {
        tv_amount = findViewById(R.id.tv_amount);
        cancel_btn = findViewById(R.id.cancel_btn);
        ok_btn = findViewById(R.id.ok_btn);
        previewView = findViewById(R.id.previewView);


        if (transAmount != null && !transAmount.isEmpty() && Utils.parseLongSafe(transAmount, 0) > 0) {
            transAmount = CurrencyConverter.convert(Utils.parseLongSafe(transAmount, 0));
            tv_amount.setText(String.format("Amount:%s", transAmount));
        }

        initUI();
    }


    @Override
    protected void setListeners() {
        cancel_btn.setOnClickListener(this);
        ok_btn.setOnClickListener(this);
    }

    @Override
    protected void loadParam() {
        title = getIntent().getStringExtra(SCAN_TITLE);
        transAmount = getIntent().getStringExtra(TRANS_AMOUNT);
    }


    public void initUI() {
        initCameraScan();
        startCamera();
    }


    /**
     * 初始化CameraScan
     */
    public void initCameraScan() {
        mCameraScan = createCameraScan(previewView)
                .setAnalyzer(new WeChatScanningAnalyzer())
                .setOnScanResultCallback(this);
    }

    /**
     * 启动相机预览
     */
    public void startCamera() {
        if (mCameraScan != null) {
            if (PermissionUtils.checkPermission(this, Manifest.permission.CAMERA)) {
                mCameraScan.startCamera();

            } else {
                LogUtils.d("checkPermissionResult != PERMISSION_GRANTED");
                PermissionUtils.requestPermission(this, Manifest.permission.CAMERA, CAMERA_PERMISSION_REQUEST_CODE);
            }
        }
    }

    /**
     * 释放相机
     */
    private void releaseCamera() {
        if (mCameraScan != null) {
            mCameraScan.release();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            requestCameraPermissionResult(permissions, grantResults);
        }
    }

    /**
     * 请求Camera权限回调结果
     *
     * @param permissions
     * @param grantResults
     */
    public void requestCameraPermissionResult(@NonNull String[] permissions, @NonNull int[] grantResults) {
        if (PermissionUtils.requestPermissionsResult(Manifest.permission.CAMERA, permissions, grantResults)) {
            startCamera();
        } else {
            finish();
        }
    }

    @Override
    protected void onDestroy() {
        releaseCamera();
        super.onDestroy();
    }

    /**
     * 创建{@link CameraScan}
     *
     * @param previewView
     * @return
     */
    public CameraScan<List<String>> createCameraScan(PreviewView previewView) {
        return new BaseCameraScan<>(this, previewView);
    }


    @Override
    public void onScanResultCallback(@NonNull AnalyzeResult<List<String>> result) {
        mCameraScan.setAnalyzeImage(false);
        releaseCamera();
        LogUtils.e(result.getResult().get(0));
        finish(new ActionResult(TransResult.SUCC, result.getResult().get(0)));
    }


    @Override
    public void onScanResultFailure() {

    }

    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {

    }

    @Override
    protected void onClickProtected(View v) {
        super.onClickProtected(v);
        switch (v.getId()) {
            case R.id.cancel_btn:
                releaseCamera();
                finish(new ActionResult(TransResult.ERR_USER_CANCEL, null));
                break;
            case R.id.ok_btn:
                break;
        }
    }

}
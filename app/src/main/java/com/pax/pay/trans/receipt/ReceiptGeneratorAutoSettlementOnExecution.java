package com.pax.pay.trans.receipt;

import android.graphics.Bitmap;
import android.view.Gravity;

import com.pax.device.Device;
import com.pax.edc.R;
import com.pax.glwrapper.imgprocessing.IImgProcessing;
import com.pax.glwrapper.page.IPage;
import com.pax.pay.app.FinancialApplication;
import com.pax.pay.constant.Constants;
import com.pax.pay.trans.component.Component;
import com.pax.pay.utils.Utils;

import java.sql.Timestamp;
import java.util.List;

public class ReceiptGeneratorAutoSettlementOnExecution implements IReceiptGenerator {

    private IPage page = null;
    private IImgProcessing imgProcessing = null;
    private String title = "";

    public ReceiptGeneratorAutoSettlementOnExecution() {
        imgProcessing = FinancialApplication.getGl().getImgProcessing();
    }

    public ReceiptGeneratorAutoSettlementOnExecution(String title) {
        imgProcessing = FinancialApplication.getGl().getImgProcessing();
        this.title = title;
    }

    @Override
    public Bitmap generateBitmap() {
        return null;
    }

    @Override
    public String generateString() {
        return null;
    }

    public Bitmap generateBitmapAutoSettlementWakeupHost(List<String> hostList) {
        IPage page = Device.generatePage();
        IImgProcessing imgProcessing = FinancialApplication.getGl().getImgProcessing();

        page.addLine().addUnit(page.createUnit().setFontSize(FONT_NORMAL).setGravity(Gravity.CENTER).setText(Utils.getString(R.string.receipt_one_line)));
        page.addLine().addUnit(page.createUnit().setFontSize(FONT_NORMAL).setGravity(Gravity.CENTER).setText(title));
        page.addLine().addUnit(page.createUnit().setFontSize(FONT_NORMAL).setGravity(Gravity.CENTER).setText(Utils.getString(R.string.receipt_one_line)));
        page.addLine().addUnit(page.createUnit().setFontSize(FONT_SMALL).setGravity(Gravity.START).setText(" Execute on : "));
        page.addLine().addUnit(page.createUnit().setFontSize(FONT_SMALL).setGravity(Gravity.START).setText(" \t DATE :" + Device.getTime("dd/MM/yyyy")));
        page.addLine().addUnit(page.createUnit().setFontSize(FONT_SMALL).setGravity(Gravity.START).setText(" \t TIME :" + Device.getTime("HH:mm:ss") + "\n"));
        page.addLine().addUnit(page.createUnit().setFontSize(FONT_SMALL).setGravity(Gravity.START).setText(" Host : "));
        if (hostList!=null & !hostList.isEmpty()) {
            int index = 0;
            for (String hostName : hostList) {
                index+=1;
                page.addLine().addUnit(page.createUnit().setFontSize(FONT_SMALL).setGravity(Gravity.START).setText( "\t" +index + ". " +hostName));
            }
        }
        page.addLine().addUnit(page.createUnit().setFontSize(FONT_NORMAL).setGravity(Gravity.CENTER).setText(Utils.getString(R.string.receipt_one_line)));
        page.addLine().addUnit(page.createUnit().setFontSize(FONT_SMALL).setGravity(Gravity.START).setText( "\n\n"));

        return imgProcessing.pageToBitmap(page,384);

    }

    public Bitmap generateBitmapAutoSettleERMUploadFailed() {
        IPage page = Device.generatePage();
        IImgProcessing imgProcessing = FinancialApplication.getGl().getImgProcessing();

        page.addLine().addUnit(page.createUnit().setFontSize(FONT_NORMAL).setGravity(Gravity.CENTER).setText("SETTLEMENT END\nE-RECEIPT UPLOAD FAILED !!\n" + Device.getTime("dd/MM/yyyy HH:mm:ss")));

        return imgProcessing.pageToBitmap(page,384);
    }

    public Bitmap generateBitmapAutoSettleHostNotFound() {
        IPage page = Device.generatePage();
        IImgProcessing imgProcessing = FinancialApplication.getGl().getImgProcessing();

        page.addLine().addUnit(page.createUnit().setFontSize(FONT_NORMAL).setGravity(Gravity.CENTER).setText("SETTLEMENT END\nHOST NOT FOUND\n" + Device.getTime("dd/MM/yyyy HH:mm:ss")));

        return imgProcessing.pageToBitmap(page,384);

    }

}

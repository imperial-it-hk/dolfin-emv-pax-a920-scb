package com.pax.pay.trans.receipt;

import android.graphics.Bitmap;
import android.view.Gravity;

import com.pax.device.Device;
import com.pax.edc.R;
import com.pax.glwrapper.imgprocessing.IImgProcessing;
import com.pax.glwrapper.page.IPage;
import com.pax.pay.app.FinancialApplication;
import com.pax.pay.trans.model.ETransType;
import com.pax.pay.trans.model.TransData;
import com.pax.pay.trans.model.TransTotal;
import com.pax.pay.utils.Utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ReceiptGeneratorPreAuthDetail extends ReceiptGeneratorDetail {

    public ReceiptGeneratorPreAuthDetail(TransTotal total, String title) {
        super(total, title);
    }

    @Override
    public List<Bitmap> generateBitmaps() throws Exception {
        List<Bitmap> bitmaps = new ArrayList<>();

        try {
            IImgProcessing imgProcessing = FinancialApplication.getGl().getImgProcessing();

            bitmaps.add(imgProcessing.pageToBitmap(generateHeader(), 384));
            addTransDetail(bitmaps);
        } catch (Exception e) {
            throw new Exception("Generate Bitmap fail", e);
        }

        return bitmaps;
    }

    @Override
    protected void addTransDetail(List<Bitmap> bitmaps) {
        IImgProcessing imgProcessing = FinancialApplication.getGl().getImgProcessing();

        List<ETransType> preAuthTypes = Arrays.asList(ETransType.PREAUTHORIZATION, ETransType.PREAUTHORIZATION_CANCELLATION);
        filter2.add(TransData.ETransStatus.SALE_COMPLETED);
        List<TransData> details = FinancialApplication.getTransDataDbHelper().findTransData(preAuthTypes, filter2, total.getAcquirer());

        IPage page;
        int transNo = 0, j, transPerPage;
        int tranSize = details != null ? details.size() : 0;
        int totalPage = (int) Math.ceil((double) tranSize / MAX_SIZE);
        for (int i = 1 ; i <= totalPage ; i++) {
            page = Device.generatePage();
            transPerPage = (transPerPage = i * MAX_SIZE) > tranSize ? tranSize : transPerPage;
            for (j = transNo ; j < transPerPage ; j++) {
                TransData transData = details.get(j);
                generateTransDetail(transData, page);
                if (j == (tranSize - 1)) { // last record
                    page.addLine().addUnit(page.createUnit()
                            .setText(Utils.getString(R.string.receipt_double_line))
                            .setGravity(Gravity.CENTER));
                }
            }
            transNo = j;
            bitmaps.add(imgProcessing.pageToBitmap(page, 384));
        }
    }

    @Override
    protected String getTextTransTypeByAcquirer(TransData transData) {
        ETransType transType = transData.getTransType();
        if (transType == ETransType.PREAUTHORIZATION_CANCELLATION) {
            return "Pre-Auth Cancel";
        } else {
            return "Pre-Auth";
        }
    }
}

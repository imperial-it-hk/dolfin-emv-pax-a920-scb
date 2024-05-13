package com.pax.pay.trans.receipt;

import android.content.Context;
import android.view.Gravity;

import com.pax.device.Device;
import com.pax.glwrapper.page.IPage;
import com.pax.pay.app.FinancialApplication;
import com.pax.pay.base.CardRange;

import java.util.ArrayList;
import java.util.List;

public class ReceiptGeneratorCardRangeList extends ReceiptGeneratorParam implements IReceiptGenerator {

    public ReceiptGeneratorCardRangeList() {
        //do nothing
    }

    @Override
    protected List<IPage> generatePages(Context context) {
        List<IPage> pages = new ArrayList<>();
        List<CardRange> cardRanges = FinancialApplication.getAcqManager().findAllCardRanges();

        IPage page = Device.generatePage();
        page.addLine()
                .addUnit(page.createUnit()
                        .setText("\nCARD RANGE\n")
                        .setGravity(Gravity.CENTER));
        pages.add(page);

        for (CardRange i : cardRanges) {
            page = Device.generatePage();
            //Card Name
            page.addLine()
                    .addUnit(page.createUnit()
                            .setText("Card Name")
                            .setWeight(2.0f))
                    .addUnit(page.createUnit()
                            .setText(i.getName())
                            .setGravity(Gravity.END)
                            .setWeight(3.0f));

            //Issuer Name
            page.addLine()
                    .addUnit(page.createUnit()
                            .setText("Issuer Name")
                            .setWeight(2.0f))
                    .addUnit(page.createUnit()
                            .setText(i.getIssuerName())
                            .setGravity(Gravity.END)
                            .setWeight(3.0f));

            //Range Low
            page.addLine()
                    .addUnit(page.createUnit()
                            .setText("Range Low")
                            .setWeight(2.0f))
                    .addUnit(page.createUnit()
                            .setText(i.getPanRangeLow())
                            .setGravity(Gravity.END)
                            .setWeight(3.0f));

            //Range High
            page.addLine()
                    .addUnit(page.createUnit()
                            .setText("Range High")
                            .setWeight(2.0f))
                    .addUnit(page.createUnit()
                            .setText(i.getPanRangeHigh())
                            .setGravity(Gravity.END)
                            .setWeight(3.0f));

            //Pan Length
            page.addLine()
                    .addUnit(page.createUnit()
                            .setText("Pan Length")
                            .setWeight(2.0f))
                    .addUnit(page.createUnit()
                            .setText(String.valueOf(i.getPanLength()))
                            .setGravity(Gravity.END)
                            .setWeight(3.0f));
            pages.add(page);
        }

        page = Device.generatePage();
        page.addLine().addUnit(page.createUnit().setText("\n\n\n\n"));
        pages.add(page);
        return pages;
    }

    @Override
    protected IPage generatePage(Context context) {
        return null;
    }
}

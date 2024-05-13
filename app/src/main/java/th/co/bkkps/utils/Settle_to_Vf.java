package th.co.bkkps.utils;

import android.view.Gravity;

import com.pax.device.Device;
import com.pax.edc.R;
import com.pax.eemv.utils.Tools;
import com.pax.glwrapper.page.IPage;
import com.pax.pay.app.FinancialApplication;
import com.pax.pay.constant.Constants;
import com.pax.pay.trans.component.Component;
import com.pax.pay.utils.Convert;
import com.pax.pay.utils.CurrencyConverter;
import com.pax.pay.utils.EReceiptUtils;
import com.pax.pay.utils.Utils;
import com.pax.settings.SysParam;

import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.util.List;

public class Settle_to_Vf {
    //private static final String TAG = Convert.class.getSimpleName();
    private static Settle_to_Vf instance;
    private static ByteArrayOutputStream settleStream;
    private final int NORMAL_SIZE_LEN = 23;
    private final int HALF_SIZE_LEN = 44;
    private final byte D7 = (byte) 0xD7;
    private final byte CE = (byte) 0xCE;
    int FONT_BIG = 30;
    int FONT_NORMAL_26 = 26;
    int FONT_NORMAL = 24;
    int FONT_NORMAL_22 = 22;
    int FONT_SMALL = 20;
    int FONT_SMALL_18 = 18;
    int FONT_SMALL_19 = 19;
    private String settSlip;

    private Settle_to_Vf() {
    }

    public synchronized static Settle_to_Vf getInstance() {
        if (instance == null) {
            instance = new Settle_to_Vf();
        }
        return instance;
    }

    private String pageToVfErm(IPage page) {

        StringBuilder printLine = new StringBuilder();

        IPage.ILine.IUnit unit1;
        IPage.ILine.IUnit unit2;
        IPage.ILine.IUnit unit3;

        List<IPage.ILine> allLine = page.getLines();
        for (IPage.ILine line : allLine) {
            List<IPage.ILine.IUnit> allUnit = line.getUnits();
            if (allUnit.size() == 1) {
                unit1 = allUnit.get(0);
                printLine.append(addOne(unit1));
            } else if (allUnit.size() == 2) {
                unit1 = allUnit.get(0);
                unit2 = allUnit.get(1);
                printLine.append(addTwo(unit1, unit2));
            } else if (allUnit.size() == 3) {
                unit1 = allUnit.get(0);
                unit2 = allUnit.get(1);
                unit3 = allUnit.get(2);
                printLine.append(addThree(unit1, unit2, unit3));
            }

        }

        Log.d("SETT", new String(Tools.str2Bcd(printLine.toString())));
        return printLine.toString();
    }

    private String addOne(IPage.ILine.IUnit unit) {
        boolean isNormal;
        int strLen;
        byte Header;

        if (unit.getText() == null) {
            return "";
        }

        if (unit.getFontSize() > 20) {
            isNormal = true;
            strLen = NORMAL_SIZE_LEN;
        } else {
            isNormal = false;
            strLen = HALF_SIZE_LEN;
        }

        if (isNormal) {
            Header = D7;
        } else {
            Header = CE;
        }


        String unitText = unit.getText().substring(0, Math.min(unit.getText().length(), strLen));

        // TODO:
        // if string '-' or '=' just cut

        // TODO check Gravity
        String strFormat;

        int left = 0;
        if (unit.getGravity() == Gravity.CENTER
                && unitText.length() < (strLen - 2)) {
            left = (strLen - unitText.length()) / 2;
            strFormat = "%" + left + "s%n";
        } else {
            strFormat = "%s%n";
        }


        String result = null;
        try {
            result = Tools.bcd2Str(String.format(strFormat, EReceiptUtils.StringPadding(unitText, left + unitText.length(), " ", Convert.EPaddingPosition.PADDING_LEFT)).getBytes("UTF-8"));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        result = String.format("%x", Header) + result;

        return result;
    }

    private String addTwo(IPage.ILine.IUnit unit1
            , IPage.ILine.IUnit unit2) {
        // Check Big/Small
        boolean isNormal;
        int strLen;
        byte Header;
        if (unit1.getFontSize() > 20
                || unit2.getFontSize() > 20) {
            isNormal = true;
            strLen = NORMAL_SIZE_LEN;
        } else {
            isNormal = false;
            strLen = HALF_SIZE_LEN;
        }

        String strFormat;
        if (isNormal) {
            Header = D7;
            strFormat = "%-11s%11s%n";
        } else {
            Header = CE;
            strFormat = "%-22s%22s%n";
        }

        if (unit2.getGravity() != Gravity.END) {
            strFormat = "%s%s%n";
        }

        String result = null;
        try {
            result = Tools.bcd2Str(String.format(strFormat, unit1.getText(), unit2.getText()).getBytes("UTF-8"));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        result = String.format("%x", Header) + result;

        return result;
    }

    private String addThree(IPage.ILine.IUnit unit1
            , IPage.ILine.IUnit unit2
            , IPage.ILine.IUnit unit3) {

        boolean isNormal;
        int strLen;
        byte Header;
        if (unit1.getFontSize() > 20
                && unit2.getFontSize() > 20
                && unit3.getFontSize() > 20
                && !(unit1.getScaleX() < 1)
                && !(unit2.getScaleX() < 1)
                && !(unit3.getScaleX() < 1)) {
            isNormal = true;
            strLen = NORMAL_SIZE_LEN;
        } else {
            isNormal = false;
            strLen = HALF_SIZE_LEN;
        }

        String strFormat;
        if (isNormal) {
            Header = D7;
            strFormat = "%-8s%-8s%7s%n";
        } else {
            Header = CE;
            strFormat = "%-20s%-14s%10s%n";
        }

        //System.out.println("   ********************L********R");
        String result = null;
        try {
            result = Tools.bcd2Str(String.format(strFormat, unit1.getText(), unit2.getText(), unit3.getText()).getBytes("UTF-8"));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        result = String.format("%x", Header) + result;

        return result;
    }

    public void Append(IPage page) {
        settSlip += pageToVfErm(page);
    }

    public void Reset() {
        settSlip = "";
    }

    public String getSettleSlip() {
        return settSlip;
    }

    public String test() {
        IPage page = Device.generatePage(true);

        /*Header*/
        // title

        page.addLine()
                .addUnit(page.createUnit()
                        .setBitmap(Component.getImageFromInternalFile(Constants.SLIP_LOGO_NAME))
                        .setGravity(Gravity.CENTER));
        page.addLine().addUnit(page.createUnit().setText(" "));


        //merchant name
        SysParam sysParam = FinancialApplication.getSysParam();
        String merName = sysParam.get(SysParam.StringParam.EDC_MERCHANT_NAME_EN);
        String merAddress = sysParam.get(SysParam.StringParam.EDC_MERCHANT_ADDRESS);
        String merAddress1 = sysParam.get(SysParam.StringParam.EDC_MERCHANT_ADDRESS1);
        page.addLine()
                .addUnit(page.createUnit()
                        .setText(merName)
                        .setFontSize(FONT_NORMAL_26)
                        .setGravity(Gravity.CENTER));
        page.addLine()
                .addUnit(page.createUnit()
                        .setText(merAddress)
                        .setFontSize(FONT_NORMAL_26)
                        .setGravity(Gravity.CENTER));
        page.addLine()
                .addUnit(page.createUnit()
                        .setText(merAddress1)
                        .setFontSize(FONT_NORMAL_26)
                        .setGravity(Gravity.CENTER));
        page.addLine().addUnit(page.createUnit().setText(" "));

        // terminal ID/operator ID
        page.addLine()
                .addUnit(page.createUnit()
                        .setText("TID")
                        .setFontSize(FONT_NORMAL))
                .addUnit(page.createUnit()
                        .setText(": " + "123445667")
                        .setWeight(3.0f)
                        .setGravity(Gravity.LEFT)
                );

        // merchant ID
        page.addLine()
                .addUnit(page.createUnit()
                        .setText("MID")
                        .setFontSize(FONT_NORMAL))
                .addUnit(page.createUnit()
                        .setText(": " + "1233423423423424")
                        .setGravity(Gravity.LEFT)
                        .setWeight(3.0f));

        page.addLine()
                .addUnit(page.createUnit()
                        .setText("TEST 1")
                        .setFontSize(FONT_NORMAL_26)
                        .setGravity(Gravity.START))
                .addUnit(page.createUnit()
                        .setText("TEST 2")
                        .setFontSize(FONT_NORMAL_26)
                        .setGravity(Gravity.END));

        page.addLine().addUnit(page.createUnit()
                .setText(Utils.getString(R.string.receipt_double_line))
                .setGravity(Gravity.CENTER));

        // Total Sale
        long totalSaleNum = 3;
        long totalSaleAmt = 10000;
        float halfWidthSize = 0.6f;
        float fullSizeOneLine = 1.2f;
        float fullSizeDoubleLine = 0.8f;
        page.addLine()
                .addUnit(page.createUnit()
                        .setText("TOTAL SALES")
                        .setWeight(2.0f)
                        .setScaleX(halfWidthSize)
                        .setFontSize(FONT_NORMAL))
                .addUnit(page.createUnit()
                        .setText(Component.getPaddedNumber(totalSaleNum, 3))
                        .setWeight(1.0f)
                        .setScaleX(halfWidthSize)
                        .setGravity(Gravity.CENTER)
                        .setFontSize(FONT_NORMAL))
                .addUnit(page.createUnit()
                        .setText(CurrencyConverter.convert(totalSaleAmt))
                        .setWeight(2.0f)
                        .setScaleX(halfWidthSize)
                        .setFontSize(FONT_NORMAL)
                        .setGravity(Gravity.END));
        Append(page);
        return settSlip;
    }

}

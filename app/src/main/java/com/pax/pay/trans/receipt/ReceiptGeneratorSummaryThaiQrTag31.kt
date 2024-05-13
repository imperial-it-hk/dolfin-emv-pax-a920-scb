package com.pax.pay.trans.receipt

import android.graphics.Bitmap
import android.graphics.Typeface
import android.view.Gravity
import com.pax.device.Device
import com.pax.edc.R
import com.pax.glwrapper.imgprocessing.IImgProcessing
import com.pax.glwrapper.page.IPage
import com.pax.pay.app.FinancialApplication
import com.pax.pay.trans.model.ETransType
import com.pax.pay.trans.model.TransData
import com.pax.pay.trans.model.TransTotal
import com.pax.pay.utils.CurrencyConverter
import com.pax.pay.utils.Utils
import java.time.format.TextStyle

internal class ReceiptGeneratorSummaryThaiQrTag31(val total: TransTotal, val title: String, val isShowHeader: Boolean, val isShowContentBody: Boolean, val isShowFooter: Boolean, val sourceOfFund: String, val transList: List<TransData>) : AReceiptGenerator(total, title) {

    val widthPixel : Int = 384
    val lineDouble = Utils.getString(R.string.receipt_double_line)
    val lineSingle = Utils.getString(R.string.receipt_one_line)

    override fun generateBitmaps(): List<Bitmap> {
        val bitmaps = ArrayList<Bitmap>()

        try {
            val imgProcessing = FinancialApplication.getGl().imgProcessing

            // generate HEADER
            if (isShowHeader) { bitmaps.add(imgProcessing.pageToBitmap(super.generateHeader(), widthPixel)) }

            // generate BODY CONTENT
            if (isShowContentBody) { generateContent(bitmaps, sourceOfFund) }

            // generate FOOTER
            if (isShowFooter) { bitmaps.add(imgProcessing.pageToBitmap(generateFooter(), widthPixel)) }

        } catch (ex: Exception) {
            ex.printStackTrace()
        }

        return bitmaps
    }

    fun generateContent(bitmaps: ArrayList<Bitmap>, sof: String)  {
        val imgProcessing = FinancialApplication.getGl().imgProcessing
        val page = generatePageData(sof)
        bitmaps.add(imgProcessing.pageToBitmap(page, widthPixel))
    }

    val STR_SALE_TRANS_COUNT = "SALE_TRANS_COUNT"
    val STR_SALE_TRANS_AMOUNT = "SALE_AMOUNT"
    val STR_VOID_TRANS_COUNT = "VOID_TRANS_COUNT"
    val STR_VOID_TRANS_AMOUNT = "VOID_AMOUNT"
    val STR_TOTAL_TRANS_COUNT = "TOTAL_TRANS_COUNT"
    val STR_TOTAL_TRANS_AMOUNT = "TOTAL_AMOUNT"

    fun summaryBySourceOfFund(transList: List<TransData>) : HashMap<String, Long>  {
        val transMap = transList.groupBy { it.transType }

        // set default to hashmap
        val mapper = HashMap<String, Long>()
        mapper.put(STR_SALE_TRANS_COUNT, 0)
        mapper.put(STR_SALE_TRANS_AMOUNT, 0)
        mapper.put(STR_VOID_TRANS_COUNT, 0)
        mapper.put(STR_VOID_TRANS_AMOUNT, 0)
        mapper.put(STR_TOTAL_TRANS_COUNT, 0)
        mapper.put(STR_TOTAL_TRANS_AMOUNT, 0)

        if (transList.size > 0) {
            var sale_trans_count  : Long = 0
            var sale_trans_amount : Long = 0
            var void_trans_count  : Long = 0
            var void_trans_amount : Long = 0
            for (type:ETransType in transMap.keys) {
                val lists = transMap.get(type)
                lists?.let {
                    for (targetRecord: TransData in lists) {
                        if (type.equals(ETransType.QR_INQUIRY) || type.equals(ETransType.QR_VERIFY_PAY_SLIP)) {
                            sale_trans_count +=1
                            sale_trans_amount += targetRecord.amount.toLong()
                        } else if (type.equals(ETransType.QR_VOID_KPLUS)) {
                            void_trans_count += 1
                            void_trans_amount += targetRecord.amount.toLong()
                        }
                    }
                }
            }
            mapper[STR_SALE_TRANS_COUNT] = sale_trans_count
            mapper[STR_SALE_TRANS_AMOUNT] = sale_trans_amount
            mapper[STR_VOID_TRANS_COUNT] = void_trans_count
            mapper[STR_VOID_TRANS_AMOUNT] = void_trans_amount * (-1)
            mapper[STR_TOTAL_TRANS_COUNT] = sale_trans_count + void_trans_count
            mapper[STR_TOTAL_TRANS_AMOUNT] = sale_trans_amount + (void_trans_amount * (-1))
        }

        return mapper
    }

    override fun generateFooter(): IPage {
        return generatePageData(Utils.getString(R.string.grand_total))
    }

    fun generatePageData(printTitle: String) : IPage {
        val page = Device.generatePage()

        val fontSizeHeading : Int = if(isShowFooter) FONT_BIG else FONT_NORMAL
        val fontStyle : Int = if(isShowFooter) Typeface.BOLD else Typeface.NORMAL

        page.addLine().addUnit(page.createUnit().setGravity(Gravity.CENTER).setFontSize(fontSizeHeading).setTextStyle(fontStyle).setText(printTitle))
        page.addLine().addUnit(page.createUnit().setGravity(Gravity.CENTER).setFontSize(FONT_NORMAL).setText(lineDouble))

        val summaryInfo = summaryBySourceOfFund(transList)
        // SALE
        page.addLine().adjustTopSpace(1).addUnit(page.createUnit().setGravity(Gravity.START).setFontSize(FONT_NORMAL).setWeight(3.0f).setText(Utils.getString(R.string.trans_sale)))
            .addUnit(page.createUnit().setGravity(Gravity.END).setFontSize(FONT_NORMAL).setWeight(3.0f).setText(summaryInfo[STR_SALE_TRANS_COUNT].toString()))
        page.addLine().addUnit(page.createUnit().setGravity(Gravity.END).setFontSize(FONT_NORMAL).setText(CurrencyConverter.convert(summaryInfo[STR_SALE_TRANS_AMOUNT]) ))

        // VOID
        page.addLine().adjustTopSpace(1).addUnit(page.createUnit().setGravity(Gravity.START).setFontSize(FONT_NORMAL).setText(Utils.getString(R.string.trans_void)))
            .addUnit(page.createUnit().setGravity(Gravity.END).setFontSize(FONT_NORMAL).setWeight(3.0f).setText(summaryInfo[STR_VOID_TRANS_COUNT].toString()))
        page.addLine().addUnit(page.createUnit().setGravity(Gravity.END).setFontSize(FONT_NORMAL).setText(CurrencyConverter.convert(summaryInfo[STR_VOID_TRANS_AMOUNT]) ))

        // SECTION SEPERATOR
        page.addLine().addUnit(page.createUnit().setGravity(Gravity.CENTER).setFontSize(FONT_NORMAL).setText(lineSingle))

        // TOTAL

        page.addLine().adjustTopSpace(1).addUnit(page.createUnit().setGravity(Gravity.START).setFontSize(FONT_NORMAL).setText(Utils.getString(R.string.receipt_amount_total)))
            .addUnit(page.createUnit().setGravity(Gravity.END).setFontSize(FONT_NORMAL).setWeight(3.0f).setText(summaryInfo[STR_SALE_TRANS_COUNT].toString()))
        page.addLine().addUnit(page.createUnit().setGravity(Gravity.END).setFontSize(FONT_NORMAL).setText(CurrencyConverter.convert(summaryInfo[STR_SALE_TRANS_AMOUNT]) ))

        // SOURCE-OF-FUND SEPERATOR
        page.addLine().addUnit(page.createUnit().setGravity(Gravity.CENTER).setFontSize(FONT_NORMAL).setText(lineDouble))

        if (isShowFooter) {
            page.addLine().addUnit(page.createUnit().setGravity(Gravity.CENTER).setFontSize(FONT_NORMAL).setTextStyle(Typeface.BOLD).setText(Utils.getString(R.string.receipt_end_of_report)))
        }


        return page
    }

    override fun generateBitmap(): Bitmap? { return null }
    override fun generateString(): String? { return null }

}
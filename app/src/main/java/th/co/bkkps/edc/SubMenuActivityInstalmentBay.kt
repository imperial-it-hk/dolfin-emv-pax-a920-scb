package th.co.bkkps.edc

import com.pax.edc.R
import com.pax.pay.menu.BaseMenuActivity
import com.pax.pay.trans.InstalmentBayTrans
import com.pax.pay.trans.action.ActionSearchCard.SearchMode.*
import com.pax.view.MenuPage
import kotlin.experimental.or

class SubMenuActivityInstalmentBay : BaseMenuActivity() {
    override fun createMenuPage(): MenuPage {
        val builder = MenuPage.Builder(this@SubMenuActivityInstalmentBay, 6, 2)
            .addTransItem(
                getString(R.string.menu_specific_msc),
                R.drawable.app_sale,
                InstalmentBayTrans(applicationContext, (SWIPE or INSERT or WAVE or KEYIN), true, null, true)
            )
            .addTransItem(
                getString(R.string.menu_non_specific_msc),
                R.drawable.app_sale,
                InstalmentBayTrans(applicationContext, (SWIPE or INSERT or WAVE or KEYIN), true, null, false)
            )
        return builder.create()
    }

    /*companion object {
        private const val TAG: String = "SubMenuActivityInstalmentBay"
    }

    override fun getLayoutId(): Int {
        return R.layout.activity_sub_menu_instalment_bay
    }
    override fun initViews() {
    }

    override fun setListeners() {
    }

    override fun loadParam() {
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sub_menu_instalment_bay)

        imageButtonSpMsc.setOnClickListener{
            val mSaleInstalmentBayTransBPS = InstalmentBayTrans(
                applicationContext,
                (SWIPE or INSERT or WAVE or KEYIN),
                true, null, true
            )
            mSaleInstalmentBayTransBPS.execute()
        }

        imageButtonNonSpMsc.setOnClickListener{
            val mSaleInstalmentBayTransBPS = InstalmentBayTrans(
                applicationContext,
                (SWIPE or INSERT or WAVE or KEYIN),
                true, null, false
            )
            mSaleInstalmentBayTransBPS.execute()
        }
    }*/
}
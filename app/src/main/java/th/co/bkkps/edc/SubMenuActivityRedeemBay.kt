package th.co.bkkps.edc

import com.pax.edc.R
import com.pax.pay.menu.BaseMenuActivity
import com.pax.pay.trans.RedeemBayTrans
import com.pax.pay.trans.action.ActionSearchCard.SearchMode.*
import com.pax.view.MenuPage
import kotlin.experimental.or

class SubMenuActivityRedeemBay : BaseMenuActivity() {
    override fun createMenuPage(): MenuPage {
        val builder = MenuPage.Builder(this@SubMenuActivityRedeemBay, 6, 2)
            .addTransItem(
                getString(R.string.menu_full_redeem),
                R.drawable.app_sale,
                RedeemBayTrans(applicationContext, (SWIPE or INSERT or WAVE or KEYIN), true, null, 1)
            )
            .addTransItem(
                getString(R.string.menu_partial_redeem),
                R.drawable.app_sale,
                RedeemBayTrans(applicationContext, (SWIPE or INSERT or WAVE or KEYIN), true, null, 2)
            )
            .addTransItem(
                getString(R.string.menu_catalogue_redeem),
                R.drawable.app_sale,
                RedeemBayTrans(applicationContext, (SWIPE or INSERT or WAVE or KEYIN), true, null, 3)
            )
        return builder.create()
    }

    /*companion object {
        private const val TAG: String = "SubMenuActivityRedeemBay"
    }

    override fun getLayoutId(): Int {
        return R.layout.activity_sub_menu_redeem_bay
    }
    override fun initViews() {
    }

    override fun setListeners() {
    }

    override fun loadParam() {
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sub_menu_redeem_bay)

        imageButtonFullRedeem.setOnClickListener{
            val mSaleRedeemBayTransBPS = RedeemBayTrans(
                applicationContext,
                (SWIPE or INSERT or WAVE or KEYIN),
                true, null, 1
            )
            mSaleRedeemBayTransBPS.execute()
        }

        imageButtonPartialRedeem.setOnClickListener{
            val mSaleRedeemBayTransBPS = RedeemBayTrans(
                applicationContext,
                (SWIPE or INSERT or WAVE or KEYIN),
                true, null, 2
            )
            mSaleRedeemBayTransBPS.execute()
        }

        imageButtonCatalogueRedeem.setOnClickListener{
            val mSaleRedeemBayTransBPS = RedeemBayTrans(
                applicationContext,
                (SWIPE or INSERT or WAVE or KEYIN),
                true, null, 3
            )
            mSaleRedeemBayTransBPS.execute()
        }
    }*/
}
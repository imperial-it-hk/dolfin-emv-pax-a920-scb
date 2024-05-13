package th.co.bkkps.scbapi.menu

import android.content.Intent
import com.pax.abl.core.AAction
import com.pax.edc.R
import com.pax.pay.MainActivity
import com.pax.pay.app.ActivityStack
import com.pax.pay.app.FinancialApplication
import com.pax.pay.constant.Constants
import com.pax.pay.menu.BaseMenuActivity
import com.pax.pay.utils.Utils
import com.pax.view.MenuPage
import th.co.bkkps.scbapi.ScbIppService
import th.co.bkkps.scbapi.trans.ScbVoidTran

class ScbMenuActivity: BaseMenuActivity() {
    override fun createMenuPage(): MenuPage? {

        val builder = MenuPage.Builder(this, 6, 2)

            if (Utils.isEnableScbIpp()) {
                builder.addMenuItem(
                    getString(R.string.menu_scb_ipp),
                    R.drawable.app_sale,
                    ScbIppMenuActivity::class.java
                )
            }

            if (Utils.isEnableScbRedeem()) {
                builder.addMenuItem(
                    getString(R.string.menu_scb_redeem),
                    R.drawable.app_sale,
                    ScbRedeemMenuActivity::class.java
                )
            }

            builder.addTransItem(
                getString(R.string.menu_void), R.drawable.app_void,
                ScbVoidTran(this, null, null)
            )
            .addActionItem(getString(R.string.menu_report), R.drawable.app_query, doScbHistory())

        return builder.create()
    }

    private fun doScbHistory(): AAction? {
        return ScbIppService.executeHistoryMenu(this, tickTimer)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        ActivityStack.getInstance().popTo(MainActivity::class.java)
    }
}
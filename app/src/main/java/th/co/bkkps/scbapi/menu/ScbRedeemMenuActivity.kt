package th.co.bkkps.scbapi.menu

import com.pax.edc.R
import com.pax.edc.opensdk.TransResult
import com.pax.pay.constant.Constants
import com.pax.pay.menu.BaseMenuActivity
import com.pax.pay.trans.component.Component
import com.pax.pay.utils.TransResultUtils
import com.pax.view.MenuPage
import th.co.bkkps.scbapi.trans.ScbRedeemInqTran
import th.co.bkkps.scbapi.trans.ScbRedeemSaleTran

class ScbRedeemMenuActivity: BaseMenuActivity() {

    override fun onResume() {
        super.onResume()

        if (Component.chkSettlementStatus(Constants.ACQ_SCB_REDEEM)) {
            showErrorDialog(this, TransResultUtils.getMessage(TransResult.ERR_SETTLE_NOT_COMPLETED))
            return
        }
    }

    override fun createMenuPage(): MenuPage? {
        val builder = MenuPage.Builder(this, 6, 2)
            .addTransItem(
                getString(R.string.menu_scb_redeem_4), R.drawable.app_sale,
                ScbRedeemSaleTran(this, null, ScbRedeemSaleTran.SPECIFIC_PRODUCT)
            )
            .addTransItem(
                getString(R.string.menu_scb_redeem_0), R.drawable.app_query,
                ScbRedeemInqTran(this, null)
            )

        return builder.create()
    }
}
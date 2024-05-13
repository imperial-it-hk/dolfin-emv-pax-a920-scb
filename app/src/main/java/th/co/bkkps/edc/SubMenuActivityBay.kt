package th.co.bkkps.edc

import com.pax.abl.core.AAction
import com.pax.abl.core.ActionResult
import com.pax.edc.R
import com.pax.edc.opensdk.TransResult
import com.pax.pay.constant.Constants
import com.pax.pay.menu.BaseMenuActivity
import com.pax.pay.trans.action.ActionGetT1CMemberID
import com.pax.pay.trans.action.ActionSearchCard
import com.pax.pay.trans.component.Component
import com.pax.pay.trans.model.AcqManager
import com.pax.pay.trans.model.TransData
import com.pax.view.MenuPage

class SubMenuActivityBay : BaseMenuActivity() {
    override fun onResume() {
        super.onResume()

        val acq = AcqManager.getInstance().findActiveAcquirer(Constants.ACQ_BAY_INSTALLMENT)
        if (acq == null || !acq.isEnable) {
            showMsgNotAllowed(this@SubMenuActivityBay)
        }
    }

    override fun createMenuPage(): MenuPage {
        val builder = MenuPage.Builder(this@SubMenuActivityBay, 8, 3)
        builder.addMenuItem(getString(R.string.menu_instalment_bay), R.drawable.app_sale, SubMenuActivityInstalmentBay::class.java);
        //builder.addMenuItem(getString(R.string.menu_redeem_bay), R.drawable.app_sale, SubMenuActivityRedeemBay::class.java)
        //builder.addActionItem("Get T1C MemeberID", R.drawable.app_sale, GetT1C());
        return builder.create()
    }

    private fun GetT1C(): AAction? {
        val transData: TransData = Component.transInit()
        val searchCardAction: ActionSearchCard?
        searchCardAction = ActionSearchCard(object : AAction.ActionStartListener {
            override fun onStart(action: AAction?) {
                (action as ActionSearchCard).setParam(
                        this@SubMenuActivityBay, getString(R.string.trans_get_t1c), 0x02, "000000000001",
                        null, "", transData, 9
                )
            }
        });
        searchCardAction.setEndListener(object : AAction.ActionEndListener {
            override fun onEnd(action: AAction?, result: ActionResult?) {
                if (result!!.ret == TransResult.SUCC) {
                    var getT1C_action: ActionGetT1CMemberID? = null
                    getT1C_action = ActionGetT1CMemberID(object : AAction.ActionStartListener {
                        override fun onStart(action: AAction?) {
                            (action as ActionGetT1CMemberID).setParam(this@SubMenuActivityBay, transData);
                        }
                    })
                    getT1C_action.execute()
                }
            }

        })

        return searchCardAction as AAction;
    }
}
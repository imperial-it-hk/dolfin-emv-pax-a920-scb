package th.co.bkkps.kcheckidAPI.menu

import com.pax.edc.R
import com.pax.pay.constant.Constants
import com.pax.pay.menu.BaseMenuActivity
import com.pax.pay.trans.model.AcqManager
import com.pax.pay.trans.model.ETransType
import com.pax.view.MenuPage
import th.co.bkkps.kcheckidAPI.trans.KCheckIDTrans

class KCheckIDMenuActivity : BaseMenuActivity() {

    override fun onResume() {
        super.onResume()

        val acq = AcqManager.getInstance().findActiveAcquirer(Constants.ACQ_KCHECKID)
        if (acq == null || !acq.isEnable) {
            showMsgNotAllowed(this@KCheckIDMenuActivity)
        }
    }

    override fun createMenuPage(): MenuPage {
        val builder = MenuPage.Builder(this, 9, 3)
        builder.addTransItem( getString(R.string.menu_ekyc_kcheckid_verify), R.drawable.idcard_verify, KCheckIDTrans(this, ETransType.KCHECKID_DUMMY, KCheckIDTrans.Companion.ProcessType.VERIFY, null))
        builder.addTransItem( getString(R.string.menu_ekyc_kcheckid_inquiry), R.drawable.verify_status, KCheckIDTrans(this, ETransType.KCHECKID_DUMMY, KCheckIDTrans.Companion.ProcessType.INQUIRY, null))

        return builder.create()
    }
}
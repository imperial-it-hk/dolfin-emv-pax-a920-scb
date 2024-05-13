package th.co.bkkps.edc.receiver

import android.content.Context
import android.content.Intent
import android.os.SystemClock
import com.pax.device.Device
import com.pax.pay.MainActivity
import com.pax.pay.SettlementRegisterActivity
import com.pax.pay.app.ActivityStack
import com.pax.pay.app.FinancialApplication
import com.pax.pay.constant.Constants
import com.pax.pay.record.Printer
import com.pax.settings.SysParam
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import th.co.bkkps.amexapi.AmexTransService
import th.co.bkkps.dofinAPI.DolfinApi
import th.co.bkkps.edc.receiver.activity.ForceSettleActivity
import th.co.bkkps.edc.receiver.process.AutoSettleAlarmProcess
import th.co.bkkps.edc.receiver.process.ForceSettleAlarmProcess
import th.co.bkkps.edc.receiver.process.SettleAlarmProcess
import th.co.bkkps.kcheckidAPI.KCheckIDService
import th.co.bkkps.scbapi.ScbIppService
import th.co.bkkps.utils.Log

class ForceSettleAlarmReceiver : SettleBroadcastReceiver() {

    override fun runProcess(context: Context) {
        GlobalScope.async {
            val process = async { executeForceSettleProcess(context) }
            process.await()
        }
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        val edcConfigSettlementMode :String = FinancialApplication.getSysParam().get(SysParam.StringParam.EDC_SETTLEMENT_MODE, SettleAlarmProcess.SettlementMode.DISABLE.value)
        if (!edcConfigSettlementMode.equals(SettleAlarmProcess.SettlementMode.FORCE_SETTLE.value)) {
            return
        }

        val currMillis = System.currentTimeMillis()
        intent?.let {
            if (it.action.equals("FORCE_SETTLE")) {
                isFromDetected = it.getBooleanExtra("IS_FROM_DETECTED", false)
                val acqList : String = it.getStringExtra(SettlementRegisterActivity.param_name)!!
                val acqNames = (acqList.split("|")).toCollection(ArrayList())
                val printOnExecution = FinancialApplication.getSysParam().get(SysParam.BooleanParam.EDC_ENABLE_PRINT_ON_EXECUTE_SETTLE, false)
                if (printOnExecution) Printer.printAutoSettlementOnWakeUp(ActivityStack.getInstance().top(), acqNames, "FORCE SETTLEMENT")
                Log.d(ForceSettleAlarmProcess.TAG, "  >>  HOST [${acqNames}] activate   on ${Device.getTime("dd/MM/yyyy HH:mm:ss")}")

                // set next day auto-settlement
                val itemIndex : String = it.getStringExtra(SettlementRegisterActivity.param_item_index)!!
                try {
                    Log.d(ForceSettleAlarmProcess.TAG, "  >>  start set next-day-force-settlement")
                    val settleRegActivity = SettlementRegisterActivity()
                    settleRegActivity.setNextDaySettlement(context!!, acqList, itemIndex, currMillis)
                    Log.d(ForceSettleAlarmProcess.TAG, "  >>  set next day by completely")
                } catch (ex: Exception) {
                    Log.d(ForceSettleAlarmProcess.TAG, "  >>  failed to set next day\n" + ex.message)
                    ex.printStackTrace()
                }

                acqNames?.let{ acq ->
                    if (acq.size > 0 ){
                        wakeUpList.addAll(acqNames)
                        runProcess(context!!)
                    }
                }
            }
        }
    }

    private suspend fun executeForceSettleProcess(context: Context) {
        Log.d(ForceSettleAlarmProcess.TAG, ">> run execute --->")
        Log.d(ForceSettleAlarmProcess.TAG, "\t\tisSyncLock = $isExecuting")
        Log.d(ForceSettleAlarmProcess.TAG, "\t\twaitUserPayment = $waitUserCompletePayment")

        if (waitUserCompletePayment) {
            Log.d(ForceSettleAlarmProcess.TAG, "wait execute in next 10 seconds")
            SystemClock.sleep(10000L)
            executeForceSettleProcess(context)
            return
        }

        if (!isExecuting) {
            if (wakeUpList.isNotEmpty()) {
//                checkAndRemoveNotInstallMultiApp()
                val activeAcquirerList : ArrayList<String> = wakeUpList
                val dispAcqName = getAcquirerNames(activeAcquirerList)

                // display notification before run auto-settlement 10 seconds.
                makeNotification("Force-Settle : $dispAcqName", "ForceSettle will execute in ${SettleAlarmProcess.TIME_SECONDS_NOTIFY/1000L} sec, Please wait.")
                Log.d(ForceSettleAlarmProcess.TAG, "\t\tExecute ForceSettle : [$dispAcqName]")

                delay(SettleAlarmProcess.TIME_SECONDS_NOTIFY)
                if (waitUserCompletePayment) {
                    executeForceSettleProcess(context)
                    return
                }

                isExecuting = true
                Log.d(ForceSettleAlarmProcess.TAG, "\t\t[$dispAcqName] ===> IS-NOW-EXECUTING = ON")

                val shadowContext : Context? = ActivityStack.getInstance().top()
                shadowContext?.let{
                    val intent = Intent(it, ForceSettleActivity::class.java)
                    intent.putStringArrayListExtra("WAKE_UP_LIST", wakeUpList)
                    intent.putExtra("IS_FROM_DETECTED", isFromDetected)
                    context.startActivity(intent)
                }
            } else {
                Log.d(ForceSettleAlarmProcess.TAG, "No any Execution needed")
                if (ActivityStack.getInstance().exists(MainActivity::class.java)) {
                    ActivityStack.getInstance().popTo(MainActivity::class.java)
                } else {
                    val shadowContext : Context? = ActivityStack.getInstance().top()
                    shadowContext?.let{
                        val intent = Intent(it, MainActivity::class.java)
                        it.startActivity(intent)
                    }
                }

                return
            }
        } else {
            Log.d(ForceSettleAlarmProcess.TAG, "wait execute in next 10 seconds")
            executeForceSettleProcess(context)
            return
        }
    }

    private fun checkAndRemoveNotInstallMultiApp() {
        if (wakeUpList.size > 0) {
            try {
                val multiAppAcqList : ArrayList<String> = arrayListOf(Constants.ACQ_DOLFIN, Constants.ACQ_SCB_IPP, Constants.ACQ_SCB_REDEEM, Constants.ACQ_AMEX, Constants.ACQ_KCHECKID)
                for (mAppAcqName : String in multiAppAcqList) {
                    if ((mAppAcqName == Constants.ACQ_DOLFIN && !DolfinApi.getInstance().dolfinServiceBinded)
                        || ((mAppAcqName == Constants.ACQ_SCB_IPP || mAppAcqName == Constants.ACQ_SCB_REDEEM) && !ScbIppService.isSCBInstalled(ActivityStack.getInstance().top()))
                        || (mAppAcqName == Constants.ACQ_AMEX && !AmexTransService.isAmexAppInstalled(ActivityStack.getInstance().top()))
                        || (mAppAcqName == Constants.ACQ_KCHECKID && !KCheckIDService.isKCheckIDInstalled(ActivityStack.getInstance().top())) ) {
                        wakeUpList.removeAll { acq -> acq == mAppAcqName }
                    }
                }
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
    }
}
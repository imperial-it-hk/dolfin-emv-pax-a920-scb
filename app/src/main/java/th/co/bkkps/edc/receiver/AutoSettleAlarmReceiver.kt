package th.co.bkkps.edc.receiver

import android.app.Activity
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import com.pax.abl.core.ATransaction
import com.pax.abl.core.ActionResult
import com.pax.device.Device
import com.pax.pay.MainActivity
import com.pax.pay.SettlementRegisterActivity
import com.pax.pay.app.ActivityStack
import com.pax.pay.app.FinancialApplication
import com.pax.pay.constant.Constants
import com.pax.pay.record.Printer
import com.pax.pay.trans.SettleTrans
import com.pax.pay.trans.TransContext
import com.pax.pay.trans.action.activity.AutoSettlementActivity
import com.pax.settings.SysParam
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import th.co.bkkps.amexapi.AmexTransService
import th.co.bkkps.dofinAPI.DolfinApi
import th.co.bkkps.edc.receiver.process.AlarmProcess
import th.co.bkkps.edc.receiver.process.AutoSettleAlarmProcess
import th.co.bkkps.edc.receiver.process.SettleAlarmProcess
import th.co.bkkps.edc.receiver.process.SettleAlarmProcess.Companion.TIME_SECONDS_NOTIFY
import th.co.bkkps.kcheckidAPI.KCheckIDService
import th.co.bkkps.scbapi.ScbIppService
import th.co.bkkps.utils.Log

class AutoSettleAlarmReceiver : SettleBroadcastReceiver() {

    override fun runProcess(context: Context) {
        GlobalScope.async {
            val deffered = async { executeSettlementProcess(context) }
            deffered.await()
        }
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        val edcConfigSettlementMode :String = FinancialApplication.getSysParam().get(SysParam.StringParam.EDC_SETTLEMENT_MODE, SettleAlarmProcess.SettlementMode.DISABLE.value)
        if (!edcConfigSettlementMode.equals(SettleAlarmProcess.SettlementMode.AUTO_SETTLE.value)) {
            return
        }

        val currMillis = System.currentTimeMillis()
        intent?.let {
            if (it.action.equals("AUTO_SETTLE")) {

                val acqList : String = it.getStringExtra(SettlementRegisterActivity.param_name)!!
                val acqNames = (acqList.split("|")).toCollection(ArrayList())
                val printOnExecution = FinancialApplication.getSysParam().get(SysParam.BooleanParam.EDC_ENABLE_PRINT_ON_EXECUTE_SETTLE, false)
                if (printOnExecution) {Printer.printAutoSettlementOnWakeUp(ActivityStack.getInstance().top(), acqNames, "AUTO SETTLEMENT");}
                Log.d(AutoSettleAlarmProcess.TAG, "  >>  HOST [${acqNames}] activate   on ${Device.getTime("dd/MM/yyyy HH:mm:ss")}")

                // set next day auto-settlement
                val itemIndex : String = it.getStringExtra(SettlementRegisterActivity.param_item_index)!!
                try {
                    Log.d(AutoSettleAlarmProcess.TAG, "  >>  start set next-day-auto-settlement")
                    val settleRegActivity = SettlementRegisterActivity()
                    settleRegActivity.setNextDaySettlement(context!!, acqList, itemIndex, currMillis)
                    Log.d(AutoSettleAlarmProcess.TAG, "  >>  set next day by completely")
                } catch (ex: Exception) {
                    Log.d(AutoSettleAlarmProcess.TAG, "  >>  failed to set next day\n" + ex.message)
                    ex.printStackTrace()
                }

                acqNames?.let{
                    if (it.size > 0 ){
                        wakeUpList.addAll(acqNames)
                        runProcess(ActivityStack.getInstance().top())
                    }
                }
            }
        }
    }

    private fun executeSettlementProcess(context: Context) {
        Log.d(AutoSettleAlarmProcess.TAG, ">> run execute --->")
        Log.d(AutoSettleAlarmProcess.TAG, "\t\tisSyncLock = $isExecuting")
        Log.d(AutoSettleAlarmProcess.TAG, "\t\twaitUserPayment = $waitUserCompletePayment")

        if (waitUserCompletePayment) {
            Log.d(AutoSettleAlarmProcess.TAG, "wait execute in next 10 seconds")
            SystemClock.sleep(10000L)
            executeSettlementProcess(context)
            return
        }

        if (!isExecuting) {
            if (wakeUpList.size>0) {
                checkAndRemoveNotInstallMultiApp()
                val activeAcquirerList : ArrayList<String> = wakeUpList
                val dispAcqName = getAcquirerNames(activeAcquirerList)

                // display notification before run auto-settlement 10 seconds.
                makeNotification("Auto-Settle : $dispAcqName", "AutoSettle will execute in ${TIME_SECONDS_NOTIFY/1000L} sec, Please wait.")

                val intent = Intent(context, AutoSettlementActivity::class.java)
                intent.putStringArrayListExtra(AutoSettlementActivity.PARAM_NAME_WAKEUPLIST, wakeUpList)
                context.startActivity(intent)

                //SystemClock.sleep(AutoSettleAlarmProcess.TIME_SECONDS_NOTIFY)
//                Log.d(AutoSettleAlarmProcess.TAG, "\t\tExecute AutoSettle : [$dispAcqName]")
//                val endlistener = object: ATransaction.TransEndListener {
//                    override fun onEnd(result: ActionResult?) {
//                        isExecuting=false
//                        Log.d(AutoSettleAlarmProcess.TAG, "\t\t[$dispAcqName] ===> IS-NOW-EXECUTING = OFF")
//                        wakeUpList = removeFromList(activeAcquirerList, wakeUpList)
//                        Log.d(AutoSettleAlarmProcess.TAG, "\t\tgoback to MainActivity" )
//                        openMainActivity(FinancialApplication.getApp().applicationContext)
////                        if (wakeUpList.size > 0) {
////                            try {
////                                Log.d(AutoSettleAlarmProcess.TAG, "\t\tWakupList after settlement [${getAcquirerNames(wakeUpList)}]")
////                                TransContext.getInstance().currentAction?.let {
////                                    it.isFinished=true
////                                    it.setResult(result)
////                                }
////                                TransContext.getInstance().currentAction = null
////                            } catch (ex: Exception) {
////                                Log.d(AutoSettleAlarmProcess.TAG, "\t\t\t $ex.message")
////                                ex.printStackTrace()
////                            } finally {
////                                Log.d(AutoSettleAlarmProcess.TAG, "\t\tgoback to MainActivity" )
////                                openMainActivity(FinancialApplication.getApp().applicationContext)
////                            }
////                        } else {
////                            Log.d(AutoSettleAlarmProcess.TAG, "\t\tgoback to MainActivity on No-WakeUpList" )
////                            openMainActivity(FinancialApplication.getApp().applicationContext)
////                        }
//                    }
//                }
//                val settleTrans = SettleTrans(context, false, true,false, activeAcquirerList, endlistener, true)
//                settleTrans.execute()
//                isExecuting = true
//                Log.d(AutoSettleAlarmProcess.TAG, "\t\t[$dispAcqName] ===> IS-NOW-EXECUTING = ON")





            } else {
                Log.d(AutoSettleAlarmProcess.TAG, "No any Execution needed")
                Log.d(AutoSettleAlarmProcess.TAG, "\t\tgoback to MainActivity on No-WakeUpList" )
                openMainActivity(FinancialApplication.getApp().applicationContext)

                return
            }
        } else {
            Log.d(AutoSettleAlarmProcess.TAG, "wait execute in next 10 seconds")
            executeSettlementProcess(context)
            return
        }
    }

    private fun checkAndRemoveNotInstallMultiApp() {
        if (wakeUpList.size > 0) {
            try {
                val multiAppAcqList : ArrayList<String> = arrayListOf(Constants.ACQ_DOLFIN, Constants.ACQ_SCB_IPP, Constants.ACQ_SCB_REDEEM, Constants.ACQ_AMEX, Constants.ACQ_KCHECKID)
                for (mAppAcqName : String in multiAppAcqList) {
                    var removeRequire : Boolean = false
                    if ((mAppAcqName.equals(Constants.ACQ_DOLFIN) && !DolfinApi.getInstance().dolfinServiceBinded)
                         || ((mAppAcqName.equals(Constants.ACQ_SCB_IPP) || mAppAcqName.equals(Constants.ACQ_SCB_REDEEM)) && !ScbIppService.isSCBInstalled(ActivityStack.getInstance().top()))
                         || (mAppAcqName.equals(Constants.ACQ_AMEX) && !AmexTransService.isAmexAppInstalled(ActivityStack.getInstance().top()))
                         || (mAppAcqName.equals(Constants.ACQ_KCHECKID) && !KCheckIDService.isKCheckIDInstalled(ActivityStack.getInstance().top())) ) {
                        val remIndex = wakeUpList.indexOf(mAppAcqName)
                        wakeUpList.removeAt(remIndex)
                    }
                }
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
    }

    fun removeFromList(removeList: ArrayList<String>,targArrList: ArrayList<String>) : ArrayList<String> {
        val tmpRemoveList: ArrayList<String> = removeList
        var tmpTargArrList: ArrayList<String> = targArrList

        if (tmpRemoveList.size>0 && tmpTargArrList.size>0) {
            try {
                for (hostName : String in tmpRemoveList) {
                    if (tmpTargArrList.contains(hostName)) {
                        val index = tmpTargArrList.indexOf(hostName)
                        tmpTargArrList.removeAt(index)
                    }
                }
            } catch (ex:Exception) {
                ex.printStackTrace()
            }

            return tmpTargArrList
        }
        return targArrList
    }

    fun openMainActivity(context: Context) {
        val intent = Intent(context, MainActivity::class.java)
        context.startActivity(intent)
    }

}

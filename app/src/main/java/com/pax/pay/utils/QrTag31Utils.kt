package com.pax.pay.utils

import com.pax.pay.app.FinancialApplication
import com.pax.pay.base.Acquirer
import com.pax.pay.constant.Constants
import com.pax.pay.trans.model.ETransType
import com.pax.pay.trans.model.TransData
import com.pax.settings.SysParam
import kotlinx.serialization.descriptors.StructureKind

class QrTag31Utils {
    companion object {

        fun getDistinctSourceOfFund(transList: List<TransData>) : Map<String, List<TransData>> {
            var i : Int = -1
            for (element : TransData in transList) {
                i+=1
                try {
                    if (element.qrSourceOfFund==null &&  (element.transType.equals(ETransType.QR_VERIFY_PAY_SLIP) || element.origTransType.equals(ETransType.QR_VERIFY_PAY_SLIP))) {
                        element.qrSourceOfFund = "VERIFY-QR"
                        transList[i].qrSourceOfFund = "VERIFY-QR"
                        FinancialApplication.getTransDataDbHelper().updateTransData(element)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            return transList.groupBy { it.qrSourceOfFund }
        }

        fun getThaiQrTransDataForReporting() : List<TransData> {
            val thaiQrSupportedTransTypes = listOf<ETransType>(ETransType.QR_INQUIRY, ETransType.QR_VOID_KPLUS)
            val filters = listOf<TransData.ETransStatus>(TransData.ETransStatus.VOIDED)
            val acquirer = FinancialApplication.getAcqManager().findAcquirer(Constants.ACQ_KPLUS)

            return  FinancialApplication.getTransDataDbHelper().findTransData(thaiQrSupportedTransTypes, filters, acquirer)
        }

        fun getRecordCountByTransType(acquirer: Acquirer,transTypes : List<ETransType>, transState: List<TransData.ETransStatus>, transList :  List<TransData>) : Long {
            return countof(getFilteredRecordByTransType(acquirer, transTypes, transState, transList))
        }

        fun getSummaryAmountByTransType(acquirer: Acquirer,transTypes : List<ETransType>, transState: List<TransData.ETransStatus>, transList :  List<TransData>) : Long {
            return sumof(getFilteredRecordByTransType(acquirer, transTypes, transState, transList))
        }

        fun getFilteredRecordByTransType(acquirer: Acquirer, transTypes : List<ETransType>, transState: List<TransData.ETransStatus>, transList :  List<TransData>) : List<TransData> {
            return (transList.filter { transTypes.contains(it.transType)
                    && it.acquirer.name.equals(acquirer.name)
                    && transState.contains(it.transState)
                    && it.reversalStatus.equals(TransData.ReversalStatus.NORMAL) })
        }

        fun countof(transList: List<TransData>) : Long {
            return transList.size.toLong()
        }

        fun sumof(transList : List<TransData>) : Long {
            if (transList.size > 0) {
                var sumVal : Long = 0L
                for (element : TransData in transList) {
                    try {
                        sumVal += element.amount.toLong()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                return sumVal
            } else {
                return 0L
            }
        }

        fun isEcrReturnSourceOfFund() : Int {
            if (FinancialApplication.getSysParam().get(SysParam.BooleanParam.EDC_QR_TAG_31_ENABLE, false)) {
                return FinancialApplication.getSysParam().get(SysParam.NumberParam.EDC_QR_TAG_31_ECR_RETURN_MODE, 0)
            } else {
                return QRTAG31_TENDER_MODE.NONE.intVal
            }
        }
    }

    enum class QRTAG31_TENDER_MODE(val intVal: Int) {
        NONE(-1), DEFAULT(0), SOURCE_OF_FUNDS(1)
    }
}
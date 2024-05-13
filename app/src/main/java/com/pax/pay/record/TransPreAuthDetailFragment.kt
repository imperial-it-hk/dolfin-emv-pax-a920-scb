package com.pax.pay.record

import android.view.View
import com.pax.edc.R
import com.pax.edc.expandablerecyclerview.BaseViewHolder
import com.pax.edc.expandablerecyclerview.ExpandableRecyclerAdapter
import com.pax.pay.app.FinancialApplication
import com.pax.pay.constant.Constants
import com.pax.pay.trans.model.ETransType
import com.pax.pay.trans.model.TransData
import com.pax.pay.utils.CurrencyConverter
import com.pax.pay.utils.TimeConverter
import com.pax.pay.utils.Utils

class TransPreAuthDetailFragment: TransDetailFragment() {

    private var mAdapter: ExpandableRecyclerAdapter<TransData>? = null

    override fun syncRecord() {
        mRecordAsyncTask = RecordAsyncTask()
        mRecordAsyncTask.execute()
    }

    private inner class RecordAsyncTask : TransDetailFragment.RecordAsyncTask() {
        override fun doInBackground(vararg params: Void?): List<TransData> {
            mListItems.clear()
            val list: List<TransData>? = if (Utils.getString(R.string.acq_all_acquirer) == acquirerName) {
                FinancialApplication.getTransDataDbHelper().findAllPreAuthTransaction(false)
            } else {
                val acquirer = FinancialApplication.getAcqManager().findAcquirer(acquirerName)
                acquirer?.let {
                    FinancialApplication.getTransDataDbHelper().findAllPreAuthTransaction(false, it)
                }
            }
            list?.let {
                mListItems.addAll(it)
                mListItems.reverse()
            }
            return mListItems
        }

        override fun onPostExecute(result: List<TransData>) {
            super.onPostExecute(result)
            if (mListItems.isEmpty()) {
                mRecyclerView!!.visibility = View.GONE
                noTransRecord!!.visibility = View.VISIBLE
                return
            }
            mRecyclerView!!.visibility = View.VISIBLE
            noTransRecord!!.visibility = View.GONE
            if (mAdapter == null) {
                mAdapter = ExpandableRecyclerAdapter(activity, R.layout.trans_item) { view -> RecordViewHolder(view) }.setDataBeanList(mListItems)
            } else {
                mAdapter!!.setDataBeanList(mListItems)
            }
            mRecyclerView.adapter = mAdapter
        }
    }

    private inner class RecordViewHolder(itemView: View?) : TransDetailFragment.RecordViewHolder(itemView) {

        override fun bindView(dataBean: TransData?, viewHolder: BaseViewHolder<*>?, pos: Int) {
            val transType = dataBean!!.transType
            transTypeTv.text = when (transType) {
                ETransType.PREAUTHORIZATION -> "Pre-Auth"
                ETransType.PREAUTHORIZATION_CANCELLATION -> "Pre-Auth Cancel"
                else -> ""
            }

            transAmountTv.text = if (!transType.isSymbolNegative) {
                transAmountTv.setTextColor(context.resources.getColor(R.color.accent_amount))
                CurrencyConverter.convert(Utils.parseLongSafe(dataBean.amount, 0), dataBean.currency)
            } else {
                transAmountTv.setTextColor(context.resources.getColor(R.color.accent))
                CurrencyConverter.convert(0 - Utils.parseLongSafe(dataBean.amount, 0), dataBean.currency)
            }

            transIssuerTv.text = dataBean.issuer.name
            transNoTv.text = dataBean.traceNo.toString(10).padStart(6, '0')
            stanNoTv.text = dataBean.stanNo.toString(10).padStart(6, '0')

            val formattedDate = TimeConverter.convert(
                dataBean.dateTime,
                Constants.TIME_PATTERN_TRANS,
                Constants.TIME_PATTERN_DISPLAY2
            )
            transDateTv.text = formattedDate

            if (dataBean.isDccRequired) {
                dccAmountTv.text = if (!transType.isSymbolNegative) {
                    dccAmountTv.setTextColor(context.resources.getColor(R.color.accent_amount))
                    CurrencyConverter.convert(Utils.parseLongSafe(dataBean.dccAmount, 0), dataBean.dccCurrencyCode.toString())
                } else {
                    dccAmountTv.setTextColor(context.resources.getColor(R.color.accent))
                    CurrencyConverter.convert(0 - Utils.parseLongSafe(dataBean.dccAmount, 0), dataBean.dccCurrencyCode.toString())
                }
                dccAmountTvLayout.visibility = View.VISIBLE
            }

            if (viewHolder!!.expandView.visibility == View.VISIBLE) {
                updateExpandableLayout(dataBean)
            }
        }

        override fun onClick(v: View?) {
            super.onClick(v)
        }

    }
}
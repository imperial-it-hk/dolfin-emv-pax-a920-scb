package th.co.bkkps.edc.recycleradapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.Adapter
import com.pax.edc.R
import com.pax.pay.trans.action.activity.DisplayEDCParamActivity.EDCTextViwInfo
import kotlin.collections.LinkedHashMap

class EDCParamRecyclerAdapter(private val mapValues: LinkedHashMap<String, EDCTextViwInfo?>): Adapter<EDCParamRecyclerAdapter.ViewHolder>() {

    // Provide a direct reference to each of the views within a data item
    // Used to cache the views within the item layout for fast access
    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        // Your holder should contain and initialize a member variable
        // for any view that will be set as you render a row
        val paramTextView: TextView = itemView.findViewById(R.id.para_name)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.edc_param_list_item, parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val arrayParamTv = mapValues.values.toTypedArray()
        val paramTv: EDCTextViwInfo? = arrayParamTv[position]

        paramTv?.let {
            val textView = holder.paramTextView
            textView.text = it.text
            textView.textSize = it.textSize
            textView.gravity = it.textAlignment
        }
    }

    override fun getItemCount(): Int {
        return mapValues.size
    }
}
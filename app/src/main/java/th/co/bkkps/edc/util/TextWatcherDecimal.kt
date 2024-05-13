package th.co.bkkps.edc.util

import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.KeyEvent
import android.widget.EditText
import java.math.BigDecimal
import java.text.NumberFormat

/*class TextWatcherDecimal(
    *//*private val context: Context,*//*
    private val editText: EditText,
    private val currentValue: String,
    private val currentKey: String,
    private val maxSize: Int,
    private val textWatcher: TextWatcher
    ) {*/
object TextWatcherDecimal {
   /* override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
        Log.e("menu","------------------- beforeTextChanged CharSequence = " + s)
        if(s.isNotEmpty()) {
            var valueInterestAsDouble = s.toString().toDouble() / 100
            valueAsString = (valueInterestAsDouble * 100).toLong().toString();
            Log.e("menu", "------------------- beforeTextChanged valueInterestAsString = " + valueAsString)
        }
    }*/

  /*  override fun afterTextChanged(s: Editable) {}*/

    /*override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
        var formatterNum: NumberFormat = NumberFormat.getInstance()
        formatterNum.minimumFractionDigits = 2
        formatterNum.maximumFractionDigits = 2
        if (s.isNotEmpty() && count > 0) {

            Log.e("menu","------------------- onTextChanged CharSequence = " + s)
            Log.e("menu","------------------- onTextChanged valueAsString = " + valueAsString)
            Log.e("menu","------------------- onTextChanged currentKey = " + currentKey)
            Log.e("menu","------------------- onTextChanged maxSize = " + maxSize)

            var displayValue: String = NumPadUtils.calculateValue(valueAsString, currentKey, maxSize)
            Log.e("menu","------------------- onTextChanged displayValue = " + displayValue)
            if (displayValue.isNotEmpty()) {

                editText.removeTextChangedListener(textWatcher)
                editText.setText(formatterNum.format(displayValue.toDouble() / 100))
                editText.setSelection(editText.text.length);
                editText.addTextChangedListener(textWatcher)
            }
        } else if (s.isNotEmpty() && count == 0) {
           *//* var displayValue: String = NumPadUtils.deleteValue(currentValue)

            if (displayValue.isEmpty()) {
                displayValue = "0.00"
            }

            editText.removeTextChangedListener(this)
            editText.setText(formatterNum.format(displayValue.toDouble() / 100))
            editText.setSelection(editText.text.length);
            editText.addTextChangedListener(this)*//*

        }

    }*/

    fun calDecimal(editText: EditText,currentValue: String,currentKey: String,maxSize: Int,textWatcher: TextWatcher,s: CharSequence,count: Int){
        var formatterNum: NumberFormat = NumberFormat.getInstance()
        var valueAsString : String = ""
        //var valueAsDouble: Double = 0.00
        formatterNum.minimumFractionDigits = 2
        formatterNum.maximumFractionDigits = 2

        if(currentValue.isNotEmpty()){
            //valueAsDouble = currentValue.toDouble() / 100
            var currentVal = currentValue.replace(",","")
            //valueAsString = (currentVal.toDouble() * 100).toLong().toString();
            val bd  = BigDecimal("100")
            val valBd = bd.multiply( BigDecimal(currentVal))
            val valLong = valBd.longValueExact()
            valueAsString = valLong.toString()
        }

        if (s.isNotEmpty() && count > 0) {
            var displayValue: String = NumPadUtils.calculateValue(valueAsString, currentKey, maxSize)
            if (displayValue.isNotEmpty()) {

                editText.removeTextChangedListener(textWatcher)
                editText.setText(formatterNum.format(displayValue.toDouble() / 100))
                editText.setSelection(editText.text.length);
                editText.addTextChangedListener(textWatcher)
            }
        } else if (s.isNotEmpty() && count == 0) {
             var displayValue: String = NumPadUtils.deleteValue(valueAsString)

            if (displayValue.isEmpty()) {
                displayValue = "0.00"
            }

            editText.removeTextChangedListener(textWatcher)
            editText.setText(formatterNum.format(displayValue.toDouble() / 100))
            editText.setSelection(editText.text.length);
            editText.addTextChangedListener(textWatcher)

        }

        /*
         if(!currentValue.isEmpty()){
            String currentVal = currentValue.replace(",","");
            BigDecimal bd = new BigDecimal("100");
            BigDecimal valBd = bd.multiply(new BigDecimal(currentVal));
            long valLong = valBd.longValue();
            valueAsString = String.valueOf(valLong);
        }

        if (s != null && s.length() > 0 && count > 0) {
            String displayValue = calculateValue(valueAsString, currentKey, maxSize);
            if(displayValue != null && !displayValue.isEmpty()){
                editText.removeTextChangedListener(textWatcher);
                editText.setText(formatterNum.format(Double.parseDouble(displayValue) / 100));
                editText.setSelection(editText.getText().length());
                editText.addTextChangedListener(textWatcher);
            }
        }else if(s != null && s.length() > 0 && count == 0){
            String  displayValue = deleteValue(valueAsString);
            if(displayValue.isEmpty()){
                displayValue = "0.00";
            }

            editText.removeTextChangedListener(textWatcher);
            editText.setText(formatterNum.format(Double.parseDouble(displayValue) / 100));
            editText.setSelection(editText.getText().length());
            editText.addTextChangedListener(textWatcher);
        }


        */

    }

    fun setCurrentKey(keyCode: Int): Int{
        var currentKey: Int = 0
        when(keyCode){
            KeyEvent.KEYCODE_0 -> currentKey = 0;
            KeyEvent.KEYCODE_1 -> currentKey = 1;
            KeyEvent.KEYCODE_2 -> currentKey = 2;
            KeyEvent.KEYCODE_3 -> currentKey = 3;
            KeyEvent.KEYCODE_4 -> currentKey = 4;
            KeyEvent.KEYCODE_5 -> currentKey = 5;
            KeyEvent.KEYCODE_6 -> currentKey = 6;
            KeyEvent.KEYCODE_7 -> currentKey = 7;
            KeyEvent.KEYCODE_8 -> currentKey = 8;
            KeyEvent.KEYCODE_9 -> currentKey = 9;
        }
        //Log.e("menu","------------------- currentKey = " + currentKey)
        return currentKey
    }

}

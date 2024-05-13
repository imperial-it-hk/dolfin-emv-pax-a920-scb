package com.pax.pay.splash

import android.app.Instrumentation

interface SplashListener {
    fun onEndProcess(activityResult : Instrumentation.ActivityResult)
    fun onUpdataUI(title:String, dispText:String)
}
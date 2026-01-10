package com.sandg.tastebuds

import android.app.Application
import android.content.Context

class MyApplication: Application() {

    companion object Globals {
        var appContext: Context? = null
    }

    override fun onCreate() {
        super.onCreate()
        appContext = applicationContext
    }
}
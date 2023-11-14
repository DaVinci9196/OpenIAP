package org.mg.iap

import android.app.Service
import android.content.Intent
import android.os.IBinder

class InAppBillingService : Service() {
    override fun onCreate() {
        super.onCreate()
        ContextProvider.init(this.application)
    }

    override fun onBind(intent: Intent?): IBinder {
        return IAPImpl
    }
}
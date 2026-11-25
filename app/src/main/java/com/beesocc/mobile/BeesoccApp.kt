package com.beesocc.mobile

import android.app.Application
import java.net.CookieHandler
import java.net.CookieManager
import java.net.CookiePolicy

class BeesoccApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Laravel/Sanctum usa cookies de sesion tambien en la API.
        if (CookieHandler.getDefault() == null) {
            CookieHandler.setDefault(CookieManager(null, CookiePolicy.ACCEPT_ALL))
        }
    }
}

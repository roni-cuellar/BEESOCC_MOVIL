package com.beesocc.mobile.core

import android.content.Context
import androidx.core.content.edit
import com.beesocc.mobile.BuildConfig

/**
 * La URL se puede cambiar en tres lugares, en este orden de prioridad:
 * 1. Ajustes dentro de la app (SharedPreferences).
 * 2. gradle.properties -> apiBaseUrl (BuildConfig.API_BASE_URL).
 * 3. Valor por defecto para emulador (10.0.2.2:8000).
 */
object ApiConfig {
    private const val PREFS = "beesocc_prefs"
    private const val KEY_BASE_URL = "api_base_url"

    fun baseUrl(context: Context): String {
        val guardada = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_BASE_URL, null)
        return (guardada?.takeIf { it.isNotBlank() } ?: BuildConfig.API_BASE_URL).trimEnd('/')
    }

    fun setBaseUrl(context: Context, url: String) {
        val normalizada = url.trim().trimEnd('/')
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit {
            putString(KEY_BASE_URL, normalizada)
        }
        ApiClient.reset()
    }

    fun reset(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit {
            remove(KEY_BASE_URL)
        }
        ApiClient.reset()
    }

    fun apiUrl(context: Context, ruta: String): String {
        return baseUrl(context) + "/api" + (if (ruta.startsWith("/")) ruta else "/$ruta")
    }

    fun imagenUrl(context: Context, ruta: String?): String {
        if (ruta.isNullOrBlank()) return ""
        val texto = ruta.trim()
        if (texto.startsWith("http://", true) || texto.startsWith("https://", true)) return texto
        val base = baseUrl(context)
        return if (texto.startsWith("/")) base + texto else "$base/storage/$texto"
    }
}

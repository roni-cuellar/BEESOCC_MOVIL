package com.beesocc.mobile.core

import android.content.Context
import org.json.JSONObject

class SessionManager(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("beesocc_prefs", Context.MODE_PRIVATE)

    var usuario: JSONObject? = null
        private set

    init {
        prefs.getString("usuario_json", null)?.let {
            runCatching { usuario = JSONObject(it) }
        }
    }

    val autenticado: Boolean
        get() = usuario != null

    val nombre: String
        get() = usuario?.optString("name", "") ?: ""

    val correo: String
        get() = usuario?.optString("email", "") ?: ""

    val id: Long
        get() = usuario?.optLong("id", 0L) ?: 0L

    fun guardar(usuarioJson: JSONObject?) {
        usuario = usuarioJson
        prefs.edit().putString("usuario_json", usuarioJson?.toString()).apply()
    }

    fun cerrar() {
        usuario = null
        prefs.edit().remove("usuario_json").apply()
    }

    var ultimaLigaId: Long
        get() = prefs.getLong("ultima_liga_id", 0L)
        set(value) { prefs.edit().putLong("ultima_liga_id", value).apply() }

    companion object {
        @Volatile
        private var instancia: SessionManager? = null

        fun get(context: Context): SessionManager {
            return instancia ?: synchronized(this) {
                instancia ?: SessionManager(context).also { instancia = it }
            }
        }
    }
}

package com.beesocc.mobile.ui.formulario

import java.io.Serializable

enum class TipoCampo { TEXTO, NUMERO, EMAIL, PASSWORD, AREA, SELECT, SWITCH, FECHA, IMAGEN }

data class FormField(
    val clave: String,
    val etiqueta: String,
    val tipo: TipoCampo = TipoCampo.TEXTO,
    val valor: String = "",
    val hint: String = "",
    val requerido: Boolean = false,
    val opciones: List<String> = emptyList(),
    val valores: List<String> = emptyList(),
) : Serializable {
    companion object {
        private const val serialVersionUID = 1L
    }
}

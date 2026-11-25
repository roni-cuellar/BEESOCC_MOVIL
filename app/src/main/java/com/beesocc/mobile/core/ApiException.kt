package com.beesocc.mobile.core

class ApiException(
    val status: Int,
    mensaje: String,
    val errores: Map<String, String> = emptyMap(),
) : Exception(mensaje)

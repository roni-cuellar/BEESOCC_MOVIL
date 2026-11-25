package com.beesocc.mobile.ui.formulario

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import com.beesocc.mobile.core.UploadFile

/** Convierte el Bundle devuelto por FormFragment en valores listos para la API. */
fun Bundle.valoresFormulario(): Map<String, String> {
    @Suppress("UNCHECKED_CAST")
    val mapa = getSerializable("valores") as? HashMap<String, String> ?: return emptyMap()
    return mapa.toMap()
}

fun Bundle.archivoFormulario(campo: String): UploadFile? {
    val bytes = getByteArray("bytes_$campo") ?: return null
    return UploadFile(
        campo = campo,
        nombre = getString("nombre_$campo") ?: "imagen.png",
        mime = getString("mime_$campo") ?: "image/png",
        bytes = bytes,
    )
}

fun Fragment.escucharFormulario(clave: String, alGuardar: (Bundle) -> Unit) {
    parentFragmentManager.setFragmentResultListener(clave, viewLifecycleOwner) { _, bundle -> alGuardar(bundle) }
}

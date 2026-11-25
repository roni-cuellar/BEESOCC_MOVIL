package com.beesocc.mobile.ui

import android.widget.Toast
import androidx.fragment.app.Fragment
import com.beesocc.mobile.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder

fun Fragment.abrir(fragment: Fragment) {
    (activity as? MainActivity)?.abrir(fragment)
}

fun Fragment.abrirRaiz(fragment: Fragment) {
    (activity as? MainActivity)?.abrirRaiz(fragment)
}

fun Fragment.titulo(texto: String, volver: Boolean = true) {
    (activity as? MainActivity)?.titulo(texto, volver)
}

fun Fragment.aviso(mensaje: String, largo: Boolean = false) {
    Toast.makeText(requireContext(), mensaje, if (largo) Toast.LENGTH_LONG else Toast.LENGTH_SHORT).show()
}

fun Fragment.confirmar(titulo: String, mensaje: String, alAceptar: () -> Unit) {
    MaterialAlertDialogBuilder(requireContext())
        .setTitle(titulo)
        .setMessage(mensaje)
        .setNegativeButton(R.string.cancelar, null)
        .setPositiveButton(R.string.aceptar) { _, _ -> alAceptar() }
        .show()
}

fun Fragment.pedirTexto(titulo: String, mensaje: String, valor: String = "", alAceptar: (String) -> Unit) {
    val campo = android.widget.EditText(requireContext()).apply { setText(valor) }
    MaterialAlertDialogBuilder(requireContext())
        .setTitle(titulo)
        .setMessage(mensaje)
        .setView(campo)
        .setNegativeButton(R.string.cancelar, null)
        .setPositiveButton(R.string.aceptar) { _, _ -> alAceptar(campo.text.toString().trim()) }
        .show()
}

fun Fragment.mensajeError(e: Exception, alReintentar: (() -> Unit)? = null) {
    val texto = e.message ?: "No se pudo completar la operación."
    if (alReintentar == null) aviso(texto, true)
    else MaterialAlertDialogBuilder(requireContext())
        .setTitle("No se pudo completar")
        .setMessage(texto)
        .setNegativeButton(R.string.cancelar, null)
        .setPositiveButton(R.string.reintentar) { _, _ -> alReintentar() }
        .show()
}

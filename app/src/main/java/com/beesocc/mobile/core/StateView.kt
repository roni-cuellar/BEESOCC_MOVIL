package com.beesocc.mobile.core

import android.view.View
import com.beesocc.mobile.databinding.ViewStateBinding

fun View.visible() { visibility = View.VISIBLE }
fun View.gone() { visibility = View.GONE }
fun View.invisible() { visibility = View.INVISIBLE }

class StateView(
    private val binding: ViewStateBinding,
    private val contenido: View,
    reintentar: () -> Unit,
) {
    init {
        binding.estadoReintentar.setOnClickListener { reintentar() }
    }

    fun cargando(mensaje: String = "Cargando…") {
        contenido.gone()
        binding.root.visible()
        binding.estadoProgress.visible()
        binding.estadoVacio.gone()
        binding.estadoError.gone()
        binding.estadoReintentar.gone()
        binding.estadoError.text = mensaje
    }

    fun contenido() {
        contenido.visible()
        binding.root.gone()
    }

    fun vacio(mensaje: String = "No hay datos para mostrar.") {
        contenido.gone()
        binding.root.visible()
        binding.estadoProgress.gone()
        binding.estadoVacio.apply { text = mensaje; visible() }
        binding.estadoError.gone()
        binding.estadoReintentar.gone()
    }

    fun error(mensaje: String, puedeReintentar: Boolean = true) {
        contenido.gone()
        binding.root.visible()
        binding.estadoProgress.gone()
        binding.estadoVacio.gone()
        binding.estadoError.apply { text = mensaje; visible() }
        binding.estadoReintentar.visibility = if (puedeReintentar) View.VISIBLE else View.GONE
    }
}

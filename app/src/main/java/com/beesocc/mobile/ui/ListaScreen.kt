package com.beesocc.mobile.ui

import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.beesocc.mobile.core.StateView
import com.beesocc.mobile.databinding.FragmentListaBinding
import com.beesocc.mobile.databinding.ViewStateBinding
import com.beesocc.mobile.ui.adapter.ListaAdapter

fun Fragment.prepararLista(
    binding: FragmentListaBinding,
    titulo: String,
    subtitulo: String = "",
    botonPrimario: Pair<String, () -> Unit>? = null,
    botonSecundario: Pair<String, () -> Unit>? = null,
    escuchas: ListaAdapter.Escuchas = object : ListaAdapter.Escuchas {},
    reintentar: () -> Unit,
): Pair<ListaAdapter, StateView> {
    titulo(titulo)
    binding.listaTitulo.text = titulo
    binding.listaSubtitulo.text = subtitulo
    binding.listaSubtitulo.visibility = if (subtitulo.isBlank()) android.view.View.GONE else android.view.View.VISIBLE

    botonPrimario?.let { (texto, accion) ->
        binding.listaBotonPrimario.apply {
            this.text = texto
            visibility = android.view.View.VISIBLE
            setOnClickListener { accion() }
        }
    }
    botonSecundario?.let { (texto, accion) ->
        binding.listaBotonSecundario.apply {
            this.text = texto
            visibility = android.view.View.VISIBLE
            setOnClickListener { accion() }
        }
    }

    val adaptador = ListaAdapter(escuchas)
    binding.listaRecycler.layoutManager = LinearLayoutManager(requireContext())
    binding.listaRecycler.adapter = adaptador
    val estado = StateView(binding.estado, binding.listaRecycler, reintentar)
    return adaptador to estado
}

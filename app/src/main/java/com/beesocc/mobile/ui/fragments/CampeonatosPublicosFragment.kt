package com.beesocc.mobile.ui.fragments

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.beesocc.mobile.R
import com.beesocc.mobile.core.StateView
import com.beesocc.mobile.core.objetos
import com.beesocc.mobile.core.primerArreglo
import com.beesocc.mobile.databinding.FragmentListaBinding
import com.beesocc.mobile.model.campeonatoItem
import com.beesocc.mobile.repository.PublicoRepo
import com.beesocc.mobile.ui.abrir
import com.beesocc.mobile.ui.adapter.ItemLista
import com.beesocc.mobile.ui.adapter.ListaAdapter
import com.beesocc.mobile.ui.pedirTexto
import com.beesocc.mobile.ui.prepararLista
import kotlinx.coroutines.launch

class CampeonatosPublicosFragment : Fragment(R.layout.fragment_lista) {
    private var _binding: FragmentListaBinding? = null
    private val binding get() = _binding!!
    private lateinit var adaptador: ListaAdapter
    private lateinit var estado: StateView

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentListaBinding.bind(view)
        val escuchas = object : ListaAdapter.Escuchas {
            override fun onItem(item: ItemLista) = detalle(item.id)
            override fun onPrimario(item: ItemLista) = detalle(item.id)
        }
        val preparado = prepararLista(
            binding = binding,
            titulo = "Campeonatos públicos",
            subtitulo = "Consulta los torneos publicados y sus resultados.",
            botonPrimario = "Buscar por código" to ::buscarCodigo,
            botonSecundario = "Actualizar" to ::cargar,
            escuchas = escuchas,
            reintentar = ::cargar,
        )
        adaptador = preparado.first
        estado = preparado.second
        cargar()
    }

    private fun detalle(id: Long) = abrir(DetallePublicoFragment.nuevo(id))

    private fun buscarCodigo() = pedirTexto("Código de liga", "Escribe los 10 caracteres del código compartido.") { codigo ->
        if (codigo.isBlank()) return@pedirTexto
        lifecycleScope.launch {
            estado.cargando("Buscando liga…")
            try {
                val respuesta = PublicoRepo.porCodigo(requireContext(), codigo)
                val campeonato = respuesta.optJSONObject("campeonato") ?: respuesta
                val id = campeonato.optLong("id")
                if (id > 0) detalle(id) else estado.vacio("No hay una liga publicada con ese código.")
            } catch (e: Exception) {
                estado.error(e.message ?: "No se encontró la liga.")
            }
        }
    }

    private fun cargar() {
        lifecycleScope.launch {
            estado.cargando()
            try {
                val datos = PublicoRepo.listar(requireContext()).primerArreglo("campeonatos", "data")
                val items = datos.objetos().map { it.campeonatoItem(publico = true) }
                adaptador.submitList(items)
                if (items.isEmpty()) estado.vacio("Aún no hay campeonatos publicados.") else estado.contenido()
            } catch (e: Exception) {
                estado.error(e.message ?: "No se pudieron cargar los campeonatos.")
            }
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}

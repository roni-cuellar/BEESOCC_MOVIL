package com.beesocc.mobile.ui.fragments

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.beesocc.mobile.R
import com.beesocc.mobile.core.objetos
import com.beesocc.mobile.core.primerArreglo
import com.beesocc.mobile.databinding.FragmentListaBinding
import com.beesocc.mobile.model.partidoItem
import com.beesocc.mobile.repository.ArbitroRepo
import com.beesocc.mobile.ui.abrir
import com.beesocc.mobile.ui.adapter.ItemLista
import com.beesocc.mobile.ui.prepararLista
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

class PartidosArbitrajeFragment : Fragment(R.layout.fragment_lista) {
    private var _binding: FragmentListaBinding? = null
    private val binding get() = _binding!!
    private lateinit var adaptador: com.beesocc.mobile.ui.adapter.ListaAdapter
    private lateinit var estado: com.beesocc.mobile.core.StateView
    private val ligaId: Long get() = arguments?.getLong("ligaId", 0L) ?: 0L
    private val ligaNombre: String get() = arguments?.getString("nombre").orEmpty()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentListaBinding.bind(view)
        val preparado = prepararLista(
            binding = binding,
            titulo = "Partidos asignados",
            subtitulo = ligaNombre,
            botonSecundario = "Actualizar" to ::cargar,
            escuchas = object : com.beesocc.mobile.ui.adapter.ListaAdapter.Escuchas {
                override fun onItem(item: ItemLista) = abrir(PartidoFragment.nuevo(item.id, ligaId))
                override fun onPrimario(item: ItemLista) = abrir(PartidoFragment.nuevo(item.id, ligaId))
            },
            reintentar = ::cargar,
        )
        adaptador = preparado.first
        estado = preparado.second
        cargar()
    }

    private fun cargar() {
        lifecycleScope.launch {
            estado.cargando()
            try {
                val datos = ArbitroRepo.partidos(requireContext(), ligaId)
                val arreglo = datos as? JSONArray ?: JSONObject(datos.toString()).primerArreglo("partidos", "data")
                val items = arreglo.objetos().map { it.partidoItem() }
                adaptador.submitList(items)
                if (items.isEmpty()) estado.vacio("No hay partidos asignados en este campeonato.")
                else estado.contenido()
            } catch (e: Exception) {
                estado.error(e.message ?: "No se pudieron cargar los partidos.")
            }
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    companion object {
        fun nuevo(ligaId: Long, nombre: String) = PartidosArbitrajeFragment().apply {
            arguments = Bundle().apply {
                putLong("ligaId", ligaId)
                putString("nombre", nombre)
            }
        }
    }
}

package com.beesocc.mobile.ui.fragments

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.beesocc.mobile.R
import com.beesocc.mobile.core.objetos
import com.beesocc.mobile.model.campeonatoItem
import com.beesocc.mobile.ui.abrir
import com.beesocc.mobile.core.primerArreglo
import com.beesocc.mobile.databinding.FragmentListaBinding
import com.beesocc.mobile.repository.ArbitroRepo
import com.beesocc.mobile.ui.adapter.ItemLista
import com.beesocc.mobile.ui.prepararLista
import kotlinx.coroutines.launch
import org.json.JSONObject

class MisArbitrajesFragment : Fragment(R.layout.fragment_lista) {
    private var _binding: FragmentListaBinding? = null
    private val binding get() = _binding!!
    private lateinit var adaptador: com.beesocc.mobile.ui.adapter.ListaAdapter
    private lateinit var estado: com.beesocc.mobile.core.StateView

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentListaBinding.bind(view)
        val preparado = prepararLista(
            binding = binding,
            titulo = "Mis arbitrajes",
            subtitulo = "Campeonatos donde tienes una asignación arbitral activa.",
            botonSecundario = "Actualizar" to ::cargar,
            escuchas = object : com.beesocc.mobile.ui.adapter.ListaAdapter.Escuchas {
                override fun onItem(item: ItemLista) = abrirPartidos(item)
                override fun onPrimario(item: ItemLista) = abrirPartidos(item)
            },
            reintentar = ::cargar,
        )
        adaptador = preparado.first
        estado = preparado.second
        cargar()
    }

    private fun abrirPartidos(item: ItemLista) = abrir(
        PartidosArbitrajeFragment.nuevo(item.id, item.datos.optJSONObject("campeonato")?.optString("nombre").orEmpty()))

    private fun cargar() {
        lifecycleScope.launch {
            estado.cargando()
            try {
                val datos = ArbitroRepo.misArbitrajes(requireContext())
                val items = datos.primerArreglo("asignaciones").objetos().map { asignacion ->
                    val campeonato = asignacion.optJSONObject("campeonato") ?: JSONObject()
                    campeonato.campeonatoItem().copy(
                        id = asignacion.optLong("id"),
                        subtitulo = "${campeonato.optString("temporada")} · ${campeonato.optString("formato").replace("_", " ")}",
                        meta = "Asignado: ${asignacion.optString("fecha_inicio")} · ${campeonato.optString("estado")}",
                        datos = asignacion,
                    )
                }
                adaptador.submitList(items)
                if (items.isEmpty()) estado.vacio("No tienes arbitrajes asignados.")
                else estado.contenido()
            } catch (e: Exception) {
                estado.error(e.message ?: "No se pudieron cargar tus arbitrajes.")
            }
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}

package com.beesocc.mobile.ui.fragments

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.beesocc.mobile.R
import com.beesocc.mobile.core.StateView
import com.beesocc.mobile.core.objetos
import com.beesocc.mobile.core.primerArreglo
import com.beesocc.mobile.core.texto
import com.beesocc.mobile.databinding.FragmentListaBinding
import com.beesocc.mobile.databinding.ViewStateBinding
import com.beesocc.mobile.repository.PublicoRepo
import com.beesocc.mobile.ui.adapter.ItemLista
import com.beesocc.mobile.ui.adapter.ListaAdapter
import com.beesocc.mobile.ui.prepararLista
import kotlinx.coroutines.launch
import org.json.JSONObject

class FichaPublicaFragment : Fragment(R.layout.fragment_lista) {
    private var _binding: FragmentListaBinding? = null
    private val binding get() = _binding!!
    private lateinit var estado: StateView
    private lateinit var adaptador: ListaAdapter
    private val campeonatoId: Long get() = arguments?.getLong("campeonatoId", 0L) ?: 0L
    private val jugadorId: Long get() = arguments?.getLong("jugadorId", 0L) ?: 0L

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentListaBinding.bind(view)
        val preparado = prepararLista(
            binding = binding,
            titulo = "Ficha del jugador",
            subtitulo = "Historial deportivo en este campeonato.",
            botonSecundario = "Actualizar" to ::cargar,
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
                val datos = PublicoRepo.jugador(requireContext(), campeonatoId, jugadorId)
                val jugador = datos.optJSONObject("jugador") ?: JSONObject()
                binding.listaTitulo.text = "${jugador.texto("nombre")} ${jugador.texto("apellido")}".trim().ifBlank { "Jugador" }
                binding.listaSubtitulo.text = "Historial y estadísticas públicas"

                val items = mutableListOf<ItemLista>()
                datos.optJSONArray("equipos")?.objetos()?.forEach { fila ->
                    val equipo = fila.optJSONObject("equipo") ?: JSONObject()
                    items.add(ItemLista(
                        id = equipo.optLong("id"),
                        titulo = equipo.texto("nombre", "Equipo"),
                        subtitulo = "Dorsal ${fila.optInt("dorsal")} · ${fila.texto("posicion")}",
                        meta = if (fila.optBoolean("vigente")) "Vigente" else "Anterior",
                        imagen = equipo.texto("escudo"),
                    ))
                }
                datos.optJSONObject("estadisticas")?.let { stats ->
                    stats.keys().forEach { clave ->
                        val valor = stats.opt(clave) ?: return@forEach
                        items.add(ItemLista(
                            id = items.size + 1L,
                            titulo = clave.replace('_', ' ').replaceFirstChar { it.uppercase() },
                            subtitulo = valor.toString(),
                        ))
                    }
                }
                adaptador.submitList(items)
                if (items.isEmpty()) estado.vacio("No hay información pública para este jugador.")
                else estado.contenido()
            } catch (e: Exception) {
                estado.error(e.message ?: "No se pudo cargar la ficha.")
            }
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    companion object {
        fun nuevo(campeonatoId: Long, jugadorId: Long) = FichaPublicaFragment().apply {
            arguments = Bundle().apply {
                putLong("campeonatoId", campeonatoId)
                putLong("jugadorId", jugadorId)
            }
        }
    }
}

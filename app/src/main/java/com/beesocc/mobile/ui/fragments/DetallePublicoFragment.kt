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
import com.beesocc.mobile.model.campeonatoItem
import com.beesocc.mobile.model.equipoPublicoItem
import com.beesocc.mobile.model.estadisticaItem
import com.beesocc.mobile.model.filaTablaItem
import com.beesocc.mobile.model.partidoItem
import com.beesocc.mobile.repository.PublicoRepo
import com.beesocc.mobile.ui.abrir
import com.beesocc.mobile.ui.adapter.ItemLista
import com.beesocc.mobile.ui.adapter.ListaAdapter
import com.beesocc.mobile.ui.aviso
import com.beesocc.mobile.ui.formulario.FormField
import com.beesocc.mobile.ui.formulario.FormFragment
import com.beesocc.mobile.ui.formulario.TipoCampo
import com.beesocc.mobile.ui.formulario.escucharFormulario
import com.beesocc.mobile.ui.formulario.valoresFormulario
import com.beesocc.mobile.ui.prepararLista
import com.beesocc.mobile.ui.titulo
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch
import org.json.JSONObject

class DetallePublicoFragment : Fragment(R.layout.fragment_lista) {
    private var _binding: FragmentListaBinding? = null
    private val binding get() = _binding!!
    private lateinit var estado: StateView
    private lateinit var adaptador: ListaAdapter
    private val campeonatoId: Long get() = arguments?.getLong("campeonatoId", 0L) ?: 0L
    private var campeonato: JSONObject = JSONObject()
    private var vista = "equipos"
    private var tipoEstadistica = "goleadores"

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentListaBinding.bind(view)
        titulo("Campeonato")
        val escuchas = object : ListaAdapter.Escuchas {
            override fun onItem(item: ItemLista) = alPulsar(item)
            override fun onPrimario(item: ItemLista) = alPulsar(item)
            override fun onSecundario(item: ItemLista) = alPulsar(item)
        }
        val preparado = prepararLista(
            binding = binding,
            titulo = "Campeonato",
            subtitulo = "Selecciona una sección para explorar la información pública.",
            botonPrimario = null,
            botonSecundario = "Actualizar" to ::cargar,
            escuchas = escuchas,
            reintentar = ::cargar,
        )
        adaptador = preparado.first
        estado = preparado.second
        binding.listaBotonPrimario.visibility = View.GONE
        configurarBotonesSeccion()
        registrarFormularios()
        cargar()
    }

    private fun configurarBotonesSeccion() {
        val contenedor = binding.listaBotonesContenido
        val nombres = listOf("Equipos", "Partidos", "Tabla", "Deporte", "Estadísticas", "Identidades")
        nombres.forEachIndexed { indice, nombre ->
            val boton = com.google.android.material.button.MaterialButton(requireContext()).apply {
                text = nombre
                isAllCaps = false
                if (indice == 0) setBackgroundColor(resources.getColor(R.color.verde, null))
                setOnClickListener {
                    vista = when (nombre) {
                        "Equipos" -> "equipos"
                        "Partidos" -> "partidos"
                        "Tabla" -> "tabla"
                        "Deporte" -> "deporte"
                        "Estadísticas" -> "estadisticas"
                        else -> "identidades"
                    }
                    binding.listaSubtitulo.text = nombre
                    for (i in 0 until contenedor.childCount) {
                        val child = contenedor.getChildAt(i)
                        (child as? com.google.android.material.button.MaterialButton)?.setBackgroundColor(
                            resources.getColor(if (child === this@apply) R.color.verde else R.color.verde_oscuro, null))
                    }
                    cargar()
                }
            }
            contenedor.addView(boton)
        }
    }

    private fun registrarFormularios() {
        escucharFormulario("form_tipo_estadistica") { datos ->
            tipoEstadistica = datos.valoresFormulario()["tipo"].orEmpty()
            cargar()
        }
    }

    private fun alPulsar(item: ItemLista) {
        when (vista) {
            "equipos" -> {
                val inscripcion = item.datos.optLong("inscripcion_id")
                lifecycleScope.launch {
                    try {
                        val plantilla = PublicoRepo.plantilla(requireContext(), campeonatoId, inscripcion)
                        val jugadores = plantilla.primerArreglo("plantilla", "jugadores").objetos()
                        if (jugadores.isEmpty()) {
                            MaterialAlertDialogBuilder(requireContext()).setTitle(item.titulo)
                                .setMessage("Este equipo todavía no tiene plantilla publicada.")
                                .setPositiveButton(R.string.aceptar, null).show()
                        } else {
                            val nombres = jugadores.map { fila ->
                                val j = fila.optJSONObject("jugador") ?: JSONObject()
                                "#${fila.optInt("dorsal", 0)} ${j.texto("nombre")} ${j.texto("apellido")}".trim()
                            }.toTypedArray()
                            MaterialAlertDialogBuilder(requireContext()).setTitle("Plantilla · ${item.titulo}")
                                .setItems(nombres) { _, cual ->
                                    val jugadorId = jugadores[cual].optJSONObject("jugador")?.optLong("id") ?: 0L
                                    if (jugadorId > 0) abrir(FichaPublicaFragment.nuevo(campeonatoId, jugadorId))
                                    else aviso("Este jugador no tiene ficha pública.")
                                }
                                .setPositiveButton(R.string.aceptar, null).show()
                        }
                    } catch (e: Exception) { aviso(e.message ?: "No se pudo cargar la plantilla.", true) }
                }
            }
            "partidos" -> {
                val partidoId = item.datos.optLong("id")
                if (partidoId > 0) abrir(PartidoFragment.publico(partidoId, campeonatoId))
                else aviso("Este partido todavía no tiene ficha disponible.")
            }
            "deporte" -> {
                val faseId = item.datos.optLong("id")
                if (faseId > 0) abrir(FaseFragment.nuevo(faseId, item.datos.texto("tipo")))
                else aviso("Esta fase no tiene identificador.")
            }
            else -> MaterialAlertDialogBuilder(requireContext()).setTitle(item.titulo)
                .setMessage(listOf(item.subtitulo, item.meta).filter { it.isNotBlank() }.joinToString("\n"))
                .setPositiveButton(R.string.aceptar, null).show()
        }
    }

    private fun abrirSelectorEstadistica() {
        abrir(FormFragment.nuevo("Tipo de estadística", listOf(
            FormField("tipo", "Estadística", TipoCampo.SELECT, tipoEstadistica, requerido = true,
                opciones = listOf("Goleadores", "Asistencias", "Amarillas", "Rojas", "Faltas", "Equipos"),
                valores = listOf("goleadores", "asistencias", "amarillas", "rojas", "faltas", "equipos")),
        ), "form_tipo_estadistica", "Aplicar"))
    }

    private fun cargar() {
        lifecycleScope.launch {
            estado.cargando()
            try {
                if (campeonato.optString("nombre").isBlank()) campeonato = PublicoRepo.obtener(requireContext(), campeonatoId)
                binding.listaTitulo.text = campeonato.texto("nombre", "Campeonato")
                binding.listaSubtitulo.text = when (vista) {
                    "equipos" -> "Equipos"
                    "partidos" -> "Partidos"
                    "tabla" -> "Tabla"
                    "deporte" -> "Deporte"
                    "estadisticas" -> "Estadísticas: $tipoEstadistica"
                    else -> "Identidades deportivas"
                }
                if (vista == "estadisticas") binding.listaBotonPrimario.apply {
                    visibility = View.VISIBLE
                    text = "Cambiar estadística"
                    setOnClickListener { abrirSelectorEstadistica() }
                } else binding.listaBotonPrimario.visibility = View.GONE

                val datos: Any = when (vista) {
                    "equipos" -> PublicoRepo.equipos(requireContext(), campeonatoId)
                    "partidos" -> PublicoRepo.partidos(requireContext(), campeonatoId)
                    "tabla" -> PublicoRepo.tabla(requireContext(), campeonatoId)
                    "deporte" -> PublicoRepo.deporte(requireContext(), campeonatoId)
                    "estadisticas" -> PublicoRepo.estadisticas(requireContext(), campeonatoId, tipoEstadistica)
                    else -> PublicoRepo.identidades(requireContext(), campeonatoId)
                }
                val items: List<ItemLista> = when (vista) {
                    "equipos" -> datos.primerArreglo("equipos").objetos().map { it.equipoPublicoItem() }
                    "partidos" -> datos.primerArreglo("partidos").objetos().map { it.partidoItem(publico = true) }
                    "tabla" -> datos.primerArreglo("tabla").objetos().map { it.filaTablaItem() }
                    "deporte" -> datos.primerArreglo("fases").objetos().map { it.campeonatoItem() }
                    "estadisticas" -> datos.primerArreglo("resultados").objetos().map { it.estadisticaItem() }
                    else -> datos.primerArreglo("identidades").objetos().map { it.campeonatoItem() }
                }
                adaptador.submitList(items)
                if (items.isEmpty()) estado.vacio("No hay información publicada en esta sección.")
                else estado.contenido()
            } catch (e: Exception) {
                estado.error(e.message ?: "No se pudo cargar la información pública.")
            }
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    companion object {
        fun nuevo(campeonatoId: Long) = DetallePublicoFragment().apply {
            arguments = Bundle().apply { putLong("campeonatoId", campeonatoId) }
        }
    }
}

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
import com.beesocc.mobile.model.correccionItem
import com.beesocc.mobile.model.eventoItem
import com.beesocc.mobile.databinding.FragmentPartidoBinding
import com.beesocc.mobile.databinding.ViewStateBinding
import com.beesocc.mobile.repository.ArbitroRepo
import com.beesocc.mobile.repository.CorreccionRepo
import com.beesocc.mobile.repository.PartidoRepo
import com.beesocc.mobile.repository.PublicoRepo
import com.beesocc.mobile.ui.abrir
import com.beesocc.mobile.ui.adapter.ItemLista
import com.beesocc.mobile.ui.adapter.ListaAdapter
import com.beesocc.mobile.ui.aviso
import com.beesocc.mobile.ui.confirmar
import com.beesocc.mobile.ui.formulario.FormField
import com.beesocc.mobile.ui.formulario.FormFragment
import com.beesocc.mobile.ui.formulario.TipoCampo
import com.beesocc.mobile.ui.formulario.escucharFormulario
import com.beesocc.mobile.ui.formulario.valoresFormulario
import com.beesocc.mobile.ui.titulo
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

class PartidoFragment : Fragment(R.layout.fragment_partido) {
    private var _binding: FragmentPartidoBinding? = null
    private val binding get() = _binding!!
    private lateinit var estado: StateView
    private lateinit var adaptador: ListaAdapter
    private var esPublico = false
    private var datos: JSONObject = JSONObject()
    private var panel: JSONObject = JSONObject()
    private var modoCorrecciones = false
    private var correcciones: List<JSONObject> = emptyList()
    private var plantillas: JSONObject = JSONObject()

    private val partidoId: Long get() = arguments?.getLong("partidoId", 0L) ?: 0L
    private val ligaId: Long get() = arguments?.getLong("ligaId", 0L) ?: 0L

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentPartidoBinding.bind(view)
        titulo("Acta del partido")
        estado = StateView(binding.estado, binding.partidoContenido, ::cargar)
        esPublico = ligaId == 0L

        adaptador = ListaAdapter(object : ListaAdapter.Escuchas {
            override fun onItem(item: ItemLista) = alPulsar(item)
            override fun onPrimario(item: ItemLista) = accionPrimaria(item)
            override fun onSecundario(item: ItemLista) = accionSecundaria(item)
        })
        binding.partidoRecycler.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(requireContext())
        binding.partidoRecycler.adapter = adaptador

        val privado = arrayOf(
            binding.partidoEvento, binding.partidoPanel, binding.partidoPreparar, binding.partidoReloj,
            binding.partidoIniciar, binding.partidoFinalizar, binding.partidoEstadoBoton,
            binding.partidoCorrecciones,
        )
        privado.forEach { it.visibility = if (esPublico) View.GONE else View.VISIBLE }

        binding.partidoActualizar.setOnClickListener { cargar() }
        binding.partidoEvento.setOnClickListener { abrirEvento() }
        binding.partidoPanel.setOnClickListener { mostrarPanel() }
        binding.partidoPreparar.setOnClickListener { prepararPanel() }
        binding.partidoReloj.setOnClickListener { controlarReloj() }
        binding.partidoIniciar.setOnClickListener { lifecycleScope.launch { ejecutarEstado("en_curso") } }
        binding.partidoFinalizar.setOnClickListener { lifecycleScope.launch { ejecutarEstado("finalizado") } }
        binding.partidoEstadoBoton.setOnClickListener { elegirEstado() }
        binding.partidoCorrecciones.setOnClickListener {
            modoCorrecciones = !modoCorrecciones
            binding.partidoCorrecciones.text = if (modoCorrecciones) "Ver eventos" else "Correcciones"
            cargar()
        }

        registrarFormularios()
        cargar()
    }

    private fun registrarFormularios() {
        escucharFormulario("form_evento") { datos ->
            val valores = datos.valoresFormulario()
            val cuerpo = JSONObject().apply {
                put("equipo_partido_id", valores["equipo_partido_id"]?.toLongOrNull() ?: 0L)
                put("plantilla_id", valores["plantilla_id"]?.toLongOrNull() ?: 0L)
                put("tipo", valores["tipo"].orEmpty())
                val minuto = valores["minuto"]?.toIntOrNull()
                if (minuto != null && !panel.has("duracion_minutos")) put("minuto", minuto)
                valores["gol_relacionado_id"]?.toLongOrNull()?.let { put("gol_relacionado_id", it) }
            }
            lifecycleScope.launch {
                try {
                    PartidoRepo.crearEvento(requireContext(), partidoId, cuerpo)
                    aviso("Evento registrado.")
                    cargar()
                } catch (e: Exception) { aviso(e.message ?: "No se pudo registrar el evento.", true) }
            }
        }
        escucharFormulario("form_panel") { datos ->
            val valores = datos.valoresFormulario()
            val titularesLocal = valores["titulares_local"].orEmpty().split(Regex("[,;\\s]+"))
                .mapNotNull { it.toLongOrNull() }
            val titularesVisitante = valores["titulares_visitante"].orEmpty().split(Regex("[,;\\s]+"))
                .mapNotNull { it.toLongOrNull() }
            lifecycleScope.launch {
                try {
                    ArbitroRepo.preparar(requireContext(), partidoId, JSONObject().apply {
                        put("version", panel.optInt("version"))
                        put("duracion_minutos", valores["duracion_minutos"]?.toIntOrNull() ?: 90)
                        put("titulares", JSONObject().apply {
                            put("local", JSONArray(titularesLocal))
                            put("visitante", JSONArray(titularesVisitante))
                        })
                    })
                    aviso("Panel arbitral actualizado.")
                    cargar()
                } catch (e: Exception) { aviso(e.message ?: "No se pudo preparar el panel.", true) }
            }
        }
        escucharFormulario("form_correccion") { datos ->
            val valores = datos.valoresFormulario()
            lifecycleScope.launch {
                try {
                    CorreccionRepo.solicitar(requireContext(), partidoId,
                        valores["motivo"].orEmpty(), valores["alcance"].orEmpty())
                    aviso("Solicitud de corrección creada.")
                    cargar()
                } catch (e: Exception) { aviso(e.message ?: "No se pudo solicitar la corrección.", true) }
            }
        }
        escucharFormulario("form_operaciones") { datos ->
            val texto = datos.valoresFormulario()["operaciones"].orEmpty().trim()
            if (texto.isEmpty() || texto == "[]") { aviso("Escribe al menos una operación JSON."); return@escucharFormulario }
            val arreglo = runCatching { JSONArray(texto) }.getOrNull()
            if (arreglo == null) { aviso("El JSON debe ser un arreglo de operaciones.", true); return@escucharFormulario }
            val id = datos.getLong("id", 0L)
            lifecycleScope.launch {
                try {
                    CorreccionRepo.aplicar(requireContext(), id, arreglo)
                    aviso("Corrección aplicada.")
                    cargar()
                } catch (e: Exception) { aviso(e.message ?: "No se pudo aplicar la corrección.", true) }
            }
        }
    }

    private fun abrirEvento() = lifecycleScope.launch {
        try {
            val participantes = datos.optJSONArray("participantes")?.objetos() ?: emptyList()
            if (participantes.isEmpty()) { aviso("Carga el partido antes de registrar eventos."); return@launch }
            panel = runCatching { ArbitroRepo.panel(requireContext(), partidoId) }.getOrDefault(JSONObject())
            plantillas = runCatching { ArbitroRepo.plantillasArbitrales(requireContext(), partidoId) }
                .getOrDefault(JSONObject())
            val nombres = participantes.map {
                it.optJSONObject("campeonatoEquipo")?.optJSONObject("equipo")?.optString("nombre").orEmpty()
            }
            val ids = participantes.map { it.optLong("id").toString() }
            val elegibles = mutableListOf<JSONObject>()
            panel.optJSONObject("elegibles")?.optJSONArray("local")?.objetos()?.let { elegibles.addAll(it) }
            panel.optJSONObject("elegibles")?.optJSONArray("visitante")?.objetos()?.let { elegibles.addAll(it) }
            if (elegibles.isEmpty()) {
                plantillas.keys().forEach { clave ->
                    plantillas.optJSONArray(clave)?.objetos()?.let { elegibles.addAll(it) }
                }
            }
            val plantillaNombres = elegibles.map {
                it.optJSONObject("jugador")?.let { j -> "${j.texto("nombre")} ${j.texto("apellido")}".trim() }
                    ?: "Plantilla ${it.optLong("id")}"
            }
            val plantillaIds = elegibles.map { it.optLong("id").toString() }
            abrir(FormFragment.nuevo("Registrar evento", listOf(
                FormField("equipo_partido_id", "Equipo", TipoCampo.SELECT, requerido = true,
                    opciones = nombres, valores = ids),
                FormField("plantilla_id", "Jugador de la plantilla", TipoCampo.SELECT, requerido = true,
                    opciones = plantillaNombres.ifEmpty { listOf("Sin plantilla disponible") },
                    valores = plantillaIds.ifEmpty { listOf("0") }),
                FormField("tipo", "Tipo de evento", TipoCampo.SELECT, "gol", requerido = true,
                    opciones = listOf("Gol", "Autogol", "Asistencia", "Amarilla", "Roja", "Falta"),
                    valores = listOf("gol", "autogol", "asistencia", "amarilla", "roja", "falta")),
                FormField("minuto", "Minuto (si no hay reloj arbitral)", TipoCampo.NUMERO, hint = "45"),
                FormField("gol_relacionado_id", "ID del gol (solo asistencias)", TipoCampo.NUMERO),
            ), "form_evento", "Registrar"))
        } catch (e: Exception) { aviso(e.message ?: "No se pudo preparar el formulario.", true) }
    }

    private fun mostrarPanel() = lifecycleScope.launch {
        try {
            val respuesta = ArbitroRepo.panel(requireContext(), partidoId)
            val titulares = respuesta.optJSONObject("titulares")
            val texto = """
                Duración: ${respuesta.optInt("duracion_minutos")} min
                Transcurrido: ${respuesta.optInt("segundos")} s
                En marcha: ${respuesta.optBoolean("en_marcha")}
                Versión: ${respuesta.optInt("version")}
                Límites: ${respuesta.optJSONObject("limites")?.optInt("minimo")}–${respuesta.optJSONObject("limites")?.optInt("maximo")}
                Titulares local: ${titulares?.optJSONArray("local")?.join().orEmpty()}
                Titulares visitante: ${titulares?.optJSONArray("visitante")?.join().orEmpty()}
            """.trimIndent()
            MaterialAlertDialogBuilder(requireContext()).setTitle("Panel arbitral")
                .setMessage(texto).setPositiveButton(R.string.aceptar, null).show()
        } catch (e: Exception) { aviso(e.message ?: "No se pudo consultar el panel.", true) }
    }

    private fun prepararPanel() = lifecycleScope.launch {
        try {
            panel = ArbitroRepo.panel(requireContext(), partidoId)
            val elegibles = panel.optJSONObject("elegibles")
            val local = elegibles?.optJSONArray("local")?.objetos().orEmpty()
            val visitante = elegibles?.optJSONArray("visitante")?.objetos().orEmpty()
            fun nombres(lista: List<JSONObject>) = lista.map {
                val j = it.optJSONObject("jugador")
                "#${it.optInt("dorsal")} ${j?.texto("nombre").orEmpty()} ${j?.texto("apellido").orEmpty()}".trim()
            }.ifEmpty { listOf("Sin jugadores elegibles") }
            fun ids(lista: List<JSONObject>) = lista.map { it.optLong("id").toString() }.ifEmpty { listOf("0") }
            abrir(FormFragment.nuevo("Preparar panel arbitral", listOf(
                FormField("duracion_minutos", "Duración (1-150)", TipoCampo.NUMERO,
                    panel.optInt("duracion_minutos", 90).toString(), requerido = true),
                FormField("titulares_local", "Titulares local (IDs separados por coma)",
                    valor = panel.optJSONObject("titulares")?.optJSONArray("local")?.join().orEmpty(),
                    hint = nombres(local).take(3).joinToString(", ")),
                FormField("titulares_visitante", "Titulares visitante (IDs separados por coma)",
                    valor = panel.optJSONObject("titulares")?.optJSONArray("visitante")?.join().orEmpty(),
                    hint = nombres(visitante).take(3).joinToString(", ")),
            ), "form_panel", "Guardar panel"))
        } catch (e: Exception) { aviso(e.message ?: "No se pudo cargar el panel.", true) }
    }

    private fun controlarReloj() = lifecycleScope.launch {
        try {
            val respuesta = ArbitroRepo.panel(requireContext(), partidoId)
            val accion = if (respuesta.optBoolean("en_marcha")) "pausar" else "reanudar"
            confirmar("Reloj arbitral", "¿${accion.replaceFirstChar { it.uppercase() }} el reloj?") {
                lifecycleScope.launch {
                    try {
                        ArbitroRepo.reloj(requireContext(), partidoId, accion, respuesta.optInt("version"))
                        aviso("Reloj actualizado.")
                        cargar()
                    } catch (e: Exception) { aviso(e.message ?: "No se pudo actualizar el reloj.", true) }
                }
            }
        } catch (e: Exception) { aviso(e.message ?: "No se pudo consultar el reloj.", true) }
    }

    private suspend fun ejecutarEstado(estado: String) {
        try {
            if (estado == "finalizado") {
                PartidoRepo.finalizar(requireContext(), partidoId)
            } else {
                panel = runCatching { ArbitroRepo.panel(requireContext(), partidoId) }.getOrDefault(JSONObject())
                PartidoRepo.cambiarEstadoConVersion(requireContext(), partidoId, estado, panel.optInt("version"))
            }
            aviso("Estado del partido actualizado.")
            cargar()
        } catch (e: Exception) { aviso(e.message ?: "No se pudo cambiar el estado.", true) }
    }

    private fun elegirEstado() {
        val estados = arrayOf("programado", "en_curso", "finalizado", "suspendido", "cancelado")
        MaterialAlertDialogBuilder(requireContext()).setTitle("Estado del partido")
            .setItems(estados) { _, cual -> lifecycleScope.launch { ejecutarEstado(estados[cual]) } }.show()
    }

    private fun alPulsar(item: ItemLista) {
        if (!modoCorrecciones) {
            aviso("Usa los botones superiores para gestionar el acta.")
            return
        }
        val correccion = correcciones.getOrNull(item.datos.optInt("_indice", -1)) ?: return
        val estado = correccion.texto("estado")
        val opciones = when {
            estado == "pendiente" -> arrayOf("Aprobar", "Rechazar", "Aplicar corrección", "Ver detalle")
            estado == "aprobada" -> arrayOf("Aplicar corrección", "Ver detalle")
            else -> arrayOf("Ver detalle")
        }
        MaterialAlertDialogBuilder(requireContext()).setTitle(item.titulo).setItems(opciones) { _, cual ->
            when (opciones[cual]) {
                "Aprobar", "Rechazar" -> lifecycleScope.launch {
                    try {
                        CorreccionRepo.resolver(requireContext(), correccion.optLong("id"),
                            if (opciones[cual] == "Aprobar") "aprobada" else "rechazada")
                        aviso("Corrección ${opciones[cual].lowercase()}.")
                        cargar()
                    } catch (e: Exception) { aviso(e.message ?: "No se pudo resolver.", true) }
                }
                "Aplicar corrección" -> abrir(FormFragment.nuevo("Aplicar corrección", listOf(
                    FormField("operaciones", "Operaciones (JSON)", TipoCampo.AREA, "[]",
                        hint = "[{\"accion\":\"corregir_minuto\",\"evento_id\":1,\"minuto\":45}]", requerido = true),
                ), "form_operaciones", "Aplicar").apply { arguments?.putLong("id", correccion.optLong("id")) })
                else -> MaterialAlertDialogBuilder(requireContext()).setTitle("Corrección")
                    .setMessage(correccion.toString(2)).setPositiveButton(R.string.aceptar, null).show()
            }
        }.show()
    }

    private fun accionPrimaria(item: ItemLista) = alPulsar(item)
    private fun accionSecundaria(item: ItemLista) {
        if (modoCorrecciones) {
            val correccion = correcciones.getOrNull(item.datos.optInt("_indice", -1)) ?: return
            MaterialAlertDialogBuilder(requireContext()).setTitle("Corrección")
                .setMessage(correccion.toString(2)).setPositiveButton(R.string.aceptar, null).show()
        } else {
            lifecycleScope.launch {
                val evento = datos.optJSONArray("eventos")?.objetos()?.getOrNull(
                    item.datos.optInt("_indice", -1)) ?: return@launch
                confirmar("Eliminar evento", "¿Eliminar este evento?") {
                    lifecycleScope.launch {
                        try {
                            PartidoRepo.eliminarEvento(requireContext(), evento.optLong("id"))
                            cargar()
                        } catch (e: Exception) { aviso(e.message ?: "No se pudo eliminar.", true) }
                    }
                }
            }
        }
    }

    private fun cargar() {
        lifecycleScope.launch {
            estado.cargando()
            try {
                datos = if (esPublico) PublicoRepo.partido(requireContext(), ligaId, partidoId)
                    else PartidoRepo.obtener(requireContext(), partidoId)
                val participantes = datos.optJSONArray("participantes")?.objetos().orEmpty()
                binding.partidoTitulo.text = participantes.joinToString(" vs ") {
                    it.optJSONObject("equipo")?.texto("nombre")
                        ?: it.optJSONObject("campeonatoEquipo")?.optJSONObject("equipo")?.texto("nombre")
                        ?: "Equipo ${it.optLong("id")}"
                }.ifBlank { "Partido #$partidoId" }
                val goles = participantes.map {
                    it.optJSONArray("goles")?.length() ?: it.optInt("goles", 0)
                }
                binding.partidoMarcador.text = if (participantes.size >= 2) "${goles[0]} - ${goles[1]}"
                    else datos.texto("marcador", "—")
                binding.partidoEstado.text = listOf(
                    datos.texto("estado").replace("_", " "),
                    datos.texto("fecha_hora"),
                    datos.texto("lugar"),
                    datos.texto("tipo_resultado"),
                ).filter { it.isNotBlank() }.joinToString(" · ")

                val items: List<ItemLista>
                if (modoCorrecciones && !esPublico) {
                    correcciones = CorreccionRepo.listar(requireContext(), partidoId)
                        .primerArreglo("correcciones", "data").objetos()
                    items = correcciones.mapIndexed { indice, c ->
                        c.correccionItem().copy(datos = c.put("_indice", indice))
                    }
                } else {
                    val eventos = if (esPublico) runCatching {
                        PublicoRepo.evento(requireContext(), ligaId, partidoId).primerArreglo("eventos").objetos()
                    }.getOrDefault(emptyList())
                    else datos.optJSONArray("eventos")?.objetos()
                        ?: PartidoRepo.eventos(requireContext(), partidoId).primerArreglo("eventos", "data").objetos()
                    items = eventos.mapIndexed { indice, e -> e.eventoItem().copy(datos = e.put("_indice", indice)) }
                }
                adaptador.submitList(items)
                if (items.isEmpty()) estado.vacio(if (modoCorrecciones) "Sin correcciones solicitadas." else "Todavía no hay eventos registrados.")
                else estado.contenido()
            } catch (e: Exception) {
                estado.error(e.message ?: "No se pudo cargar el partido.")
            }
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    companion object {
        fun nuevo(partidoId: Long, ligaId: Long) = PartidoFragment().apply {
            arguments = Bundle().apply {
                putLong("partidoId", partidoId)
                putLong("ligaId", ligaId)
            }
        }
        fun publico(partidoId: Long, ligaId: Long) = nuevo(partidoId, ligaId)
    }
}

private fun JSONArray.join(): String = (0 until length()).joinToString(", ") { opt(it).toString() }

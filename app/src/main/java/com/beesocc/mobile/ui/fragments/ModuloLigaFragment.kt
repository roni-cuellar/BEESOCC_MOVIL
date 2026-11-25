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
import com.beesocc.mobile.repository.ArbitroRepo
import com.beesocc.mobile.repository.EquipoRepo
import com.beesocc.mobile.repository.FaseRepo
import com.beesocc.mobile.repository.JugadorRepo
import com.beesocc.mobile.repository.LigaRepo
import com.beesocc.mobile.repository.PartidoRepo
import com.beesocc.mobile.databinding.FragmentListaBinding
import com.beesocc.mobile.model.asignacionItem
import com.beesocc.mobile.model.auditoriaItem
import com.beesocc.mobile.model.equipoItem
import com.beesocc.mobile.model.faseItem
import com.beesocc.mobile.model.filaTablaItem
import com.beesocc.mobile.model.inscripcionItem
import com.beesocc.mobile.model.jugadorItem
import com.beesocc.mobile.model.partidoItem
import com.beesocc.mobile.model.plantillaItem
import com.beesocc.mobile.model.estadisticaItem
import com.beesocc.mobile.ui.abrir
import com.beesocc.mobile.ui.adapter.ItemLista
import com.beesocc.mobile.ui.adapter.ListaAdapter
import com.beesocc.mobile.ui.aviso
import com.beesocc.mobile.ui.confirmar
import com.beesocc.mobile.ui.formulario.FormField
import com.beesocc.mobile.ui.formulario.FormFragment
import com.beesocc.mobile.ui.formulario.TipoCampo
import com.beesocc.mobile.ui.formulario.archivoFormulario
import com.beesocc.mobile.ui.formulario.escucharFormulario
import com.beesocc.mobile.ui.formulario.valoresFormulario
import com.beesocc.mobile.ui.pedirTexto
import com.beesocc.mobile.ui.prepararLista
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

enum class Modulo {
    EQUIPOS, PLANTILLAS, JUGADORES, ARBITROS, PARTIDOS, CALENDARIO,
    TABLA, ESTADISTICAS, GRUPOS, ELIMINACION, AUDITORIA,
}

/** Una pantalla por módulo del dashboard. Todos los botones ejecutan una acción real contra la API. */
class ModuloLigaFragment : Fragment(R.layout.fragment_lista) {
    private var _binding: FragmentListaBinding? = null
    private val binding get() = _binding!!
    private lateinit var adaptador: ListaAdapter
    private lateinit var estado: StateView

    private val ligaId: Long get() = arguments?.getLong("ligaId", 0L) ?: 0L
    private val ligaNombre: String get() = arguments?.getString("nombre").orEmpty()
    private val modulo: Modulo get() = Modulo.valueOf(arguments?.getString("modulo") ?: Modulo.EQUIPOS.name)
    private val inscripcionId: Long get() = arguments?.getLong("inscripcionId", 0L) ?: 0L
    private var tipoEstadistica = "goleadores"

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentListaBinding.bind(view)
        val escuchas = object : ListaAdapter.Escuchas {
            override fun onItem(item: ItemLista) = alPulsar(item)
            override fun onPrimario(item: ItemLista) = accionPrimaria(item)
            override fun onSecundario(item: ItemLista) = accionSecundaria(item)
        }
        val preparado = prepararLista(
            binding = binding,
            titulo = tituloModulo(),
            subtitulo = subtituloModulo(),
            botonPrimario = botonNuevo().takeIf { it.isNotBlank() }?.let { it to ::crear },
            botonSecundario = "Actualizar" to ::cargar,
            escuchas = escuchas,
            reintentar = ::cargar,
        )
        adaptador = preparado.first
        estado = preparado.second
        registrarFormularios()
        cargar()
    }

    private fun tituloModulo() = when (modulo) {
        Modulo.EQUIPOS -> "Equipos inscritos"
        Modulo.PLANTILLAS -> "Plantilla del equipo"
        Modulo.JUGADORES -> "Jugadores"
        Modulo.ARBITROS -> "Árbitros asignados"
        Modulo.PARTIDOS -> "Partidos"
        Modulo.CALENDARIO -> "Calendario"
        Modulo.TABLA -> "Tabla de posiciones"
        Modulo.ESTADISTICAS -> "Estadísticas"
        Modulo.GRUPOS -> "Fases de grupos"
        Modulo.ELIMINACION -> "Cuadro eliminatorio"
        Modulo.AUDITORIA -> "Auditoría del ciclo"
    }

    private fun subtituloModulo() = when (modulo) {
        Modulo.PLANTILLAS -> "Gestiona los jugadores inscritos en este equipo."
        Modulo.ESTADISTICAS -> "Tipo actual: ${tipoEstadistica.replace("_", " ")}"
        else -> ligaNombre
    }

    private fun botonNuevo() = when (modulo) {
        Modulo.EQUIPOS -> "Nuevo equipo"
        Modulo.PLANTILLAS -> "Agregar jugador"
        Modulo.JUGADORES -> "Nuevo jugador"
        Modulo.ARBITROS -> "Asignar árbitro"
        Modulo.PARTIDOS -> "Nuevo partido"
        Modulo.GRUPOS -> "Crear fase de grupos"
        Modulo.ELIMINACION -> "Crear fase eliminatoria"
        Modulo.ESTADISTICAS -> "Cambiar estadística"
        else -> ""
    }

    private fun registrarFormularios() {
        escucharFormulario("form_equipo") { datos ->
            val valores = datos.valoresFormulario()
            val escudo = datos.archivoFormulario("escudo")
            lifecycleScope.launch {
                try {
                    val respuesta = if (escudo != null) {
                        EquipoRepo.crearConEscudo(requireContext(), valores, escudo)
                    } else {
                        EquipoRepo.crear(requireContext(), JSONObject(valores))
                    }
                    val id = respuesta.optJSONObject("equipo")?.optLong("id") ?: respuesta.optLong("id")
                    if (id > 0) EquipoRepo.inscribir(requireContext(), ligaId, id)
                    aviso("Equipo creado e inscrito.")
                    cargar()
                } catch (e: Exception) { aviso(e.message ?: "No se pudo crear el equipo.", true) }
            }
        }
        escucharFormulario("form_jugador") { datos ->
            val valores = datos.valoresFormulario()
            val foto = datos.archivoFormulario("foto")
            lifecycleScope.launch {
                try {
                    if (foto != null) JugadorRepo.crearConFoto(requireContext(), valores, foto)
                    else JugadorRepo.crear(requireContext(), JSONObject(valores))
                    aviso("Jugador creado.")
                    cargar()
                } catch (e: Exception) { aviso(e.message ?: "No se pudo crear el jugador.", true) }
            }
        }
        escucharFormulario("form_partido") { datos ->
            val valores = datos.valoresFormulario()
            lifecycleScope.launch {
                try {
                    val cuerpo = JSONObject().apply {
                        put("jornada", valores["jornada"]?.toIntOrNull() ?: 1)
                        put("local_id", valores["local_id"]?.toLongOrNull() ?: 0)
                        put("visitante_id", valores["visitante_id"]?.toLongOrNull() ?: 0)
                        valores["fecha_hora"]?.takeIf { it.isNotBlank() }?.let { put("fecha_hora", it) }
                        valores["lugar"]?.takeIf { it.isNotBlank() }?.let { put("lugar", it) }
                    }
                    val partidoId = datos.getLong("id", 0L)
                    if (partidoId > 0) PartidoRepo.actualizar(requireContext(), partidoId, cuerpo)
                    else PartidoRepo.crear(requireContext(), ligaId, cuerpo)
                    aviso(if (partidoId > 0) "Partido actualizado." else "Partido creado.")
                    cargar()
                } catch (e: Exception) { aviso(e.message ?: "No se pudo crear el partido.", true) }
            }
        }
        escucharFormulario("form_fase") { datos ->
            val valores = datos.valoresFormulario()
            lifecycleScope.launch {
                try {
                    FaseRepo.crear(requireContext(), ligaId,
                        valores["nombre"].orEmpty(), valores["tipo"].orEmpty())
                    aviso("Fase creada.")
                    cargar()
                } catch (e: Exception) { aviso(e.message ?: "No se pudo crear la fase.", true) }
            }
        }
        escucharFormulario("form_plantilla") { datos ->
            val valores = datos.valoresFormulario()
            lifecycleScope.launch {
                try {
                    val cuerpo = JSONObject().apply {
                        put("jugador_id", valores["jugador_id"]?.toLongOrNull() ?: 0)
                        put("dorsal", valores["dorsal"]?.toIntOrNull() ?: 1)
                        put("posicion", valores["posicion"].orEmpty())
                        put("fecha_ingreso", valores["fecha_ingreso"].orEmpty())
                    }
                    JugadorRepo.inscribir(requireContext(), datos.getLong("inscripcionId", inscripcionId), cuerpo)
                    aviso("Jugador agregado a la plantilla.")
                    cargar()
                } catch (e: Exception) { aviso(e.message ?: "No se pudo agregar el jugador.", true) }
            }
        }
        escucharFormulario("form_arbitro") { datos ->
            val email = datos.valoresFormulario()["email"].orEmpty()
            lifecycleScope.launch {
                try {
                    val respuesta = ArbitroRepo.buscar(requireContext(), ligaId, email)
                    val usuario = respuesta.optJSONObject("usuario")
                    val id = usuario?.optLong("id") ?: 0L
                    if (id == 0L) { aviso("No hay un usuario con ese correo."); return@launch }
                    val nombre = usuario!!.optString("name")
                    confirmar("Asignar árbitro", "¿Asignar a $nombre?") {
                        lifecycleScope.launch {
                            try {
                                ArbitroRepo.asignar(requireContext(), ligaId, id)
                                aviso("Árbitro asignado.")
                                cargar()
                            } catch (e: Exception) { aviso(e.message ?: "No se pudo asignar.", true) }
                        }
                    }
                } catch (e: Exception) { aviso(e.message ?: "No se pudo buscar el usuario.", true) }
            }
        }
        escucharFormulario("form_reemplazo") { datos ->
            val valores = datos.valoresFormulario()
            lifecycleScope.launch {
                try {
                    val usuario = ArbitroRepo.buscar(requireContext(), ligaId, valores["email"].orEmpty())
                        .optJSONObject("usuario")
                    val id = usuario?.optLong("id") ?: 0L
                    if (id == 0L) { aviso("No hay un usuario con ese correo."); return@launch }
                    ArbitroRepo.reemplazar(requireContext(), ligaId, id, valores["motivo"].orEmpty())
                    aviso("Árbitro reemplazado.")
                    cargar()
                } catch (e: Exception) { aviso(e.message ?: "No se pudo reemplazar.", true) }
            }
        }
        escucharFormulario("form_motivo") { datos ->
            val motivo = datos.valoresFormulario()["motivo"].orEmpty()
            val id = datos.getLong("id", 0L)
            lifecycleScope.launch {
                try {
                    ArbitroRepo.revocar(requireContext(), ligaId, motivo)
                    aviso("Asignación revocada.")
                    cargar()
                } catch (e: Exception) { aviso(e.message ?: "No se pudo revocar.", true) }
            }
        }
    }

    private fun crear() {
        when (modulo) {
            Modulo.EQUIPOS -> abrir(FormFragment.nuevo("Nuevo equipo", listOf(
                FormField("nombre", "Nombre del equipo", requerido = true),
                FormField("ciudad", "Ciudad", requerido = true),
                FormField("escudo", "Escudo (opcional)", TipoCampo.IMAGEN),
            ), "form_equipo", "Crear e inscribir"))
            Modulo.JUGADORES -> abrir(FormFragment.nuevo("Nuevo jugador", listOf(
                FormField("nombre", "Nombre", requerido = true),
                FormField("apellido", "Apellido", requerido = true),
                FormField("fecha_nacimiento", "Fecha de nacimiento (AAAA-MM-DD)", requerido = true, hint = "2000-05-14"),
                FormField("foto", "Foto (opcional)", TipoCampo.IMAGEN),
            ), "form_jugador", "Crear jugador"))
            Modulo.ARBITROS -> abrir(FormFragment.nuevo("Asignar árbitro", listOf(
                FormField("email", "Correo del árbitro", TipoCampo.EMAIL, requerido = true),
            ), "form_arbitro", "Buscar y asignar"))
            Modulo.GRUPOS, Modulo.ELIMINACION -> {
                val tipo = if (modulo == Modulo.GRUPOS) "grupos" else "eliminacion"
                abrir(FormFragment.nuevo(if (modulo == Modulo.GRUPOS) "Nueva fase de grupos" else "Nueva fase eliminatoria", listOf(
                    FormField("nombre", "Nombre de la fase", valor = if (modulo == Modulo.GRUPOS) "Grupos" else "Eliminación", requerido = true),
                    FormField("tipo", "Tipo", TipoCampo.SELECT, tipo, requerido = true,
                        opciones = listOf("Grupos", "Eliminación directa"),
                        valores = listOf("grupos", "eliminacion")),
                ), "form_fase", "Crear fase"))
            }
            Modulo.PARTIDOS -> lifecycleScope.launch {
                val inscripciones = cargarInscripciones()
                if (inscripciones.isEmpty()) { aviso("Primero inscribe al menos dos equipos."); return@launch }
                val nombres = inscripciones.map { it.optJSONObject("equipo")?.optString("nombre").orEmpty() }
                val ids = inscripciones.map { it.optLong("id").toString() }
                abrir(FormFragment.nuevo("Nuevo partido", listOf(
                    FormField("jornada", "Jornada", TipoCampo.NUMERO, "1", requerido = true),
                    FormField("local_id", "Equipo local", TipoCampo.SELECT, requerido = true, opciones = nombres, valores = ids),
                    FormField("visitante_id", "Equipo visitante", TipoCampo.SELECT, requerido = true, opciones = nombres, valores = ids),
                    FormField("fecha_hora", "Fecha y hora (ISO 8601)", hint = "2026-12-01T16:00"),
                    FormField("lugar", "Lugar"),
                ), "form_partido", "Crear partido"))
            }
            Modulo.PLANTILLAS -> lifecycleScope.launch {
                val jugadores = runCatching { JugadorRepo.listar(requireContext()).primerArreglo("jugadores", "data") }
                    .getOrDefault(JSONArray()).objetos()
                if (jugadores.isEmpty()) { aviso("Primero crea jugadores en el módulo Jugadores."); return@launch }
                abrir(FormFragment.nuevo("Agregar jugador", listOf(
                    FormField("jugador_id", "Jugador", TipoCampo.SELECT, requerido = true,
                        opciones = jugadores.map { "${it.texto("nombre")} ${it.texto("apellido")}".trim() },
                        valores = jugadores.map { it.optLong("id").toString() }),
                    FormField("dorsal", "Dorsal (1-99)", TipoCampo.NUMERO, "1", requerido = true),
                    FormField("posicion", "Posición", requerido = true, hint = "DEL, MED, DEF, POR"),
                    FormField("fecha_ingreso", "Fecha de ingreso (AAAA-MM-DD)", requerido = true),
                ), "form_plantilla", "Agregar"))
            }
            Modulo.ESTADISTICAS -> {
                val tipos = arrayOf("goleadores", "asistencias", "amarillas", "rojas", "faltas", "equipos")
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Tipo de estadística")
                    .setItems(tipos) { _, cual ->
                        tipoEstadistica = tipos[cual]
                        binding.listaSubtitulo.text = "Tipo actual: ${tipoEstadistica.replace("_", " ")}"
                        cargar()
                    }
                    .show()
            }
            else -> aviso("Esta pantalla solo muestra información.")
        }
    }

    private fun alPulsar(item: ItemLista) {
        when (modulo) {
            Modulo.EQUIPOS -> abrir(ModuloLigaFragment().apply {
                arguments = Bundle().apply {
                    putLong("ligaId", ligaId); putString("nombre", ligaNombre)
                    putString("modulo", Modulo.PLANTILLAS.name); putLong("inscripcionId", item.id)
                }
            })
            Modulo.PLANTILLAS, Modulo.JUGADORES, Modulo.TABLA, Modulo.ESTADISTICAS, Modulo.AUDITORIA ->
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle(item.titulo)
                    .setMessage(listOf(item.subtitulo, item.meta).filter { it.isNotBlank() }.joinToString("\n"))
                    .setPositiveButton(R.string.aceptar, null)
                    .show()
            Modulo.ARBITROS -> aviso("Usa «Reemplazar» o «Revocar» para gestionar la asignación.")
            Modulo.PARTIDOS, Modulo.CALENDARIO -> abrir(PartidoFragment.nuevo(item.id, ligaId))
            Modulo.GRUPOS, Modulo.ELIMINACION -> abrir(FaseFragment.nuevo(item.id, item.datos.texto("tipo")))
        }
    }

    private fun accionPrimaria(item: ItemLista) {
        when (modulo) {
            Modulo.EQUIPOS -> lifecycleScope.launch {
                val estadoActual = item.datos.texto("estado")
                val nuevo = if (estadoActual == "activo") "retirado" else "activo"
                try {
                    EquipoRepo.estadoInscripcion(requireContext(), item.id, nuevo)
                    aviso("Estado de inscripción actualizado.")
                    cargar()
                } catch (e: Exception) { aviso(e.message ?: "No se pudo cambiar el estado.", true) }
            }
            Modulo.JUGADORES -> confirmar("Eliminar jugador", "¿Eliminar ${item.titulo}?") {
                lifecycleScope.launch {
                    try {
                        JugadorRepo.eliminar(requireContext(), item.id)
                        cargar()
                    } catch (e: Exception) { aviso(e.message ?: "No se pudo eliminar.", true) }
                }
            }
            Modulo.ARBITROS -> reemplazarArbitro()
            Modulo.GRUPOS, Modulo.ELIMINACION -> alPulsar(item)
            Modulo.PARTIDOS, Modulo.CALENDARIO -> abrir(PartidoFragment.nuevo(item.id, ligaId))
            else -> alPulsar(item)
        }
    }

    private fun accionSecundaria(item: ItemLista) {
        when (modulo) {
            Modulo.EQUIPOS -> confirmar("Quitar inscripción", "¿Quitar ${item.titulo} del campeonato?") {
                lifecycleScope.launch {
                    try {
                        EquipoRepo.eliminarInscripcion(requireContext(), item.id)
                        cargar()
                    } catch (e: Exception) { aviso(e.message ?: "No se pudo quitar la inscripción.", true) }
                }
            }
            Modulo.PLANTILLAS -> confirmar("Quitar jugador", "¿Quitar ${item.titulo} de la plantilla?") {
                lifecycleScope.launch {
                    try {
                        JugadorRepo.eliminarPlantilla(requireContext(), item.id)
                        cargar()
                    } catch (e: Exception) { aviso(e.message ?: "No se pudo quitar el jugador.", true) }
                }
            }
            Modulo.JUGADORES -> lifecycleScope.launch {
                val inscripciones = cargarInscripciones()
                if (inscripciones.isEmpty()) { aviso("No hay equipos inscritos."); return@launch }
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Agregar ${item.titulo} a un equipo")
                    .setItems(inscripciones.map {
                        it.optJSONObject("equipo")?.optString("nombre").orEmpty()
                    }.toTypedArray()) { _, cual ->
                        val inscripcion = inscripciones[cual]
                        abrir(FormFragment.nuevo("Datos en la plantilla", listOf(
                            FormField("jugador_id", "Jugador", TipoCampo.SELECT, valor = item.id.toString(),
                                opciones = listOf(item.titulo), valores = listOf(item.id.toString())),
                            FormField("dorsal", "Dorsal (1-99)", TipoCampo.NUMERO, "1", requerido = true),
                            FormField("posicion", "Posición", requerido = true),
                            FormField("fecha_ingreso", "Fecha de ingreso (AAAA-MM-DD)", requerido = true),
                        ).map { if (it.clave == "jugador_id") it.copy(requerido = true) else it }, "form_plantilla", "Agregar").apply {
                            arguments?.putLong("inscripcionId", inscripcion.optLong("id"))
                        })
                    }
                    .show()
            }
            Modulo.ARBITROS -> revocarArbitro(item)
            Modulo.PARTIDOS -> editarPartido(item)
            else -> aviso("Sin acción adicional en esta pantalla.")
        }
    }

    private fun reemplazarArbitro() = abrir(FormFragment.nuevo("Reemplazar árbitro", listOf(
        FormField("email", "Correo del nuevo árbitro", TipoCampo.EMAIL, requerido = true),
        FormField("motivo", "Motivo del cambio", requerido = true),
    ), "form_reemplazo", "Reemplazar"))

    private fun revocarArbitro(item: ItemLista) {
        val fragmento = FormFragment.nuevo("Revocar árbitro", listOf(
            FormField("motivo", "Motivo de la revocación", requerido = true),
        ), "form_motivo", "Revocar")
        fragmento.arguments?.putLong("id", item.id)
        abrir(fragmento)
    }

    private fun editarPartido(item: ItemLista) = lifecycleScope.launch {
        val inscripciones = cargarInscripciones()
        val nombres = inscripciones.map { it.optJSONObject("equipo")?.optString("nombre").orEmpty() }
        val ids = inscripciones.map { it.optLong("id").toString() }
        val fragmento = FormFragment.nuevo("Editar partido", listOf(
            FormField("jornada", "Jornada", TipoCampo.NUMERO, item.datos.optString("jornada", "1"), requerido = true),
            FormField("local_id", "Equipo local", TipoCampo.SELECT, requerido = true, opciones = nombres, valores = ids),
            FormField("visitante_id", "Equipo visitante", TipoCampo.SELECT, requerido = true, opciones = nombres, valores = ids),
            FormField("fecha_hora", "Fecha y hora (ISO 8601)", valor = item.datos.texto("fecha_hora")),
            FormField("lugar", "Lugar", valor = item.datos.texto("lugar")),
        ), "form_partido", "Guardar cambios")
        fragmento.arguments?.putLong("id", item.id)
        abrir(fragmento)
    }

    private suspend fun cargarInscripciones(): List<JSONObject> = runCatching {
        EquipoRepo.inscripciones(requireContext(), ligaId)
            .primerArreglo("inscripciones", "data").objetos()
    }.getOrDefault(emptyList())

    private fun cargar() {
        lifecycleScope.launch {
            estado.cargando()
            try {
                val datos: Any = when (modulo) {
                    Modulo.EQUIPOS -> EquipoRepo.inscripciones(requireContext(), ligaId)
                    Modulo.PLANTILLAS -> JugadorRepo.plantillas(requireContext(), inscripcionId)
                    Modulo.JUGADORES -> JugadorRepo.listar(requireContext())
                    Modulo.ARBITROS -> {
                        val vigente = runCatching { ArbitroRepo.vigente(requireContext(), ligaId) }.getOrNull()
                        val historial = runCatching { ArbitroRepo.historial(requireContext(), ligaId) }.getOrNull()
                        val lista = JSONArray()
                        vigente?.optJSONObject("asignacion")?.let { lista.put(it) }
                        historial?.primerArreglo("asignaciones", "historial")?.objetos()?.forEach { lista.put(it) }
                        lista
                    }
                    Modulo.PARTIDOS, Modulo.CALENDARIO -> PartidoRepo.listar(requireContext(), ligaId)
                    Modulo.TABLA -> LigaRepo.tabla(requireContext(), ligaId)
                    Modulo.ESTADISTICAS -> LigaRepo.estadisticas(requireContext(), ligaId, tipoEstadistica)
                    Modulo.GRUPOS, Modulo.ELIMINACION -> FaseRepo.listar(requireContext(), ligaId)
                    Modulo.AUDITORIA -> LigaRepo.auditoriaCiclo(requireContext(), ligaId)
                }
                val items = when (modulo) {
                    Modulo.EQUIPOS -> datos.primerArreglo("inscripciones", "data").objetos().map { it.inscripcionItem() }
                    Modulo.PLANTILLAS -> datos.primerArreglo("plantillas", "data").objetos().map { it.plantillaItem() }
                    Modulo.JUGADORES -> datos.primerArreglo("jugadores", "data").objetos().map { it.jugadorItem() }
                    Modulo.ARBITROS -> (datos as? JSONArray)?.objetos()?.map { it.asignacionItem() } ?: emptyList()
                    Modulo.PARTIDOS, Modulo.CALENDARIO -> datos.primerArreglo("partidos", "data").objetos().map { it.partidoItem() }
                    Modulo.TABLA -> datos.primerArreglo("tabla").objetos().map { it.filaTablaItem() }
                    Modulo.ESTADISTICAS -> datos.primerArreglo("resultados").objetos().map { it.estadisticaItem() }
                    Modulo.GRUPOS, Modulo.ELIMINACION -> datos.primerArreglo("fases", "data").objetos()
                        .filter { if (modulo == Modulo.GRUPOS) it.texto("tipo") == "grupos" else it.texto("tipo") != "grupos" }
                        .map { it.faseItem() }
                    Modulo.AUDITORIA -> datos.primerArreglo("auditorias", "data").objetos().map { it.auditoriaItem() }
                }
                adaptador.submitList(items)
                if (items.isEmpty()) estado.vacio(mensajeVacio()) else estado.contenido()
            } catch (e: Exception) {
                estado.error(e.message ?: "No se pudo cargar ${tituloModulo().lowercase()}.")
            }
        }
    }

    private fun mensajeVacio() = when (modulo) {
        Modulo.EQUIPOS -> "Aún no hay equipos inscritos."
        Modulo.PLANTILLAS -> "Esta plantilla está vacía."
        Modulo.JUGADORES -> "Aún no hay jugadores registrados."
        Modulo.ARBITROS -> "No hay árbitros asignados todavía."
        Modulo.PARTIDOS, Modulo.CALENDARIO -> "No hay partidos programados."
        Modulo.TABLA -> "La tabla estará disponible cuando haya resultados oficiales."
        Modulo.ESTADISTICAS -> "Sin estadísticas para este tipo."
        Modulo.GRUPOS -> "No hay fases de grupos creadas."
        Modulo.ELIMINACION -> "No hay fases eliminatorias creadas."
        Modulo.AUDITORIA -> "Sin movimientos de auditoría."
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    companion object {
        fun nuevo(ligaId: Long, nombre: String, modulo: Modulo, inscripcionId: Long = 0L) = ModuloLigaFragment().apply {
            arguments = Bundle().apply {
                putLong("ligaId", ligaId)
                putString("nombre", nombre)
                putString("modulo", modulo.name)
                putLong("inscripcionId", inscripcionId)
            }
        }
    }
}

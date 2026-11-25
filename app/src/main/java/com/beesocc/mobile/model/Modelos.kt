package com.beesocc.mobile.model

import com.beesocc.mobile.core.entero
import com.beesocc.mobile.core.largo
import com.beesocc.mobile.core.texto
import com.beesocc.mobile.ui.adapter.ItemLista
import org.json.JSONArray
import org.json.JSONObject
import java.net.URI

data class Campeonato(
    val id: Long,
    val nombre: String,
    val descripcion: String,
    val temporada: String,
    val formato: String,
    val estado: String,
    val logo: String,
    val codigo: String,
    val publicado: Boolean,
) {
    companion object {
        fun de(json: JSONObject) = Campeonato(
            json.largo("id"), json.texto("nombre"), json.texto("descripcion"),
            json.texto("temporada"), json.texto("formato"), json.texto("estado"),
            json.logoCampeonato(), json.texto("codigo_liga"), json.optBoolean("publicado"),
        )
    }
}

val JSONArray.campeonatos: List<Campeonato>
    get() = (0 until length()).mapNotNull { optJSONObject(it) }.map { Campeonato.de(it) }

fun JSONObject.nombreEquipoInscrito(): String {
    val inscripcion = optJSONObject("campeonatoEquipo") ?: optJSONObject("campeonato_equipo")
    val equipo = optJSONObject("equipo") ?: inscripcion?.optJSONObject("equipo")
    return equipo?.texto("nombre") ?: texto("nombre")
}

fun JSONObject.idEquipoInscrito(): Long {
    val inscripcion = optJSONObject("campeonatoEquipo") ?: optJSONObject("campeonato_equipo")
    return inscripcion?.largo("id") ?: largo("campeonato_equipo_id")
}

fun JSONObject.nombreJugador(): String {
    val jugador = optJSONObject("jugador") ?: this
    val completo = "${jugador.texto("nombre")} ${jugador.texto("apellido")}".trim()
    return completo.ifBlank { "Jugador" }
}

fun JSONObject.escudoEquipoInscrito(): String {
    val inscripcion = optJSONObject("campeonatoEquipo") ?: optJSONObject("campeonato_equipo")
    val equipo = optJSONObject("equipo") ?: inscripcion?.optJSONObject("equipo")
    return equipo?.texto("escudo") ?: texto("escudo")
}

private fun JSONObject.logoCampeonato(): String {
    val logo = (opt("logo") as? String)?.trim().orEmpty()
    val uri = runCatching { URI(logo) }.getOrNull()
    return logo.takeIf {
        uri?.host != null && (uri.scheme.equals("https", true) || uri.scheme.equals("http", true))
    }.orEmpty()
}

fun JSONObject.campeonatoItem(publico: Boolean = false): ItemLista = ItemLista(
    id = largo("id"),
    titulo = texto("nombre", "Campeonato"),
    subtitulo = texto("descripcion").ifBlank { texto("temporada") },
    meta = listOf(
        texto("temporada"),
        when (texto("formato")) {
            "eliminacion_directa" -> "Eliminación directa"
            "grupos_eliminacion" -> "Grupos + eliminación"
            else -> "Todos contra todos"
        },
        texto("estado").replace("_", " "),
    ).filter { it.isNotBlank() }.joinToString(" · "),
    imagen = logoCampeonato(),
    accionPrimaria = if (publico) "Ver" else "Entrar",
    accionSecundaria = if (publico) "" else "Acciones",
    datos = this,
)

fun JSONObject.equipoItem(): ItemLista = ItemLista(
    id = largo("id"),
    titulo = texto("nombre", "Equipo"),
    subtitulo = texto("ciudad"),
    imagen = texto("escudo"),
    datos = this,
)

fun JSONObject.inscripcionItem(): ItemLista {
    val equipo = optJSONObject("equipo") ?: JSONObject()
    return ItemLista(
        id = largo("id"),
        titulo = equipo.texto("nombre", "Equipo"),
        subtitulo = equipo.texto("ciudad"),
        meta = "Inscripción ${texto("estado")}",
        imagen = equipo.texto("escudo"),
        accionPrimaria = if (texto("estado") == "activo") "Dar de baja" else "Activar",
        accionSecundaria = "Quitar",
        datos = this,
    )
}

fun JSONObject.jugadorItem(): ItemLista = ItemLista(
    id = largo("id"),
    titulo = nombreJugador(),
    subtitulo = texto("fecha_nacimiento").ifBlank { "Sin fecha de nacimiento" },
    imagen = texto("foto"),
    datos = this,
)

fun JSONObject.plantillaItem(): ItemLista = ItemLista(
    id = largo("id"),
    titulo = nombreJugador(),
    subtitulo = listOf("Dorsal ${entero("dorsal")}", texto("posicion")).filter { it.isNotBlank() }.joinToString(" · "),
    meta = texto("fecha_ingreso"),
    imagen = optJSONObject("jugador")?.texto("foto") ?: texto("foto"),
    accionSecundaria = "Quitar",
    datos = this,
)

fun JSONObject.partidoItem(publico: Boolean = false): ItemLista {
    val participantes = optJSONArray("participantes") ?: JSONArray()
    val nombres = (0 until participantes.length()).mapNotNull { participantes.optJSONObject(it) }
        .map { it.nombreEquipoInscrito() }
    val local = nombres.getOrNull(0) ?: "Por definir"
    val visitante = nombres.getOrNull(1) ?: "Por definir"
    val marcador = optJSONObject("marcador_deportivo")
    val marcadorTexto = if (marcador != null) {
        "${marcador.opt("local") ?: "-"} - ${marcador.opt("visitante") ?: "-"}"
    } else ""
    return ItemLista(
        id = largo("id"),
        titulo = "$local vs $visitante",
        subtitulo = texto("fecha_hora").ifBlank { "Fecha por definir" },
        meta = listOf("Jornada ${entero("jornada")}", texto("estado").replace("_", " "), marcadorTexto)
            .filter { it.isNotBlank() }.joinToString(" · "),
        accionPrimaria = "Ver acta",
        accionSecundaria = if (publico) "" else "Editar",
        datos = this,
    )
}

fun JSONObject.eventoItem(): ItemLista = ItemLista(
    id = largo("id"),
    titulo = "${texto("minuto")}' · ${texto("tipo")}",
    subtitulo = nombreJugador(),
    meta = texto("rol").ifBlank { "" },
    accionSecundaria = "Quitar",
    datos = this,
)

fun JSONObject.estadisticaItem(): ItemLista = ItemLista(
    id = largo("jugador_id").takeIf { it != 0L } ?: largo("equipo_id"),
    titulo = listOf(texto("nombre"), texto("apellido")).filter { it.isNotBlank() }.joinToString(" "),
    subtitulo = texto("equipo").ifBlank { "Estadística" },
    meta = "${entero("total", entero("goles"))} total",
    datos = this,
)

fun JSONObject.filaTablaItem(): ItemLista = ItemLista(
    id = largo("equipo_id"),
    titulo = texto("nombre", "Equipo"),
    subtitulo = "PJ ${entero("PJ")} · PG ${entero("PG")} · PE ${entero("PE")} · PP ${entero("PP")}",
    meta = "GF ${entero("GF")} · GC ${entero("GC")} · DG ${entero("DG")} · PTS ${entero("PTS")}",
    datos = this,
)

fun JSONObject.asignacionItem(): ItemLista {
    val usuario = optJSONObject("usuario") ?: JSONObject()
    return ItemLista(
        id = largo("id"),
        titulo = usuario.texto("name", texto("nombre", "Árbitro")),
        subtitulo = usuario.texto("email", texto("email")),
        meta = texto("estado").ifBlank { texto("motivo_revocacion") },
        accionPrimaria = "Reemplazar",
        accionSecundaria = "Revocar",
        datos = this,
    )
}

fun JSONObject.faseItem(): ItemLista = ItemLista(
    id = largo("id"),
    titulo = texto("nombre", "Fase"),
    subtitulo = texto("tipo", "Formato de fase"),
    meta = texto("estado"),
    accionPrimaria = "Abrir",
    datos = this,
)

fun JSONObject.auditoriaItem(): ItemLista = ItemLista(
    id = largo("id"),
    titulo = texto("accion", texto("entidad_tipo", "Auditoría")),
    subtitulo = texto("motivo").ifBlank { texto("fecha") },
    meta = texto("fecha"),
    datos = this,
)

fun JSONObject.llaveItem(): ItemLista = ItemLista(
    id = largo("id"),
    titulo = texto("nombre", "Llave ${entero("numero_serie")}"),
    subtitulo = texto("ronda").ifBlank { texto("estado") },
    meta = texto("estado"),
    accionPrimaria = "Avanzar",
    accionSecundaria = "Penales",
    datos = this,
)

fun JSONObject.grupoItem(): ItemLista = ItemLista(
    id = largo("id"),
    titulo = texto("nombre", "Grupo"),
    subtitulo = "${entero("cantidad_equipos")} equipos",
    meta = texto("estado"),
    datos = this,
)

fun JSONObject.genericoItem(tituloClave: String = "nombre"): ItemLista = ItemLista(
    id = largo("id"),
    titulo = texto(tituloClave, texto("nombre", texto("titulo", "Registro"))),
    subtitulo = texto("descripcion").ifBlank { texto("detalle").ifBlank { texto("mensaje") } },
    meta = texto("estado"),
    datos = this,
)

fun JSONObject.equipoPublicoItem(): ItemLista {
    val equipo = optJSONObject("equipo") ?: JSONObject()
    return ItemLista(
        id = largo("inscripcion_id", largo("id")),
        titulo = equipo.texto("nombre", texto("nombre", "Equipo")),
        subtitulo = equipo.texto("ciudad"),
        meta = texto("estado"),
        imagen = equipo.texto("escudo"),
        datos = this,
    )
}

fun JSONObject.correccionItem(): ItemLista = ItemLista(
    id = largo("id"),
    titulo = texto("motivo", "Corrección #${largo("id")}"),
    subtitulo = "Alcance: ${texto("alcance")}",
    meta = "${texto("estado")} · ${texto("fecha_resolucion").ifBlank { texto("fecha_consumo") }}",
    accionPrimaria = "Gestionar",
    accionSecundaria = "Detalle",
    datos = this,
)

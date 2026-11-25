package com.beesocc.mobile.repository

import android.content.Context
import com.beesocc.mobile.core.ApiClient
import com.beesocc.mobile.core.UploadFile
import org.json.JSONObject

private fun cuerpo(vararg pares: Pair<String, Any?>): JSONObject = JSONObject().apply {
    pares.forEach { (clave, valor) ->
        when (valor) {
            null -> put(clave, JSONObject.NULL)
            is Number, is Boolean, is String, is JSONObject -> put(clave, valor)
            else -> put(clave, valor.toString())
        }
    }
}

object AuthRepo {
    suspend fun login(c: Context, email: String, password: String) =
        ApiClient.post(c, "/login", cuerpo("email" to email, "password" to password)).comoObjeto()

    suspend fun registro(c: Context, nombre: String, email: String, password: String) =
        ApiClient.post(c, "/registro", cuerpo(
            "name" to nombre,
            "email" to email,
            "password" to password,
            "password_confirmation" to password,
        )).comoObjeto()

    suspend fun usuario(c: Context) = ApiClient.get(c, "/usuario").comoObjeto()

    suspend fun logout(c: Context) = ApiClient.post(c, "/logout")
}

object LigaRepo {
    suspend fun listar(c: Context) = ApiClient.get(c, "/campeonatos")
    suspend fun obtener(c: Context, id: Long) = ApiClient.get(c, "/campeonatos/$id").comoObjeto()
    suspend fun crear(c: Context, datos: JSONObject) = ApiClient.post(c, "/campeonatos", datos).comoObjeto()
    suspend fun crearConLogo(c: Context, campos: Map<String, String>, archivo: UploadFile) =
        ApiClient.postMultipart(c, "/campeonatos", archivo.copy(campos = campos)).comoObjeto()
    suspend fun actualizar(c: Context, id: Long, datos: JSONObject) = ApiClient.patch(c, "/campeonatos/$id", datos).comoObjeto()
    suspend fun actualizarConLogo(c: Context, id: Long, campos: Map<String, String>, archivo: UploadFile) =
        ApiClient.postMultipart(c, "/campeonatos/$id", archivo.copy(
            campos = campos + ("_method" to "PATCH"),
        )).comoObjeto()
    suspend fun eliminar(c: Context, id: Long) = ApiClient.delete(c, "/campeonatos/$id")
    suspend fun publicacion(c: Context, id: Long, publicado: Boolean) =
        ApiClient.patch(c, "/campeonatos/$id/publicacion", cuerpo("publicado" to publicado)).comoObjeto()
    suspend fun iniciar(c: Context, id: Long) = ApiClient.post(c, "/campeonatos/$id/iniciar").comoObjeto()
    suspend fun finalizar(c: Context, id: Long) = ApiClient.post(c, "/campeonatos/$id/finalizar").comoObjeto()
    suspend fun requisitos(c: Context, id: Long, modalidad: String, minimo: Int) =
        ApiClient.patch(c, "/campeonatos/$id/requisitos-inicio",
            cuerpo("modalidad" to modalidad, "minimo_jugadores" to minimo)).comoObjeto()
    suspend fun auditoriaCiclo(c: Context, id: Long) = ApiClient.get(c, "/campeonatos/$id/auditoria-ciclo")
    suspend fun tabla(c: Context, id: Long) = ApiClient.get(c, "/campeonatos/$id/tabla-posiciones").comoObjeto()
    suspend fun estadisticas(c: Context, id: Long, tipo: String) = ApiClient.get(c, "/campeonatos/$id/estadisticas/$tipo").comoObjeto()
    suspend fun configuracion(c: Context, id: Long) = ApiClient.get(c, "/campeonatos/$id/configuracion").comoObjeto()
    suspend fun guardarConfiguracion(c: Context, id: Long, datos: JSONObject, crear: Boolean) =
        if (crear) ApiClient.post(c, "/campeonatos/$id/configuracion", datos).comoObjeto()
        else ApiClient.patch(c, "/campeonatos/$id/configuracion", datos).comoObjeto()
}

object EquipoRepo {
    suspend fun listar(c: Context) = ApiClient.get(c, "/equipos")
    suspend fun crear(c: Context, datos: JSONObject) = ApiClient.post(c, "/equipos", datos).comoObjeto()
    suspend fun crearConEscudo(c: Context, campos: Map<String, String>, archivo: UploadFile) =
        ApiClient.postMultipart(c, "/equipos", archivo.copy(campos = campos)).comoObjeto()
    suspend fun actualizar(c: Context, id: Long, datos: JSONObject) = ApiClient.patch(c, "/equipos/$id", datos).comoObjeto()
    suspend fun actualizarConEscudo(c: Context, id: Long, campos: Map<String, String>, archivo: UploadFile) =
        ApiClient.postMultipart(c, "/equipos/$id", archivo.copy(campos = campos + ("_method" to "PATCH"))).comoObjeto()
    suspend fun eliminar(c: Context, id: Long) = ApiClient.delete(c, "/equipos/$id")
    suspend fun inscripciones(c: Context, ligaId: Long) = ApiClient.get(c, "/campeonatos/$ligaId/inscripciones-equipos")
    suspend fun inscribir(c: Context, ligaId: Long, equipoId: Long) =
        ApiClient.post(c, "/campeonatos/$ligaId/inscripciones-equipos", cuerpo("equipo_id" to equipoId)).comoObjeto()
    suspend fun estadoInscripcion(c: Context, id: Long, estado: String) =
        ApiClient.patch(c, "/inscripciones-equipos/$id", cuerpo("estado" to estado)).comoObjeto()
    suspend fun eliminarInscripcion(c: Context, id: Long) = ApiClient.delete(c, "/inscripciones-equipos/$id")
}

object JugadorRepo {
    suspend fun listar(c: Context) = ApiClient.get(c, "/jugadores")
    suspend fun crear(c: Context, datos: JSONObject) = ApiClient.post(c, "/jugadores", datos).comoObjeto()
    suspend fun crearConFoto(c: Context, campos: Map<String, String>, archivo: UploadFile) =
        ApiClient.postMultipart(c, "/jugadores", archivo.copy(campos = campos)).comoObjeto()
    suspend fun actualizar(c: Context, id: Long, datos: JSONObject) = ApiClient.patch(c, "/jugadores/$id", datos).comoObjeto()
    suspend fun actualizarConFoto(c: Context, id: Long, campos: Map<String, String>, archivo: UploadFile) =
        ApiClient.postMultipart(c, "/jugadores/$id", archivo.copy(campos = campos + ("_method" to "PATCH"))).comoObjeto()
    suspend fun eliminar(c: Context, id: Long) = ApiClient.delete(c, "/jugadores/$id")
    suspend fun plantillas(c: Context, inscripcionId: Long) = ApiClient.get(c, "/inscripciones-equipos/$inscripcionId/plantillas")
    suspend fun inscribir(c: Context, inscripcionId: Long, datos: JSONObject) =
        ApiClient.post(c, "/inscripciones-equipos/$inscripcionId/plantillas", datos).comoObjeto()
    suspend fun actualizarPlantilla(c: Context, id: Long, datos: JSONObject) =
        ApiClient.patch(c, "/plantillas/$id", datos).comoObjeto()
    suspend fun eliminarPlantilla(c: Context, id: Long) = ApiClient.delete(c, "/plantillas/$id")
}

object PartidoRepo {
    suspend fun listar(c: Context, ligaId: Long) = ApiClient.get(c, "/campeonatos/$ligaId/partidos")
    suspend fun obtener(c: Context, id: Long) = ApiClient.get(c, "/partidos/$id").comoObjeto()
    suspend fun marcador(c: Context, id: Long) = ApiClient.get(c, "/partidos/$id/marcador").comoObjeto()
    suspend fun crear(c: Context, ligaId: Long, datos: JSONObject) =
        ApiClient.post(c, "/campeonatos/$ligaId/partidos", datos).comoObjeto()
    suspend fun actualizar(c: Context, id: Long, datos: JSONObject) = ApiClient.patch(c, "/partidos/$id", datos).comoObjeto()
    suspend fun cambiarEstado(c: Context, id: Long, estado: String) =
        ApiClient.patch(c, "/partidos/$id", cuerpo("estado" to estado)).comoObjeto()
    suspend fun cambiarEstadoConVersion(c: Context, id: Long, estado: String, version: Int) =
        ApiClient.patch(c, "/partidos/$id",
            cuerpo("estado" to estado, "version_panel" to version)).comoObjeto()
    suspend fun finalizar(c: Context, id: Long) = ApiClient.post(c, "/partidos/$id/finalizar").comoObjeto()
    suspend fun eliminar(c: Context, id: Long) = ApiClient.delete(c, "/partidos/$id")
    suspend fun generarCalendario(c: Context, ligaId: Long) = ApiClient.post(c, "/campeonatos/$ligaId/generar-calendario").comoObjeto()
    suspend fun eventos(c: Context, id: Long) = ApiClient.get(c, "/partidos/$id/eventos")
    suspend fun crearEvento(c: Context, id: Long, datos: JSONObject) =
        ApiClient.post(c, "/partidos/$id/eventos", datos).comoObjeto()
    suspend fun corregirMinuto(c: Context, eventoId: Long, minuto: Int) =
        ApiClient.patch(c, "/eventos/$eventoId", cuerpo("minuto" to minuto)).comoObjeto()
    suspend fun eliminarEvento(c: Context, eventoId: Long) = ApiClient.delete(c, "/eventos/$eventoId")
}

object ArbitroRepo {
    suspend fun misArbitrajes(c: Context) = ApiClient.get(c, "/mis-arbitrajes").comoObjeto()
    suspend fun partidos(c: Context, ligaId: Long) = ApiClient.get(c, "/mis-arbitrajes/$ligaId/partidos")
    suspend fun vigente(c: Context, ligaId: Long) = ApiClient.get(c, "/campeonatos/$ligaId/arbitro").comoObjeto()
    suspend fun historial(c: Context, ligaId: Long) = ApiClient.get(c, "/campeonatos/$ligaId/arbitro/historial").comoObjeto()
    suspend fun buscar(c: Context, ligaId: Long, email: String) =
        ApiClient.post(c, "/campeonatos/$ligaId/arbitro/buscar-usuario", cuerpo("email" to email)).comoObjeto()
    suspend fun asignar(c: Context, ligaId: Long, usuarioId: Long) =
        ApiClient.post(c, "/campeonatos/$ligaId/arbitro", cuerpo("usuario_id" to usuarioId)).comoObjeto()
    suspend fun reemplazar(c: Context, ligaId: Long, usuarioId: Long, motivo: String) =
        ApiClient.patch(c, "/campeonatos/$ligaId/arbitro", cuerpo("usuario_id" to usuarioId, "motivo_revocacion" to motivo)).comoObjeto()
    suspend fun revocar(c: Context, ligaId: Long, motivo: String) =
        ApiClient.delete(c, "/campeonatos/$ligaId/arbitro", cuerpo("motivo_revocacion" to motivo)).comoObjeto()
    suspend fun plantillasArbitrales(c: Context, partidoId: Long) = ApiClient.get(c, "/partidos/$partidoId/plantillas-arbitrales").comoObjeto()
    suspend fun panel(c: Context, partidoId: Long) = ApiClient.get(c, "/partidos/$partidoId/panel-arbitral").comoObjeto()
    suspend fun preparar(c: Context, partidoId: Long, datos: JSONObject) = ApiClient.put(c, "/partidos/$partidoId/panel-arbitral", datos).comoObjeto()
    suspend fun reloj(c: Context, partidoId: Long, accion: String, version: Int) =
        ApiClient.patch(c, "/partidos/$partidoId/panel-arbitral/reloj",
            cuerpo("accion" to accion, "version" to version)).comoObjeto()
}

object FaseRepo {
    suspend fun listar(c: Context, ligaId: Long) = ApiClient.get(c, "/campeonatos/$ligaId/fases")
    suspend fun crear(c: Context, ligaId: Long, nombre: String, tipo: String) =
        ApiClient.post(c, "/campeonatos/$ligaId/fases", cuerpo("nombre" to nombre, "tipo" to tipo)).comoObjeto()
    suspend fun obtener(c: Context, faseId: Long) = ApiClient.get(c, "/fases/$faseId").comoObjeto()
    suspend fun auditoria(c: Context, faseId: Long) = ApiClient.get(c, "/fases/$faseId/auditoria")
    suspend fun previsualizarLlaves(c: Context, faseId: Long, datos: JSONObject) =
        ApiClient.post(c, "/fases/$faseId/previsualizar-llaves", datos).comoObjeto()
    suspend fun generarLlaves(c: Context, faseId: Long, datos: JSONObject) =
        ApiClient.post(c, "/fases/$faseId/generar-llaves", datos).comoObjeto()
    suspend fun avanzar(c: Context, llaveId: Long) = ApiClient.post(c, "/llaves/$llaveId/avanzar").comoObjeto()
    suspend fun penales(c: Context, llaveId: Long, datos: JSONObject) = ApiClient.post(c, "/llaves/$llaveId/penales", datos).comoObjeto()
}

object GrupoRepo {
    suspend fun previsualizar(c: Context, faseId: Long, datos: JSONObject) =
        ApiClient.post(c, "/fases/$faseId/previsualizar-grupos", datos).comoObjeto()
    suspend fun confirmar(c: Context, faseId: Long, datos: JSONObject) =
        ApiClient.post(c, "/fases/$faseId/confirmar-grupos", datos).comoObjeto()
    suspend fun calendario(c: Context, faseId: Long) = ApiClient.post(c, "/fases/$faseId/generar-calendario-grupos").comoObjeto()
    suspend fun tabla(c: Context, grupoId: Long, vista: String = "oficial") =
        ApiClient.get(c, "/grupos/$grupoId/tabla", mapOf("vista" to vista)).comoObjeto()
    suspend fun clasificacion(c: Context, faseId: Long) = ApiClient.get(c, "/fases/$faseId/clasificacion-grupos").comoObjeto()
    suspend fun clasificar(c: Context, faseId: Long, confirmacion: String) =
        ApiClient.post(c, "/fases/$faseId/clasificar-grupos", cuerpo("confirmacion" to confirmacion)).comoObjeto()
    suspend fun desempates(c: Context, grupoId: Long) = ApiClient.get(c, "/grupos/$grupoId/desempates").comoObjeto()
    suspend fun previsualizarDesempate(c: Context, grupoId: Long, datos: JSONObject) =
        ApiClient.post(c, "/grupos/$grupoId/previsualizar-desempate", datos).comoObjeto()
    suspend fun generarDesempate(c: Context, grupoId: Long, datos: JSONObject) =
        ApiClient.post(c, "/grupos/$grupoId/generar-desempate", datos).comoObjeto()
    suspend fun previsualizarEliminacion(c: Context, faseId: Long, datos: JSONObject) =
        ApiClient.post(c, "/fases/$faseId/previsualizar-eliminacion", datos).comoObjeto()
    suspend fun generarEliminacion(c: Context, faseId: Long, datos: JSONObject) =
        ApiClient.post(c, "/fases/$faseId/generar-eliminacion", datos).comoObjeto()
}

object CorreccionRepo {
    suspend fun listar(c: Context, partidoId: Long) = ApiClient.get(c, "/partidos/$partidoId/correcciones")
    suspend fun auditoria(c: Context, partidoId: Long) = ApiClient.get(c, "/partidos/$partidoId/auditoria")
    suspend fun solicitar(c: Context, partidoId: Long, motivo: String, alcance: String = "eventos") =
        ApiClient.post(c, "/partidos/$partidoId/correcciones", cuerpo("motivo" to motivo, "alcance" to alcance)).comoObjeto()
    suspend fun resolver(c: Context, id: Long, estado: String) =
        ApiClient.patch(c, "/correcciones/$id/autorizar", cuerpo("estado" to estado)).comoObjeto()
    suspend fun aplicar(c: Context, id: Long, operaciones: org.json.JSONArray) =
        ApiClient.post(c, "/correcciones/$id/aplicar", JSONObject().put("operaciones", operaciones)).comoObjeto()
    suspend fun resultadoAdministrativo(c: Context, partidoId: Long, datos: JSONObject) =
        ApiClient.post(c, "/partidos/$partidoId/resultado-administrativo", datos).comoObjeto()
}

object PublicoRepo {
    suspend fun listar(c: Context) = ApiClient.get(c, "/publico/campeonatos")
    suspend fun porCodigo(c: Context, codigo: String) = ApiClient.get(c, "/publico/campeonatos/por-codigo", mapOf("codigo" to codigo)).comoObjeto()
    suspend fun obtener(c: Context, id: Long) = ApiClient.get(c, "/publico/campeonatos/$id").comoObjeto()
    suspend fun equipos(c: Context, id: Long) = ApiClient.get(c, "/publico/campeonatos/$id/equipos")
    suspend fun plantilla(c: Context, id: Long, inscripcion: Long) = ApiClient.get(c, "/publico/campeonatos/$id/equipos/$inscripcion/plantilla")
    suspend fun jugador(c: Context, id: Long, jugador: Long) = ApiClient.get(c, "/publico/campeonatos/$id/jugadores/$jugador").comoObjeto()
    suspend fun partidos(c: Context, id: Long) = ApiClient.get(c, "/publico/campeonatos/$id/partidos")
    suspend fun partido(c: Context, id: Long, partido: Long) = ApiClient.get(c, "/publico/campeonatos/$id/partidos/$partido").comoObjeto()
    suspend fun tabla(c: Context, id: Long) = ApiClient.get(c, "/publico/campeonatos/$id/tabla").comoObjeto()
    suspend fun deporte(c: Context, id: Long) = ApiClient.get(c, "/publico/campeonatos/$id/deporte").comoObjeto()
    suspend fun estadisticas(c: Context, id: Long, tipo: String) = ApiClient.get(c, "/publico/campeonatos/$id/estadisticas/$tipo").comoObjeto()
    suspend fun identidades(c: Context, id: Long) = ApiClient.get(c, "/publico/campeonatos/$id/identidades-deportivas").comoObjeto()
    suspend fun evento(c: Context, id: Long, partido: Long) = ApiClient.get(c, "/publico/campeonatos/$id/partidos/$partido/eventos")
}

private fun Any.comoObjeto(): JSONObject = this as? JSONObject ?: JSONObject()

package com.beesocc.mobile.core

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.CookieHandler
import java.net.CookieManager
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import java.net.URLEncoder
import java.util.UUID

/** Archivo opcional para los formularios que suben logo, escudo o foto. */
data class UploadFile(
    val campo: String,
    val nombre: String,
    val mime: String,
    val bytes: ByteArray,
    val campos: Map<String, String> = emptyMap(),
)

/**
 * Cliente HTTP basado en HttpURLConnection.
 * Laravel usa sesion + cookie CSRF (no tokens Bearer), por eso:
 * - se conservan cookies con CookieManager,
 * - se pide /sanctum/csrf-cookie antes de escribir,
 * - se envia X-XSRF-TOKEN con el valor decodificado,
 * - se envia Origin: http://localhost para que Sanctum trate la peticion como stateful.
 */
object ApiClient {
    private const val ORIGIN = "http://localhost"
    private const val TIMEOUT = 20_000

    @Volatile
    private var csrf: String? = null

    fun reset() {
        csrf = null
    }

    suspend fun ensureCsrf(context: Context, forzar: Boolean = false) = withContext(Dispatchers.IO) {
        if (!csrf.isNullOrBlank() && !forzar) return@withContext
        val conexion = URL(ApiConfig.baseUrl(context) + "/sanctum/csrf-cookie").openConnection() as HttpURLConnection
        try {
            conexion.requestMethod = "GET"
            conexion.connectTimeout = TIMEOUT
            conexion.readTimeout = TIMEOUT
            conexion.setRequestProperty("Accept", "application/json")
            conexion.setRequestProperty("Origin", ORIGIN)
            conexion.setRequestProperty("Referer", "$ORIGIN/")
            conexion.inputStream.use { it.readBytes() }
            csrf = leerCsrf(context)
            if (csrf.isNullOrBlank()) {
                throw ApiException(419, "No se pudo iniciar la sesión de seguridad. Intenta de nuevo.")
            }
        } finally {
            conexion.disconnect()
        }
    }

    private fun leerCsrf(context: Context): String? {
        val manager = CookieHandler.getDefault() as? CookieManager ?: return null
        val cookies = runCatching { manager.cookieStore.get(URI(ApiConfig.baseUrl(context))) }.getOrNull()
            ?: return null
        return cookies.filter { it.name == "XSRF-TOKEN" }
            .maxByOrNull { it.maxAge }
            ?.value
            ?.let { runCatching { java.net.URLDecoder.decode(it, "UTF-8") }.getOrNull() }
    }

    suspend fun get(context: Context, ruta: String, parametros: Map<String, String> = emptyMap()): Any =
        solicitar(context, "GET", ruta, parametros = parametros)

    suspend fun post(context: Context, ruta: String, cuerpo: JSONObject? = null): Any =
        solicitar(context, "POST", ruta, cuerpo)

    suspend fun postMultipart(context: Context, ruta: String, archivo: UploadFile): Any =
        solicitar(context, "POST", ruta, archivo = archivo)

    suspend fun put(context: Context, ruta: String, cuerpo: JSONObject? = null): Any =
        solicitar(context, "PUT", ruta, cuerpo)

    suspend fun patch(context: Context, ruta: String, cuerpo: JSONObject? = null): Any =
        solicitar(context, "PATCH", ruta, cuerpo)

    suspend fun delete(context: Context, ruta: String, cuerpo: JSONObject? = null): Any =
        solicitar(context, "DELETE", ruta, cuerpo)

    private suspend fun solicitar(
        context: Context,
        metodo: String,
        ruta: String,
        cuerpo: JSONObject? = null,
        parametros: Map<String, String> = emptyMap(),
        archivo: UploadFile? = null,
        reintentar: Boolean = true,
    ): Any = withContext(Dispatchers.IO) {
        val esEscritura = metodo != "GET" && metodo != "HEAD"
        if (esEscritura && csrf.isNullOrBlank()) ensureCsrf(context)

        val url = construirUrl(context, ruta, parametros)
        val conexion = URL(url).openConnection() as HttpURLConnection
        try {
            conexion.requestMethod = metodo
            conexion.connectTimeout = TIMEOUT
            conexion.readTimeout = TIMEOUT
            conexion.setRequestProperty("Accept", "application/json")
            conexion.setRequestProperty("X-Requested-With", "XMLHttpRequest")
            conexion.setRequestProperty("Origin", ORIGIN)
            conexion.setRequestProperty("Referer", "$ORIGIN/")
            csrf?.let { conexion.setRequestProperty("X-XSRF-TOKEN", it) }

            if (archivo != null) {
                conexion.doOutput = true
                conexion.setRequestProperty("Content-Type", "multipart/form-data; boundary=$BOUNDARIO")
                conexion.outputStream.use { escribirMultipart(it, archivo) }
            } else if (cuerpo != null) {
                conexion.doOutput = true
                conexion.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                conexion.outputStream.use { it.write(cuerpo.toString().toByteArray(Charsets.UTF_8)) }
            }

            val codigo = conexion.responseCode
            val texto = (if (codigo in 200..299) conexion.inputStream else conexion.errorStream)
                ?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()

            if (codigo == 419 && reintentar && esEscritura) {
                ensureCsrf(context, forzar = true)
                return@withContext solicitar(context, metodo, ruta, cuerpo, parametros, archivo, false)
            }

            if (codigo !in 200..299) throw parsearError(codigo, texto)
            if (texto.isBlank()) return@withContext JSONObject()
            val parseado = runCatching { JSONArray(texto) }.getOrNull()
            return@withContext parseado ?: runCatching { JSONObject(texto) }.getOrDefault(JSONObject())
        } catch (e: ApiException) {
            throw e
        } catch (e: Exception) {
            throw ApiException(0, "No se pudo conectar. Revisa la URL de la API y tu red.")
        } finally {
            conexion.disconnect()
        }
    }

    private fun construirUrl(context: Context, ruta: String, parametros: Map<String, String>): String {
        val base = ApiConfig.apiUrl(context, ruta)
        if (parametros.isEmpty()) return base
        val query = parametros.entries.joinToString("&") { (clave, valor) ->
            "${URLEncoder.encode(clave, "UTF-8")}=${URLEncoder.encode(valor, "UTF-8")}"
        }
        return if (base.contains("?")) "$base&$query" else "$base?$query"
    }

    private fun escribirMultipart(salida: java.io.OutputStream, archivo: UploadFile) {
        val escribirTexto = { clave: String, valor: String ->
            salida.write("--$BOUNDARIO\r\n".toByteArray())
            salida.write("Content-Disposition: form-data; name=\"$clave\"\r\n\r\n".toByteArray())
            salida.write(valor.toByteArray(Charsets.UTF_8))
            salida.write("\r\n".toByteArray())
        }
        archivo.campos.forEach { (clave, valor) -> escribirTexto(clave, valor) }
        salida.write("--$BOUNDARIO\r\n".toByteArray())
        salida.write("Content-Disposition: form-data; name=\"${archivo.campo}\"; filename=\"${archivo.nombre}\"\r\n".toByteArray())
        salida.write("Content-Type: ${archivo.mime}\r\n\r\n".toByteArray())
        salida.write(archivo.bytes)
        salida.write("\r\n".toByteArray())
        salida.write("--$BOUNDARIO--\r\n".toByteArray())
    }

    private fun parsearError(codigo: Int, texto: String): ApiException {
        var mensaje = when (codigo) {
            401 -> "Tu sesión terminó. Inicia sesión de nuevo."
            403 -> "No tienes permiso para realizar esta operación."
            404 -> "No encontramos este registro."
            419 -> "La sesión de seguridad venció. Intenta de nuevo."
            422 -> "Revisa los campos del formulario."
            429 -> "Demasiados intentos. Espera un minuto."
            else -> "No se pudo completar la operación."
        }
        val errores = linkedMapOf<String, String>()
        runCatching {
            val objeto = JSONObject(texto)
            objeto.optString("message").takeIf { it.isNotBlank() }?.let { mensaje = limpiar(it) }
            objeto.optJSONObject("errors")?.let { libros ->
                libros.keys().forEach { clave ->
                    val arreglo = libros.optJSONArray(clave)
                    val valor = arreglo?.optString(0) ?: libros.optString(clave)
                    if (valor.isNotBlank()) errores[clave] = limpiar(valor)
                }
            }
        }
        return ApiException(codigo, mensaje, errores)
    }

    private fun limpiar(texto: String): String {
        return if (texto.length > 240 || Regex("SQLSTATE|Illuminate|Stack trace|Exception|vendor[/\\\\]", RegexOption.IGNORE_CASE).containsMatchIn(texto)) {
            "No se pudo completar la operación."
        } else texto
    }

    private val BOUNDARIO = "BeesoccBoundary${UUID.randomUUID()}"
}

/** Utilidad para convertir respuestas que a veces llegan como objeto y a veces como arreglo. */
fun Any.comoObjeto(): JSONObject = this as? JSONObject ?: JSONObject()

fun Any.comoArreglo(): JSONArray = this as? JSONArray ?: JSONArray()

/** Busca el primer arreglo util dentro de una respuesta JSON. */
fun Any.primerArreglo(vararg claves: String): JSONArray {
    (this as? JSONArray)?.let { return it }
    val objeto = comoObjeto()
    for (clave in claves) {
        val arreglo = objeto.optJSONArray(clave)
        if (arreglo != null) return arreglo
    }
    val iterador = objeto.keys()
    while (iterador.hasNext()) {
        val valor = objeto.opt(iterador.next())
        if (valor is JSONArray) return valor
    }
    return JSONArray()
}

fun JSONArray.objetos(): List<JSONObject> = (0 until length()).mapNotNull { optJSONObject(it) }

fun JSONObject.texto(clave: String, porDefecto: String = ""): String = optString(clave, porDefecto) ?: porDefecto

fun JSONObject.entero(clave: String, porDefecto: Int = 0): Int = optInt(clave, porDefecto)

fun JSONObject.largo(clave: String, porDefecto: Long = 0L): Long = optLong(clave, porDefecto)

fun JSONObject.booleano(clave: String): Boolean = optBoolean(clave, false)

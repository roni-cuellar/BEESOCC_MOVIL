package com.beesocc.mobile.ui.fragments

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.beesocc.mobile.R
import com.beesocc.mobile.core.StateView
import com.beesocc.mobile.core.gone
import com.beesocc.mobile.core.objetos
import com.beesocc.mobile.core.primerArreglo
import com.beesocc.mobile.core.texto
import com.beesocc.mobile.core.visible
import com.beesocc.mobile.databinding.FragmentFaseBinding
import com.beesocc.mobile.databinding.ViewStateBinding
import com.beesocc.mobile.ui.abrir
import com.beesocc.mobile.model.auditoriaItem
import com.beesocc.mobile.model.filaTablaItem
import com.beesocc.mobile.model.grupoItem
import com.beesocc.mobile.model.llaveItem
import com.beesocc.mobile.repository.EquipoRepo
import com.beesocc.mobile.repository.FaseRepo
import com.beesocc.mobile.repository.GrupoRepo
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

class FaseFragment : Fragment(R.layout.fragment_fase) {
    private var _binding: FragmentFaseBinding? = null
    private val binding get() = _binding!!
    private lateinit var estado: StateView
    private lateinit var adaptador: ListaAdapter
    private var vistaPrevia: JSONObject? = null
    private var datosPrevia: JSONObject? = null
    private var mostrandoAuditoria = false
    private var fase: JSONObject = JSONObject()

    private val faseId: Long get() = arguments?.getLong("faseId", 0L) ?: 0L
    private val tipo: String get() = arguments?.getString("tipo").orEmpty()
    private val esGrupos: Boolean get() = tipo == "grupos"

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentFaseBinding.bind(view)
        titulo(if (esGrupos) "Fase de grupos" else "Cuadro eliminatorio")
        estado = StateView(binding.estado, binding.faseContenido, ::cargar)

        adaptador = ListaAdapter(object : ListaAdapter.Escuchas {
            override fun onItem(item: ItemLista) = alPulsar(item)
            override fun onPrimario(item: ItemLista) = avanzarLlave(item)
            override fun onSecundario(item: ItemLista) = abrirPenales(item)
        })
        binding.faseRecycler.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(requireContext())
        binding.faseRecycler.adapter = adaptador

        binding.faseCalendario.visibility = if (esGrupos) View.VISIBLE else View.GONE
        binding.faseClasificar.visibility = if (esGrupos) View.VISIBLE else View.GONE
        binding.fasePrevisualizar.text = if (esGrupos) "Previsualizar grupos" else "Previsualizar llaves"
        binding.faseConfirmar.text = if (esGrupos) "Confirmar grupos" else "Generar llaves"

        binding.faseAuditoria.setOnClickListener {
            mostrandoAuditoria = !mostrandoAuditoria
            binding.faseAuditoria.text = if (mostrandoAuditoria) "Volver a la fase" else "Auditoría"
            cargar()
        }
        binding.fasePrevisualizar.setOnClickListener { abrirPrevisualizacion() }
        binding.faseConfirmar.setOnClickListener { confirmarPrevisualizacion() }
        binding.faseCalendario.setOnClickListener {
            lifecycleScope.launch {
                try {
                    val respuesta = GrupoRepo.calendario(requireContext(), faseId)
                    aviso("Calendario generado: ${respuesta.optInt("partidos_creados")} partidos.", true)
                    cargar()
                } catch (e: Exception) { aviso(e.message ?: "No se pudo generar el calendario.", true) }
            }
        }
        binding.faseClasificar.setOnClickListener {
            lifecycleScope.launch {
                try {
                    val clasificacion = GrupoRepo.clasificacion(requireContext(), faseId)
                    val firma = clasificacion.optString("confirmacion")
                    if (firma.isBlank()) { aviso("La clasificación aún no está lista."); return@launch }
                    confirmar("Clasificar grupos", "¿Confirmar la clasificación y bloquear los resultados?") {
                        lifecycleScope.launch {
                            try {
                                GrupoRepo.clasificar(requireContext(), faseId, firma)
                                aviso("Clasificación confirmada.")
                                cargar()
                            } catch (e: Exception) { aviso(e.message ?: "No se pudo clasificar.", true) }
                        }
                    }
                } catch (e: Exception) { aviso(e.message ?: "No se pudo consultar la clasificación.", true) }
            }
        }

        escucharFormulario("form_previa_grupos") { datos ->
            val valores = datos.valoresFormulario()
            val payload = JSONObject().apply {
                put("metodo", valores["metodo"].orEmpty())
                put("cantidad_grupos", valores["cantidad_grupos"]?.toIntOrNull() ?: 2)
                put("clasificados_por_grupo", valores["clasificados_por_grupo"]?.toIntOrNull() ?: 1)
                valores["grupos"]?.takeIf { it.isNotBlank() }?.let { put("grupos", JSONArray(it)) }
            }
            previsualizar(payload)
        }
        escucharFormulario("form_previa_llaves") { datos ->
            val valores = datos.valoresFormulario()
            siembra = valores["siembra"].orEmpty()
                .split(Regex("[,;\\s]+")).mapNotNull { it.toLongOrNull() }
            rondas = valores["rondas"]?.toIntOrNull()?.coerceIn(1, 15) ?: 1
            modalidad = valores["modalidad"].orEmpty()
            desempate = valores["desempate"].orEmpty()
            previsualizar(payloadCuadro())
        }
        escucharFormulario("form_penales") { datos ->
            val valores = datos.valoresFormulario()
            val llaveId = datos.getLong("id", 0L)
            lifecycleScope.launch {
                try {
                    FaseRepo.penales(requireContext(), llaveId, JSONObject().apply {
                        put("penales_plaza_1", valores["penales_plaza_1"]?.toIntOrNull() ?: 0)
                        put("penales_plaza_2", valores["penales_plaza_2"]?.toIntOrNull() ?: 0)
                        put("motivo", valores["motivo"].orEmpty())
                    })
                    aviso("Resultado de penales registrado.")
                    cargar()
                } catch (e: Exception) { aviso(e.message ?: "No se pudieron registrar los penales.", true) }
            }
        }
        cargar()
    }

    private var siembra: List<Long> = emptyList()
    private var rondas: Int = 1
    private var modalidad: String = "unico"
    private var desempate: String = "penales"

    private fun payloadCuadro(): JSONObject = JSONObject().apply {
        put("siembra", JSONArray(siembra))
        val lista = JSONArray()
        repeat(rondas) {
            lista.put(JSONObject().put("modalidad", modalidad).put("desempate", desempate))
        }
        put("rondas", lista)
    }

    private fun abrirPrevisualizacion() {
        if (esGrupos) {
            abrir(FormFragment.nuevo("Previsualizar grupos", listOf(
                FormField("metodo", "Método", TipoCampo.SELECT, "automatico", requerido = true,
                    opciones = listOf("Automático", "Manual"), valores = listOf("automatico", "manual")),
                FormField("cantidad_grupos", "Cantidad de grupos", TipoCampo.NUMERO, "2", requerido = true),
                FormField("clasificados_por_grupo", "Clasificados por grupo", TipoCampo.NUMERO, "1", requerido = true),
                FormField("grupos", "Grupos manuales (JSON opcional)", TipoCampo.AREA,
                    hint = "[[12,13],[14,15]]"),
            ), "form_previa_grupos", "Previsualizar"))
        } else {
            val desdeGrupos = fase.optLong("fase_origen_id") > 0
            if (desdeGrupos) {
                previsualizar(JSONObject().apply {
                    put("metodo", "automatico")
                    put("rondas", JSONArray().put(JSONObject().put("modalidad", "unico").put("desempate", "penales")))
                })
            } else {
                lifecycleScope.launch {
                    val ligaId = fase.optLong("campeonato_id")
                    val sugerencia = if (ligaId > 0) runCatching {
                        EquipoRepo.inscripciones(requireContext(), ligaId).primerArreglo("inscripciones", "data")
                            .objetos().filter { it.texto("estado") == "activo" }
                            .joinToString(",") { it.optLong("id").toString() }
                    }.getOrDefault("") else ""
                    abrir(FormFragment.nuevo("Previsualizar llaves", listOf(
                        FormField("siembra", "IDs de inscripción en orden (separados por coma)", TipoCampo.AREA,
                            sugerencia, requerido = true),
                        FormField("rondas", "Número de rondas", TipoCampo.NUMERO, rondas.toString(), requerido = true),
                        FormField("modalidad", "Modalidad", TipoCampo.SELECT, modalidad, requerido = true,
                            opciones = listOf("Partido único", "Ida y vuelta"), valores = listOf("unico", "ida_vuelta")),
                        FormField("desempate", "Desempate", TipoCampo.SELECT, desempate, requerido = true,
                            opciones = listOf("Penales", "Prórroga y penales"), valores = listOf("penales", "prorroga_penales")),
                    ), "form_previa_llaves", "Previsualizar"))
                }
            }
        }
    }

    private fun previsualizar(payload: JSONObject) {
        lifecycleScope.launch {
            try {
                val respuesta = when {
                    esGrupos -> GrupoRepo.previsualizar(requireContext(), faseId, payload)
                    fase.optLong("fase_origen_id") > 0 -> GrupoRepo.previsualizarEliminacion(requireContext(), faseId, payload)
                    else -> FaseRepo.previsualizarLlaves(requireContext(), faseId, payload)
                }
                vistaPrevia = respuesta
                datosPrevia = payload
                val resumen = respuesta.toString(2).take(1600)
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Vista previa")
                    .setMessage(resumen)
                    .setPositiveButton("Entendido") { _, _ ->
                        aviso("Vista previa lista. Pulsa «${binding.faseConfirmar.text}» para aplicar.", true)
                    }
                    .show()
            } catch (e: Exception) { aviso(e.message ?: "No se pudo previsualizar.", true) }
        }
    }

    private fun confirmarPrevisualizacion() {
        val previa = vistaPrevia
        if (previa == null) {
            aviso("Primero genera una vista previa.")
            return
        }
        lifecycleScope.launch {
            try {
                val cuerpo = JSONObject(previa.toString()).apply { remove("fase_id") }
                if (esGrupos) {
                    GrupoRepo.confirmar(requireContext(), faseId, cuerpo)
                    aviso("Grupos confirmados.")
                } else if (fase.optLong("fase_origen_id") > 0) {
                    GrupoRepo.generarEliminacion(requireContext(), faseId, cuerpo)
                    aviso("Eliminación generada desde grupos.")
                } else {
                    datosPrevia?.let { cuerpo.put("confirmacion", it.optString("confirmacion", previa.optString("confirmacion"))) }
                    if (!cuerpo.has("confirmacion")) cuerpo.put("confirmacion", previa.optString("confirmacion"))
                    FaseRepo.generarLlaves(requireContext(), faseId, cuerpo)
                    aviso("Llaves generadas.")
                }
                vistaPrevia = null
                cargar()
            } catch (e: Exception) { aviso(e.message ?: "No se pudo confirmar la estructura.", true) }
        }
    }

    private fun alPulsar(item: ItemLista) {
        if (mostrandoAuditoria) {
            MaterialAlertDialogBuilder(requireContext()).setTitle(item.titulo)
                .setMessage(listOf(item.subtitulo, item.meta).filter { it.isNotBlank() }.joinToString("\n"))
                .setPositiveButton(R.string.aceptar, null).show()
            return
        }
        if (item.datos.has("ronda") || item.datos.has("numero_serie")) {
            aviso("Usa «Avanzar» o «Penales» para gestionar esta llave.")
            return
        }
        lifecycleScope.launch {
            try {
                val tabla = GrupoRepo.tabla(requireContext(), item.id)
                val filas = tabla.primerArreglo("tabla").objetos()
                val texto = if (filas.isEmpty()) "Sin partidos registrados todavía."
                else filas.joinToString("\n") { "${it.optString("posicion")}. ${it.texto("nombre")} · PTS ${it.optInt("PTS")}" }
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle(item.titulo).setMessage(texto)
                    .setPositiveButton(R.string.aceptar, null).show()
            } catch (e: Exception) { aviso(e.message ?: "No se pudo consultar el grupo.", true) }
        }
    }

    private fun avanzarLlave(item: ItemLista) = confirmar("Avanzar llave", "¿Certificar el avance de ${item.titulo}?") {
        lifecycleScope.launch {
            try {
                FaseRepo.avanzar(requireContext(), item.id)
                aviso("Llave avanzada.")
                cargar()
            } catch (e: Exception) { aviso(e.message ?: "No se pudo avanzar.", true) }
        }
    }

    private fun abrirPenales(item: ItemLista) {
        val fragmento = FormFragment.nuevo("Registrar penales", listOf(
            FormField("penales_plaza_1", "Penales plaza 1 (0-255)", TipoCampo.NUMERO, requerido = true),
            FormField("penales_plaza_2", "Penales plaza 2 (0-255)", TipoCampo.NUMERO, requerido = true),
            FormField("motivo", "Motivo", requerido = true),
        ), "form_penales", "Registrar")
        fragmento.arguments?.putLong("id", item.id)
        abrir(fragmento)
    }

    private fun cargar() {
        lifecycleScope.launch {
            if (!mostrandoAuditoria) estado.cargando()
            try {
                val datos = FaseRepo.obtener(requireContext(), faseId)
                fase = datos.optJSONObject("fase") ?: datos
                binding.faseNombre.text = fase.texto("nombre", "Fase")
                binding.faseDetalle.text = listOf(
                    fase.texto("tipo"),
                    "Estado: ${fase.texto("estado").replace("_", " ")}",
                    "Clasificados por grupo: ${fase.optInt("clasificados_por_grupo")}",
                ).filter { it.isNotBlank() }.joinToString(" · ")

                val items: List<ItemLista> = if (mostrandoAuditoria) {
                    FaseRepo.auditoria(requireContext(), faseId).primerArreglo("auditorias", "data")
                        .objetos().map { it.auditoriaItem() }
                } else if (esGrupos) {
                    val grupos = fase.optJSONArray("grupos")?.objetos()?.map { it.grupoItem() } ?: emptyList()
                    if (grupos.isNotEmpty()) grupos
                    else fase.optJSONArray("tablas_oficiales")?.objetos()?.map { it.filaTablaItem() } ?: emptyList()
                } else {
                    val llaves = mutableListOf<ItemLista>()
                    val rondasJson = fase.optJSONArray("rondas") ?: JSONArray()
                    rondasJson.objetos().forEach { ronda ->
                        (ronda.optJSONArray("llaves") ?: JSONArray()).objetos().forEach { llaves.add(it.llaveItem()) }
                    }
                    llaves
                }
                adaptador.submitList(items)
                if (items.isEmpty() && !mostrandoAuditoria) {
                    estado.vacio(if (esGrupos) "Aún no hay grupos confirmados." else "El cuadro todavía no se generó.")
                } else estado.contenido()
            } catch (e: Exception) {
                estado.error(e.message ?: "No se pudo cargar la fase.")
            }
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    companion object {
        fun nuevo(faseId: Long, tipo: String) = FaseFragment().apply {
            arguments = Bundle().apply {
                putLong("faseId", faseId)
                putString("tipo", tipo)
            }
        }
    }
}

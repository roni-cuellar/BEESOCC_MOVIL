package com.beesocc.mobile.ui.fragments

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.beesocc.mobile.R
import com.beesocc.mobile.core.StateView
import com.beesocc.mobile.databinding.FragmentDashboardBinding
import com.beesocc.mobile.databinding.ViewStateBinding
import com.beesocc.mobile.model.Campeonato
import com.beesocc.mobile.repository.EquipoRepo
import com.beesocc.mobile.repository.LigaRepo
import com.beesocc.mobile.repository.PartidoRepo
import com.beesocc.mobile.ui.abrir
import com.beesocc.mobile.ui.aviso
import com.beesocc.mobile.ui.confirmar
import com.beesocc.mobile.ui.formulario.FormField
import com.beesocc.mobile.ui.formulario.FormFragment
import com.beesocc.mobile.ui.formulario.TipoCampo
import com.beesocc.mobile.ui.formulario.escucharFormulario
import com.beesocc.mobile.ui.formulario.valoresFormulario
import com.beesocc.mobile.ui.titulo
import kotlinx.coroutines.launch

class DashboardFragment : Fragment(R.layout.fragment_dashboard) {
    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!
    private lateinit var estado: StateView
    private var campeonato: Campeonato? = null

    private val ligaId: Long get() = arguments?.getLong("ligaId", 0L) ?: 0L
    private val ligaNombre: String get() = arguments?.getString("nombre").orEmpty()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentDashboardBinding.bind(view)
        estado = StateView(binding.estado, binding.dashboardContenido, ::cargar)
        titulo(ligaNombre.ifBlank { "Panel de liga" })

        binding.dashboardCodigo.setOnClickListener {
            val codigo = campeonato?.codigo.orEmpty()
            if (codigo.isBlank()) aviso("Este campeonato aún no tiene código.")
            else {
                val gestor = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                gestor.setPrimaryClip(ClipData.newPlainText("Código de liga", codigo))
                aviso("Código $codigo copiado.")
            }
        }

        binding.dashEquipos.setOnClickListener { modulo(Modulo.EQUIPOS) }
        binding.dashJugadores.setOnClickListener { modulo(Modulo.JUGADORES) }
        binding.dashArbitros.setOnClickListener { modulo(Modulo.ARBITROS) }
        binding.dashPartidos.setOnClickListener { modulo(Modulo.PARTIDOS) }
        binding.dashCalendario.setOnClickListener { modulo(Modulo.CALENDARIO) }
        binding.dashTabla.setOnClickListener { modulo(Modulo.TABLA) }
        binding.dashEstadisticas.setOnClickListener { modulo(Modulo.ESTADISTICAS) }
        binding.dashGrupos.setOnClickListener { modulo(Modulo.GRUPOS) }
        binding.dashEliminacion.setOnClickListener { modulo(Modulo.ELIMINACION) }
        binding.dashAuditoria.setOnClickListener { modulo(Modulo.AUDITORIA) }

        binding.dashRequisitos.setOnClickListener {
            abrir(FormFragment.nuevo(
                titulo = "Requisitos de inicio",
                descripcion = "Configura la modalidad y el mínimo de jugadores por equipo.",
                boton = "Guardar",
                claveResultado = CLAVE_REQUISITOS,
                campos = listOf(
                    FormField("modalidad", "Modalidad", valor = "Fútbol 11", requerido = true,
                        opciones = listOf("Fútbol 5", "Fútbol 7", "Fútbol 11", "Otra"),
                        valores = listOf("Fútbol 5", "Fútbol 7", "Fútbol 11", "personalizada")),
                    FormField("minimo_jugadores", "Mínimo de jugadores", TipoCampo.NUMERO, "7", requerido = true),
                ),
            ))
        }
        escucharFormulario(CLAVE_REQUISITOS) { datos ->
            val valores = datos.valoresFormulario()
            lifecycleScope.launch {
                try {
                    LigaRepo.requisitos(requireContext(), ligaId,
                        valores["modalidad"].orEmpty(),
                        valores["minimo_jugadores"]?.toIntOrNull() ?: 7)
                    aviso("Requisitos guardados.")
                } catch (e: Exception) { aviso(e.message ?: "No se pudieron guardar.", true) }
            }
        }

        binding.dashIniciar.setOnClickListener {
            confirmar("Iniciar liga", "Se verificará que la configuración y el calendario estén completos.") {
                lifecycleScope.launch {
                    try {
                        LigaRepo.iniciar(requireContext(), ligaId)
                        aviso("Campeonato iniciado.")
                        cargar()
                    } catch (e: Exception) { aviso(e.message ?: "No se pudo iniciar.", true) }
                }
            }
        }
        binding.dashFinalizar.setOnClickListener {
            confirmar("Finalizar liga", "El sistema validará que existan resultados oficiales y un campeón.") {
                lifecycleScope.launch {
                    try {
                        LigaRepo.finalizar(requireContext(), ligaId)
                        aviso("Campeonato finalizado.")
                        cargar()
                    } catch (e: Exception) { aviso(e.message ?: "No se pudo finalizar.", true) }
                }
            }
        }
        binding.dashPublicar.setOnClickListener {
            val publicado = campeonato?.publicado ?: false
            lifecycleScope.launch {
                try {
                    LigaRepo.publicacion(requireContext(), ligaId, !publicado)
                    aviso(if (publicado) "Campeonato oculto." else "Campeonato publicado.")
                    cargar()
                } catch (e: Exception) { aviso(e.message ?: "No se pudo publicar.", true) }
            }
        }
        cargar()
    }

    private fun modulo(modulo: Modulo) = abrir(ModuloLigaFragment.nuevo(ligaId, ligaNombre, modulo))

    private fun cargar() {
        lifecycleScope.launch {
            estado.cargando()
            try {
                val respuesta = LigaRepo.obtener(requireContext(), ligaId)
                val json = respuesta.optJSONObject("campeonato") ?: respuesta
                val campeonato = Campeonato.de(json)
                this@DashboardFragment.campeonato = campeonato
                binding.dashboardNombre.text = campeonato.nombre
                binding.dashboardEstado.text = "Estado: ${campeonato.estado.replace("_", " ")} · ${campeonato.formato.replace("_", " ")}"
                binding.dashboardCodigoTexto.text = "Código: ${campeonato.codigo.ifBlank { "pendiente" }}"
                binding.dashPublicar.text = if (campeonato.publicado) "Ocultar del público" else "Publicar"
                titulo(campeonato.nombre)

                val totalEquipos = runCatching {
                    val datos = EquipoRepo.inscripciones(requireContext(), ligaId)
                    when (datos) {
                        is org.json.JSONArray -> datos.length()
                        else -> ((datos as? org.json.JSONObject)?.optJSONArray("inscripciones")
                            ?: (datos as? org.json.JSONObject)?.optJSONArray("data")
                            ?: org.json.JSONArray()).length()
                    }
                }.getOrDefault(-1)
                val totalPartidos = runCatching {
                    val datos = PartidoRepo.listar(requireContext(), ligaId)
                    when (datos) {
                        is org.json.JSONArray -> datos.length()
                        else -> ((datos as? org.json.JSONObject)?.optJSONArray("partidos")
                            ?: (datos as? org.json.JSONObject)?.optJSONArray("data")
                            ?: org.json.JSONArray()).length()
                    }
                }.getOrDefault(-1)
                binding.dashboardCifras.text = "Equipos inscritos: ${if (totalEquipos < 0) "—" else totalEquipos}    Partidos: ${if (totalPartidos < 0) "—" else totalPartidos}"
                estado.contenido()
            } catch (e: Exception) {
                estado.error(e.message ?: "No se pudo cargar el panel.")
            }
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    companion object {
        private const val CLAVE_REQUISITOS = "form_requisitos"

        fun nuevo(ligaId: Long, nombre: String) = DashboardFragment().apply {
            arguments = Bundle().apply {
                putLong("ligaId", ligaId)
                putString("nombre", nombre)
            }
        }
    }
}

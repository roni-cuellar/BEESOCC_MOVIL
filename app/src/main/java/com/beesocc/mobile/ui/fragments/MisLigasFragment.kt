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
import com.beesocc.mobile.core.objetos
import com.beesocc.mobile.core.primerArreglo
import com.beesocc.mobile.core.texto
import com.beesocc.mobile.databinding.FragmentListaBinding
import com.beesocc.mobile.model.campeonatoItem
import com.beesocc.mobile.repository.LigaRepo
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
import com.beesocc.mobile.ui.prepararLista
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch
import org.json.JSONObject

class MisLigasFragment : Fragment(R.layout.fragment_lista) {
    private var _binding: FragmentListaBinding? = null
    private val binding get() = _binding!!
    private lateinit var adaptador: ListaAdapter
    private lateinit var estado: StateView

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentListaBinding.bind(view)
        val escuchas = object : ListaAdapter.Escuchas {
            override fun onItem(item: ItemLista) = entrar(item)
            override fun onPrimario(item: ItemLista) = entrar(item)
            override fun onSecundario(item: ItemLista) = acciones(item)
        }
        val preparado = prepararLista(
            binding = binding,
            titulo = "Mis ligas",
            subtitulo = "Administra tus campeonatos y su configuración.",
            botonPrimario = "Nueva liga" to ::nuevaLiga,
            botonSecundario = "Actualizar" to ::cargar,
            escuchas = escuchas,
            reintentar = ::cargar,
        )
        adaptador = preparado.first
        estado = preparado.second
        escucharFormulario(CLAVE_FORMULARIO) { datos ->
            val valores = datos.valoresFormulario()
            val archivo = datos.archivoFormulario("logo")
            val id = datos.getLong("id", 0L)
            lifecycleScope.launch {
                try {
                    val cuerpo = JSONObject().apply {
                        put("nombre", valores["nombre"].orEmpty())
                        put("temporada", valores["temporada"].orEmpty())
                        put("descripcion", valores["descripcion"].orEmpty())
                        put("formato", valores["formato"].orEmpty())
                    }
                    if (id > 0) {
                        if (archivo != null) LigaRepo.actualizarConLogo(requireContext(), id, valoresFiltrados(valores), archivo)
                        else LigaRepo.actualizar(requireContext(), id, cuerpo)
                        aviso("Campeonato actualizado.")
                    } else {
                        if (archivo != null) LigaRepo.crearConLogo(requireContext(), valoresFiltrados(valores), archivo)
                        else LigaRepo.crear(requireContext(), cuerpo)
                        aviso("Campeonato creado.")
                    }
                    cargar()
                } catch (e: Exception) {
                    aviso(e.message ?: "No se pudo guardar el campeonato.", true)
                }
            }
        }
        cargar()
    }

    private fun valoresFiltrados(valores: Map<String, String>) = valores
        .filter { (clave, valor) -> clave != "logo" && valor.isNotBlank() }

    private fun entrar(item: ItemLista) = abrir(DashboardFragment.nuevo(item.id, item.titulo))

    private fun nuevaLiga() = abrir(
        FormFragment.nuevo(
            titulo = "Nueva liga",
            descripcion = "Define el nombre, la temporada y el formato del campeonato.",
            boton = "Crear liga",
            claveResultado = CLAVE_FORMULARIO,
            campos = camposLiga(),
        )
    )

    private fun acciones(item: ItemLista) {
        val publicado = item.datos.optBoolean("publicado")
        val opciones = arrayOf("Editar", if (publicado) "Ocultar del público" else "Publicar", "Copiar código", "Eliminar")
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(item.titulo)
            .setItems(opciones) { _, cual ->
                when (cual) {
                    0 -> editar(item)
                    1 -> cambiarPublicacion(item, !publicado)
                    2 -> copiarCodigo(item.datos.texto("codigo_liga"))
                    3 -> confirmar("Eliminar liga", "¿Eliminar ${item.titulo}? Esta acción no se puede deshacer.") {
                        lifecycleScope.launch {
                            try {
                                LigaRepo.eliminar(requireContext(), item.id)
                                aviso("Campeonato eliminado.")
                                cargar()
                            } catch (e: Exception) {
                                aviso(e.message ?: "No se pudo eliminar.", true)
                            }
                        }
                    }
                }
            }
            .show()
    }

    private fun editar(item: ItemLista) {
        val campos = camposLiga(
            nombre = item.datos.texto("nombre"),
            temporada = item.datos.texto("temporada"),
            descripcion = item.datos.texto("descripcion"),
            formato = item.datos.texto("formato"),
        )
        val fragmento = FormFragment.nuevo("Editar liga", campos, CLAVE_FORMULARIO, "Guardar cambios")
        fragmento.arguments?.putLong("id", item.id)
        abrir(fragmento)
    }

    private fun cambiarPublicacion(item: ItemLista, publicar: Boolean) {
        lifecycleScope.launch {
            try {
                LigaRepo.publicacion(requireContext(), item.id, publicar)
                aviso(if (publicar) "Campeonato publicado." else "Campeonato oculto.")
                cargar()
            } catch (e: Exception) {
                aviso(e.message ?: "No se pudo cambiar la publicación.", true)
            }
        }
    }

    private fun copiarCodigo(codigo: String) {
        if (codigo.isBlank()) {
            aviso("Este campeonato aún no tiene código.")
            return
        }
        val gestor = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        gestor.setPrimaryClip(ClipData.newPlainText("Código de liga", codigo))
        aviso("Código $codigo copiado.")
    }

    private fun camposLiga(
        nombre: String = "",
        temporada: String = "",
        descripcion: String = "",
        formato: String = "todos_contra_todos",
    ) = listOf(
        FormField("nombre", "Nombre del campeonato", valor = nombre, requerido = true),
        FormField("temporada", "Temporada", valor = temporada.ifBlank { "2026" }, requerido = true, hint = "Ej. 2026"),
        FormField("descripcion", "Descripción", TipoCampo.AREA, descripcion),
        FormField(
            "formato", "Formato", TipoCampo.SELECT, formato, requerido = true,
            opciones = listOf("Todos contra todos", "Eliminación directa", "Grupos y eliminación"),
            valores = listOf("todos_contra_todos", "eliminacion_directa", "grupos_eliminacion"),
        ),
        FormField("logo", "Logo (opcional)", TipoCampo.IMAGEN),
    )

    private fun cargar() {
        lifecycleScope.launch {
            estado.cargando()
            try {
                val datos = LigaRepo.listar(requireContext()).primerArreglo("campeonatos", "data")
                val items = datos.objetos().map { it.campeonatoItem() }
                adaptador.submitList(items)
                if (items.isEmpty()) estado.vacio("Aún no tienes ligas. Crea la primera con el botón «Nueva liga».") else estado.contenido()
            } catch (e: Exception) {
                estado.error(e.message ?: "No se pudieron cargar tus ligas.")
            }
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    companion object {
        private const val CLAVE_FORMULARIO = "form_liga"
    }
}

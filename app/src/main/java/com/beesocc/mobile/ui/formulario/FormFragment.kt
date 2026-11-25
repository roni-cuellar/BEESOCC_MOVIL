package com.beesocc.mobile.ui.formulario

import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Spinner
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.beesocc.mobile.databinding.FragmentFormBinding
import com.beesocc.mobile.databinding.ItemCampoImagenBinding
import com.beesocc.mobile.databinding.ItemCampoSpinnerBinding
import com.beesocc.mobile.databinding.ItemCampoSwitchBinding
import com.beesocc.mobile.databinding.ItemCampoTextoBinding
import com.beesocc.mobile.ui.aviso
import com.beesocc.mobile.ui.titulo

class FormFragment : Fragment() {
    private var _binding: FragmentFormBinding? = null
    private val binding get() = _binding!!

    private lateinit var campos: List<FormField>
    private lateinit var claveResultado: String
    private lateinit var selectorImagen: ActivityResultLauncher<String>

    private val entradas = mutableMapOf<String, EditText>()
    private val selectores = mutableMapOf<String, Pair<Spinner, List<String>>>()
    private val interruptores = mutableMapOf<String, com.google.android.material.materialswitch.MaterialSwitch>()
    private val imagenes = mutableMapOf<String, ByteArray>()
    private val nombresImagen = mutableMapOf<String, String>()
    private val mimesImagen = mutableMapOf<String, String>()
    private var campoImagenActual: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        @Suppress("DEPRECATION")
        campos = (arguments?.getSerializable("campos") as? ArrayList<FormField>) ?: arrayListOf()
        claveResultado = arguments?.getString("claveResultado").orEmpty()
        selectorImagen = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            val campo = campoImagenActual ?: return@registerForActivityResult
            if (uri == null) return@registerForActivityResult
            val contexto = requireContext()
            runCatching {
                val bytes = contexto.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                if (bytes != null) {
                    imagenes[campo] = bytes
                    nombresImagen[campo] = uri.lastPathSegment?.takeLast(60) ?: "imagen.png"
                    mimesImagen[campo] = contexto.contentResolver.getType(uri) ?: "image/png"
                    (binding.formContenedor.findViewWithTag("archivo_$campo") as? View)?.let { vista ->
                        vista.findViewById<android.widget.TextView>(com.beesocc.mobile.R.id.campoArchivo)?.text = nombresImagen[campo]
                    }
                }
            }.onFailure { aviso("No se pudo leer la imagen.") }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentFormBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.formTitulo.text = arguments?.getString("titulo").orEmpty()
        val descripcion = arguments?.getString("descripcion").orEmpty()
        binding.formDescripcion.text = descripcion
        binding.formDescripcion.isVisible = descripcion.isNotBlank()
        binding.formGuardar.text = arguments?.getString("boton").orEmpty().ifBlank { getString(com.beesocc.mobile.R.string.guardar) }
        titulo(arguments?.getString("titulo").orEmpty())

        campos.forEach { campo ->
            when (campo.tipo) {
                TipoCampo.SELECT -> agregarSelector(campo)
                TipoCampo.SWITCH -> agregarInterruptor(campo)
                TipoCampo.IMAGEN -> agregarImagen(campo)
                else -> agregarTexto(campo)
            }
        }

        binding.formCancelar.setOnClickListener { parentFragmentManager.popBackStack() }
        binding.formGuardar.setOnClickListener { guardar() }
    }

    private fun agregarTexto(campo: FormField) {
        val vista = ItemCampoTextoBinding.inflate(layoutInflater, binding.formContenedor, false)
        vista.campoLayout.hint = campo.etiqueta + if (campo.requerido) " *" else ""
        vista.campoEntrada.setText(campo.valor)
        vista.campoEntrada.hint = campo.hint
        vista.campoEntrada.inputType = when (campo.tipo) {
            TipoCampo.NUMERO -> InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            TipoCampo.EMAIL -> InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
            TipoCampo.PASSWORD -> InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            TipoCampo.FECHA -> InputType.TYPE_CLASS_DATETIME or InputType.TYPE_DATETIME_VARIATION_DATE
            TipoCampo.AREA -> InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
            else -> InputType.TYPE_CLASS_TEXT
        }
        if (campo.tipo == TipoCampo.AREA) vista.campoEntrada.minLines = 3
        binding.formContenedor.addView(vista.root)
        entradas[campo.clave] = vista.campoEntrada
    }

    private fun agregarSelector(campo: FormField) {
        val vista = ItemCampoSpinnerBinding.inflate(layoutInflater, binding.formContenedor, false)
        vista.campoEtiqueta.text = campo.etiqueta + if (campo.requerido) " *" else ""
        val etiquetas = listOf("Selecciona…") + campo.opciones
        val valores = listOf("") + campo.valores
        vista.campoSelector.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, etiquetas)
        val indice = valores.indexOf(campo.valor)
        if (indice > 0) vista.campoSelector.setSelection(indice)
        binding.formContenedor.addView(vista.root)
        selectores[campo.clave] = vista.campoSelector to valores
    }

    private fun agregarInterruptor(campo: FormField) {
        val vista = ItemCampoSwitchBinding.inflate(layoutInflater, binding.formContenedor, false)
        vista.campoEtiqueta.text = campo.etiqueta
        vista.campoInterruptor.isChecked = campo.valor == "1" || campo.valor.equals("true", true)
        binding.formContenedor.addView(vista.root)
        interruptores[campo.clave] = vista.campoInterruptor
    }

    private fun agregarImagen(campo: FormField) {
        val vista = ItemCampoImagenBinding.inflate(layoutInflater, binding.formContenedor, false)
        vista.campoEtiqueta.text = campo.etiqueta
        vista.root.tag = "archivo_${campo.clave}"
        vista.campoAdjuntar.setOnClickListener {
            campoImagenActual = campo.clave
            selectorImagen.launch("image/*")
        }
        binding.formContenedor.addView(vista.root)
    }

    private fun guardar() {
        var valido = true
        val valores = linkedMapOf<String, String>()
        campos.forEach { campo ->
            val valor = when (campo.tipo) {
                TipoCampo.SELECT -> selectores[campo.clave]?.let { (spinner, vals) ->
                    val i = spinner.selectedItemPosition
                    if (i in vals.indices) vals[i] else ""
                } ?: ""
                TipoCampo.SWITCH -> if (interruptores[campo.clave]?.isChecked == true) "1" else "0"
                TipoCampo.IMAGEN -> ""
                else -> entradas[campo.clave]?.text?.toString()?.trim().orEmpty()
            }
            valores[campo.clave] = valor
            if (campo.requerido && campo.tipo != TipoCampo.SWITCH && campo.tipo != TipoCampo.IMAGEN && valor.isBlank()) {
                valido = false
                entradas[campo.clave]?.let { (it.parent as? com.google.android.material.textfield.TextInputLayout)?.error = "Campo obligatorio" }
                if (campo.tipo == TipoCampo.SELECT) aviso("Selecciona ${campo.etiqueta.lowercase()}.")
            }
        }
        if (!valido) return

        val resultado = Bundle().apply {
            arguments?.getLong("id", 0L)?.takeIf { it > 0 }?.let { putLong("id", it) }
            arguments?.getLong("inscripcionId", 0L)?.takeIf { it > 0 }?.let { putLong("inscripcionId", it) }
            putSerializable("valores", HashMap(valores))
            imagenes.forEach { (clave, bytes) ->
                putByteArray("bytes_$clave", bytes)
                putString("nombre_$clave", nombresImagen[clave])
                putString("mime_$clave", mimesImagen[clave])
            }
        }
        parentFragmentManager.setFragmentResult(claveResultado, resultado)
        parentFragmentManager.popBackStack()
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    companion object {
        fun nuevo(
            titulo: String,
            campos: List<FormField>,
            claveResultado: String,
            boton: String = "Guardar",
            descripcion: String = "",
        ): FormFragment = FormFragment().apply {
            arguments = Bundle().apply {
                putString("titulo", titulo)
                putString("descripcion", descripcion)
                putString("boton", boton)
                putString("claveResultado", claveResultado)
                putSerializable("campos", ArrayList(campos))
            }
        }
    }
}

package com.beesocc.mobile.ui.fragments

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.beesocc.mobile.R
import com.beesocc.mobile.core.ApiException
import com.beesocc.mobile.core.SessionManager
import com.beesocc.mobile.core.gone
import com.beesocc.mobile.core.visible
import com.beesocc.mobile.databinding.FragmentAccesoBinding
import com.beesocc.mobile.repository.AuthRepo
import com.beesocc.mobile.ui.abrir
import com.beesocc.mobile.ui.abrirRaiz
import com.beesocc.mobile.ui.titulo
import kotlinx.coroutines.launch

abstract class AccesoFragment : Fragment(R.layout.fragment_acceso) {
    protected abstract val esRegistro: Boolean

    private var _binding: FragmentAccesoBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentAccesoBinding.bind(view)
        val registro = esRegistro
        titulo(if (registro) "Crear cuenta" else "Iniciar sesión")

        binding.accesoTitulo.text = if (registro) "Regístrate" else "Bienvenido"
        binding.accesoNombreLayout.visibility = if (registro) View.VISIBLE else View.GONE
        binding.accesoConfirmarLayout.visibility = if (registro) View.VISIBLE else View.GONE
        binding.accesoBoton.text = if (registro) "Crear cuenta" else "Entrar"
        binding.accesoCambiar.text = if (registro) "Ya tengo cuenta" else "Crear una cuenta"

        binding.accesoCambiar.setOnClickListener {
            if (registro) abrir(LoginFragment()) else abrir(RegistroFragment())
        }
        binding.accesoBoton.setOnClickListener { enviar() }
    }

    private fun enviar() {
        val email = binding.accesoEmail.text?.toString()?.trim().orEmpty()
        val password = binding.accesoPassword.text?.toString().orEmpty()
        val nombre = binding.accesoNombre.text?.toString()?.trim().orEmpty()
        val confirmacion = binding.accesoConfirmar.text?.toString().orEmpty()

        binding.accesoNombreLayout.error = null
        binding.accesoEmailLayout.error = null
        binding.accesoPasswordLayout.error = null
        binding.accesoConfirmarLayout.error = null
        binding.accesoAviso.gone()

        when {
            esRegistro && nombre.isBlank() -> binding.accesoNombreLayout.error = "Escribe tu nombre"
            email.isBlank() -> binding.accesoEmailLayout.error = "Escribe tu correo"
            password.length < if (esRegistro) 8 else 1 -> binding.accesoPasswordLayout.error = "Contraseña inválida"
            esRegistro && password != confirmacion -> binding.accesoConfirmarLayout.error = "Las contraseñas no coinciden"
            else -> ejecutar(email, password, nombre)
        }
    }

    private fun ejecutar(email: String, password: String, nombre: String) {
        binding.accesoProgreso.visible()
        binding.accesoBoton.isEnabled = false
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val respuesta = if (esRegistro) {
                    AuthRepo.registro(requireContext(), nombre, email, password)
                } else {
                    AuthRepo.login(requireContext(), email, password)
                }
                val usuario = respuesta.optJSONObject("usuario")
                SessionManager.get(requireContext()).guardar(usuario)
                abrirRaiz(SeleccionRolesFragment())
            } catch (e: Exception) {
                val errores = (e as? ApiException)?.errores?.values?.joinToString(" ")
                binding.accesoAviso.text = errores?.takeIf { it.isNotBlank() } ?: e.message ?: getString(R.string.error_generico)
                binding.accesoAviso.visible()
            } finally {
                binding.accesoProgreso.gone()
                binding.accesoBoton.isEnabled = true
            }
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}

class LoginFragment : AccesoFragment() {
    override val esRegistro = false
}

class RegistroFragment : AccesoFragment() {
    override val esRegistro = true
}

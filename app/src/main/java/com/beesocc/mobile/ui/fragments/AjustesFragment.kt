package com.beesocc.mobile.ui.fragments

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.beesocc.mobile.R
import com.beesocc.mobile.core.ApiClient
import com.beesocc.mobile.core.ApiConfig
import com.beesocc.mobile.core.SessionManager
import com.beesocc.mobile.databinding.FragmentAjustesBinding
import com.beesocc.mobile.repository.AuthRepo
import com.beesocc.mobile.repository.PublicoRepo
import com.beesocc.mobile.ui.abrirRaiz
import com.beesocc.mobile.ui.aviso
import com.beesocc.mobile.ui.confirmar
import com.beesocc.mobile.ui.titulo
import kotlinx.coroutines.launch

class AjustesFragment : Fragment(R.layout.fragment_ajustes) {
    private var _binding: FragmentAjustesBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentAjustesBinding.bind(view)
        titulo("Ajustes")
        val sesion = SessionManager.get(requireContext())
        binding.ajustesUsuario.text = sesion.nombre.ifBlank { "Sin sesión iniciada" }
        binding.ajustesCorreo.text = sesion.correo
        binding.ajustesUrl.setText(ApiConfig.baseUrl(requireContext()))
        binding.ajustesEstado.text = "URL activa: ${ApiConfig.baseUrl(requireContext())}"

        binding.ajustesUrlLayout.setEndIconOnClickListener {
            val gestor = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            gestor.setPrimaryClip(ClipData.newPlainText("URL API", binding.ajustesUrl.text.toString()))
            aviso("URL copiada.")
        }

        binding.ajustesGuardar.setOnClickListener {
            val url = binding.ajustesUrl.text?.toString()?.trim().orEmpty()
            if (!url.startsWith("http://", true) && !url.startsWith("https://", true)) {
                binding.ajustesUrlLayout.error = "La URL debe empezar con http:// o https://"
                return@setOnClickListener
            }
            binding.ajustesUrlLayout.error = null
            ApiConfig.setBaseUrl(requireContext(), url)
            binding.ajustesEstado.text = "Probando conexión con $url…"
            lifecycleScope.launch {
                val resultado = runCatching { PublicoRepo.listar(requireContext()) }
                binding.ajustesEstado.text = resultado.fold(
                    onSuccess = { "Conexión correcta. URL activa: ${ApiConfig.baseUrl(requireContext())}" },
                    onFailure = { "URL guardada, pero no responde: ${it.message}" },
                )
            }
        }

        binding.ajustesRestablecer.setOnClickListener {
            ApiConfig.reset(requireContext())
            binding.ajustesUrl.setText(ApiConfig.baseUrl(requireContext()))
            binding.ajustesEstado.text = "URL activa: ${ApiConfig.baseUrl(requireContext())}"
            aviso("Valor restablecido.")
        }

        binding.ajustesSalir.setOnClickListener {
            confirmar("Cerrar sesión", "¿Quieres salir de la cuenta actual?") {
                lifecycleScope.launch {
                    runCatching { AuthRepo.logout(requireContext()) }
                    ApiClient.reset()
                    SessionManager.get(requireContext()).cerrar()
                    abrirRaiz(com.beesocc.mobile.ui.fragments.InicioFragment())
                    aviso("Sesión cerrada.")
                }
            }
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}

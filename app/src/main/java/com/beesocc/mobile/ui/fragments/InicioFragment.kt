package com.beesocc.mobile.ui.fragments

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.beesocc.mobile.R
import com.beesocc.mobile.databinding.FragmentInicioBinding
import com.beesocc.mobile.ui.abrir
import com.beesocc.mobile.ui.abrirRaiz
import com.beesocc.mobile.ui.titulo

class InicioFragment : Fragment(R.layout.fragment_inicio) {
    private var _binding: FragmentInicioBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentInicioBinding.bind(view)
        titulo("BEESOCC", volver = false)
        binding.inicioCampeonatos.setOnClickListener { abrirRaiz(CampeonatosPublicosFragment()) }
        binding.inicioLogin.setOnClickListener { abrir(LoginFragment()) }
        binding.inicioRegistro.setOnClickListener { abrir(RegistroFragment()) }
        binding.inicioMisLigas.setOnClickListener { abrirRaiz(MisLigasFragment()) }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}

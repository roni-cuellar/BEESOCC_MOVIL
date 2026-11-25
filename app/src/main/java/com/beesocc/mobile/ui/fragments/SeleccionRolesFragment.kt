package com.beesocc.mobile.ui.fragments

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.beesocc.mobile.R
import com.beesocc.mobile.databinding.FragmentRolesBinding
import com.beesocc.mobile.ui.abrirRaiz
import com.beesocc.mobile.ui.titulo

class SeleccionRolesFragment : Fragment(R.layout.fragment_roles) {
    private var _binding: FragmentRolesBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentRolesBinding.bind(view)
        titulo("Selecciona tu rol")
        binding.rolAdmin.setOnClickListener { abrirRaiz(MisLigasFragment()) }
        binding.rolArbitro.setOnClickListener { abrirRaiz(MisArbitrajesFragment()) }
        binding.rolEspectador.setOnClickListener { abrirRaiz(CampeonatosPublicosFragment()) }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}

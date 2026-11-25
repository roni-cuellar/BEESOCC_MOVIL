package com.beesocc.mobile.ui

import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.beesocc.mobile.R
import com.beesocc.mobile.core.SessionManager
import com.beesocc.mobile.databinding.ActivityMainBinding
import com.beesocc.mobile.ui.fragments.AjustesFragment
import com.beesocc.mobile.ui.fragments.CampeonatosPublicosFragment
import com.beesocc.mobile.ui.fragments.InicioFragment
import com.beesocc.mobile.ui.fragments.LoginFragment
import com.beesocc.mobile.ui.fragments.MisLigasFragment

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    val sesion: SessionManager by lazy { SessionManager.get(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        binding.toolbarLogo.setOnClickListener { abrirRaiz(InicioFragment()) }
        binding.toolbar.setNavigationOnClickListener { supportFragmentManager.popBackStack() }

        binding.navegacion.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_inicio -> abrirRaiz(InicioFragment())
                R.id.nav_ligas -> abrirRaiz(if (sesion.autenticado) MisLigasFragment() else LoginFragment())
                R.id.nav_consulta -> abrirRaiz(CampeonatosPublicosFragment())
                R.id.nav_ajustes -> abrirRaiz(AjustesFragment())
            }
            true
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (supportFragmentManager.backStackEntryCount > 0) supportFragmentManager.popBackStack()
                else finish()
            }
        })

        if (savedInstanceState == null) abrirRaiz(InicioFragment())
    }

    fun titulo(texto: String, mostrarVolver: Boolean = false) {
        binding.toolbarTitulo.text = texto
        binding.toolbar.navigationIcon = if (mostrarVolver) {
            androidx.appcompat.content.res.AppCompatResources.getDrawable(this, R.drawable.ic_flecha)
        } else null
    }

    fun abrir(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.contenedor, fragment)
            .addToBackStack(fragment::class.java.name)
            .commit()
    }

    fun abrirRaiz(fragment: Fragment) {
        supportFragmentManager.popBackStackImmediate(null, androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE)
        supportFragmentManager.beginTransaction()
            .replace(R.id.contenedor, fragment)
            .commit()
        titulo(getString(R.string.app_name))
    }

    fun marcarNavegacion(itemId: Int) {
        binding.navegacion.selectedItemId = itemId
    }
}

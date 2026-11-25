package com.beesocc.mobile.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.beesocc.mobile.R
import com.beesocc.mobile.core.cargarEn
import com.beesocc.mobile.databinding.ItemListaBinding
import org.json.JSONObject

data class ItemLista(
    val id: Long,
    val titulo: String,
    val subtitulo: String = "",
    val meta: String = "",
    val imagen: String = "",
    val accionPrimaria: String = "",
    val accionSecundaria: String = "",
    val datos: JSONObject = JSONObject(),
)

class ListaAdapter(
    private val escuchas: Escuchas,
) : ListAdapter<ItemLista, ListaAdapter.ViewHolder>(Diff()) {

    interface Escuchas {
        fun onItem(item: ItemLista) {}
        fun onPrimario(item: ItemLista) {}
        fun onSecundario(item: ItemLista) {}
    }

    inner class ViewHolder(private val binding: ItemListaBinding) : RecyclerView.ViewHolder(binding.root) {
        fun enlazar(item: ItemLista): Unit = with(binding) {
            root.setOnClickListener { escuchas.onItem(item) }
            itemTitulo.text = item.titulo
            itemSubtitulo.text = item.subtitulo
            itemSubtitulo.visibility = if (item.subtitulo.isBlank()) android.view.View.GONE else android.view.View.VISIBLE
            itemMeta.text = item.meta
            itemMeta.visibility = if (item.meta.isBlank()) android.view.View.GONE else android.view.View.VISIBLE
            cargarEn(itemImagen, item.imagen, R.drawable.isotipo_beesocc)

            val tieneBotones = item.accionPrimaria.isNotBlank() || item.accionSecundaria.isNotBlank()
            itemAcciones.visibility = if (tieneBotones) android.view.View.VISIBLE else android.view.View.GONE

            itemPrimario.apply {
                visibility = if (item.accionPrimaria.isNotBlank()) android.view.View.VISIBLE else android.view.View.GONE
                text = item.accionPrimaria
                setOnClickListener { escuchas.onPrimario(item) }
            }
            itemSecundario.apply {
                visibility = if (item.accionSecundaria.isNotBlank()) android.view.View.VISIBLE else android.view.View.GONE
                text = item.accionSecundaria
                setOnClickListener { escuchas.onSecundario(item) }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemListaBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.enlazar(getItem(position))

    class Diff : DiffUtil.ItemCallback<ItemLista>() {
        override fun areItemsTheSame(a: ItemLista, b: ItemLista) = a.id == b.id
        override fun areContentsTheSame(a: ItemLista, b: ItemLista) = a == b
    }
}

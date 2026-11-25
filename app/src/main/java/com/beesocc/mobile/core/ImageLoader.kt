package com.beesocc.mobile.core

import android.content.Context
import android.graphics.BitmapFactory
import android.widget.ImageView
import com.beesocc.mobile.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

suspend fun cargarBitmap(url: String): android.graphics.Bitmap? = withContext(Dispatchers.IO) {
    if (url.isBlank()) return@withContext null
    runCatching {
        val conexion = URL(url).openConnection() as HttpURLConnection
        conexion.connectTimeout = 15_000
        conexion.readTimeout = 15_000
        conexion.setRequestProperty("Accept", "image/*")
        try {
            conexion.inputStream.use { BitmapFactory.decodeStream(it) }
        } finally {
            conexion.disconnect()
        }
    }.getOrNull()
}

fun cargarEn(imageView: ImageView, url: String, placeholder: Int) {
    imageView.setImageResource(placeholder)
    imageView.setTag(R.id.tag_imagen, url)
    if (url.isBlank()) return
    val contexto: Context = imageView.context
    kotlinx.coroutines.CoroutineScope(Dispatchers.Main + kotlinx.coroutines.SupervisorJob()).launch {
        val bitmap = cargarBitmap(ApiConfig.imagenUrl(contexto, url))
        if (imageView.isAttachedToWindow && imageView.getTag(R.id.tag_imagen) == url && bitmap != null) {
            imageView.setImageBitmap(bitmap)
        }
    }
}

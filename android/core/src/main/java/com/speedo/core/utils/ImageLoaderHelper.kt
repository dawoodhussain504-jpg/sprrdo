package com.speedo.core.utils

import android.content.Context
import coil.Coil
import coil.ImageLoader
import coil.decode.SvgDecoder

object ImageLoaderHelper {
    fun init(context: Context) {
        try {
            val imageLoader = ImageLoader.Builder(context)
                .components {
                    add(SvgDecoder.Factory())
                }
                .crossfade(true)
                .build()
            Coil.setImageLoader(imageLoader)
        } catch (e: Exception) {
            android.util.Log.e("ImageLoaderHelper", "Failed initializing Coil SVG decoder", e)
        }
    }
}

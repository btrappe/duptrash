package com.duptrash.app

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.VideoFrameDecoder
import com.duptrash.app.data.db.AppDatabase

class DupTrashApp : Application(), ImageLoaderFactory {
    val database: AppDatabase by lazy { AppDatabase.get(this) }

    override fun newImageLoader(): ImageLoader =
        ImageLoader.Builder(this)
            .components { add(VideoFrameDecoder.Factory()) }
            .build()
}

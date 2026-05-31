package com.duptrash.app

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.VideoFrameDecoder
import com.duptrash.app.data.db.AppDatabase
import com.duptrash.app.data.prefs.UiPrefs

class DupTrashApp : Application(), ImageLoaderFactory {
    val database: AppDatabase by lazy { AppDatabase.get(this) }
    val uiPrefs: UiPrefs by lazy { UiPrefs(this) }

    override fun newImageLoader(): ImageLoader =
        ImageLoader.Builder(this)
            .components { add(VideoFrameDecoder.Factory()) }
            .build()
}

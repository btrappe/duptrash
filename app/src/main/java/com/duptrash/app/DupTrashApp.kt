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

    override fun onCreate() {
        super.onCreate()
        // Drop folder URIs whose persisted permission was lost (e.g. after
        // auto-backup restore on a fresh install).
        uiPrefs.reconcileFolderGrants()
    }

    override fun newImageLoader(): ImageLoader =
        ImageLoader.Builder(this)
            .components { add(VideoFrameDecoder.Factory()) }
            .build()
}

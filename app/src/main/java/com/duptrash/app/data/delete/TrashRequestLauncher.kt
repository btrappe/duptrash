package com.duptrash.app.data.delete

import android.content.ContentResolver
import android.content.IntentSender
import android.net.Uri
import android.os.Build
import android.provider.MediaStore

object TrashRequestLauncher {

    fun isSupported(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R

    fun buildTrashRequest(resolver: ContentResolver, uris: List<Uri>): IntentSender? {
        if (!isSupported() || uris.isEmpty()) return null
        return MediaStore.createTrashRequest(resolver, uris, true).intentSender
    }
}

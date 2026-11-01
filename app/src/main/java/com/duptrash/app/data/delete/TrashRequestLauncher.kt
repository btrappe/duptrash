package com.duptrash.app.data.delete

import android.content.ContentResolver
import android.content.ContentUris
import android.content.IntentSender
import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.util.Log
import com.duptrash.app.data.db.MediaFileEntity

object TrashRequestLauncher {

    private const val TAG = "TrashRequest"

    /**
     * Max URIs per single createTrashRequest call. Android's Binder IPC
     * caps a single transaction at ~1 MB; with content URI strings
     * averaging 50 bytes the practical safe ceiling is a few thousand,
     * but vendor system dialogs (Samsung, Xiaomi) often choke earlier
     * and silently dismiss themselves. 500 stays well clear and only
     * costs the user a few extra confirm dialogs on huge deletes.
     */
    const val BATCH_SIZE = 500

    fun isSupported(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R

    fun partition(uris: List<Uri>): Pair<List<Uri>, List<Uri>> {
        val mediaStore = mutableListOf<Uri>()
        val saf = mutableListOf<Uri>()
        for (u in uris) {
            if (u.authority == MediaStore.AUTHORITY) mediaStore += u else saf += u
        }
        return mediaStore to saf
    }

    /**
     * createTrashRequest rejects generic Files URIs (content://media/external/file/ID)
     * with IllegalArgumentException — it only accepts Images/Video/Audio item URIs.
     * Rewrite scanner-stored Files URIs to the typed collection; SAF URIs pass through.
     */
    fun trashableUri(file: MediaFileEntity): Uri {
        val uri = Uri.parse(file.uri)
        if (uri.authority != MediaStore.AUTHORITY || file.id < 0L) return uri
        val volume = uri.pathSegments.firstOrNull() ?: MediaStore.VOLUME_EXTERNAL
        val collection = when (file.mediaType) {
            MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE -> MediaStore.Images.Media.getContentUri(volume)
            MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO -> MediaStore.Video.Media.getContentUri(volume)
            else -> return uri
        }
        return ContentUris.withAppendedId(collection, file.id)
    }

    /** Splits the MediaStore URIs into Binder-safe batches. */
    fun batch(mediaStoreUris: List<Uri>): List<List<Uri>> =
        if (mediaStoreUris.isEmpty()) emptyList() else mediaStoreUris.chunked(BATCH_SIZE)

    fun buildTrashRequest(resolver: ContentResolver, mediaStoreUris: List<Uri>): IntentSender? {
        if (!isSupported() || mediaStoreUris.isEmpty()) return null
        return MediaStore.createTrashRequest(resolver, mediaStoreUris, true).intentSender
    }

    /**
     * Permanently delete SAF document URIs. SAF has no trash concept — these
     * are gone immediately. Returns the number successfully deleted.
     */
    fun deleteSafFiles(resolver: ContentResolver, safUris: List<Uri>): Int {
        var deleted = 0
        for (uri in safUris) {
            try {
                if (DocumentsContract.deleteDocument(resolver, uri)) deleted++
            } catch (t: Throwable) {
                Log.w(TAG, "SAF delete failed for $uri: ${t.message}")
            }
        }
        return deleted
    }
}

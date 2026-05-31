package com.duptrash.app.data.scan

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.DocumentsContract
import android.util.Log

data class SafFileInfo(
    val uri: Uri,
    val displayName: String,
    val mimeType: String,
    val sizeBytes: Long,
    val dateModified: Long,
    val relativePath: String,
    val absolutePath: String?,
    val mediaType: Int,
)

class SafFolderWalker(private val context: Context) {

    fun walk(treeUri: Uri, skipPaths: Set<String>): List<SafFileInfo> {
        val out = mutableListOf<SafFileInfo>()
        try {
            val rootDocId = DocumentsContract.getTreeDocumentId(treeUri)
            val rootRel = rootDocId.substringAfter(':', "")
            val rootFolder = rootRel.substringAfterLast('/', rootRel)
            walkChildren(treeUri, rootDocId, rootFolder, skipPaths, out)
        } catch (t: Throwable) {
            Log.w(TAG, "SAF walk failed for $treeUri: ${t.message}")
        }
        return out
    }

    private fun walkChildren(
        treeUri: Uri,
        parentDocId: String,
        relativePathInTree: String,
        skipPaths: Set<String>,
        out: MutableList<SafFileInfo>,
    ) {
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, parentDocId)
        val proj = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
            DocumentsContract.Document.COLUMN_SIZE,
            DocumentsContract.Document.COLUMN_LAST_MODIFIED,
        )
        context.contentResolver.query(childrenUri, proj, null, null, null)?.use { c ->
            while (c.moveToNext()) {
                val docId = c.getString(0) ?: continue
                val name = c.getString(1) ?: continue
                val mime = c.getString(2) ?: ""
                val size = if (!c.isNull(3)) c.getLong(3) else 0L
                val mtime = if (!c.isNull(4)) c.getLong(4) else 0L
                val docUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, docId)
                val nextRel = if (relativePathInTree.isEmpty()) name else "$relativePathInTree/$name"

                if (mime == DocumentsContract.Document.MIME_TYPE_DIR) {
                    walkChildren(treeUri, docId, nextRel, skipPaths, out)
                } else if (mime.startsWith("image/") || mime.startsWith("video/")) {
                    if (size <= 0L) continue
                    val absPath = absolutePathFromDocId(docId)
                    if (absPath != null && skipPaths.contains(absPath)) continue
                    val mediaType = if (mime.startsWith("image/")) MEDIA_TYPE_IMAGE else MEDIA_TYPE_VIDEO
                    val parentRel = nextRel.substringBeforeLast('/', "")
                        .let { if (it.isEmpty()) "" else "$it/" }
                    out += SafFileInfo(
                        uri = docUri,
                        displayName = name,
                        mimeType = mime,
                        sizeBytes = size,
                        dateModified = mtime / 1000L,
                        relativePath = parentRel,
                        absolutePath = absPath,
                        mediaType = mediaType,
                    )
                }
            }
        }
    }

    private fun absolutePathFromDocId(docId: String): String? {
        val colon = docId.indexOf(':')
        if (colon < 0) return null
        val volume = docId.substring(0, colon)
        val rel = docId.substring(colon + 1)
        return when (volume) {
            "primary" -> "${Environment.getExternalStorageDirectory().absolutePath}/$rel"
            else -> "/storage/$volume/$rel"
        }
    }

    companion object {
        private const val TAG = "SafFolderWalker"
        private const val MEDIA_TYPE_IMAGE = 1
        private const val MEDIA_TYPE_VIDEO = 3
    }
}

fun safSyntheticId(docUri: Uri): Long {
    val hash = docUri.toString().hashCode().toLong() and 0xFFFFFFFFL
    return -(hash + 1L)
}

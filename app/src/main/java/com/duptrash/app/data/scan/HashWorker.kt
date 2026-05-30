package com.duptrash.app.data.scan

import android.content.Context
import android.net.Uri
import com.duptrash.app.data.db.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.security.MessageDigest

class HashWorker(private val context: Context) {

    suspend fun hashCandidates(onProgress: (done: Int, total: Int, name: String) -> Unit): Int =
        withContext(Dispatchers.IO) {
            val dao = AppDatabase.get(context).mediaFileDao()
            val sizes = dao.sizesNeedingHash()
            val candidates = sizes.flatMap { dao.rowsBySizeMissingHash(it) }
            val total = candidates.size
            var done = 0
            for (row in candidates) {
                onProgress(done, total, row.displayName)
                val digest = try {
                    md5Of(Uri.parse(row.uri))
                } catch (_: Throwable) {
                    null
                }
                if (digest != null) dao.setMd5(row.id, digest)
                done++
            }
            onProgress(done, total, "")
            done
        }

    private fun md5Of(uri: Uri): String? {
        val md = MessageDigest.getInstance("MD5")
        val buf = ByteArray(64 * 1024)
        val stream: InputStream = context.contentResolver.openInputStream(uri) ?: return null
        stream.use { s ->
            while (true) {
                val read = s.read(buf)
                if (read <= 0) break
                md.update(buf, 0, read)
            }
        }
        return md.digest().toHex()
    }

    private fun ByteArray.toHex(): String {
        val sb = StringBuilder(size * 2)
        for (b in this) {
            val v = b.toInt() and 0xff
            sb.append(HEX[v ushr 4]).append(HEX[v and 0x0f])
        }
        return sb.toString()
    }

    companion object {
        private val HEX = "0123456789abcdef".toCharArray()
    }
}

package com.duptrash.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.duptrash.app.data.db.MediaFileEntity
import com.duptrash.app.ui.humanBytes
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun FileDetailsDialog(file: MediaFileEntity, onDismiss: () -> Unit) {
    val df = remember { SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()) }
    val mtimeText = if (file.dateModified > 0) df.format(Date(file.dateModified * 1000)) else "—"
    val source = if (file.id < 0L) "Extra folder (SAF) · permanent delete" else "MediaStore · trash on delete"
    val mediaTypeText = when (file.mediaType) {
        1 -> "image"
        2 -> "video (legacy)"
        3 -> "video"
        else -> "type ${file.mediaType}"
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                file.displayName,
                fontWeight = FontWeight.SemiBold,
                fontFamily = FontFamily.Monospace,
            )
        },
        text = {
            Column(
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Field("Full path", file.fullPath, mono = true)
                Field("Content URI", file.uri, mono = true)
                Field("Size", "${humanBytes(file.sizeBytes)}  (${file.sizeBytes} bytes)")
                Field("Modified", mtimeText)
                Field("MIME", file.mimeType.ifBlank { "—" })
                Field("Kind", mediaTypeText)
                Field("Source", source)
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
    )
}

@Composable
private fun Field(label: String, value: String, mono: Boolean = false) {
    Column {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            value,
            style = MaterialTheme.typography.bodySmall,
            fontFamily = if (mono) FontFamily.Monospace else FontFamily.Default,
        )
    }
}

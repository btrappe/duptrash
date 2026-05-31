package com.duptrash.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.request.videoFrameMillis
import com.duptrash.app.MainViewModel
import com.duptrash.app.data.db.MediaFileEntity
import com.duptrash.app.data.model.KeeperGroup
import com.duptrash.app.data.model.KeeperReason
import com.duptrash.app.ui.humanBytes

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RandomPicksScreen(viewModel: MainViewModel, nav: NavController) {
    val plan by viewModel.plan.collectAsState()
    val randomGroups = plan?.groups.orEmpty().filter { it.reason == KeeperReason.RANDOM }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Random picks (${randomGroups.size})") },
                navigationIcon = {
                    IconButton(onClick = { nav.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        }
    ) { padding ->
        if (randomGroups.isEmpty()) {
            Column(
                Modifier.fillMaxSize().padding(padding).padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("Nothing here — every duplicate group has either a regex-determined or similarity-determined keeper.")
            }
            return@Scaffold
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(randomGroups, key = { it.md5 }) { group ->
                RandomPickCard(group) { fileId ->
                    viewModel.setKeeperOverride(group.md5, fileId)
                }
            }
        }
    }
}

@Composable
private fun RandomPickCard(group: KeeperGroup, onPick: (Long) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                "${group.victims.size + 1} copies · ${humanBytes(group.sizeBytes)} each · tap a copy to make it the keeper",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold,
            )
            val allCopies = listOf(group.keeper) + group.victims
            allCopies.forEach { f ->
                CopyRow(file = f, isCurrent = f.id == group.keeper.id, onPick = { onPick(f.id) })
            }
        }
    }
}

@Composable
private fun CopyRow(file: MediaFileEntity, isCurrent: Boolean, onPick: () -> Unit) {
    val ctx = LocalContext.current
    val req = ImageRequest.Builder(ctx)
        .data(file.uri.toUri())
        .videoFrameMillis(500L)
        .crossfade(true)
        .build()
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable { onPick() }
            .padding(6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        AsyncImage(
            model = req,
            contentDescription = file.displayName,
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(6.dp))
                .border(
                    if (isCurrent) 2.dp else 1.dp,
                    if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                    RoundedCornerShape(6.dp),
                ),
        )
        Text(
            file.fullPath,
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            modifier = Modifier.padding(end = 6.dp),
        )
        Box(Modifier.weight(1f))
        if (isCurrent) {
            Box(
                Modifier.size(22.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Default.Check, null, tint = Color.Black, modifier = Modifier.size(14.dp)) }
        }
    }
}

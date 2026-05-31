package com.duptrash.app.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Numbers
import androidx.compose.material.icons.filled.PanTool
import androidx.compose.material.icons.filled.QuestionMark
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
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
import com.duptrash.app.data.delete.TrashRequestLauncher
import com.duptrash.app.data.model.KeeperGroup
import com.duptrash.app.data.model.KeeperReason
import com.duptrash.app.ui.humanBytes
import kotlinx.coroutines.flow.distinctUntilChanged

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DuplicatesScreen(viewModel: MainViewModel, nav: NavController) {
    val ctx = LocalContext.current
    val plan by viewModel.plan.collectAsState()
    val splitRatio by viewModel.splitRatio.collectAsState()

    val trashLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        viewModel.onTrashRequestResult(result.resultCode == android.app.Activity.RESULT_OK)
    }

    var overrideTarget by remember { mutableStateOf<KeeperGroup?>(null) }
    var detailsTarget by remember { mutableStateOf<MediaFileEntity?>(null) }

    val groups = plan?.groups.orEmpty()
    val toDeleteCount = plan?.toDelete?.size ?: 0
    val reclaim = plan?.reclaimableBytes ?: 0
    val randomCount = plan?.randomCount ?: 0

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Duplicates (${groups.size})")
                        if (randomCount > 0) {
                            Text(
                                "$randomCount random pick(s) need review",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.clickable { nav.navigate(Routes.RANDOM_PICKS) },
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { nav.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
        bottomBar = {
            val safCount = plan?.toDelete?.count { it.id < 0L } ?: 0
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                if (safCount > 0) {
                    Text(
                        "$safCount file(s) from extra folders will be deleted PERMANENTLY (SAF has no trash).",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                Button(
                    onClick = {
                        val all = plan?.toDelete?.map { it.uri.toUri() }.orEmpty()
                        val (mediaStoreUris, safUris) = TrashRequestLauncher.partition(all)
                        // Delete SAF entries inline (permanent — SAF has no trash).
                        if (safUris.isNotEmpty()) {
                            TrashRequestLauncher.deleteSafFiles(ctx.contentResolver, safUris)
                        }
                        val sender = TrashRequestLauncher.buildTrashRequest(ctx.contentResolver, mediaStoreUris)
                        if (sender != null) {
                            trashLauncher.launch(IntentSenderRequest.Builder(sender).build())
                        } else if (safUris.isNotEmpty()) {
                            // No system dialog needed — just refresh.
                            viewModel.onTrashRequestResult(true)
                        }
                    },
                    enabled = toDeleteCount > 0,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                ) {
                    Text("Move $toDeleteCount file(s) to trash — reclaim ${humanBytes(reclaim)}")
                }
            }
        },
    ) { padding ->
        if (groups.isEmpty()) {
            Column(
                Modifier.fillMaxSize().padding(padding).padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) { Text("No duplicates found. Run a scan first.") }
            return@Scaffold
        }
        SplitView(
            modifier = Modifier.fillMaxSize().padding(padding),
            groups = groups,
            splitRatio = splitRatio,
            onSplitChange = viewModel::setSplitRatio,
            onRandomBadgeTap = { overrideTarget = it },
            onFileTap = { detailsTarget = it },
        )
    }

    overrideTarget?.let { target ->
        OverridePickerDialog(
            group = target,
            onPick = { fileId ->
                viewModel.setKeeperOverride(target.md5, fileId)
                overrideTarget = null
            },
            onDismiss = { overrideTarget = null },
        )
    }

    detailsTarget?.let { file ->
        FileDetailsDialog(file = file, onDismiss = { detailsTarget = null })
    }
}

@Composable
private fun SplitView(
    modifier: Modifier = Modifier,
    groups: List<KeeperGroup>,
    splitRatio: Float,
    onSplitChange: (Float) -> Unit,
    onRandomBadgeTap: (KeeperGroup) -> Unit,
    onFileTap: (MediaFileEntity) -> Unit,
) {
    var containerHeightPx by remember { mutableStateOf(1) }

    val topState = rememberLazyListState()
    val bottomState = rememberLazyListState()

    // Bi-directional scroll sync: whichever list is being actively dragged drives the other.
    LaunchedEffect(topState) {
        snapshotFlow { Triple(topState.firstVisibleItemIndex, topState.firstVisibleItemScrollOffset, topState.isScrollInProgress) }
            .distinctUntilChanged()
            .collect { (idx, off, scrolling) ->
                if (scrolling) bottomState.scrollToItem(idx, off)
            }
    }
    LaunchedEffect(bottomState) {
        snapshotFlow { Triple(bottomState.firstVisibleItemIndex, bottomState.firstVisibleItemScrollOffset, bottomState.isScrollInProgress) }
            .distinctUntilChanged()
            .collect { (idx, off, scrolling) ->
                if (scrolling) topState.scrollToItem(idx, off)
            }
    }

    Column(
        modifier = modifier.onSizeChanged { containerHeightPx = it.height.coerceAtLeast(1) }
    ) {
        VictimsPanel(
            groups = groups,
            state = topState,
            onFileTap = onFileTap,
            modifier = Modifier.fillMaxWidth().weight(splitRatio),
        )
        DragHandle(
            onDrag = { dyPx ->
                onSplitChange(splitRatio + dyPx / containerHeightPx.toFloat())
            },
        )
        KeepersPanel(
            groups = groups,
            state = bottomState,
            onRandomBadgeTap = onRandomBadgeTap,
            onFileTap = onFileTap,
            modifier = Modifier.fillMaxWidth().weight(1f - splitRatio),
        )
    }
}

@Composable
private fun VictimsPanel(
    groups: List<KeeperGroup>,
    state: LazyListState,
    onFileTap: (MediaFileEntity) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        state = state,
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(groups, key = { "v-" + it.md5 }) { group ->
            VictimsRow(group, onFileTap)
        }
    }
}

@Composable
private fun VictimsRow(group: KeeperGroup, onFileTap: (MediaFileEntity) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                "${group.victims.size} → trash  ·  ${humanBytes(group.sizeBytes)} each  ·  save ${humanBytes(group.reclaimableBytes)}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.error,
                fontWeight = FontWeight.SemiBold,
            )
            if (group.victims.isEmpty()) {
                Text(
                    "(only one copy — nothing to delete)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(group.victims, key = { it.id }) { f -> VictimThumb(f, onFileTap) }
                }
            }
        }
    }
}

@Composable
private fun VictimThumb(file: MediaFileEntity, onTap: (MediaFileEntity) -> Unit) {
    val ctx = LocalContext.current
    val req = ImageRequest.Builder(ctx)
        .data(file.uri.toUri())
        .videoFrameMillis(500L)
        .crossfade(true)
        .build()
    Column(
        horizontalAlignment = Alignment.Start,
        modifier = Modifier
            .width(120.dp)
            .clickable { onTap(file) },
    ) {
        AsyncImage(
            model = req,
            contentDescription = file.displayName,
            modifier = Modifier
                .size(72.dp)
                .clip(RoundedCornerShape(8.dp))
                .border(2.dp, MaterialTheme.colorScheme.error, RoundedCornerShape(8.dp)),
        )
        Text(
            file.fullPath,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
            fontFamily = FontFamily.Monospace,
            fontSize = 10.sp,
            maxLines = 3,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

@Composable
private fun KeepersPanel(
    groups: List<KeeperGroup>,
    state: LazyListState,
    onRandomBadgeTap: (KeeperGroup) -> Unit,
    onFileTap: (MediaFileEntity) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        state = state,
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(groups, key = { "k-" + it.md5 }) { group ->
            KeeperRow(group, onRandomBadgeTap, onFileTap)
        }
    }
}

@Composable
private fun KeeperRow(
    group: KeeperGroup,
    onRandomBadgeTap: (KeeperGroup) -> Unit,
    onFileTap: (MediaFileEntity) -> Unit,
) {
    val ctx = LocalContext.current
    val req = ImageRequest.Builder(ctx)
        .data(group.keeper.uri.toUri())
        .videoFrameMillis(500L)
        .crossfade(true)
        .build()
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onFileTap(group.keeper) },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            AsyncImage(
                model = req,
                contentDescription = group.keeper.displayName,
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp)),
            )
            Column(Modifier.weight(1f)) {
                Text(
                    group.keeper.fullPath,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    maxLines = 4,
                )
            }
            ReasonBadge(group.reason, onClick = if (group.reason == KeeperReason.RANDOM) {
                { onRandomBadgeTap(group) }
            } else null)
        }
    }
}

@Composable
private fun ReasonBadge(reason: KeeperReason, onClick: (() -> Unit)?) {
    val (color, label, icon) = when (reason) {
        KeeperReason.REGEX -> Triple(Color(0xFF00A884), "regex", Icons.Default.Check)
        KeeperReason.NAME -> Triple(Color(0xFFBA68C8), "name", Icons.Default.Numbers)
        KeeperReason.SIMILARITY -> Triple(Color(0xFF4FC3F7), "similar", Icons.Default.Tag)
        KeeperReason.RANDOM -> Triple(Color(0xFFFFB830), "random", Icons.Default.QuestionMark)
        KeeperReason.USER_OVERRIDE -> Triple(Color(0xFF26C6DA), "yours", Icons.Default.PanTool)
    }
    val base = Modifier
        .clip(RoundedCornerShape(16.dp))
        .background(color)
        .padding(horizontal = 8.dp, vertical = 4.dp)
    val mod = if (onClick != null) base.clickable { onClick() } else base
    Row(
        modifier = mod,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(icon, contentDescription = label, tint = Color.Black, modifier = Modifier.size(12.dp))
        Text(label, color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun DragHandle(onDrag: (Float) -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(20.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .pointerInput(Unit) {
                detectDragGestures { _, drag ->
                    onDrag(drag.y)
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            Icons.Default.DragHandle,
            contentDescription = "Drag to resize",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
fun OverridePickerDialog(
    group: KeeperGroup,
    onPick: (Long) -> Unit,
    onDismiss: () -> Unit,
) {
    val ctx = LocalContext.current
    val allCopies = listOf(group.keeper) + group.victims

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Pick the keeper") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                allCopies.forEach { file ->
                    val req = ImageRequest.Builder(ctx)
                        .data(file.uri.toUri())
                        .videoFrameMillis(500L)
                        .build()
                    val isCurrent = file.id == group.keeper.id
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onPick(file.id) }
                            .padding(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        AsyncImage(
                            model = req,
                            contentDescription = file.displayName,
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .border(
                                    if (isCurrent) 2.dp else 1.dp,
                                    if (isCurrent) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.outline,
                                    RoundedCornerShape(6.dp),
                                ),
                        )
                        Text(
                            file.fullPath,
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            modifier = Modifier.weight(1f),
                        )
                        if (isCurrent) {
                            Box(
                                Modifier.size(20.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary),
                                contentAlignment = Alignment.Center,
                            ) { Icon(Icons.Default.Check, null, tint = Color.Black, modifier = Modifier.size(14.dp)) }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } },
    )
}

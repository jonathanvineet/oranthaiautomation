package com.oranthai.postcraft.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.oranthai.postcraft.Choice
import com.oranthai.postcraft.Look
import com.oranthai.postcraft.StudioViewModel
import com.oranthai.postcraft.Styles

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudioScreen(vm: StudioViewModel, onSettings: () -> Unit) {
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    val picker = rememberLauncherForActivityResult(PickVisualMedia()) { uri -> uri?.let(vm::loadUri) }
    val pickPhoto = { picker.launch(PickVisualMediaRequest(PickVisualMedia.ImageOnly)) }

    LaunchedEffect(vm.message) {
        vm.message?.let { snackbar.showSnackbar(it, withDismissAction = true); vm.message = null }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("PostCraft", fontWeight = FontWeight.Bold) },
                actions = { IconButton(onClick = onSettings) { Icon(Icons.Default.Settings, "Settings") } },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { pad ->
        Column(
            Modifier.padding(pad).fillMaxSize().imePadding().verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            if (!vm.hasKey) {
                Card(
                    onClick = onSettings,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                ) {
                    Text("Tap here to add a free Gemini API key for AI captions.", Modifier.padding(14.dp))
                }
            }

            SourceCard(vm, pickPhoto)

            OptionRow("Look", Styles.aesthetics, vm.aesthetic, Look::label) { vm.aesthetic = it }
            OptionRow("Caption tone", Styles.tones, vm.tone, Choice::label) { vm.tone = it }
            OptionRow("Format", Styles.ratios, vm.ratio, Choice::label) { vm.ratio = it }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Variations", style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
                SingleChoiceSegmentedButtonRow {
                    (1..4).forEach { n ->
                        SegmentedButton(
                            selected = vm.variations == n,
                            onClick = { vm.variations = n },
                            shape = SegmentedButtonDefaults.itemShape(n - 1, 4),
                        ) { Text("$n") }
                    }
                }
            }

            OutlinedTextField(
                value = vm.extra,
                onValueChange = { vm.extra = it },
                label = { Text("Caption context (optional)") },
                placeholder = { Text("e.g. new arrivals, mention our Diwali sale") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Hashtags", Modifier.weight(1f), style = MaterialTheme.typography.labelLarge)
                Switch(checked = vm.hashtags, onCheckedChange = { vm.hashtags = it })
            }

            GradientButton(
                text = if (vm.generating) "Creating ${vm.variations} post${if (vm.variations > 1) "s" else ""}…" else "✨ Generate post",
                enabled = vm.sourceJpeg != null && !vm.generating,
                loading = vm.generating,
                onClick = vm::generate,
            )

            if (vm.results.isNotEmpty() || vm.caption.isNotBlank() || vm.captioning) {
                Results(vm)
                CaptionBox(vm)
                GradientButton(
                    text = when {
                        vm.posting -> "Posting…"
                        vm.prefs.directPublish -> "Post to Instagram now"
                        else -> "Post to Instagram"
                    },
                    enabled = vm.selectedResult != null && !vm.posting,
                    loading = vm.posting,
                    onClick = { vm.postToInstagram(context) },
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(onClick = vm::saveSelected, enabled = vm.selectedResult != null, modifier = Modifier.weight(1f)) {
                        Text("Save to gallery")
                    }
                    OutlinedButton(onClick = { vm.shareElsewhere(context) }, enabled = vm.selectedResult != null, modifier = Modifier.weight(1f)) {
                        Text("Share elsewhere")
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SourceCard(vm: StudioViewModel, pickPhoto: () -> Unit) {
    val bmp = vm.sourceBitmap
    Box(
        Modifier.fillMaxWidth().height(if (bmp == null) 180.dp else 240.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable(onClick = pickPhoto),
        contentAlignment = Alignment.Center,
    ) {
        when {
            vm.loadingSource -> CircularProgressIndicator()
            bmp != null -> {
                Image(bmp.asImageBitmap(), "Your photo", Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                Text(
                    "Change photo",
                    Modifier.align(Alignment.BottomEnd).padding(10.dp)
                        .background(Color.Black.copy(alpha = 0.55f), RoundedCornerShape(50))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelMedium, color = Color.White,
                )
            }
            else -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("📷", style = MaterialTheme.typography.displaySmall)
                Text("Pick a photo", style = MaterialTheme.typography.titleMedium)
                Text("or share one to PostCraft from Google Photos / Gallery",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> OptionRow(title: String, options: List<T>, selected: T, label: (T) -> String, onSelect: (T) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title, style = MaterialTheme.typography.labelLarge)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            itemsIndexed(options) { _, o ->
                FilterChip(selected = o == selected, onClick = { onSelect(o) }, label = { Text(label(o)) })
            }
        }
    }
}

@Composable
private fun Results(vm: StudioViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Pick your favourite", style = MaterialTheme.typography.labelLarge)
        if (vm.results.isEmpty()) {
            Box(Modifier.fillMaxWidth().height(260.dp).clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceContainer), contentAlignment = Alignment.Center) {
                if (vm.generating) CircularProgressIndicator() else Text("No images yet")
            }
        } else {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                itemsIndexed(vm.results) { i, r ->
                    val ratio = r.bitmap.width.toFloat() / r.bitmap.height
                    val isSel = i == vm.selected
                    Image(
                        r.bitmap.asImageBitmap(), "Variation ${i + 1}",
                        Modifier.height(320.dp).aspectRatio(ratio)
                            .clip(RoundedCornerShape(16.dp))
                            .then(if (isSel) Modifier.border(BorderStroke(3.dp, InstaGradient), RoundedCornerShape(16.dp)) else Modifier)
                            .clickable { vm.selected = i },
                        contentScale = ContentScale.Crop,
                    )
                }
            }
        }
    }
}

@Composable
private fun CaptionBox(vm: StudioViewModel) {
    OutlinedTextField(
        value = vm.caption,
        onValueChange = { vm.caption = it },
        label = { Text(if (vm.captioning) "Writing caption…" else "Caption") },
        modifier = Modifier.fillMaxWidth(),
        minLines = 4,
        trailingIcon = {
            if (vm.captioning) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
            else IconButton(onClick = vm::regenerateCaption) { Icon(Icons.Default.Refresh, "Rewrite caption") }
        },
    )
}

@Composable
private fun GradientButton(text: String, enabled: Boolean, loading: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.fillMaxWidth().height(54.dp).clip(RoundedCornerShape(16.dp))
            .background(if (enabled || loading) InstaGradient else SolidGray)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (loading) CircularProgressIndicator(Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
            Text(text, color = Color.White, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium)
        }
    }
}

private val SolidGray = androidx.compose.ui.graphics.SolidColor(Color(0xFF3A3540))

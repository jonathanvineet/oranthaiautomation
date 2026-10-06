package com.oranthai.postcraft.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.oranthai.postcraft.Prefs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(prefs: Prefs, onClose: () -> Unit) {
    var geminiKey by remember { mutableStateOf(prefs.geminiKey) }
    var imageModel by remember { mutableStateOf(prefs.imageModel) }
    var textModel by remember { mutableStateOf(prefs.textModel) }
    var brand by remember { mutableStateOf(prefs.brand) }
    var direct by remember { mutableStateOf(prefs.directPublish) }
    var igUserId by remember { mutableStateOf(prefs.igUserId) }
    var igToken by remember { mutableStateOf(prefs.igToken) }
    var imgbbKey by remember { mutableStateOf(prefs.imgbbKey) }
    val uri = LocalUriHandler.current

    fun save() {
        prefs.geminiKey = geminiKey
        prefs.imageModel = imageModel
        prefs.textModel = textModel
        prefs.brand = brand
        prefs.directPublish = direct
        prefs.igUserId = igUserId
        prefs.igToken = igToken
        prefs.imgbbKey = imgbbKey
        onClose()
    }
    BackHandler { save() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = ::save) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
                },
                actions = { TextButton(onClick = ::save) { Text("Save") } },
            )
        },
    ) { pad ->
        Column(
            Modifier.padding(pad).fillMaxSize().imePadding().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Section("AI (Google Gemini)")
            Text("Free key from Google AI Studio. Image generation may need billing enabled on the key's project.",
                style = MaterialTheme.typography.bodySmall)
            TextButton(onClick = { uri.openUri("https://aistudio.google.com/apikey") }) { Text("Get a Gemini API key ↗") }
            Secret("Gemini API key", geminiKey) { geminiKey = it }
            Field("Image model", imageModel) { imageModel = it }
            Field("Caption model", textModel) { textModel = it }

            Section("Your account")
            Field("Brand / creator description (optional)", brand, singleLine = false,
                hint = "e.g. Oranthai - stationery & books store in Chennai, friendly voice") { brand = it }

            Section("Instagram posting")
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Direct publish (true one-tap)")
                    Text(
                        if (direct) "Posts straight to your feed via the Instagram API."
                        else "Off: opens the Instagram app with the photo ready and caption copied.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                Switch(checked = direct, onCheckedChange = { direct = it })
            }
            if (direct) {
                Text(
                    "Needs an Instagram Business or Creator account. Create a Meta app with the Instagram API, " +
                        "generate a long-lived access token, and copy your Instagram user ID. " +
                        "The image is uploaded to imgbb (expires after 1 hour) because Instagram only accepts a public image URL.",
                    style = MaterialTheme.typography.bodySmall,
                )
                TextButton(onClick = { uri.openUri("https://developers.facebook.com/docs/instagram-platform/content-publishing") }) {
                    Text("Instagram API setup guide ↗")
                }
                Field("Instagram user ID", igUserId) { igUserId = it }
                Secret("Instagram access token", igToken) { igToken = it }
                TextButton(onClick = { uri.openUri("https://api.imgbb.com/") }) { Text("Get a free imgbb key ↗") }
                Secret("imgbb API key", imgbbKey) { imgbbKey = it }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun Section(title: String) =
    Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 8.dp))

@Composable
private fun Field(label: String, value: String, singleLine: Boolean = true, hint: String? = null, onChange: (String) -> Unit) =
    OutlinedTextField(value, onChange, label = { Text(label) }, singleLine = singleLine,
        placeholder = hint?.let { { Text(it) } }, modifier = Modifier.fillMaxWidth())

@Composable
private fun Secret(label: String, value: String, onChange: (String) -> Unit) =
    OutlinedTextField(value, onChange, label = { Text(label) }, singleLine = true,
        visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())

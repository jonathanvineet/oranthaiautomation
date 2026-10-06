package com.oranthai.postcraft

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.graphics.Color
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.core.content.IntentCompat
import com.oranthai.postcraft.ui.PostCraftTheme
import com.oranthai.postcraft.ui.SettingsScreen
import com.oranthai.postcraft.ui.StudioScreen

class MainActivity : ComponentActivity() {
    private val vm: StudioViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(SystemBarStyle.dark(Color.TRANSPARENT), SystemBarStyle.dark(Color.TRANSPARENT))
        if (savedInstanceState == null) handleShare(intent)
        setContent {
            PostCraftTheme {
                var settings by rememberSaveable { mutableStateOf(false) }
                if (settings) SettingsScreen(vm.prefs) { settings = false }
                else StudioScreen(vm) { settings = true }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleShare(intent)
    }

    private fun handleShare(intent: Intent?) {
        if (intent?.action != Intent.ACTION_SEND) return
        val stream = IntentCompat.getParcelableExtra(intent, Intent.EXTRA_STREAM, Uri::class.java)
        when {
            stream != null -> vm.loadUri(stream)
            intent.type?.startsWith("image/") == true && intent.clipData != null ->
                intent.clipData?.getItemAt(0)?.uri?.let(vm::loadUri)
            else -> intent.getStringExtra(Intent.EXTRA_TEXT)?.let(vm::loadSharedText)
        }
    }
}

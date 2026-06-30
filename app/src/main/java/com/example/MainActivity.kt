package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.MusicDatabase
import com.example.data.repository.MusicRepository
import com.example.playback.PlaybackConnection
import com.example.ui.MusicViewModel
import com.example.ui.MusicViewModelFactory
import com.example.ui.screens.AppNavigation
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Core singletons setup using clean constructor injection dependency tree
        val database = MusicDatabase.getDatabase(applicationContext)
        val repository = MusicRepository(applicationContext, database.musicDao())
        val playbackConnection = PlaybackConnection(applicationContext, repository)

        val factory = MusicViewModelFactory(application, repository, playbackConnection)
        val viewModel = ViewModelProvider(this, factory)[MusicViewModel::class.java]

        setContent {
            val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
            val themeAccent by viewModel.themeAccent.collectAsStateWithLifecycle()
            val lang by viewModel.language.collectAsStateWithLifecycle()

            MyApplicationTheme(themeMode = themeMode, themeAccent = themeAccent, lang = lang) {
                Surface(
                    modifier = Modifier.fillMaxSize()
                ) {
                    AppNavigation(viewModel = viewModel)
                }
            }
        }
    }
}

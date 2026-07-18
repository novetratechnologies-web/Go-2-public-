package com.example

import android.os.Bundle
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import com.example.data.database.AppDatabase
import com.example.data.repository.ProgressRepository
import com.example.data.sync.CloudSyncService
import com.example.ui.BrowserApp
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.BrowserViewModel
import com.example.viewmodel.BrowserViewModelFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Proactively create WebView WebAssembly Code Cache directory to suppress harmless Chromium opendir error in logs
        try {
            val wasmCacheDir = java.io.File(applicationContext.cacheDir, "WebView/Default/HTTP Cache/Code Cache/wasm")
            if (!wasmCacheDir.exists()) {
                wasmCacheDir.mkdirs()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Initialize dependencies
        val database = AppDatabase.getDatabase(applicationContext)
        val repository = ProgressRepository(database)
        val syncService = CloudSyncService()
        
        val viewModelFactory = BrowserViewModelFactory(applicationContext, repository, syncService)
        val viewModel = ViewModelProvider(this, viewModelFactory)[BrowserViewModel::class.java]

        setContent {
            val themeMode by viewModel.themeMode.collectAsState()
            val isDark = when (themeMode) {
                "Dark" -> true
                "Light" -> false
                else -> androidx.compose.foundation.isSystemInDarkTheme()
            }
            MyApplicationTheme(darkTheme = isDark) {
                BrowserApp(viewModel = viewModel)
            }
        }
    }
}


package com.konvoiku.touring

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import com.konvoiku.touring.data.FirestoreConvoyRepository
import com.konvoiku.touring.ui.navigation.ConvoyNavGraph
import com.konvoiku.touring.ui.theme.KonvoiKuTheme
import com.konvoiku.touring.viewmodel.ConvoyViewModel
import org.osmdroid.config.Configuration

class MainActivity : ComponentActivity() {
    private lateinit var convoyViewModel: ConvoyViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirestoreConvoyRepository.init(applicationContext)
        convoyViewModel = ViewModelProvider(this)[ConvoyViewModel::class.java]
        convoyViewModel.handleIncomingIntent(intent)

        Configuration.getInstance().apply {
            userAgentValue = packageName
        }
        enableEdgeToEdge()
        setContent {
            KonvoiKuTheme {
                ConvoyNavGraph(viewModel = convoyViewModel)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (::convoyViewModel.isInitialized) {
            convoyViewModel.handleIncomingIntent(intent)
        }
    }
}


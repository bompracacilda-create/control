package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import com.example.data.TaxiDatabase
import com.example.data.TaxiRepository
import com.example.ui.TaxiDashboard
import com.example.ui.TaxiViewModel
import com.example.ui.TaxiViewModelFactory
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Setup edge-to-edge drawing
        enableEdgeToEdge()

        // Setup local Room database & repo
        val database = TaxiDatabase.getDatabase(applicationContext)
        val repository = TaxiRepository(database.taxiDao())

        // Build ViewModel using local repository factory
        val viewModel = ViewModelProvider(
            this,
            TaxiViewModelFactory(application, repository)
        )[TaxiViewModel::class.java]

        setContent {
            MyApplicationTheme {
                TaxiDashboard(viewModel = viewModel)
            }
        }
    }
}

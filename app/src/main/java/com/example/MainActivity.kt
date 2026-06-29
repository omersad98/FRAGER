package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import com.example.data.AppDatabase
import com.example.data.ShopRepository
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize Local Storage components
        val database = AppDatabase.getDatabase(this)
        val repository = ShopRepository(database.shopDao())
        val viewModelFactory = ShopViewModelFactory(repository)
        val viewModel = ViewModelProvider(this, viewModelFactory)[ShopViewModel::class.java]

        setContent {
            MyApplicationTheme {
                ShopDashboard(viewModel = viewModel)
            }
        }
    }
}

package com.speedo.admin.ui

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.speedo.admin.viewmodel.AdminViewModel
import com.speedo.core.theme.SpeedoTheme

class AdminMainActivity : ComponentActivity() {
    private val viewModel: AdminViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            splashScreen.setOnExitAnimationListener { splashScreenView ->
                splashScreenView.remove()
            }
        }
        setContent {
            SpeedoTheme {
                AdminMainScaffold(viewModel = viewModel)
            }
        }
    }
}

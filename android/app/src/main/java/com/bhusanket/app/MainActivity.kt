package com.bhusanket.app

import android.os.Bundle
import android.Manifest
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= 33) requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 100)
        CriticalAlerts.createChannel(this)
        setContent {
            BhuSanketApp(forceWarning = intent.getBooleanExtra("critical_warning", false))
        }
    }
}

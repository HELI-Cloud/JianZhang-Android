package com.jianzhang.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.jianzhang.app.ui.JzApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val route = intent.getStringExtra("reminder_route")
        setContent { JzApp(application as JzApplication, initialRoute = route) }
    }
}

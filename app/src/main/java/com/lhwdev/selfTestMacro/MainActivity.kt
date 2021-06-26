package com.lhwdev.selfTestMacro

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat


const val IGNORE_BATTERY_OPTIMIZATION_REQUEST = 1001


class MainActivity : ComponentActivity() {
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		
		// init UIs
		WindowCompat.setDecorFitsSystemWindows(window, false)
		
		setContent {
			ComposeApp(this)
		}
	}
}

package com.lhwdev.selfTestMacro

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat


const val IGNORE_BATTERY_OPTIMIZATION_REQUEST = 1001


class MainActivity : BaseActivity() {
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		
		// init UIs
		WindowCompat.setDecorFitsSystemWindows(window, false)
		
		setContent {
			ComposeApp(this)
		}
	}
}


interface Hi {
	companion object : Hi
}

fun hi() {
	var a: Hi = Hi
	a = object : Hi {}
}

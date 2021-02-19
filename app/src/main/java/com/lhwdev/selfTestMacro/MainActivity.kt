package com.lhwdev.selfTestMacro

import android.os.Bundle
import androidx.compose.ui.platform.setContent


const val IGNORE_BATTERY_OPTIMIZATION_REQUEST = 1001


@Suppress("SpellCheckingInspection")
class MainActivity : BaseActivity() {
	
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		
		setContent { ComposeApp(this) }
	}
}

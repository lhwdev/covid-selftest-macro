package com.lhwdev.selfTestMacro

import android.os.Bundle
import androidx.activity.compose.setContent


const val IGNORE_BATTERY_OPTIMIZATION_REQUEST = 1001


class MainActivity : BaseActivity() {
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		
		setContent {
			ComposeApp(this)
		}
	}
}

package com.lhwdev.selfTestMacro

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.lifecycle.lifecycleScope
import com.lhwdev.selfTestMacro.database.preferenceState
import com.lhwdev.selfTestMacro.navigation.ComposeNavigationHost
import com.lhwdev.selfTestMacro.navigation.NavigatorImpl
import com.lhwdev.selfTestMacro.ui.LocalPreference
import kotlinx.coroutines.launch


class UpdateActivity : AppCompatActivity() {
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		
		val navigator = NavigatorImpl()
		
		setContent {
			CompositionLocalProvider(
				LocalPreference provides preferenceState
			) {
				ComposeNavigationHost(navigator)
			}
		}
		
		lifecycleScope.launch {
			checkAndAskUpdate(navigator, 1001)
		}
	}
}

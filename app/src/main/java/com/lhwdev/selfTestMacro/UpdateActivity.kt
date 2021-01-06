package com.lhwdev.selfTestMacro

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch


class UpdateActivity : AppCompatActivity() {
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		
		lifecycleScope.launch {
			val update = getUpdateAvailable()
			if(update == null) finish()
			else askUpdate(update, 1001)
		}
	}
}

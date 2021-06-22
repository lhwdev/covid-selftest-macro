package com.lhwdev.selfTestMacro

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Switch
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import com.lhwdev.selfTestMacro.api.getUserGroup
import com.lhwdev.selfTestMacro.api.getUserInfo


@Preview
@Composable
fun Main() {
	val context = LocalActivity.current
	val pref = LocalPreference.current
	
	AutoScaffold(
		topBar = {
			TopAppBar(title = { Text("코로나19 자가진단 매크르") })
		}
	) { paddingValue ->
		
	}
}


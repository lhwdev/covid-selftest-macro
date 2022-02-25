package com.lhwdev.selfTestMacro.ui.pages.setup

import android.webkit.WebView
import androidx.compose.foundation.layout.Column
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.lhwdev.selfTestMacro.R
import com.lhwdev.selfTestMacro.navigation.Navigator
import com.lhwdev.selfTestMacro.ui.common.SimpleIconButton
import com.lhwdev.selfTestMacro.ui.utils.TextCheckbox
import com.vanpra.composematerialdialogs.Buttons
import com.vanpra.composematerialdialogs.NegativeButton
import com.vanpra.composematerialdialogs.PositiveButton
import com.vanpra.composematerialdialogs.showFullDialog



suspend fun Navigator.showSelfTestAgreementDialog(): Boolean? = showFullDialog { dismiss ->
	Scaffold(
		topBar = {
			TopAppBar(
				title = { Text("건강상태 자가진단 약관 동의") },
				navigationIcon = {
					SimpleIconButton(R.drawable.ic_clear_24, contentDescription = "닫기", onClick = requestClose)
				}
			)
		}
	) {
		Column {
			AndroidView(
				factory = {
					WebView(it).also { view ->
						view.loadUrl("https://hcs.eduro.go.kr/agreement")
					}
				},
				modifier = Modifier.weight(1f)
			)
			
			val (agree, setAgree) = remember { mutableStateOf(false) }
			
			TextCheckbox(
				text = { Text("위 약관에 모두 동의합니다.") },
				checked = agree, setChecked = setAgree,
				modifier = Modifier.align(Alignment.CenterHorizontally)
			)
			
			Buttons {
				PositiveButton(onClick = { dismiss(true) })
				NegativeButton(onClick = { dismiss(false) })
			}
		}
	}
}

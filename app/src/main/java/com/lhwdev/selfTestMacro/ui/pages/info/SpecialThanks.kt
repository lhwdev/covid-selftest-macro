package com.lhwdev.selfTestMacro.ui.pages.info

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.lhwdev.fetch.toJson
import com.lhwdev.selfTestMacro.App
import com.lhwdev.selfTestMacro.R
import com.lhwdev.selfTestMacro.navigation.LocalNavigator
import com.lhwdev.selfTestMacro.navigation.Navigator
import com.lhwdev.selfTestMacro.ui.DefaultContentColor
import com.lhwdev.selfTestMacro.ui.common.LinkedText
import com.lhwdev.selfTestMacro.ui.systemUi.AutoSystemUi
import com.lhwdev.selfTestMacro.ui.systemUi.TopAppBar
import com.vanpra.composematerialdialogs.Buttons
import com.vanpra.composematerialdialogs.PositiveButton
import com.vanpra.composematerialdialogs.showDialogAsync
import com.vanpra.composematerialdialogs.showFullDialogAsync
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


internal fun Navigator.showSpecialThanks() = showFullDialogAsync {
	AutoSystemUi { scrims ->
		TopAppBar(
			title = {}, statusBarScrim = scrims.statusBars,
			navigationIcon = {
				IconButton(onClick = requestClose) {
					Icon(painterResource(R.drawable.ic_arrow_back_24), contentDescription = "뒤로")
				}
			},
			elevation = 0.dp
		)
		Divider()
		
		SpecialThanksText()
		
		Spacer(Modifier.weight(1f))
		
		Buttons {
			PositiveButton(requestClose)
		}
		
		scrims.navigationBars()
	}
}

@Composable
private fun SpecialThanksText() {
	val navigator = LocalNavigator
	val data = produceState<Any?>(null) {
		withContext(Dispatchers.Default) {
			value = try {
				App.github.meta.specialThanks.get()
					.toJson(InfoUserStructure.Root.serializer(), anyContentType = true)
			} catch(th: Throwable) {
				false
			}
		}
	}.value
	
	if(data is InfoUserStructure.Root) for(line in data.titles) Row {
		for(element in line) {
			if(element.first() == '@') { // special meta
				val detailName = element.drop(1)
				val detail = data.details[detailName]
				if(detail == null) {
					Text(detailName)
				} else {
					LinkedText(detail.name, onClick = {
						navigator.showDialogAsync(maxHeight = Dp.Infinity) { InfoUsersDetail(detail) }
					})
				}
			} else {
				Text(element)
			}
		}
	} else {
		if(data == null) Text("Special Thanks 불러오는 중")
		else Text("Special Thanks를 불러오지 못했습니다", color = DefaultContentColor.copy(alpha = ContentAlpha.medium))
	}
}

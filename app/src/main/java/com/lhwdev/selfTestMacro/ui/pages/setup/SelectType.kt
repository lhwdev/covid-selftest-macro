package com.lhwdev.selfTestMacro.ui.pages.setup

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.lhwdev.selfTestMacro.R
import com.lhwdev.selfTestMacro.api.InstituteType
import com.lhwdev.selfTestMacro.ui.AutoSystemUi
import com.lhwdev.selfTestMacro.ui.OnScreenSystemUiMode
import com.lhwdev.selfTestMacro.ui.utils.DropdownPicker
import com.lhwdev.selfTestMacro.ui.utils.IconOnlyTopAppBar
import kotlinx.coroutines.launch



val AppBarHeight = 56.dp


@Composable
internal fun WizardSelectType(model: SetupModel, parameters: SetupParameters, wizard: SetupWizard) {
	val scope = rememberCoroutineScope()
	
	Surface(
		color = MaterialTheme.colors.primarySurface,
		modifier = Modifier.fillMaxSize()
	) {
		AutoSystemUi(
			enabled = wizard.isCurrent,
			onScreenMode = OnScreenSystemUiMode.Immersive(scrimColor = Color.Transparent)
		) { scrims ->
			scrims.statusBar()
			
			if(parameters.endRoute != null) IconOnlyTopAppBar(
				navigationIcon = painterResource(R.drawable.ic_clear_24),
				contentDescription = "닫기",
				onClick = parameters.endRoute
			) else Spacer(Modifier.height(AppBarHeight))
			
			WizardCommon(
				wizard,
				showNext = model.instituteInfo != null,
				wizardFulfilled = model.instituteInfo != null,
				showNotFulfilledWarning = {
					scope.launch {
						model.scaffoldState.snackbarHostState.showSnackbar(
							"기관 유형을 선택해주세요",
							actionLabel = "확인"
						)
					}
				},
				modifier = Modifier.weight(1f)
			) {
				Column(
					modifier = Modifier.padding(12.dp),
					verticalArrangement = Arrangement.Bottom,
					horizontalAlignment = Alignment.CenterHorizontally,
				) {
					Spacer(Modifier.weight(3f))
					
					Text("사용자 추가", style = MaterialTheme.typography.h3)
					
					Spacer(Modifier.weight(5f))
					
					DropdownPicker(
						dropdown = { onDismiss ->
							val institutes = /*InstituteType.values()*/
								arrayOf(InstituteType.school) // TODO: support all types
							for(type in institutes) DropdownMenuItem(onClick = {
								val newSelect = when(type) {
									InstituteType.school -> InstituteInfoModel.School()
									InstituteType.university -> TODO()
									InstituteType.academy -> TODO()
									InstituteType.office -> TODO()
								}
								val previousSelect = model.instituteInfo
								if(previousSelect == null || previousSelect::class != newSelect::class) {
									model.instituteInfo = newSelect
								}
								onDismiss()
								wizard.next()
							}) {
								Text(type.displayName)
							}
						},
						isEmpty = model.instituteInfo == null,
						label = { Text("기관 유형") },
						modifier = Modifier.padding(8.dp)
					) {
						Text(model.instituteInfo?.type?.displayName ?: "")
					}
					
					Spacer(Modifier.weight(8f))
				}
			}
			
			scrims.navigationBar()
		}
	}
}

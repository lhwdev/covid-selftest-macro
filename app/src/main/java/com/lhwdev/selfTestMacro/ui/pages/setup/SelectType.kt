package com.lhwdev.selfTestMacro.ui.pages.setup

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.lhwdev.selfTestMacro.R
import com.lhwdev.selfTestMacro.api.InstituteType
import com.lhwdev.selfTestMacro.ui.systemUi.AutoSystemUi
import com.lhwdev.selfTestMacro.ui.systemUi.OnScreenSystemUiMode
import com.lhwdev.selfTestMacro.ui.utils.ExposedDropdownMenuField
import com.lhwdev.selfTestMacro.ui.utils.IconOnlyTopAppBar
import com.lhwdev.selfTestMacro.ui.utils.myTextFieldColors
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
			onScreen = OnScreenSystemUiMode.Immersive()
		) { scrims ->
			scrims.statusBars()
			
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
					
					val (expanded, setExpanded) = remember { mutableStateOf(false) }
					ExposedDropdownMenuField(
						expanded = expanded,
						onExpandedChange = setExpanded,
						isEmpty = model.instituteInfo == null,
						label = { Text("기관 유형") },
						colors = TextFieldDefaults.myTextFieldColors(),
						fieldModifier = Modifier.padding(8.dp).fillMaxWidth(),
						fieldContent = {
							Text(model.instituteInfo?.type?.displayName ?: "")
						},
						dropdownContent = {
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
								setExpanded(false)
								wizard.next()
							}) {
								Text(type.displayName)
							}
						}
					)
					
					Spacer(Modifier.weight(8f))
				}
			}
			
			scrims.navigationBars()
		}
	}
}

package com.lhwdev.selfTestMacro.ui.pages.setup

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.lhwdev.selfTestMacro.R
import com.lhwdev.selfTestMacro.api.InstituteInfo
import com.lhwdev.selfTestMacro.api.InstituteType
import com.lhwdev.selfTestMacro.api.SearchKey
import com.lhwdev.selfTestMacro.debug.LocalDebugContext
import com.lhwdev.selfTestMacro.debug.log
import com.lhwdev.selfTestMacro.navigation.LocalNavigator
import com.lhwdev.selfTestMacro.repository.LocalSelfTestManager
import com.lhwdev.selfTestMacro.sRegions
import com.lhwdev.selfTestMacro.sSchoolLevels
import com.lhwdev.selfTestMacro.ui.common.SimpleIconButton
import com.lhwdev.selfTestMacro.ui.primaryActive
import com.lhwdev.selfTestMacro.ui.systemUi.AutoSystemUi
import com.lhwdev.selfTestMacro.ui.systemUi.OnScreenSystemUiMode
import com.lhwdev.selfTestMacro.ui.systemUi.TopAppBar
import com.lhwdev.selfTestMacro.ui.utils.ExposedDropdownMenuField
import com.vanpra.composematerialdialogs.*
import kotlinx.coroutines.launch


@Composable
internal fun WizardInstituteInfo(
	model: InstituteInfoModel,
	setupModel: SetupModel,
	parameters: SetupParameters,
	wizard: SetupWizard,
) {
	val notFulfilled = remember { mutableStateOf(-1) }
	
	AutoSystemUi(
		enabled = wizard.isCurrent,
		navigationBars = OnScreenSystemUiMode.Immersive()
	) { scrims ->
		Scaffold(
			topBar = {
				TopAppBar(
					title = { Text("${model.type.displayName} 정보 입력") },
					backgroundColor = MaterialTheme.colors.surface,
					navigationIcon = if(parameters.endRoute == null) null else ({
						SimpleIconButton(
							icon = R.drawable.ic_clear_24, contentDescription = "닫기",
							onClick = parameters.endRoute
						)
					}),
					statusBarScrim = { scrims.statusBars() }
				)
			},
			modifier = Modifier.weight(1f)
		) { paddingValues ->
			Column(Modifier.padding(paddingValues)) {
				when(model) {
					is InstituteInfoModel.School -> WizardSchoolInfo(
						model,
						setupModel,
						notFulfilled,
						wizard
					)
					// null -> wizard.before()
				}
			}
		}
		
		scrims.navigationBars()
	}
}


@Composable
internal fun FloatingMaterialDialogScope.MultipleInstituteDialog(
	instituteType: InstituteType,
	institutes: List<InstituteInfo>,
	onSelect: (InstituteInfo?) -> Unit,
) {
	Title { Text("${instituteType.displayName} 선택") }
	
	Column(modifier = Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState())) {
		for(institute in institutes) ListItem(
			modifier = Modifier.clickable { onSelect(institute) }
		) {
			Text(institute.name, style = MaterialTheme.typography.body1)
		}
	}
	
	Buttons {
		NegativeButton(onClick = requestClose)
	}
}


@Composable
internal fun ColumnScope.WizardSchoolInfo(
	model: InstituteInfoModel.School,
	setupModel: SetupModel,
	notFulfilled: MutableState<Int>,
	wizard: SetupWizard
) {
	val scope = rememberCoroutineScope()
	LocalContext.current
	val selfTestManager = LocalSelfTestManager.current
	val navigator = LocalNavigator
	val debugContext = LocalDebugContext.current
	
	var complete by remember { mutableStateOf(false) }
	
	fun findSchool() = scope.launch find@{
		fun selectSchool(info: InstituteInfo, searchKey: SearchKey) {
			model.institute = info
			model.schoolName = info.name
			setupModel.searchKey = searchKey
			complete = true
			wizard.next()
		}
		
		val snackbarHostState = setupModel.scaffoldState.snackbarHostState
		
		val searchResult = runCatching {
			log("#1. 학교 정보 찾기")
			selfTestManager.api.findSchool(
				regionCode = model.regionCode,
				schoolLevelCode = model.schoolLevel,
				name = model.schoolName
			)
		}.getOrElse { exception ->
			debugContext.onError("학교를 찾지 못했어요.", exception)
			return@find
		}
		val schools = searchResult.instituteList
		
		if(schools.isEmpty()) {
			snackbarHostState.showSnackbar("학교를 찾지 못했어요.", "확인")
			return@find
		}
		
		if(schools.size > 1) navigator.showDialogUnit { removeRoute ->
			MultipleInstituteDialog(
				instituteType = InstituteType.school,
				institutes = schools,
				onSelect = {
					removeRoute()
					if(it != null) selectSchool(it, searchResult.searchKey)
				}
			)
		} else selectSchool(schools[0], searchResult.searchKey)
	}
	
	
	WizardCommon(
		wizard,
		onNext = {
			if(model.institute != null && complete) wizard.next()
			else findSchool()
		},
		wizardFulfilled = model.notFulfilledIndex == -1,
		showNotFulfilledWarning = { notFulfilled.value = model.notFulfilledIndex },
		modifier = Modifier.weight(1f)
	) {
		Column(
			modifier = Modifier
				.padding(12.dp)
				.verticalScroll(rememberScrollState())
		) {
			val commonModifier = Modifier
				.fillMaxWidth()
				.padding(8.dp)
			
			val (levelExpanded, setLevelExpanded) = remember { mutableStateOf(false) }
			ExposedDropdownMenuField(
				expanded = levelExpanded,
				onExpandedChange = setLevelExpanded,
				isEmpty = model.schoolLevel == 0,
				isError = notFulfilled.value == 0,
				label = { Text("학교 단계") },
				fieldModifier = commonModifier,
				fieldContent = {
					Text(sSchoolLevels[model.schoolLevel] ?: "")
				},
				dropdownContent = {
					for((code, name) in sSchoolLevels.entries) DropdownMenuItem(onClick = {
						model.schoolLevel = code
						notFulfilled.value = -1
						complete = false
						setLevelExpanded(false)
					}) {
						Text(name)
					}
				}
			)
			
			val (regionExpanded, setRegionExpanded) = remember { mutableStateOf(false) }
			ExposedDropdownMenuField(
				expanded = regionExpanded,
				onExpandedChange = setRegionExpanded,
				isEmpty = model.regionCode == "",
				isError = notFulfilled.value == 1,
				label = { Text("지역") },
				fieldModifier = commonModifier,
				fieldContent = {
					Text(if(model.regionCode == null) "(전국에서 검색)" else sRegions[model.regionCode] ?: "")
				},
				dropdownContent = {
					fun onPick(code: String?) = ({
						model.regionCode = code
						notFulfilled.value = -1
						complete = false
						setRegionExpanded(false)
					})
					
					DropdownMenuItem(onClick = onPick(null)) {
						Text("전국에서 검색", color = MaterialTheme.colors.primaryActive)
					}
					
					for((code, name) in sRegions.entries) DropdownMenuItem(onClick = onPick(code)) {
						Text(name)
					}
				}
			)
			
			TextField(
				value = model.schoolName,
				onValueChange = {
					model.schoolName = it
					notFulfilled.value = -1
					complete = false
				},
				label = { Text("학교 이름") },
				keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
				keyboardActions = KeyboardActions { findSchool() },
				singleLine = true,
				isError = notFulfilled.value == 2,
				trailingIcon = {
					SimpleIconButton(
						icon = if(complete) R.drawable.ic_check_24 else R.drawable.ic_search_24,
						contentDescription = "검색",
						onClick = { findSchool() }
					)
				},
				modifier = commonModifier
			)
		}
	}
}

package com.lhwdev.selfTestMacro

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.lhwdev.selfTestMacro.api.InstituteInfo
import com.lhwdev.selfTestMacro.api.getSchoolData
import com.vanpra.composematerialdialogs.Buttons
import com.vanpra.composematerialdialogs.MaterialDialog
import com.vanpra.composematerialdialogs.Title
import kotlinx.coroutines.launch
import kotlin.math.max


@Stable
class SetupModel {
	var scaffoldState = ScaffoldState(DrawerState(DrawerValue.Closed), SnackbarHostState())
	var selectInstitute by mutableStateOf<WizardSecondModel?>(null)
	var studentName by mutableStateOf("")
	var studentBirth by mutableStateOf("")
}


class WizardIndexPreviewProvider : PreviewParameterProvider<Int> {
	override val values: Sequence<Int>
		get() = (0 until sPagesCount).asSequence()
	override val count: Int get() = sPagesCount
}

@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
fun SetupPreview(@PreviewParameter(WizardIndexPreviewProvider::class) index: Int) {
	PreviewBase(statusBar = true) {
		val model = SetupModel().apply {
			selectInstitute = WizardSecondModel.School()
		}
		
		SetupWizardPage(model, SetupWizard(index, index, sPagesCount) {})
	}
}


@Composable
fun Setup() {
	val model = remember { SetupModel() }
	SetupWizardView(model)
}

private data class SetupWizard(
	val index: Int,
	val currentIndex: Int,
	val count: Int,
	val scrollTo: (index: Int) -> Unit,
)

private const val sPagesCount = 3

@Composable
fun SetupWizardView(model: SetupModel) {
	var pageIndex by remember { mutableStateOf(0) }
	
	AutoScaffold(
		scaffoldState = model.scaffoldState
	) {
		WizardPager(pageIndex = pageIndex, pagesCount = sPagesCount) { index ->
			val wizard = SetupWizard(index, pageIndex, sPagesCount) {
				pageIndex = it
			}
			SetupWizardPage(model, wizard)
		}
	}
}

@Composable
private fun SetupWizardPage(model: SetupModel, wizard: SetupWizard) {
	when(wizard.index) {
		0 -> SetupWizardFirst(model, wizard)
		1 -> SetupWizardSecond(model.selectInstitute ?: return, model, wizard)
		2 -> SetupWizardThird(model, wizard)
		else -> error("unknown page")
	}
}


private const val sPreloadPages = 1

@Composable
fun WizardPager(
	pageIndex: Int,
	pagesCount: Int,
	content: @Composable (index: Int) -> Unit,
) {
	var maxLoads by remember { mutableStateOf(1) }
	
	BoxWithConstraints {
		val scope = rememberCoroutineScope()
		val width = maxWidth
		val widthPx = with(LocalDensity.current) { width.roundToPx() }
		
		var targetPage by remember(pagesCount) { mutableStateOf(pageIndex) }
		val scrollState = remember(pagesCount) { ScrollState(pageIndex) }
		
		fun scrollTo(target: Int) {
			check(target in 0 until pagesCount) { "target index $target not in bound (0 until pagesCount)" }
			targetPage = target
			scope.launch {
				scrollState.animateScrollTo(target * widthPx)
			}
		}
		
		if(pageIndex != targetPage) {
			maxLoads = max(maxLoads, pageIndex + 1)
			scrollTo(pageIndex)
		}
		
		Row(
			modifier = Modifier.horizontalScroll(
				scrollState,
				enabled = false
			)
		) {
			for(index in 0 until pagesCount) {
				Box(Modifier.requiredWidth(width)) {
					if(index < maxLoads +
						if(scrollState.isScrollInProgress) sPreloadPages else 0
					) content(index)
				}
			}
		}
	}
}


private fun SetupWizard.before() {
	scrollTo(index - 1)
}

private fun SetupWizard.next() {
	scrollTo(index + 1)
}


@Composable
private fun SetupWizardCommon(
	wizard: SetupWizard,
	wizardFulfilled: Boolean,
	showNotFulfilledWarning: () -> Unit,
	modifier: Modifier = Modifier,
	showNext: Boolean = true,
	content: @Composable () -> Unit,
) {
	Column(modifier = modifier) {
		Box(Modifier.weight(1f)) {
			content()
		}
		
		Row {
			if(wizard.index != 0) IconButton(onClick = { wizard.before() }) {
				Icon(
					painterResource(id = R.drawable.ic_arrow_left_24),
					contentDescription = "before"
				)
			}
			
			Spacer(Modifier.weight(100f))
			
			
			if(showNext && wizard.index != wizard.count - 1) IconButton(
				onClick = {
					if(wizardFulfilled) {
						wizard.next()
					} else showNotFulfilledWarning()
				}
			) {
				val contentColor = LocalContentColor.current.copy(alpha = LocalContentAlpha.current)
				
				Icon(
					painterResource(id = R.drawable.ic_arrow_right_24),
					contentDescription = "next",
					tint = if(wizardFulfilled) contentColor else contentColor.copy(alpha = 0.9f)
				)
			}
		}
	}
}


// Setup wizards

@Composable
private fun SetupWizardFirst(model: SetupModel, wizard: SetupWizard) {
	val scope = rememberCoroutineScope()
	
	AutoSystemUi(
		enabled = wizard.currentIndex == 0,
		onScreenMode = OnScreenSystemUiMode.Immersive(scrimColor = Color.Transparent)
	) { scrims ->
		Surface(
			color = MaterialTheme.colors.primarySurface,
			modifier = Modifier.fillMaxSize()
		) {
			Column {
				SetupWizardCommon(
					wizard,
					showNext = model.selectInstitute != null,
					wizardFulfilled = model.selectInstitute != null,
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
					scrims.statusBar()
					
					Column(
						modifier = Modifier.padding(16.dp),
						verticalArrangement = Arrangement.Bottom,
						horizontalAlignment = Alignment.CenterHorizontally,
					) {
						Spacer(Modifier.weight(3f))
						
						Text("사용자 추가", style = MaterialTheme.typography.h3)
						
						Spacer(Modifier.weight(5f))
						
						DropdownPicker(
							dropdown = { onDismiss ->
								for(type in InstituteType.values()) DropdownMenuItem(onClick = {
									model.selectInstitute = when(type) {
										InstituteType.school -> WizardSecondModel.School()
										InstituteType.university -> TODO()
										InstituteType.academy -> TODO()
										InstituteType.office -> TODO()
									}
									onDismiss()
									wizard.next()
								}) {
									Text(type.displayName)
								}
							},
							isEmpty = model.selectInstitute == null,
							label = { Text("기관 유형") }
						) {
							Text(model.selectInstitute?.type?.displayName ?: "")
						}
						
						Spacer(Modifier.weight(8f))
					}
				}
				
				scrims.navigationBar()
			}
		}
	}
}


@Composable
private fun SetupWizardSecond(
	model: WizardSecondModel,
	setupModel: SetupModel,
	wizard: SetupWizard,
) {
	val notFulfilled = remember { mutableStateOf(-1) }
	
	AutoSystemUi(
		enabled = wizard.currentIndex == 1,
		statusBarMode = OnScreenSystemUiMode.Immersive(scrimColor = Color.Transparent)
	) { scrims ->
		Column {
			SetupWizardCommon(
				wizard,
				wizardFulfilled = model.notFulfilledIndex == -1,
				showNotFulfilledWarning = { notFulfilled.value = model.notFulfilledIndex },
				modifier = Modifier.weight(1f)
			) {
				Scaffold(
					topBar = {
						TopAppBar(
							title = { Text("${model.type.displayName} 정보 입력") },
							backgroundColor = MaterialTheme.colors.surface,
							statusBarScrim = { scrims.statusBar() }
						)
					}
				) { paddingValues ->
					Column(
						modifier = Modifier
							.padding(12.dp)
							.padding(paddingValues)
					) {
						
						when(model) {
							is WizardSecondModel.School -> SetupWizardSecondSchool(
								model,
								setupModel,
								notFulfilled,
								wizard
							)
							// null -> wizard.before()
						}
						
					}
				}
			}
			
			scrims.navigationBar()
		}
	}
}


@Composable
private fun MultipleInstituteDialog(
	instituteType: InstituteType,
	institutes: List<InstituteInfo>,
	onSelect: (InstituteInfo?) -> Unit,
) {
	MaterialDialog(onCloseRequest = { onSelect(null) }) {
		Title { Text("${instituteType.displayName} 선택") }
		
		Column {
			@OptIn(ExperimentalMaterialApi::class)
			for(institute in institutes) ListItem(
				modifier = Modifier
					.clickable { onSelect(institute) }
					.padding(vertical = 4.dp)
			) {
				Text(institute.name, style = MaterialTheme.typography.body1)
			}
		}
		
		Buttons {
			NegativeButton(onClick = { onSelect(null) }) { Text("취소") }
		}
	}
}


@Suppress("CanSealedSubClassBeObject") // model: no comparison needed
@Stable
sealed class WizardSecondModel {
	abstract val type: InstituteType
	abstract val notFulfilledIndex: Int
	
	class School : WizardSecondModel() {
		var schoolLevel by mutableStateOf(0)
		var regionCode by mutableStateOf("")
		var schoolName by mutableStateOf("")
		var instituteInfo by mutableStateOf<InstituteInfo?>(null)
		
		override val notFulfilledIndex: Int
			get() = when {
				schoolLevel == 0 -> 0
				regionCode.isBlank() -> 1
				schoolName.isBlank() -> 2
				instituteInfo == null -> 3
				else -> -1
			}
		
		override val type: InstituteType
			get() = InstituteType.school
	}
}

@Composable
private fun SetupWizardSecondSchool(
	model: WizardSecondModel.School,
	setupModel: SetupModel,
	notFulfilled: MutableState<Int>,
	wizard: SetupWizard,
) {
	val scope = rememberCoroutineScope()
	val route = LocalRoute.current
	val commonModifier = Modifier
		.fillMaxWidth()
		.padding(8.dp)
	
	DropdownPicker(
		dropdown = { onDismiss ->
			for((code, name) in sSchoolLevels.entries) DropdownMenuItem(onClick = {
				model.schoolLevel = code
				notFulfilled.value = -1
				onDismiss()
			}) {
				Text(name)
			}
		},
		isEmpty = model.schoolLevel == 0,
		isErrorValue = notFulfilled.value == 0,
		label = { Text("학교 단계") },
		modifier = commonModifier
	) {
		Text(sSchoolLevels[model.schoolLevel] ?: "")
	}
	
	DropdownPicker(
		dropdown = { onDismiss ->
			for((code, name) in sRegions.entries) DropdownMenuItem(onClick = {
				model.regionCode = code
				notFulfilled.value = -1
				onDismiss()
			}) {
				Text(name)
			}
		},
		isEmpty = model.regionCode.isEmpty(),
		isErrorValue = notFulfilled.value == 1,
		label = { Text("지역") },
		modifier = commonModifier
	) {
		Text(sRegions[model.regionCode] ?: "")
	}
	
	
	fun findSchool() = scope.launchIoTask find@{
		val snackbarHostState = setupModel.scaffoldState.snackbarHostState
		
		val schools = runCatching {
			getSchoolData(
				regionCode = model.regionCode,
				schoolLevelCode = "${model.schoolLevel}",
				name = model.schoolName
			).instituteList
		}.getOrElse { exception ->
			onError(snackbarHostState, "학교를 찾지 못했습니다.", exception)
			return@find
		}
		
		if(schools.isEmpty()) {
			snackbarHostState.showSnackbar("학교를 찾지 못했습니다.", "확인")
			return@find
		}
		
		if(schools.size > 1) route.add @Composable {
			MultipleInstituteDialog(
				instituteType = InstituteType.school,
				institutes = schools,
				onSelect = {
					if(it != null) model.instituteInfo = it
					route.removeLast()
				}
			)
		}
		else model.instituteInfo = schools[0]
		wizard.next()
	}
	
	
	TextField(
		value = model.schoolName,
		onValueChange = {
			model.schoolName = it
			notFulfilled.value = -1
		},
		label = { Text("학교 이름") },
		keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
		keyboardActions = KeyboardActions { findSchool() },
		singleLine = true,
		isError = notFulfilled.value == 2,
		trailingIcon = {
			IconButton(onClick = { findSchool() }) {
				Icon(painterResource(R.drawable.ic_search_24), contentDescription = "search")
			}
		},
		modifier = commonModifier
	)
}


@Composable
private fun SetupWizardThird(model: SetupModel, wizard: SetupWizard) {
	// SetupWizardCommon(wizard) {
	// when(model.instituteType) {
	// 	InstituteType.school -> SetupWizardThirdSchool(model)
	// 	InstituteType.university -> TODO()
	// 	InstituteType.academy -> TODO()
	// 	InstituteType.office -> TODO()
	// 	null -> TODO()
	// }
	// }
}


@Composable
private fun SetupWizardThirdSchool(model: SetupModel) {

}

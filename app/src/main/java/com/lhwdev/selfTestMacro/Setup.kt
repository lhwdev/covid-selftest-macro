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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.lhwdev.selfTestMacro.api.InstituteInfo
import com.lhwdev.selfTestMacro.api.getSchoolData
import kotlinx.coroutines.launch


@Stable
class SetupModel {
	var scaffoldState = ScaffoldState(DrawerState(DrawerValue.Closed), SnackbarHostState())
	var institutionType by mutableStateOf<InstitutionType?>(null)
	var region by mutableStateOf<String?>(null)
	var level by mutableStateOf<Int?>(null)
	var institutionInfo by mutableStateOf<InstituteInfo?>(null)
	var studentName by mutableStateOf("")
	var studentBirth by mutableStateOf("")
}


@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
fun SetupPreview() {
	PreviewBase {
		Setup()
	}
}


@Composable
fun Setup(isFirst: Boolean = false) {
	val pref = LocalPreference.current
	val model = remember { SetupModel() }
	SetupWizardView(model)
}

private data class SetupWizardItem(
	val index: Int,
	val count: Int,
	val scrollTo: (index: Int) -> Unit
)

@Composable
fun SetupWizardView(model: SetupModel) {
	var pageIndex by remember { mutableStateOf(0) }
	val pagesCount = 2
	
	Scaffold(
		scaffoldState = model.scaffoldState
	) {
		WizardPager(pageIndex = pageIndex, pagesCount = pagesCount) { index ->
			val item = SetupWizardItem(index, pagesCount) { pageIndex = it }
			
			when(index) {
				0 -> SetupWizardFirst(model, item)
				1 -> SetupWizardSecond(model, item)
				else -> error("unknown page")
			}
		}
	}
}


@Composable
fun WizardPager(
	pageIndex: Int,
	pagesCount: Int,
	content: @Composable (index: Int) -> Unit
) {
	BoxWithConstraints {
		val scope = rememberCoroutineScope()
		val width = maxWidth
		val widthPx = with(LocalDensity.current) { width.roundToPx() }
		
		var targetPage by remember(pagesCount) { mutableStateOf(pageIndex) }
		val scrollState = remember(pagesCount) { ScrollState(pageIndex) }
		println(scrollState.value)
		fun scrollTo(target: Int) {
			targetPage = target
			scope.launch {
				scrollState.animateScrollTo(target * widthPx)
			}
		}
		
		if(pageIndex != targetPage) scrollTo(pageIndex)
		
		Row(
			modifier = Modifier.horizontalScroll(
				scrollState,
				enabled = false
			)
		) {
			repeat(pagesCount) { index ->
				Box(Modifier.requiredWidth(width)) {
					content(index)
				}
			}
		}
	}
}


private fun SetupWizardItem.before() {
	scrollTo(index - 1)
}

private fun SetupWizardItem.next() {
	scrollTo(index + 1)
}


@Composable
private fun SetupWizardCommon(
	item: SetupWizardItem,
	content: @Composable (SetupWizardItem) -> Unit
) {
	Column {
		Box(Modifier.weight(1f)) {
			content(item)
		}
		
		Row {
			if(item.index != 0) IconButton(onClick = { item.before() }) {
				Icon(
					painterResource(id = R.drawable.ic_arrow_left_24),
					contentDescription = "before"
				)
			}
			
			Spacer(Modifier.weight(100f))
			
			if(item.index != item.count - 1) IconButton(onClick = {
				item.next()
			}) {
				Icon(
					painterResource(id = R.drawable.ic_arrow_right_24),
					contentDescription = "next"
				)
			}
		}
	}
}


// Setup wizards


private object SetupModelPreviewProvider : PreviewParameterProvider<SetupModel> {
	override val values: Sequence<SetupModel>
		get() = listOf(SetupModel().apply {
			institutionType = InstitutionType.school
			region = "03"
			level = 4
			institutionInfo = InstituteInfo("어느고등학교", "code", "address", "...")
			studentName = "김철수"
			studentBirth = "121212"
		}).asSequence()
}

private object SetupWizardItemProvider : PreviewParameterProvider<SetupWizardItem> {
	override val values: Sequence<SetupWizardItem>
		get() = listOf(SetupWizardItem(1, 3) {}).asSequence()
}


@Preview
@Composable
private fun SetupWizardFirst(
	@PreviewParameter(SetupModelPreviewProvider::class) model: SetupModel,
	@PreviewParameter(SetupWizardItemProvider::class) item: SetupWizardItem
) {
	Surface(
		color = MaterialTheme.colors.primarySurface,
		modifier = Modifier.fillMaxSize()
	) {
		SetupWizardCommon(item) {
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
						for(type in InstitutionType.values()) DropdownMenuItem(onClick = {
							model.institutionType = type
							onDismiss()
						}) {
							Text(type.displayName)
						}
					},
					isEmpty = model.institutionType == null,
					label = { Text("기관 유형") }
				) {
					Text(model.institutionType?.displayName ?: "")
				}
				
				Spacer(Modifier.weight(8f))
			}
		}
	}
}


@Composable
private fun SetupWizardSecond(model: SetupModel, item: SetupWizardItem) {
	SetupWizardCommon(item) {
		Scaffold(
			topBar = {
				TopAppBar(
					title = { Text("${model.institutionType?.displayName ?: "기관"} 정보 입력") }
				)
			}
		) { paddingValues ->
			Column(
				modifier = Modifier
					.padding(12.dp)
					.padding(paddingValues)
			) {
				
				when(model.institutionType) {
					InstitutionType.school -> {
						SetupWizardSecondSchool(model)
					}
					
					InstitutionType.academy -> {
					
					}
					
				}
				
			}
		}
	}
}


@Composable
private fun MultipleInstitutionDialog(
	institutionType: InstitutionType,
	institutions: List<InstituteInfo>,
	onSelect: (InstituteInfo?) -> Unit
) {
	Dialog(
		onDismissRequest = { onSelect(null) }
	) {
		Column {
			Text("${institutionType.displayName} 선택", style = MaterialTheme.typography.h4)
			
			Spacer(Modifier.height(8.dp))
			
			Column {
				@OptIn(ExperimentalMaterialApi::class)
				for(institution in institutions) ListItem(
					modifier = Modifier
						.clickable { onSelect(institution) }
						.padding(vertical = 4.dp)
				) {
					Text(institution.name, style = MaterialTheme.typography.body1)
				}
			}
			
			Spacer(Modifier.height(8.dp))
			
			Row(modifier = Modifier.padding(4.dp), horizontalArrangement = Arrangement.End) {
				TextButton(onClick = { onSelect(null) }) { Text("취소") }
			}
		}
	}
	
}


@Composable
private fun SetupWizardSecondSchool(model: SetupModel) {
	val scope = rememberCoroutineScope()
	val route = LocalRoute.current
	val commonModifier = Modifier
		.fillMaxWidth()
		.padding(8.dp)
	
	var schoolLevel by remember { mutableStateOf(0) }
	var regionCode by remember { mutableStateOf("") }
	var schoolName by remember { mutableStateOf("") }
	
	DropdownPicker(
		dropdown = { onDismiss ->
			for((code, name) in sSchoolLevels.entries) DropdownMenuItem(onClick = {
				schoolLevel = code
				onDismiss()
			}) {
				Text(name)
			}
		},
		isEmpty = schoolLevel == 0,
		label = { Text("학교 단계") },
		modifier = commonModifier
	) {
		Text(sSchoolLevels[schoolLevel] ?: "")
	}
	
	DropdownPicker(
		dropdown = { onDismiss ->
			for((code, name) in sRegions.entries) DropdownMenuItem(onClick = {
				regionCode = code
				onDismiss()
			}) {
				Text(name)
			}
		},
		isEmpty = regionCode.isEmpty(),
		label = { Text("지역") },
		modifier = commonModifier
	) {
		Text(sRegions[regionCode] ?: "")
	}
	
	
	fun findSchool() = scope.launchIoTask find@{
		val snackbarHostState = model.scaffoldState.snackbarHostState
		
		val schools = runCatching {
			getSchoolData(
				regionCode = regionCode,
				schoolLevelCode = "$schoolLevel",
				name = schoolName
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
			MultipleInstitutionDialog(
				institutionType = model.institutionType
					?: InstitutionType.school, // will never null but just in case
				institutions = schools,
				onSelect = {
					if(it != null) model.institutionInfo = it
					route.removeLast()
				}
			)
		}
		else model.institutionInfo = schools[0]
	}
	
	
	TextField(
		value = schoolName,
		onValueChange = { schoolName = it },
		label = { Text("학교 이름") },
		keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
		keyboardActions = KeyboardActions { findSchool() },
		singleLine = true,
		trailingIcon = {
			IconButton(onClick = { findSchool() }) {
				Icon(painterResource(R.drawable.ic_search_24), contentDescription = "search")
			}
		},
		modifier = commonModifier
	)
}

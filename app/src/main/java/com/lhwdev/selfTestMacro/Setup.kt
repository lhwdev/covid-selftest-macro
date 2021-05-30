package com.lhwdev.selfTestMacro

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.lhwdev.selfTestMacro.api.LoginType
import kotlinx.coroutines.launch


@Stable
class SetupModel {
	var institutionType by mutableStateOf<InstitutionType?>(null)
	var region by mutableStateOf<String?>(null)
	var level by mutableStateOf<Int?>(null)
	var schoolName by mutableStateOf("")
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

val sLoginTypes = mapOf(
	LoginType.school to "학교",
	LoginType.univ to "대학교",
	LoginType.office to "회사"
)

private data class SetupWizardItem(val index: Int, val scrollTo: (index: Int) -> Unit)

@Composable
fun SetupWizardView(model: SetupModel) {
	var pageIndex by mutableStateOf(0)
	WizardPager(pageIndex = pageIndex, pagesCount = 1) { index ->
		val item = SetupWizardItem(index) { pageIndex = it }
		
		when(index) {
			0 -> SetupWizardFirst(model, item)
			else -> error("unknown page")
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
		
		fun scrollTo(target: Int) {
			targetPage = target
			scope.launch {
				scrollState.scrollTo(target)
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


@Composable
private fun SetupWizardCommon(
	item: SetupWizardItem,
	content: @Composable (SetupWizardItem) -> Unit
) {
	Column {
		content(item)
		
		Row {
			Icon(
				painterResource(id = R.drawable.ic_arrow_left_24),
				contentDescription = "before"
			)
			Spacer(Modifier.weight(100f))
			Icon(painterResource(
				id = R.drawable.ic_arrow_right_24),
				contentDescription = "next"
			)
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
			schoolName = "어느고등학교"
			studentName = "김철수"
			studentBirth = "121212"
		}).asSequence()
}

private object SetupWizardItemProvider : PreviewParameterProvider<SetupWizardItem> {
	override val values: Sequence<SetupWizardItem>
		get() = listOf(SetupWizardItem(1) {}).asSequence()
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
				
				Spacer(Modifier.weight(2f))
				
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
				
				Spacer(Modifier.weight(4.5f))
			}
		}
	}
}


// @Composable
// private fun SetupWizardSecond(model: SetupModel, item: SetupWizardItem) {
// 	Column(modifier = Modifier.padding(12.dp)) {
// 		val loginType = model.loginType
// 		val commonModifier = Modifier
// 			.fillMaxWidth()
// 			.padding(8.dp)
//
// 		DropdownPicker(
// 			dropdown = { onDismiss ->
// 				for(type in LoginType.values()) DropdownMenuItem(onClick = {
// 					model.loginType = type
// 					onDismiss()
// 				}) {
// 					Text(sLoginTypes[type] ?: "???")
// 				}
// 			},
// 			isEmpty = loginType == null,
// 			label = { Text("학교") },
// 			modifier = commonModifier
// 		) {
// 			Text(if(loginType == null) "" else sLoginTypes[loginType] ?: "???")
// 		}
//
// 		TextField(
// 			value = model.studentName,
// 			onValueChange = { model.studentName = it },
// 			label = { Text("이름") },
// 			modifier = commonModifier
// 		)
// 	}
// }

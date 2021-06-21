package com.lhwdev.selfTestMacro

import android.content.Context
import androidx.activity.compose.BackHandler
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
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.lhwdev.selfTestMacro.api.*
import com.vanpra.composematerialdialogs.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.max


@Stable
class SetupModel {
	var scaffoldState = ScaffoldState(DrawerState(DrawerValue.Closed), SnackbarHostState())
	
	var selectInstitute by mutableStateOf<WizardSecondModel?>(null)
	
	var userName by mutableStateOf("")
	var userBirth by mutableStateOf("")
	
	var usersToken by mutableStateOf<UsersToken?>(null)
	
	suspend inline fun showSnackbar(
		message: String,
		actionLabel: String? = null,
		duration: SnackbarDuration = SnackbarDuration.Short
	): SnackbarResult = scaffoldState.snackbarHostState.showSnackbar(message, actionLabel, duration)
	
	suspend fun onError(context: Context, message: String, throwable: Throwable) {
		context.onError(scaffoldState.snackbarHostState, message, throwable)
	}
}


class WizardIndexPreviewProvider : PreviewParameterProvider<Int> {
	override val values: Sequence<Int>
		get() = (0 until sPagesCount).asSequence()
	override val count: Int get() = values.count()
}

@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF, name = "setup wizard")
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
	val route = LocalRoute.current
	SetupWizardView(model)
}

private data class SetupWizard(
	val index: Int,
	val currentIndex: Int,
	val count: Int,
	val scrollTo: (index: Int) -> Unit,
)

private val SetupWizard.isCurrent get() = currentIndex == index

private const val sPagesCount = 4

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
	
	BackHandler(enabled = pageIndex != 0) {
		pageIndex--
	}
}

@Composable
private fun SetupWizardPage(model: SetupModel, wizard: SetupWizard) {
	when(wizard.index) {
		0 -> SetupWizardFirst(model, wizard)
		1 -> SetupWizardSecond(model.selectInstitute ?: return, model, wizard)
		2 -> SetupWizardThird(model, wizard)
		3 -> SetupWizardForth(model, wizard)
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
	onNext: () -> Unit = { wizard.next() },
	onBefore: () -> Unit = { wizard.before() },
	showNext: Boolean = true,
	content: @Composable () -> Unit,
) {
	Column(modifier = modifier) {
		Box(Modifier.weight(1f)) {
			content()
		}
		
		Row {
			if(wizard.index != 0) IconButton(onClick = onBefore) {
				Icon(
					painterResource(id = R.drawable.ic_arrow_left_24),
					contentDescription = "before"
				)
			}
			
			Spacer(Modifier.weight(100f))
			
			
			if(showNext) IconButton(
				onClick = {
					if(wizardFulfilled) onNext() else showNotFulfilledWarning()
				}
			) {
				val contentColor = LocalContentColor.current.copy(alpha = LocalContentAlpha.current)
				
				Icon(
					painterResource(
						id = if(wizard.index != wizard.count - 1) R.drawable.ic_arrow_right_24
						else R.drawable.ic_check_24
					),
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
	
	Surface(
		color = MaterialTheme.colors.primarySurface,
		modifier = Modifier.fillMaxSize()
	) {
		AutoSystemUi(
			enabled = wizard.isCurrent,
			onScreenMode = OnScreenSystemUiMode.Immersive(scrimColor = Color.Transparent)
		) { scrims ->
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
					modifier = Modifier.padding(12.dp),
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
						label = { Text("기관 유형") },
						modifier = Modifier.padding(8.dp)
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


@Composable
private fun SetupWizardSecond(
	model: WizardSecondModel,
	setupModel: SetupModel,
	wizard: SetupWizard,
) {
	val notFulfilled = remember { mutableStateOf(-1) }
	
	AutoSystemUi(
		enabled = wizard.isCurrent,
		statusBarMode = OnScreenSystemUiMode.Immersive(scrimColor = Color.Transparent)
	) { scrims ->
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
	abstract val instituteInfo: InstituteInfo?
	
	class School : WizardSecondModel() {
		var schoolLevel by mutableStateOf(0)
		var regionCode by mutableStateOf("")
		var schoolName by mutableStateOf("")
		override var instituteInfo by mutableStateOf<InstituteInfo?>(null)
		
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
	val context = LocalContext.current
	val route = LocalRoute.current
	val commonModifier = Modifier.fillMaxWidth().padding(8.dp)
	
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
			selfLog("#1. 학교 정보 찾기")
			getSchoolData(
				regionCode = model.regionCode,
				schoolLevelCode = "${model.schoolLevel}",
				name = model.schoolName
			).instituteList
		}.getOrElse { exception ->
			context.onError(snackbarHostState, "학교를 찾지 못했습니다.", exception)
			return@find
		}
		
		if(schools.isEmpty()) {
			snackbarHostState.showSnackbar("학교를 찾지 못했습니다.", "확인")
			return@find
		}
		
		if(schools.size > 1) showRouteUnit(route) { removeRoute ->
			MultipleInstituteDialog(
				instituteType = InstituteType.school,
				institutes = schools,
				onSelect = {
					removeRoute()
					if(it != null) {
						model.instituteInfo = it
						model.schoolName = it.name
						wizard.next()
					}
				}
			)
		} else {
			model.instituteInfo = schools[0]
			wizard.next()
		}
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
	when(model.selectInstitute) {
		is WizardSecondModel.School -> SetupWizardThirdSchool(model, wizard)
		null -> wizard.before()
	}
}


private suspend fun submitLogin(context: Context, model: SetupModel, route: Route) {
	val name = model.userName
	val birth = model.userBirth
	val selectInstitute = model.selectInstitute ?: return
	val institute = selectInstitute.instituteInfo ?: return
	
	val userId = try {
		selfLog("#2. 사용자 찾기")
		findUser(
			institute = institute,
			name = name, birthday = birth,
			loginType = selectInstitute.type.loginType
		)
	} catch(e: Throwable) {
		model.onError(context, "사용자를 찾을 수 없습니다", e)
		return
	}
	
	// user agreement
	if(!userId.agreement) {
		selfLog("약관 동의 필요!")
		showRouteUnit(route) { close ->
			MaterialDialog(onCloseRequest = close) {
				Title { Text("알림") }
				Content { Text("공식 자가진단 사이트나 앱에서 로그인한 후 약관에 동의해 주세요.") }
				Buttons {
					PositiveButton { Text("확인") }
				}
			}
		}
		return
	}
	
	// ask for password
	val password = showRoute<String?>(route) { close ->
		val (password, setPassword) = remember { mutableStateOf("") }
		
		MaterialDialog(onCloseRequest = { close(null) }) {
			Title { Text("비밀번호를 입력해주세요") }
			
			Input(true) {
				TextField(
					password, setPassword,
					modifier = Modifier.fillMaxWidth(),
					label = { Text("비밀번호") },
					isError = password.length > 4,
					keyboardOptions = KeyboardOptions(
						keyboardType = KeyboardType.NumberPassword,
						imeAction = ImeAction.Done
					),
					visualTransformation = PasswordVisualTransformation(),
					keyboardActions = KeyboardActions { close(password) }
				)
			}
			
			Buttons {
				PositiveButton(onClick = { close(password) }) { Text("확인") }
				NegativeButton { Text("취소") }
			}
		}
	} ?: return
	
	// validate & login with password
	val result = try {
		selfLog("#3. 비밀번호 확인")
		validatePassword(institute, userId, password)
	} catch(e: Throwable) {
		model.onError(context, "로그인에 실패하였습니다.", e)
		return
	}
	
	when(result) {
		is PasswordWrong -> showRouteUnit(route) { close ->
			MaterialDialog(onCloseRequest = close) {
				Title { Text("로그인 실패") }
				Content { Text(result.errorMessage ?: "로그인에 실패하였습니다. (에러코드 ${result.errorCode}") }
				Buttons {
					PositiveButton { Text("확인") }
				}
			}
		}
		is UsersToken -> {
			model.usersToken = result
		}
	}
}

@Composable
private fun SetupWizardThirdSchool(
	model: SetupModel,
	wizard: SetupWizard
): Unit = MaterialTheme(
	colors = MaterialTheme.colors.copy(
		primary = Color(0xFF008170),
		onPrimary = MaterialTheme.colors.onSurface
	)
) {
	val scope = rememberCoroutineScope()
	val context = LocalContext.current
	val route = LocalRoute.current
	
	val colors = MaterialTheme.colors
	val commonModifier = Modifier.fillMaxWidth().padding(8.dp)
	
	var notFulfilled by remember { mutableStateOf(-1) }
	
	fun submit() = scope.launch {
		submitLogin(context, model, route)
		if(model.usersToken != null) wizard.next()
	}
	
	
	Surface(color = if(colors.isLight) Color(0xffa7ffeb) else colors.surface) {
		AutoSystemUi(
			enabled = wizard.isCurrent
		) { scrims ->
			scrims.statusBar()
			
			SetupWizardCommon(
				wizard = wizard,
				onNext = {
					if(model.usersToken == null) submit()
					else wizard.next()
				},
				wizardFulfilled = model.userName.isNotBlank() &&
					model.userBirth.isNotBlank() && model.userBirth.length <= 6,
				showNotFulfilledWarning = {
					notFulfilled = when {
						model.userName.isBlank() -> 0
						model.userBirth.isBlank() || model.userBirth.length > 6 -> 1
						else -> -1
					}
				},
				modifier = Modifier.weight(1f)
			) {
				Column(modifier = Modifier.padding(12.dp)) {
					val focusManager = LocalFocusManager.current
					
					// header
					Column(
						modifier = Modifier.padding(28.dp).fillMaxWidth(),
						horizontalAlignment = Alignment.CenterHorizontally
					) {
						Icon(
							painterResource(R.drawable.ic_school_24),
							contentDescription = null, // not that important
							tint = Color.Black,
							modifier = Modifier.padding(20.dp).size(72.dp)
						)
						
						Text("학생", style = MaterialTheme.typography.h4)
					}
					
					OutlinedTextField(
						model.userName,
						onValueChange = {
							model.userName = it
							notFulfilled = -1
						},
						label = { Text("사용자 이름") },
						keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
						keyboardActions = KeyboardActions { focusManager.moveFocus(FocusDirection.Next) },
						isError = notFulfilled == 0,
						singleLine = true,
						modifier = commonModifier
					)
					
					val isBirthWrong = model.userBirth.length > 6
					OutlinedTextField(
						model.userBirth,
						onValueChange = {
							model.userBirth = it
							notFulfilled = -1
						},
						label = { Text("생년월일(6글자)") },
						placeholder = {
							@Suppress("SpellCheckingInspection")
							Text("YYMMDD")
						},
						keyboardOptions = KeyboardOptions(
							imeAction = ImeAction.Go,
							keyboardType = KeyboardType.Number
						),
						isError = isBirthWrong || notFulfilled == 1,
						keyboardActions = KeyboardActions { submit() },
						singleLine = true,
						modifier = commonModifier
					)
				}
			}
			
			scrims.navigationBar()
		}
	}
}


@Composable
private fun SetupWizardForth(model: SetupModel, wizard: SetupWizard) {
	var enabled by remember { mutableStateOf(BooleanArray(0)) }
	
	val institute = model.selectInstitute?.instituteInfo ?: return
	val usersToken = model.usersToken ?: return
	
	val usersState by lazyState(
		listOf(model.selectInstitute?.instituteInfo, model.usersToken)
	) main@{
		try {
			val group = getUserGroup(institute, usersToken)
			enabled = BooleanArray(group.size)
			group
		} catch(e: Throwable) {
			wizard.before()
			emptyList()
		}
	}
	
	
	AutoSystemUi(
		enabled = wizard.isCurrent,
		onScreenMode = OnScreenSystemUiMode.Opaque(Color.Transparent)
	) {
		SetupWizardCommon(
			wizard,
			wizardFulfilled = true,
			showNotFulfilledWarning = {},
			onNext = { } // complete
		) {
			Column(horizontalAlignment = Alignment.CenterHorizontally) {
				Box(Modifier.padding(32.dp, 72.dp)) {
					Text("사용자 선택", style = MaterialTheme.typography.h3)
				}
				
				val users = usersState
				
				when {
					users == null ->
						Text("잠시만 기다려주세요", style = MaterialTheme.typography.body1)
					users.isEmpty() ->
						Text("오류가 발생했습니다", style = MaterialTheme.typography.body1)
					else -> SetupSelectUsers(
						institute = institute,
						users = users,
						enabled = enabled,
						setEnabled = { index, isEnabled -> enabled[index] = isEnabled }
					)
				}
			}
		}
	}
}


@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun SetupSelectUsers(
	institute: InstituteInfo,
	users: List<User>,
	enabled: BooleanArray,
	setEnabled: (index: Int, isEnabled: Boolean) -> Unit
) {
	for((index, user) in users.withIndex()) { // no need for key: `users` never changes
		// val detailedInfo by lazyState {
		// 	try {
		// 		getUserInfo(institute, user)
		// 	} catch(e: Throwable) {
		// 		null
		// 	}
		// }
		
		ListItem(
			icon = {
				Checkbox(
					checked = enabled[index],
					onCheckedChange = { setEnabled(index, it) }
				)
			},
			text = { Text(user.name) },
			// secondaryText = { Text(detailedInfo?.instituteName ?: "?") }
		)
	}
}


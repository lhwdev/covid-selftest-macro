package com.lhwdev.selfTestMacro

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.lhwdev.selfTestMacro.api.*
import com.vanpra.composematerialdialogs.*
import kotlinx.coroutines.*
import kotlin.math.max


@Stable
class SetupModel {
	var scaffoldState = ScaffoldState(DrawerState(DrawerValue.Closed), SnackbarHostState())
	
	var selectInstitute by mutableStateOf<WizardSecondModel?>(null)
	
	var userName by mutableStateOf("")
	var userBirth by mutableStateOf("")
	
	var userIdentifier by mutableStateOf<UserIdentifier?>(null)
	var usersToken by mutableStateOf<UsersToken?>(null)
	
	var userList by mutableStateOf<List<User>?>(null)
	var userInfoList by mutableStateOf<List<UserInfo>?>(null)
	
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


private const val sPreloadPages = 0

@Composable
fun WizardPager(
	pageIndex: Int,
	pagesCount: Int,
	content: @Composable (index: Int) -> Unit,
) {
	var maxLoads by remember { mutableStateOf(1) }
	
	BoxWithConstraints(Modifier.clipToBounds()) {
		val scope = rememberCoroutineScope()
		val width = maxWidth
		val widthPx = with(LocalDensity.current) { width.roundToPx() }
		
		var targetPage by remember(pagesCount) { mutableStateOf(pageIndex) }
		val scrollState = remember(pagesCount) { ScrollState(pageIndex) }
		
		fun scrollTo(target: Int) {
			if(target !in 0 until pagesCount) return
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
	content: @Composable () -> Unit
) {
	Column(modifier) {
		Box(Modifier.weight(1f, fill = true)) {
			content()
		}
		
		Row {
			IconButton(
				onClick = onBefore,
				modifier = if(wizard.index == 0) Modifier.alpha(0f) else Modifier
			) {
				Icon(
					painterResource(id = R.drawable.ic_arrow_left_24),
					contentDescription = "앞으로"
				)
			}
			
			Spacer(Modifier.weight(100f))
			
			
			if(showNext) TextIconButton(
				onClick = {
					if(wizardFulfilled) onNext() else showNotFulfilledWarning()
				}
			) {
				val contentColor = DefaultContentColor
				
				Text(if(wizard.index != wizard.count - 1) "다음" else "완료 ")
				
				Icon(
					painterResource(
						id = if(wizard.index != wizard.count - 1) R.drawable.ic_arrow_right_24
						else R.drawable.ic_check_24
					),
					contentDescription = "뒤로",
					tint = if(wizardFulfilled) contentColor else contentColor.copy(alpha = 0.9f)
				)
			}
		}
	}
}


// Setup wizards

/////////////////////////////////////// #1 /////////////////////////////////////////////////////////

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
			scrims.statusBar()
			
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
									InstituteType.school -> WizardSecondModel.School()
									InstituteType.university -> TODO()
									InstituteType.academy -> TODO()
									InstituteType.office -> TODO()
								}
								val previousSelect = model.selectInstitute
								if(previousSelect == null || previousSelect::class != newSelect::class) {
									model.selectInstitute = newSelect
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
		navigationBarMode = OnScreenSystemUiMode.Immersive(scrimColor = Color.Transparent)
	) { scrims ->
		Scaffold(
			topBar = {
				TopAppBar(
					title = { Text("${model.type.displayName} 정보 입력") },
					backgroundColor = MaterialTheme.colors.surface,
					statusBarScrim = { scrims.statusBar() }
				)
			},
			modifier = Modifier.weight(1f)
		) { paddingValues ->
			Column(Modifier.padding(paddingValues)) {
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
				modifier = Modifier.clickable { onSelect(institute) }
			) {
				Text(institute.name, style = MaterialTheme.typography.body1)
			}
		}
		
		Buttons {
			NegativeButton { Text("취소") }
		}
	}
}


/////////////////////////////////////// #2 /////////////////////////////////////////////////////////

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
				else -> -1
			}
		
		override val type: InstituteType
			get() = InstituteType.school
	}
}

@Composable
private fun ColumnScope.SetupWizardSecondSchool(
	model: WizardSecondModel.School,
	setupModel: SetupModel,
	notFulfilled: MutableState<Int>,
	wizard: SetupWizard
) {
	val scope = rememberCoroutineScope()
	val context = LocalContext.current
	val route = LocalRoute.current
	
	var complete by remember { mutableStateOf(false) }
	
	fun findSchool() = scope.launch find@{
		fun selectSchool(info: InstituteInfo) {
			model.instituteInfo = info
			model.schoolName = info.name
			complete = true
			wizard.next()
		}
		
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
					if(it != null) selectSchool(it)
				}
			)
		} else selectSchool(schools[0])
	}
	
	
	SetupWizardCommon(
		wizard,
		onNext = {
			if(model.instituteInfo != null && complete) wizard.next()
			else findSchool()
		},
		wizardFulfilled = model.notFulfilledIndex == -1,
		showNotFulfilledWarning = { notFulfilled.value = model.notFulfilledIndex },
		modifier = Modifier.weight(1f)
	) {
		Column(modifier = Modifier.padding(12.dp).verticalScroll(rememberScrollState())) {
			val commonModifier = Modifier.fillMaxWidth().padding(8.dp)
			
			DropdownPicker(
				dropdown = { onDismiss ->
					for((code, name) in sSchoolLevels.entries) DropdownMenuItem(onClick = {
						model.schoolLevel = code
						notFulfilled.value = -1
						complete = false
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
						complete = false
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
					IconButton(onClick = { findSchool() }) {
						Icon(
							painterResource(if(complete) R.drawable.ic_check_24 else R.drawable.ic_search_24),
							contentDescription = "검색"
						)
					}
				},
				modifier = commonModifier
			)
		}
	}
}


/////////////////////////////////////// #3 /////////////////////////////////////////////////////////

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
	
	model.userIdentifier = userId
	
	// ask for password
	val password = showRoute<String?>(route) { close ->
		val (password, setPassword) = remember { mutableStateOf("") }
		
		MaterialDialog(onCloseRequest = { close(null) }) {
			Title { Text("비밀번호를 입력해주세요") }
			
			Input(focusOnShow = true) {
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
	}
	println("password: $password")
	password ?: return
	
	// validate & login with password
	val result = try {
		selfLog("#3. 비밀번호 확인")
		validatePassword(institute, userId, password)
	} catch(e: Throwable) {
		model.onError(context, "로그인에 실패하였습니다.", e)
		return
	}
	
	selfLog("#4. 비밀번호 결과")
	
	when(result) {
		is PasswordWrong -> showRouteUnit(route) { close ->
			MaterialDialog(onCloseRequest = close) {
				Title { Text("로그인 실패") }
				Content {
					Text(
						result.errorMessage ?: "로그인에 실패하였습니다. (에러코드 ${result.errorCode}"
					)
				}
				Buttons {
					PositiveButton { Text("확인") }
				}
			}
		}
		is UsersToken -> {
			try {
				val userList = getUserGroup(institute, result)
				val userInfoList = userList.map { getUserInfo(institute, it) }
				
				model.usersToken = result
				model.userList = userList
				model.userInfoList = userInfoList
				
			} catch(e: Throwable) {
				model.onError(context, "사용자 정보를 불러오지 못했습니다.", e)
			}
			
		}
	}
}

@OptIn(ExperimentalComposeUiApi::class)
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
	var complete by remember { mutableStateOf(false) }
	
	fun submit() = scope.launch {
		submitLogin(context, model, route)
		if(model.userList != null) {
			complete = true
			wizard.next()
		}
	}
	
	val nameRef = remember { FocusRequester() }
	
	
	EmptyRestartable {
		val requestShowCondition = isImeVisible
		if(wizard.isCurrent) DisposableEffect(Unit) {
			if(requestShowCondition) nameRef.requestFocus()
			onDispose {}
		}
	}
	
	
	Surface(color = if(colors.isLight) Color(0xffa7ffeb) else colors.surface) {
		AutoSystemUi(
			enabled = wizard.isCurrent,
			onScreenMode = OnScreenSystemUiMode.Immersive(scrimColor = Color.Transparent)
		) { scrims ->
			scrims.statusBar()
			
			SetupWizardCommon(
				wizard = wizard,
				onNext = {
					if(complete) wizard.next()
					else submit()
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
				Column(modifier = Modifier.padding(12.dp).verticalScroll(rememberScrollState())) {
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
							modifier = Modifier.padding(12.dp).size(72.dp)
						)
						
						Text(
							"학생 정보 입력",
							style = MaterialTheme.typography.h4,
							modifier = Modifier.padding(8.dp)
						)
						
						Text(
							model.selectInstitute?.instituteInfo?.name ?: "학교",
							style = MaterialTheme.typography.h6,
							color = LocalContentColor.current.copy(alpha = ContentAlpha.medium)
						)
					}
					
					OutlinedTextField(
						model.userName,
						onValueChange = {
							model.userName = it
							notFulfilled = -1
							complete = false
						},
						label = { Text("사용자 이름") },
						keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
						keyboardActions = KeyboardActions { focusManager.moveFocus(FocusDirection.Down) },
						isError = notFulfilled == 0,
						singleLine = true,
						modifier = commonModifier.focusRequester(nameRef)
					)
					
					val isBirthWrong = model.userBirth.length > 6
					OutlinedTextField(
						model.userBirth,
						onValueChange = {
							model.userBirth = it
							notFulfilled = -1
							complete = false
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


/////////////////////////////////////// #4 /////////////////////////////////////////////////////////

@Composable
private fun SetupWizardForth(model: SetupModel, wizard: SetupWizard) {
	val route = LocalRoute.current
	val pref = LocalPreference.current
	
	// wow
	val selectInstitute = model.selectInstitute ?: return
	val institute = selectInstitute.instituteInfo ?: return
	val userIdentifier = model.userIdentifier ?: return
	val userList = model.userList ?: return
	val userInfoList = model.userInfoList ?: return
	
	val enabled = remember(userList) {
		val list = mutableStateListOf<Boolean>()
		list += List(userList.size) { true }
		list
	}
	
	Surface(color = MaterialTheme.colors.surface) {
		AutoSystemUi(
			enabled = wizard.isCurrent,
			onScreenMode = OnScreenSystemUiMode.Opaque(Color.Transparent)
		) {
			SetupWizardCommon(
				wizard,
				wizardFulfilled = true,
				showNotFulfilledWarning = {},
				onNext = next@{ // complete
					val realUsers = userList.filterIndexed { index, _ -> enabled[index] }
					val realUserInfo = userInfoList.filterIndexed { index, _ -> enabled[index] }
					
					val userGroups = pref.db.userGroups
					val userGroupsId = userGroups.maxId + 1
					
					val previousUsers = pref.db.users
					var usersId = previousUsers.maxId
					
					val dbUsersList = realUsers.mapIndexed { index, user ->
						val info = realUserInfo[index]
						DbUser(
							++usersId,
							user,
							info.instituteName,
							info.instituteType,
							userGroupsId
						)
					}
					val dbUsers = dbUsersList.associateBy { it.id }
					
					val userGroup = DbUserGroup(
						userGroupsId,
						dbUsersList.map { it.id },
						userIdentifier,
						selectInstitute.type,
						institute
					)
					val dbUserGroups = userGroups.copy(
						maxId = userGroupsId,
						groups = userGroups.groups.added(userGroupsId, userGroup)
					)
					
					pref.db.userGroups = dbUserGroups
					pref.db.users = previousUsers.copy(
						maxId = usersId,
						users = previousUsers.users + dbUsers
					)
					
					// go!
					pref.firstState = 1
					route[0] = { Main() }
				}
			) {
				Column(
					horizontalAlignment = Alignment.CenterHorizontally,
					modifier = Modifier.padding(vertical = 12.dp).fillMaxSize()
				) {
					Box(Modifier.padding(32.dp, 72.dp)) {
						Text("사용자 선택", style = MaterialTheme.typography.h3)
					}
					
					SetupSelectUsers(
						userList = userList, userInfoList = userInfoList,
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
	userList: List<User>,
	userInfoList: List<UserInfo>,
	enabled: List<Boolean>,
	setEnabled: (index: Int, isEnabled: Boolean) -> Unit
) {
	for((index, user) in userList.withIndex()) { // no need for key: `users` never changes
		val info = userInfoList[index]
		
		ListItem(
			icon = {
				Checkbox(checked = enabled[index], onCheckedChange = null)
			},
			text = { Text(user.name) },
			secondaryText = { Text(info.instituteName) },
			modifier = Modifier
				// .clearAndSetSemantics {
				// 	text = AnnotatedString("${info.instituteName} ${user.name}")
				// 	toggleableState = ToggleableState(enabled[index])
				// 	role = Role.Checkbox
				// }
				.clickable { // this adds semantics onClick
					setEnabled(
						index,
						!enabled[index]
					)
				}
				.padding(horizontal = 12.dp)
		)
	}
}


//////////////////////////////////// Utilities /////////////////////////////////////////////////////



@Composable
fun TextIconButton(
	onClick: () -> Unit,
	modifier: Modifier = Modifier,
	enabled: Boolean = true,
	interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
	content: @Composable () -> Unit
) {
	Row(
		modifier = modifier
			.clip(RoundedCornerShape(percent = 100))
			.clickable(
				onClick = onClick,
				enabled = enabled,
				role = Role.Button,
				interactionSource = interactionSource,
				indication = rememberRipple(bounded = false)
			)
			.then(IconButtonSizeModifier)
			.padding(12.dp),
		verticalAlignment = Alignment.CenterVertically
	) {
		val contentAlpha = if(enabled) LocalContentAlpha.current else ContentAlpha.disabled
		CompositionLocalProvider(LocalContentAlpha provides contentAlpha, content = content)
	}
}


// per-component definitions of this size.
// Diameter of the IconButton, to allow for correct minimum touch target size for accessibility
private val IconButtonSizeModifier = Modifier.height(48.dp)




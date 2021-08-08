package com.lhwdev.selfTestMacro.ui

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.annotation.VisibleForTesting
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.material.DropdownMenuItem
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import com.lhwdev.selfTestMacro.*
import com.lhwdev.selfTestMacro.R
import com.lhwdev.selfTestMacro.api.*
import com.lhwdev.selfTestMacro.repository.MainRepositoryImpl
import com.vanpra.composematerialdialogs.*
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.max


@Immutable
data class SetupParameters(
	val targetTestGroup: DbTestGroup? = null,
	val endRoute: (() -> Unit)? = null
) {
	companion object {
		val Default = SetupParameters()
	}
}


@Stable
internal class SetupModel {
	val session = Session()
	
	var scaffoldState = ScaffoldState(DrawerState(DrawerValue.Closed), SnackbarHostState())
	
	var instituteInfo by mutableStateOf<InstitutionInfoModel?>(null)
	
	var userName by mutableStateOf("")
	var userBirth by mutableStateOf("")
	
	val userList = mutableStateListOf<WizardUser>()
	
	suspend inline fun showSnackbar(
		message: String,
		actionLabel: String? = null,
		duration: SnackbarDuration = SnackbarDuration.Short
	): SnackbarResult = scaffoldState.snackbarHostState.showSnackbar(message, actionLabel, duration)
	
	suspend fun onError(context: Context, message: String, throwable: Throwable) {
		context.onError(scaffoldState.snackbarHostState, message, throwable)
	}
}

@Immutable
internal data class WizardUser(val user: User, val info: UserInfo, val master: MasterUser)

@Immutable
internal data class MasterUser(
	val identifier: UsersIdentifier,
	val instituteInfo: InstituteInfo,
	val instituteType: InstituteType
)


@Composable
fun Setup(parameters: SetupParameters = SetupParameters.Default) {
	Surface(color = MaterialTheme.colors.surface) {
		val model = remember { SetupModel() }
		SetupWizardView(model, parameters)
	}
}

@Immutable
internal data class SetupWizard(
	val index: Int,
	val currentIndex: Int,
	val count: Int,
	val scrollTo: (index: Int) -> Unit
)

private val SetupWizard.isCurrent get() = currentIndex == index

internal const val sSetupPagesCount = 4

@Composable
private fun SetupWizardView(model: SetupModel, parameters: SetupParameters) {
	sDebugFetch = true
	var pageIndex by remember { mutableStateOf(0) }
	
	AutoScaffold(
		scaffoldState = model.scaffoldState
	) {
		WizardPager(pageIndex = pageIndex) { index ->
			val wizard = SetupWizard(index, pageIndex, sSetupPagesCount) {
				pageIndex = it
			}
			SetupWizardPage(model, parameters, wizard)
		}
	}
	
	BackHandler(enabled = pageIndex != 0) {
		pageIndex--
	}
}

@Composable
@VisibleForTesting
internal fun SetupWizardPage(model: SetupModel, parameters: SetupParameters, wizard: SetupWizard) {
	when(wizard.index) {
		0 -> WizardSelectType(model, wizard)
		1 -> WizardInstitutionInfo(model.instituteInfo ?: return, model, wizard)
		2 -> WizardUserInfo(model, wizard)
		3 -> WizardSelectUsers(model, parameters, wizard)
		else -> error("unknown page")
	}
}


private const val sPreloadPages = 0

@Composable
private fun WizardPager(
	pageIndex: Int,
	content: @Composable (index: Int) -> Unit
) {
	var maxLoads by remember { mutableStateOf(1) }
	
	BoxWithConstraints(Modifier.clipToBounds()) {
		val scope = rememberCoroutineScope()
		val width = maxWidth
		val widthPx = with(LocalDensity.current) { width.roundToPx() }
		
		var targetPage by remember { mutableStateOf(pageIndex) }
		val scrollState = remember { ScrollState(pageIndex) }
		
		fun scrollTo(target: Int) {
			if(target !in 0 until sSetupPagesCount) return
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
			for(index in 0 until sSetupPagesCount) {
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
private fun WizardCommon(
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
			
			
			if(showNext) RoundButton(
				onClick = {
					if(wizardFulfilled) onNext() else showNotFulfilledWarning()
				},
				trailingIcon = {
					val contentColor = DefaultContentColor
					Icon(
						painterResource(
							id = if(wizard.index != wizard.count - 1) R.drawable.ic_arrow_right_24
							else R.drawable.ic_check_24
						),
						contentDescription = "뒤로",
						tint = if(wizardFulfilled) contentColor else contentColor.copy(alpha = 0.9f)
					)
				}
			) {
				val pref = LocalPreference.current
				
				val text = when {
					wizard.index != wizard.count - 1 -> "다음"
					pref.isFirstTime -> "완료"
					else -> "추가"
				}
				
				Text(text)
			}
		}
	}
}


// Setup wizards

/////////////////////////////////////// #1 /////////////////////////////////////////////////////////

@Composable
private fun WizardSelectType(model: SetupModel, wizard: SetupWizard) {
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
									InstituteType.school -> InstitutionInfoModel.School()
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


@Composable
private fun WizardInstitutionInfo(
	model: InstitutionInfoModel,
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
					is InstitutionInfoModel.School -> WizardSchoolInfo(
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
private fun FloatingMaterialDialogScope.MultipleInstituteDialog(
	instituteType: InstituteType,
	institutes: List<InstituteInfo>,
	onSelect: (InstituteInfo?) -> Unit,
) {
	Title { Text("${instituteType.displayName} 선택") }
	
	Column {
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


/////////////////////////////////////// #2 /////////////////////////////////////////////////////////

@Suppress("CanSealedSubClassBeObject") // model: no comparison needed
@Stable
internal sealed class InstitutionInfoModel {
	abstract val type: InstituteType
	abstract val notFulfilledIndex: Int
	abstract val institute: InstituteInfo?
	
	class School : InstitutionInfoModel() {
		var schoolLevel by mutableStateOf(0)
		var regionCode by mutableStateOf("")
		var schoolName by mutableStateOf("")
		override var institute by mutableStateOf<InstituteInfo?>(null)
		
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
private fun ColumnScope.WizardSchoolInfo(
	model: InstitutionInfoModel.School,
	setupModel: SetupModel,
	notFulfilled: MutableState<Int>,
	wizard: SetupWizard
) {
	val scope = rememberCoroutineScope()
	val context = LocalContext.current
	val navigator = LocalNavigator
	
	var complete by remember { mutableStateOf(false) }
	
	fun findSchool() = scope.launch find@{
		fun selectSchool(info: InstituteInfo) {
			model.institute = info
			model.schoolName = info.name
			complete = true
			wizard.next()
		}
		
		val snackbarHostState = setupModel.scaffoldState.snackbarHostState
		
		val schools = runCatching {
			selfLog("#1. 학교 정보 찾기")
			setupModel.session.getSchoolData(
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
		
		if(schools.size > 1) navigator.showDialogUnit { removeRoute ->
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
private fun WizardUserInfo(model: SetupModel, wizard: SetupWizard) {
	when(model.instituteInfo) {
		is InstitutionInfoModel.School -> WizardStudentInfo(model, wizard)
		null -> wizard.before()
	}
}


private suspend fun submitLogin(
	context: Context,
	model: SetupModel,
	navigator: Navigator
): Boolean {
	val name = model.userName
	val birth = model.userBirth
	val instituteInfo = model.instituteInfo ?: return false
	val institute = instituteInfo.institute ?: return false
	
	val userId = try {
		selfLog("#2. 사용자 찾기")
		model.session.findUser(
			institute = institute,
			name = name, birthday = birth,
			loginType = instituteInfo.type.loginType
		)
	} catch(e: Throwable) {
		model.onError(context, "사용자를 찾을 수 없습니다", e)
		return false
	}
	
	// user agreement
	if(!userId.agreement) {
		selfLog("약관 동의 필요!")
		navigator.showDialogUnit {
			Title { Text("알림") }
			Content { Text("공식 자가진단 사이트나 앱에서 로그인한 후 약관에 동의해 주세요.") }
			Buttons {
				PositiveButton { Text("확인") }
			}
		}
		return false
	}
	
	// ask for password
	val password = navigator.showDialog<String?> { close ->
		val (password, setPassword) = remember { mutableStateOf("") }
		
		Title { Text("비밀번호를 입력해주세요") }
		
		Input(focusOnShow = true) {
			TextField(
				password, setPassword,
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
	password ?: return false
	
	// validate & login with password
	val result = try {
		selfLog("#3. 비밀번호 확인")
		model.session.validatePassword(institute, userId, password)
	} catch(e: Throwable) {
		model.onError(context, "로그인에 실패하였습니다.", e)
		return false
	}
	
	selfLog("#4. 비밀번호 결과")
	
	when(result) {
		is PasswordWrong -> navigator.showDialogUnit {
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
		is UsersToken -> {
			try {
				val userList = model.session.getUserGroup(institute, result)
				val list = userList.map {
					WizardUser(
						user = it,
						info = model.session.getUserInfo(institute, it),
						master = MasterUser(
							identifier = userId,
							instituteInfo = institute,
							instituteType = instituteInfo.type
						)
					)
				}
				
				val target = model.userList
				
				for(user in list) { // I don't know if this is indeed needed
					val index = target.indexOfFirst { it.user.userCode == user.user.userCode }
					if(index == -1) target += user
					else target[index] = user
				}
				
				return true
			} catch(e: Throwable) {
				model.onError(context, "사용자 정보를 불러오지 못했습니다.", e)
			}
		}
	}
	return false
}

@Composable
private fun WizardStudentInfo(
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
	val navigator = LocalNavigator
	
	val colors = MaterialTheme.colors
	val commonModifier = Modifier
		.fillMaxWidth()
		.padding(8.dp)
	
	var notFulfilled by remember { mutableStateOf(-1) }
	var complete by remember { mutableStateOf(false) }
	
	// this is for returning from 'add new user from same institute'
	if(model.userName.isBlank()) complete = false
	
	fun submit() = scope.launch {
		val success = submitLogin(context, model, navigator)
		if(success) {
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
			
			WizardCommon(
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
				Column(
					modifier = Modifier
						.padding(12.dp)
						.verticalScroll(rememberScrollState())
				) {
					val focusManager = LocalFocusManager.current
					
					// header
					Column(
						modifier = Modifier
							.padding(28.dp)
							.fillMaxWidth(),
						horizontalAlignment = Alignment.CenterHorizontally
					) {
						Icon(
							painterResource(R.drawable.ic_school_24),
							contentDescription = null, // not that important
							tint = Color.Black,
							modifier = Modifier
								.padding(12.dp)
								.size(72.dp)
						)
						
						Text(
							"학생 정보 입력",
							style = MaterialTheme.typography.h4,
							modifier = Modifier.padding(8.dp)
						)
						
						Text(
							model.instituteInfo?.institute?.name ?: "학교",
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
private fun WizardSelectUsers(model: SetupModel, parameters: SetupParameters, wizard: SetupWizard) {
	val navigator = LocalNavigator
	val pref = LocalPreference.current
	val scope = rememberCoroutineScope()
	
	val userList = model.userList
	
	if(userList.isEmpty()) {
		wizard.before()
		return
	}
	
	val enabled = remember {
		val list = mutableStateListOf<Boolean>()
		list += List(userList.size) { true }
		list
	}
	
	val enabledSizeDelta = userList.size - enabled.size
	if(enabledSizeDelta != 0) { // not necessarily correct, but doesn't need to be correct
		if(enabledSizeDelta > 0) repeat(enabledSizeDelta) { enabled += true }
		else repeat(-enabledSizeDelta) { enabled.removeLast() }
	}
	
	var isAllGrouped by remember { mutableStateOf(false) }
	
	Surface(color = MaterialTheme.colors.surface) {
		AutoSystemUi(
			enabled = wizard.isCurrent,
			onScreenMode = OnScreenSystemUiMode.Opaque(Color.Transparent)
		) {
			WizardCommon(
				wizard,
				wizardFulfilled = if(pref.isFirstTime) enabled.isNotEmpty() else true,
				showNotFulfilledWarning = {
					scope.launch {
						model.showSnackbar("사용자를 최소 한 명 선택해주세요")
					}
				},
				onNext = next@{ // complete
					val realUsers = userList.filterIndexed { index, _ -> enabled[index] }
					
					// group by 'master user'
					val usersMap = realUsers.groupBy { it.master }
					
					val previousUserGroups = pref.db.userGroups
					var userGroupId = previousUserGroups.maxId
					val newUserGroups = ArrayList<DbUserGroup>(usersMap.size)
					
					val previousUsers = pref.db.users
					var userId = previousUsers.maxId
					val newUsers = ArrayList<DbUser>(realUsers.size)
					
					for((master, users) in usersMap) {
						val thisGroupId = ++userGroupId
						
						// user
						val dbUsers = users.map { user ->
							DbUser(
								id = ++userId,
								user = user.user,
								instituteName = user.info.instituteName,
								instituteType = user.info.instituteType,
								userGroupId = thisGroupId
							)
						}
						newUsers += dbUsers
						
						// userGroup
						val dbGroup = DbUserGroup(
							id = thisGroupId,
							userIds = dbUsers.map { it.id },
							usersIdentifier = master.identifier,
							instituteType = master.instituteType,
							institute = master.instituteInfo
						)
						newUserGroups += dbGroup
					}
					
					
					pref.db.users = previousUsers.copy(
						users = previousUsers.users + newUsers.associateBy { it.id },
						maxId = userId
					)
					
					pref.db.userGroups = previousUserGroups.copy(
						groups = previousUserGroups.groups + newUserGroups.associateBy { it.id },
						maxId = userGroupId
					)
					
					val previousTestGroups = pref.db.testGroups
					val targetTestGroup = parameters.targetTestGroup
					val hadAnyTestGroups = previousTestGroups.groups.isNotEmpty()
					
					if(targetTestGroup == null) {
						// add new group
						var maxGroupGeneratedNameIndex =
							previousTestGroups.maxGroupGeneratedNameIndex
						
						// testGroup
						val testTargets = if(isAllGrouped) listOf(
							DbTestTarget.Group(
								"그룹 ${++maxGroupGeneratedNameIndex}",
								newUsers.map { it.id }
							)
						) else newUsers.map {
							DbTestTarget.Single(it.id)
						}
						
						val ids = previousTestGroups.ids
						val newTestGroups = testTargets.map { target ->
							val id = ids.nextTestGroupId()
							ids += id
							DbTestGroup(id = id, target = target)
						}
						
						pref.db.testGroups = previousTestGroups.copy(
							groups = previousTestGroups.groups + newTestGroups,
							maxGroupGeneratedNameIndex = maxGroupGeneratedNameIndex
						)
					} else {
						// add to existing group
						val testGroups = pref.db.testGroups.groups.toMutableList()
						
						val targetIndex = testGroups.indexOf(targetTestGroup)
						if(targetIndex == -1) error("what the..?") // what the error
						
						val testTarget = targetTestGroup.target as DbTestTarget.Group
						val added = testTarget.copy(
							userIds = testTarget.userIds + newUsers.map { it.id }
						)
						
						testGroups[targetIndex] = targetTestGroup.copy(target = added)
						pref.db.testGroups = pref.db.testGroups.copy(groups = testGroups)
					}
					
					when {
						parameters.endRoute != null -> parameters.endRoute.invoke()
						
						navigator.routes.size == 1 -> {
							pref.isFirstTime = false
							navigator.clearRoute()
							val repository = MainRepositoryImpl(pref, model.session)
							navigator.pushRoute { Main(repository) }
						}
						else -> {
							navigator.popRoute()
						}
					}
				}
			) {
				Column(
					horizontalAlignment = Alignment.CenterHorizontally,
					modifier = Modifier
						.padding(vertical = 12.dp)
						.fillMaxSize()
				) {
					Box(Modifier.padding(32.dp, 72.dp)) {
						Text("사용자 선택", style = MaterialTheme.typography.h3)
					}
					
					WizardSelectUsersContent(
						model = model, parameters = parameters, wizard = wizard,
						userList = userList,
						enabled = enabled,
						setEnabled = { index, isEnabled -> enabled[index] = isEnabled },
						isAllGrouped = isAllGrouped,
						setIsAllGrouped = { isAllGrouped = it }
					)
				}
			}
		}
	}
}


@Composable
private fun ColumnScope.WizardSelectUsersContent(
	model: SetupModel,
	parameters: SetupParameters,
	wizard: SetupWizard,
	userList: List<WizardUser>,
	enabled: List<Boolean>,
	setEnabled: (index: Int, isEnabled: Boolean) -> Unit,
	isAllGrouped: Boolean,
	setIsAllGrouped: (Boolean) -> Unit
) {
	for((index, user) in userList.withIndex()) key(user) {
		ListItem(
			icon = {
				Checkbox(checked = enabled[index], onCheckedChange = null)
			},
			text = { Text(user.user.name) },
			secondaryText = { Text(user.info.instituteName) },
			modifier = Modifier
				.clearAndSetSemantics {
					text = AnnotatedString("${user.info.instituteName} ${user.user.name}")
					toggleableState = ToggleableState(enabled[index])
					role = Role.Checkbox
				}
				.clickable { // this adds semantics onClick
					setEnabled(index, !enabled[index])
				}
				.padding(horizontal = 12.dp)
		)
	}
	
	val addSameInstituteText = when(model.instituteInfo?.type) {
		null -> null
		InstituteType.school -> "같은 학교의 학생 추가"
		InstituteType.university -> "같은 학교의 수강생 추가"
		InstituteType.academy -> "같은 학원의 수강생 추가"
		InstituteType.office -> "같은 회사의 직원 추가"
	}
	
	if(addSameInstituteText != null) ListItem(
		icon = { Icon(painterResource(R.drawable.ic_add_24), contentDescription = null) },
		text = { Text(addSameInstituteText) },
		modifier = Modifier
			.clickable {
				// reset fields
				model.userName = ""
				model.userBirth = ""
				
				wizard.scrollTo(2) // #3: user info
			}
			.padding(horizontal = 12.dp)
	)
	
	Spacer(Modifier.weight(1f))
	
	if(userList.size > 1 && parameters.targetTestGroup == null) Box(
		contentAlignment = Alignment.Center,
		modifier = Modifier.fillMaxWidth()
	) {
		Row(verticalAlignment = Alignment.CenterVertically) {
			val interactionSource = remember { MutableInteractionSource() }
			
			Checkbox(
				checked = isAllGrouped,
				onCheckedChange = setIsAllGrouped,
				interactionSource = interactionSource
			)
			
			Text(
				"한 그룹으로 묶기",
				style = MaterialTheme.typography.body1,
				modifier = Modifier
					.clickable(
						interactionSource = interactionSource,
						indication = null,
						onClick = { setIsAllGrouped(!isAllGrouped) }
					)
					.padding(8.dp)
			)
		}
	}
}

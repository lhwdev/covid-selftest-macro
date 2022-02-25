package com.lhwdev.selfTestMacro.ui.pages.setup

import android.content.Context
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.google.accompanist.insets.LocalWindowInsets
import com.lhwdev.selfTestMacro.R
import com.lhwdev.selfTestMacro.api.DangerousHcsApi
import com.lhwdev.selfTestMacro.api.PasswordResult
import com.lhwdev.selfTestMacro.api.updateAgreement
import com.lhwdev.selfTestMacro.debug.log
import com.lhwdev.selfTestMacro.navigation.LocalNavigator
import com.lhwdev.selfTestMacro.navigation.Navigator
import com.lhwdev.selfTestMacro.repository.LocalSelfTestManager
import com.lhwdev.selfTestMacro.repository.MasterUser
import com.lhwdev.selfTestMacro.repository.SelfTestManager
import com.lhwdev.selfTestMacro.repository.WizardUser
import com.lhwdev.selfTestMacro.showToastSuspendAsync
import com.lhwdev.selfTestMacro.ui.*
import com.lhwdev.selfTestMacro.ui.utils.IconOnlyTopAppBar
import com.lhwdev.selfTestMacro.ui.utils.RoundButton
import com.vanpra.composematerialdialogs.*
import kotlinx.coroutines.launch


@Composable
internal fun WizardUserInfo(model: SetupModel, parameters: SetupParameters, wizard: SetupWizard) {
	when(model.instituteInfo) {
		is InstituteInfoModel.School -> WizardStudentInfo(model, parameters, wizard)
		null -> wizard.before()
	}
}


private suspend fun submitLogin(
	context: Context,
	model: SetupModel,
	parameters: SetupParameters,
	selfTestManager: SelfTestManager,
	navigator: Navigator
): Boolean {
	val name = model.userName
	val birth = model.userBirth
	val instituteInfo = model.instituteInfo ?: return false
	val institute = instituteInfo.institute ?: return false
	
	val sessionInfo = selfTestManager.createSession()
	val session = sessionInfo.session
	
	val userId = try {
		log("#2. 사용자 찾기")
		selfTestManager.findUser(
			session = session,
			institute = institute,
			name = name, birthday = birth,
			loginType = instituteInfo.type.loginType
		)
	} catch(e: Throwable) {
		selfTestManager.debugContext.onError("사용자를 찾을 수 없어요.", e)
		return false
	}
	
	// user agreement
	if(!userId.agreement) {
		log("약관 동의 필요!")
		
		val agree = navigator.showSelfTestAgreementDialog()
		if(agree == true) {
			@OptIn(DangerousHcsApi::class) // confirmed to user
			session.updateAgreement(institute, userId.token)
			context.showToastSuspendAsync("약관에 동의했습니다.")
		} else {
			return false
		}
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
			PositiveButton(onClick = { close(password) })
			NegativeButton(onClick = requestClose)
		}
	}
	password ?: return false
	
	// validate & login with password
	val result = try {
		log("#3. 비밀번호 확인")
		selfTestManager.validatePassword(session, institute, userId.token, password)
	} catch(e: Throwable) {
		selfTestManager.debugContext.onError("로그인에 실패했어요.", e)
		return false
	}
	
	log("#4. 비밀번호 결과")
	
	when(result) {
		is PasswordResult.Failed -> navigator.showDialogUnit {
			Title { Text("로그인 실패") }
			Content {
				Text(
					result.errorMessage ?: "로그인에 실패했어요. (에러코드 ${result.errorCode}"
				)
			}
			Buttons {
				PositiveButton(onClick = requestClose)
			}
		}
		is PasswordResult.Success -> try {
			val userList = selfTestManager.getUserGroup(session, institute, result.token)
			
			val master = MasterUser(
				identifier = userId,
				birth = birth,
				password = password,
				instituteInfo = institute,
				instituteType = instituteInfo.type
			)
			
			val list = userList.map {
				WizardUser(
					user = it,
					info = selfTestManager.getUserInfo(session, institute, it),
					master = master
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
			selfTestManager.debugContext.onError("사용자 정보를 불러오지 못했어요.", e)
		}
		
	}
	return false
}

@Composable
private fun WizardStudentInfo(
	model: SetupModel,
	parameters: SetupParameters,
	wizard: SetupWizard
): Unit = MaterialTheme(
	colors = MaterialTheme.colors.copy(
		primary = Color(0xFF008170),
		onPrimary = MaterialTheme.colors.onSurface
	)
) {
	val scope = rememberCoroutineScope()
	val context = LocalContext.current
	val selfTestManager = LocalSelfTestManager.current
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
		val success = submitLogin(context, model, parameters, selfTestManager, navigator)
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
	
	
	val blendSurface = if(colors.isLight) Color(0xffb4fce3) else colors.surface
	Surface(color = blendSurface) {
		AutoSystemUi(
			enabled = wizard.isCurrent,
			onScreenMode = OnScreenSystemUiMode.Immersive()
		) { scrims ->
			val addingSameInstituteUser = model.addingSameInstituteUser
			
			WizardCommon(
				wizard = wizard,
				onNext = {
					if(complete) wizard.next()
					else submit()
				},
				extra = if(addingSameInstituteUser != null) ({
					RoundButton(
						onClick = {
							complete = true
							notFulfilled = -1
							model.userName = addingSameInstituteUser.name
							model.userBirth = addingSameInstituteUser.birth
							model.addingSameInstituteUser = null
							wizard.next()
						},
						trailingIcon = {
							Icon(painterResource(R.drawable.ic_clear_24), contentDescription = null)
						}
					) { Text("추가 취소") }
				}) else null,
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
					modifier = Modifier.verticalScroll(rememberScrollState())
				) {
					val focusManager = LocalFocusManager.current
					
					// header
					AnimatedContent(
						targetState = LocalWindowInsets.current.ime.isVisible
					) { // For legibility: ime may hide TextField
						if(it) Column {
							TopAppBar(
								title = { Text("사용자 정보 입력") },
								navigationIcon = if(parameters.endRoute == null) null else ({
									IconButton(onClick = parameters.endRoute) {
										Icon(painterResource(R.drawable.ic_clear_24), contentDescription = "닫기")
									}
								}),
								backgroundColor = Color.Transparent,
								elevation = 0.dp,
								statusBarScrim = scrims.statusBar
							)
						} else Column {
							if(parameters.endRoute != null) IconOnlyTopAppBar(
								navigationIcon = painterResource(R.drawable.ic_clear_24),
								contentDescription = "닫기",
								onClick = parameters.endRoute,
								statusBarScrim = scrims.statusBar
							) else scrims.statusBar()
							
							Spacer(Modifier.height(40.dp))
							
							StudentInfoHeader(model)
						}
					}
					
					Column(Modifier.padding(horizontal = 12.dp)) {
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
								(Text("YYMMDD"))
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
			}
			
			scrims.navigationBar()
		}
	}
}


@Composable
private fun StudentInfoHeader(model: SetupModel) {
	Column(
		modifier = Modifier
			.padding(vertical = 28.dp)
			.fillMaxWidth(),
		horizontalAlignment = Alignment.CenterHorizontally
	) {
		Icon(
			painterResource(R.drawable.ic_school_24),
			contentDescription = null, // not that important
			tint = DefaultContentColor,
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
}

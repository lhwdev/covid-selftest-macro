package com.lhwdev.selfTestMacro.ui.pages.setup

import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import com.lhwdev.selfTestMacro.R
import com.lhwdev.selfTestMacro.api.*
import com.lhwdev.selfTestMacro.repository.SessionManager
import com.lhwdev.selfTestMacro.repository.SessionUserKey
import com.lhwdev.selfTestMacro.selfLog
import com.lhwdev.selfTestMacro.ui.*
import com.vanpra.composematerialdialogs.*
import kotlinx.coroutines.launch


@Composable
internal fun WizardUserInfo(model: SetupModel, wizard: SetupWizard) {
	when(model.instituteInfo) {
		is InstituteInfoModel.School -> WizardStudentInfo(model, wizard)
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
	
	val sessionInfo = SessionManager.sessionInfoFor(
		key = SessionUserKey(name = name, birth = birth, instituteCode = institute.code)
	)
	val session = sessionInfo.session
	
	val userId = try {
		selfLog("#2. 사용자 찾기")
		session.findUser(
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
		session.validatePassword(institute, userId.token, password)
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
			sessionInfo.sessionFullyLoaded = true
			
			try {
				val userList = session.getUserGroup(institute, result)
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
						info = session.getUserInfo(institute, it),
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
			
			scrims.navigationBar()
		}
	}
}

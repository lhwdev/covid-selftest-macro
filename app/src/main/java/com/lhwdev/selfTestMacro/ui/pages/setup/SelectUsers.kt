package com.lhwdev.selfTestMacro.ui.pages.setup

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.lhwdev.selfTestMacro.R
import com.lhwdev.selfTestMacro.api.InstituteInfo
import com.lhwdev.selfTestMacro.api.InstituteType
import com.lhwdev.selfTestMacro.navigation.LocalNavigator
import com.lhwdev.selfTestMacro.navigation.pushRoute
import com.lhwdev.selfTestMacro.repository.LocalSelfTestManager
import com.lhwdev.selfTestMacro.repository.WizardUser
import com.lhwdev.selfTestMacro.ui.LocalPreference
import com.lhwdev.selfTestMacro.ui.pages.main.MainRoute
import com.lhwdev.selfTestMacro.ui.systemUi.AutoSystemUi
import com.lhwdev.selfTestMacro.ui.systemUi.OnScreenSystemUiMode
import com.lhwdev.selfTestMacro.ui.utils.IconOnlyTopAppBar
import com.lhwdev.selfTestMacro.ui.utils.TextCheckbox
import kotlinx.coroutines.launch


class AddSameInstituteUser(val name: String, val birth: String)

@Composable
internal fun WizardSelectUsers(model: SetupModel, parameters: SetupParameters, wizard: SetupWizard) {
	val navigator = LocalNavigator
	val pref = LocalPreference.current
	val selfTestManager = LocalSelfTestManager.current
	val scope = rememberCoroutineScope()
	
	val userList = model.userList
	
	if(userList.isEmpty()) {
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
			onScreen = OnScreenSystemUiMode.Opaque(Color.Transparent)
		) {
			if(parameters.endRoute != null) IconOnlyTopAppBar(
				navigationIcon = painterResource(R.drawable.ic_clear_24),
				contentDescription = "닫기",
				onClick = parameters.endRoute
			) else Spacer(Modifier.height(AppBarHeight))
			
			WizardCommon(
				wizard,
				wizardFulfilled = if(pref.db.users.users.isEmpty()) enabled.isNotEmpty() else true,
				showNotFulfilledWarning = {
					scope.launch {
						model.showSnackbar("사용자를 최소 한 명 선택해주세요")
					}
				},
				onNext = next@{ // complete
					selfTestManager.addTestGroupToDb(
						usersToAdd = userList.filterIndexed { index, _ -> enabled[index] },
						targetGroup = parameters.targetTestGroup,
						isAllGrouped = isAllGrouped
					)
					
					when {
						parameters.endRoute != null -> parameters.endRoute.invoke()
						
						navigator.routes.size == 1 -> {
							navigator.clearRoute()
							navigator.pushRoute(MainRoute)
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
	Column(modifier = Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState())) {
		for((index, user) in userList.withIndex()) key(user) {
			ListItem(
				icon = {
					Checkbox(checked = enabled[index], onCheckedChange = null)
				},
				text = { Text(user.info.userName) },
				secondaryText = { Text(user.info.instituteName) },
				modifier = Modifier
					.clearAndSetSemantics {
						text = AnnotatedString("${user.info.instituteName} ${user.info.userName}")
						toggleableState = ToggleableState(enabled[index])
						role = Role.Checkbox
					}
					.clickable { // this adds semantics onClick
						setEnabled(index, !enabled[index])
					}
					.padding(horizontal = 12.dp)
			)
		}
	}
	
	val commonInstitute = remember(model.userList) {
		var institute: InstituteInfo? = null
		
		for(user in model.userList) {
			val last = institute
			val now = user.info.instituteStub
			
			when {
				last == null -> institute = now
				last.code == user.info.instituteCode -> continue
				else -> {
					institute = null
					break
				}
			}
		}
		institute
	}
	val addSameInstituteText = when(commonInstitute?.type) {
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
				model.addingSameInstituteUser = AddSameInstituteUser(model.userName, model.userBirth)
				model.userName = ""
				model.userBirth = ""
				// TODO: ability to go back and go forward back
				
				wizard.scrollTo(2) // #3: user info
			}
			.padding(horizontal = 12.dp)
	)
	
	Spacer(Modifier.weight(1f))
	
	if(userList.size > 1 && parameters.targetTestGroup == null) Box(
		contentAlignment = Alignment.Center,
		modifier = Modifier.fillMaxWidth()
	) {
		TextCheckbox(
			text = { Text("한 그룹으로 묶기") },
			checked = isAllGrouped,
			setChecked = setIsAllGrouped
		)
	}
}

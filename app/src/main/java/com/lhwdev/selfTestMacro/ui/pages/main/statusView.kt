package com.lhwdev.selfTestMacro.ui.pages.main

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
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.lhwdev.selfTestMacro.R
import com.lhwdev.selfTestMacro.database.DbTestTarget
import com.lhwdev.selfTestMacro.database.DbUser
import com.lhwdev.selfTestMacro.navigation.LocalNavigator
import com.lhwdev.selfTestMacro.repository.GroupStatus
import com.lhwdev.selfTestMacro.repository.LocalSelfTestManager
import com.lhwdev.selfTestMacro.repository.Status
import com.lhwdev.selfTestMacro.repository.SuspiciousKind
import com.lhwdev.selfTestMacro.ui.*
import com.lhwdev.selfTestMacro.ui.utils.SmallIconButton
import com.vanpra.composematerialdialogs.*


private val SuspiciousKind?.displayText get() = when(this) {
	null -> "정상"
	SuspiciousKind.quarantined -> "자가격리 중"
	SuspiciousKind.symptom -> "의심증상 있음"
}

@Composable
internal fun (@Suppress("unused") ColumnScope).SingleStatusView(
	target: DbTestTarget.Single,
	statusKey: MutableState<Int>
) {
	val pref = LocalPreference.current
	val selfTestManager = LocalSelfTestManager.current
	
	if(changed(target)) statusKey.value++
	val status = lazyState(null, key = statusKey.value) {
		with(pref.db) { selfTestManager.getCurrentStatus(target.user) }
	}.value
	
	Row(verticalAlignment = Alignment.CenterVertically) {
		Spacer(Modifier.width(44.dp))
		
		Text("자가진단 상태", style = MaterialTheme.typography.h6)
		
		Spacer(Modifier.width(8.dp))
		
		SmallIconButton(onClick = {
			statusKey.value++
		}) {
			Icon(
				painterResource(R.drawable.ic_refresh_24),
				contentDescription = "새로 고침",
				modifier = Modifier.size(18.dp)
			)
		}
	}
	Spacer(Modifier.height(16.dp))
	
	when(status) {
		null -> Text("불러오는 중...", style = MaterialTheme.typography.h3)
		is Status.Submitted -> {
			Text(status.suspicious.displayText, style = MaterialTheme.typography.h3)
			
			Spacer(Modifier.height(20.dp))
			
			Text(
				status.time,
				style = MaterialTheme.typography.h6,
				color = MaterialTheme.colors.primaryActive
			)
		}
		is Status.NotSubmitted -> Text("제출하지 않음", style = MaterialTheme.typography.h3)
	}
}

@Suppress("unused")
@Composable
internal fun ColumnScope.GroupStatusView(target: DbTestTarget.Group, statusKey: MutableState<Int>) {
	val pref = LocalPreference.current
	val selfTestManager = LocalSelfTestManager.current
	val navigator = LocalNavigator
	val users = with(pref.db) { target.allUsers }
	
	var allStatus by remember { mutableStateOf<Map<DbUser, Status>>(emptyMap()) } // stub
	var forceAllowInit by remember { mutableStateOf(false) }
	val allowInit = users.size <= 4 || forceAllowInit
	
	if(changed(target)) statusKey.value++
	val groupStatus = lazyState(null, key = statusKey.value, allowInit = allowInit) state@{
		val statusMap = users.associateWith {
			selfTestManager.getCurrentStatus(it) ?: return@state null
		}
		allStatus = statusMap
		
		val symptom = mutableListOf<DbUser>()
		val quarantined = mutableListOf<DbUser>()
		var notSubmitted = 0
		
		for((user, status) in statusMap) when(status) {
			is Status.Submitted -> when(status.suspicious) {
				SuspiciousKind.symptom -> symptom += user
				SuspiciousKind.quarantined -> quarantined += user
			}
			is Status.NotSubmitted -> notSubmitted++
		}
		
		GroupStatus(notSubmittedCount = notSubmitted, symptom = symptom, quarantined = quarantined)
	}.value
	
	if(groupStatus == null && !allowInit) {
		Text(
			"자가진단 상태 불러오기(${users.size}명)",
			style = MaterialTheme.typography.h6,
			color = MaterialTheme.colors.primaryActive,
			modifier = Modifier.clickable { forceAllowInit = true }
		)
	} else {
		Row(verticalAlignment = Alignment.CenterVertically) {
			Spacer(Modifier.width(44.dp))
			
			Text("자가진단 상태", style = MaterialTheme.typography.h6)
			
			Spacer(Modifier.width(8.dp))
			
			SmallIconButton(onClick = {
				statusKey.value++
				forceAllowInit = true
			}) {
				Icon(
					painterResource(R.drawable.ic_refresh_24),
					contentDescription = "새로 고침",
					modifier = Modifier.size(18.dp)
				)
			}
		}
		
		Spacer(Modifier.height(16.dp))
		
		if(groupStatus == null) {
			Text("불러오는 중...", style = MaterialTheme.typography.h3)
		} else {
			val noSpecial = groupStatus.symptom.isEmpty() && groupStatus.quarantined.isEmpty()
			
			if(groupStatus.notSubmittedCount == 0) {
				if(noSpecial) {
					Text("모두 정상", style = MaterialTheme.typography.h3)
				} else {
					Text("이상 있음", style = MaterialTheme.typography.h3)
				}
			} else {
				Text(
					"자가진단 ${groupStatus.notSubmittedCount}명 미완료",
					style = MaterialTheme.typography.h4
				)
			}
			
			if(!noSpecial) {
				Spacer(Modifier.height(18.dp))
				
				if(groupStatus.symptom.isNotEmpty()) Text(
					"유증상자: ${groupStatus.symptom.joinToString { it.name }}",
					style = MaterialTheme.typography.body1
				)
				if(groupStatus.quarantined.isNotEmpty()) Text(
					"자가격리 중: ${groupStatus.quarantined.joinToString { it.name }}",
					style = MaterialTheme.typography.body1
				)
			}
		}
		
		Spacer(Modifier.height(12.dp))
		
		var showDetails by remember { mutableStateOf(false) }
		TextButton(onClick = { showDetails = true }) {
			Text("자세히 보기")
		}
		
		if(showDetails) MaterialDialog(onCloseRequest = { showDetails = false }) {
			Title { Text("${target.name}의 자가진단 현황") }
			ListContent {
				Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
					for((user, status) in allStatus) ListItem(
						icon = {
							val icon = when(status) {
								is Status.Submitted -> if(status.suspicious == null) {
									R.drawable.ic_check_24
								} else {
									R.drawable.ic_warning_24
								}
								Status.NotSubmitted -> R.drawable.ic_clear_24
							}
							
							Icon(painterResource(icon), contentDescription = null)
						},
						trailing = { Icon(painterResource(R.drawable.ic_arrow_right_24), contentDescription = null) },
						modifier = Modifier.clickable {
							navigator.showDialogAsync { OneUserDetail(user, status) }
						}
					) {
						Row {
							val text = buildAnnotatedString {
								append(user.name)
								append(" ")
								withStyle(SpanStyle(color = MediumContentColor)) {
									append("(${user.institute.name})")
								}
								append(": ")
								
								when(status) {
									is Status.Submitted -> if(status.suspicious == null) withStyle(
										SpanStyle(
											color = Color(
												onLight = Color(0xff285db9),
												onDark = Color(0xffadcbff)
											)
										)
									) {
										append("정상")
									} else withStyle(
										SpanStyle(
											color = Color(
												onLight = Color(0xfffd2f5f),
												onDark = Color(0xffffa6aa)
											)
										)
									) {
										append(status.suspicious.displayText)
									}
									
									Status.NotSubmitted -> withStyle(SpanStyle(MediumContentColor)) {
										append("미제출")
									}
								}
								
							}
							
							Text(text)
						}
					}
				}
			}
			
			Buttons {
				PositiveButton { Text("확인") }
			}
		}
	}
}

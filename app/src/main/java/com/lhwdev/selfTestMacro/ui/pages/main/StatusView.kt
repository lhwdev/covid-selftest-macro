package com.lhwdev.selfTestMacro.ui.pages.main

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.lhwdev.selfTestMacro.R
import com.lhwdev.selfTestMacro.database.DbTestGroup
import com.lhwdev.selfTestMacro.database.DbTestTarget
import com.lhwdev.selfTestMacro.database.DbUser
import com.lhwdev.selfTestMacro.navigation.LocalNavigator
import com.lhwdev.selfTestMacro.repository.GroupStatus
import com.lhwdev.selfTestMacro.repository.LocalSelfTestManager
import com.lhwdev.selfTestMacro.repository.Status
import com.lhwdev.selfTestMacro.repository.SuspiciousKind
import com.lhwdev.selfTestMacro.ui.LocalPreference
import com.lhwdev.selfTestMacro.ui.changed
import com.lhwdev.selfTestMacro.ui.lazyState
import com.lhwdev.selfTestMacro.ui.pages.common.promptSelectUserInGroupDialog
import com.lhwdev.selfTestMacro.ui.pages.common.showGroupTestStatusDialog
import com.lhwdev.selfTestMacro.ui.primaryActive
import com.lhwdev.selfTestMacro.ui.utils.AutoSizeText
import com.lhwdev.selfTestMacro.ui.utils.RoundButton
import com.lhwdev.selfTestMacro.ui.utils.SmallIconButton
import kotlinx.coroutines.launch


@Composable
internal fun (@Suppress("unused") ColumnScope).SingleStatusView(
	group: DbTestGroup,
	statusKey: MutableState<Int>
) {
	val target = group.target as DbTestTarget.Single
	val pref = LocalPreference.current
	val selfTestManager = LocalSelfTestManager.current
	val user = with(pref.db) { target.user }
	
	if(changed(target)) statusKey.value++
	val status = lazyState(null, key = statusKey.value) {
		selfTestManager.getCurrentStatus(user)
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
		null -> AutoSizeText("불러오는 중...", style = MaterialTheme.typography.h3)
		is Status.Submitted -> {
			AutoSizeText(status.suspicious.displayText, style = MaterialTheme.typography.h3)
			
			Spacer(Modifier.height(10.dp))
			
			Text(
				status.time,
				style = MaterialTheme.typography.h6
			)
		}
		is Status.NotSubmitted -> AutoSizeText("제출하지 않음", style = MaterialTheme.typography.h3)
	}
	
	Spacer(Modifier.height(12.dp))
	
	val allStatus = if(status == null) emptyMap() else mapOf(user to status)
	StatusAction(allStatus, group, statusKey, statusReady = status != null)
}

@Suppress("unused")
@Composable
internal fun ColumnScope.GroupStatusView(group: DbTestGroup, statusKey: MutableState<Int>) {
	val target = group.target as DbTestTarget.Group
	val pref = LocalPreference.current
	val selfTestManager = LocalSelfTestManager.current
	val navigator = LocalNavigator
	val users = with(pref.db) { target.allUsers }
	val scope = rememberCoroutineScope()
	
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
				else -> Unit
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
			AutoSizeText("불러오는 중...", style = MaterialTheme.typography.h3)
		} else {
			val noSpecial = groupStatus.symptom.isEmpty() && groupStatus.quarantined.isEmpty()
			
			if(groupStatus.notSubmittedCount == 0) {
				if(noSpecial) {
					AutoSizeText("모두 정상", style = MaterialTheme.typography.h3)
				} else {
					AutoSizeText("이상 있음", style = MaterialTheme.typography.h3)
				}
			} else {
				AutoSizeText(
					"자가진단 ${groupStatus.notSubmittedCount}명 미완료",
					style = MaterialTheme.typography.h4
				)
			}
			
			if(!noSpecial) {
				Spacer(Modifier.height(18.dp))
				
				if(groupStatus.symptom.isNotEmpty()) AutoSizeText(
					"유증상자: ${groupStatus.symptom.joinToString { it.name }}",
					style = MaterialTheme.typography.body1
				)
				if(groupStatus.quarantined.isNotEmpty()) AutoSizeText(
					"자가격리 중: ${groupStatus.quarantined.joinToString { it.name }}",
					style = MaterialTheme.typography.body1
				)
			}
		}
	}
	Spacer(Modifier.height(12.dp))
	
	StatusAction(allStatus, group, statusKey, statusReady = groupStatus != null)
}

@Composable
private fun StatusAction(
	allStatus: Map<DbUser, Status>,
	group: DbTestGroup,
	statusKey: MutableState<Int>,
	statusReady: Boolean
) {
	val navigator = LocalNavigator
	val scope = rememberCoroutineScope()
	val pref = LocalPreference.current
	
	val target = group.target
	
	Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
		val allStatusState = rememberUpdatedState(allStatus)
		RoundButton(
			onClick = {
				navigator.showGroupTestStatusDialog(group, statusKey, allStatusBlock = { allStatusState.value })
			},
			enabled = statusReady,
			colors = ButtonDefaults.textButtonColors()
		) {
			Text("자세히 보기")
		}
		
		RoundButton(
			onClick = {
				when(target) {
					is DbTestTarget.Single ->
						navigator.showChangeAnswerDialog(with(pref.db) { target.user })
					
					is DbTestTarget.Group -> scope.launch {
						val changeTarget = navigator.promptSelectUserInGroupDialog(
							title = "응답을 수정할 대상 선택",
							target = target,
							database = pref.db
						)
						if(changeTarget != null) {
							navigator.showChangeAnswerDialog(changeTarget)
						}
					}
				}
			},
			colors = ButtonDefaults.textButtonColors()
		) {
			Text("응답 수정")
		}
	}
}



package com.lhwdev.selfTestMacro

import androidx.annotation.DrawableRes
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.lhwdev.selfTestMacro.api.UserInfo
import com.lhwdev.selfTestMacro.api.getUserInfo
import com.lhwdev.selfTestMacro.icons.ExpandMore
import com.lhwdev.selfTestMacro.icons.Icons
import com.vanpra.composematerialdialogs.Buttons
import com.vanpra.composematerialdialogs.ListContent
import com.vanpra.composematerialdialogs.MaterialDialog
import com.vanpra.composematerialdialogs.Title


fun <T> List<T>.repeat(count: Int): List<T> {
	val list = ArrayList<T>(size * count)
	repeat(count) {
		list.addAll(this)
	}
	return list
}


@OptIn(ExperimentalMaterialApi::class)
@Preview
@Composable
fun Main(): Unit = Surface(color = MaterialTheme.colors.surface) {
	// val context = LocalActivity.current
	val route = LocalRoute.current
	val pref = LocalPreference.current
	
	val users = pref.db.users.users
	
	if(users.isEmpty()) {
		route[0] = { Setup() }
		return@Surface
	}
	
	val groups = pref.db.testGroups.groups
	
	
	var selectedTestGroup by remember {
		mutableStateOf(groups.getOrElse(pref.headUser) { groups.first() })
	}
	val selectedGroup = GroupInfo(selectedTestGroup)
	
	
	if(changed(groups)) {
		if(selectedTestGroup !in groups) selectedTestGroup = groups.first()
	}
	
	var showSelect by remember { mutableStateOf(false) }
	
	
	AutoSystemUi(enabled = true) { scrims ->
		Scaffold(
			topBar = {
				var showMoreActions by remember { mutableStateOf(false) }
				TopAppBar(
					title = { Text("코로나19 자가진단 매크로") },
					actions = {
						IconButton(onClick = { showMoreActions = true }) {
							Icon(
								painterResource(R.drawable.ic_more_vert_24),
								contentDescription = "옵션 더보기"
							)
						}
						
						DropdownMenu(
							expanded = showMoreActions,
							onDismissRequest = { showMoreActions = false }
						) {
							DropdownMenuItem(onClick = {
								showMoreActions = false
								route.add { Info() }
							}) {
								Text("정보")
							}
						}
					},
					statusBarScrim = { scrims.statusBar() }
				)
			},
			modifier = Modifier.weight(1f)
		) { paddingValue ->
			Column(
				modifier = Modifier.padding(paddingValue).padding(vertical = 24.dp).fillMaxSize(),
				horizontalAlignment = Alignment.CenterHorizontally
			) {
				MainContent(
					group = selectedGroup,
					showSelectingUser = { showSelect = true }
				)
			}
		}
		
		scrims.navigationBar()
	}
	
	if(showSelect) MaterialDialog(onCloseRequest = { showSelect = false }) {
		Scaffold(
			topBar = {
				Column {
					TopAppBar(
						title = { Text("대상 선택") },
						navigationIcon = {
							IconButton(onClick = { showSelect = false }) {
								Icon(
									painterResource(R.drawable.ic_clear_24),
									contentDescription = "닫기"
								)
							}
						},
						elevation = 0.dp,
						backgroundColor = Color.Transparent
					)
					Divider()
				}
			}
		) {
			Column {
				Column(modifier = Modifier.verticalScroll(rememberScrollState()).weight(1f)) {
					for(group in groups) UserListItem(
						group = group,
						onClick = {
							selectedTestGroup = group
							showSelect = false
						}
					)
				}
				
				ListItem(
					icon = {
						Icon(painterResource(R.drawable.ic_edit_24), contentDescription = null)
					},
					modifier = Modifier.clickable {
						showSelect = false
						route.add { EditUsers() }
					}
				) {
					Text("편집")
				}
			}
		}
	}
}


@DrawableRes
fun DatabaseManager.iconFor(group: DbTestTarget): Int = when(group) {
	is DbTestTarget.Group -> R.drawable.ic_group_24
	is DbTestTarget.Single -> when(group.user.instituteType) {
		InstituteType.school -> R.drawable.ic_school_24
		InstituteType.university -> TODO()
		InstituteType.academy -> TODO()
		InstituteType.office -> TODO()
	}
}


@Suppress("ComposableNaming") // factory
@Composable
private fun GroupInfo(group: DbTestGroup): GroupInfo {
	val pref = LocalPreference.current
	
	return remember(group) {
		GroupInfo(
			icon = pref.db.iconFor(group.target),
			name = with(pref.db) { group.target.name },
			instituteName = with(pref.db) { group.target.commonInstitute?.name },
			group = group
		)
	}
}


@Stable
private data class GroupInfo(
	@DrawableRes val icon: Int,
	val name: String,
	val instituteName: String?,
	val group: DbTestGroup
) {
	val isGroup: Boolean get() = group.target is DbTestTarget.Group
	
	val subtitle: String
		get() = when {
			instituteName == null -> "그룹"
			isGroup -> "그룹, $instituteName"
			else -> instituteName
		}
}


sealed class Status {
	data class Submitted(val isHealthy: Boolean, val time: String) : Status()
	object NotSubmitted : Status()
}

fun Status(info: UserInfo): Status = when {
	info.isHealthy != null && info.lastRegisterAt != null ->
		Status.Submitted(info.isHealthy!!, formatRegisterTime(info.lastRegisterAt!!))
	else -> Status.NotSubmitted
}

private fun formatRegisterTime(time: String): String = time.substring(0, time.lastIndexOf('.'))

suspend fun DatabaseManager.getCurrentStatus(user: DbUser): Status {
	return Status(getUserInfo(user.institute, user.user))
}


@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun ColumnScope.MainContent(
	group: GroupInfo,
	showSelectingUser: () -> Unit
) {
	val pref = LocalPreference.current
	
	// select test group
	TextIconButton(
		onClick = showSelectingUser,
		icon = { Icon(painterResource(group.icon), contentDescription = null) },
		trailingIcon = { Icon(imageVector = Icons.Filled.ExpandMore, contentDescription = "더보기") }
	) {
		Text(group.name)
		
		Spacer(Modifier.width(4.dp))
		
		val subText = buildString {
			if(group.instituteName != null) {
				append(group.instituteName)
				if(group.isGroup) append(", ")
			}
			
			if(group.isGroup) append("${group.group.target.allUserIds.size}명")
		}
		Text("($subText)", color = LocalContentColor.current.copy(ContentAlpha.medium))
	}
	
	// test group members hint
	if(group.isGroup) {
		val users = with(pref.db) { group.group.target.allUsers }
			.joinToString(separator = ", ", limit = 4) { it.user.name }
		Text(users, style = MaterialTheme.typography.body1)
	}
	
	Spacer(Modifier.weight(1f))
	
	when(val target = group.group.target) {
		is DbTestTarget.Group -> {
			GroupStatusView(target)
		}
		is DbTestTarget.Single -> {
			val state = lazyState(null) {
				with(pref.db) { getCurrentStatus(target.user) }
			}.value
			
			SingleStatusView(state)
		}
	}
	
	Spacer(Modifier.weight(2f))
	
	TextIconButton(
		onClick = {},
		icon = { Icon(painterResource(R.drawable.ic_access_alarm_24), contentDescription = null) }
	) {
		val text = buildAnnotatedString {
			withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append("자가진단 예약") }
			append(": ")
			
			fun fixed(fixed: DbTestSchedule.Fixed) {
				append("${fixed.hour}시 ${fixed.minute}분")
			}
			
			when(val schedule = group.group.schedule) {
				DbTestSchedule.None -> append("꺼짐")
				is DbTestSchedule.Fixed -> {
					append("매일 ")
					fixed(schedule)
				}
				is DbTestSchedule.Random -> {
					fixed(schedule.from)
					append("~")
					fixed(schedule.to)
				}
			}
		}
		
		Text(text)
	}
	
	Spacer(Modifier.weight(1f))
}


@Composable
private fun SingleStatusView(status: Status?) {
	Text("자가진단 상태", style = MaterialTheme.typography.subtitle1)
	
	Spacer(Modifier.height(16.dp))
	
	when(status) {
		null -> Text("불러오는 중...", style = MaterialTheme.typography.h3)
		is Status.Submitted -> {
			if(status.isHealthy) Text("정상", style = MaterialTheme.typography.h3)
			else Text("의심증상 있음", style = MaterialTheme.typography.h3)
			
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

private data class GroupStatus(val notSubmittedCount: Int, val suspicious: List<DbUser>)

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun GroupStatusView(target: DbTestTarget.Group) {
	val pref = LocalPreference.current
	val users = with(pref.db) { target.allUsers }
	
	var allStatus by remember { mutableStateOf<Map<DbUser, Status>>(emptyMap()) } // stub
	var forceAllowInit by remember { mutableStateOf(false) }
	val allowInit = users.size <= 5 || forceAllowInit
	
	val groupStatus = lazyState(null, allowInit = allowInit) {
		val statusMap = users.map {
			it to with(pref.db) { getCurrentStatus(it) }
		}.toMap()
		allStatus = statusMap
		
		val suspicious = mutableListOf<DbUser>()
		var notSubmitted = 0
		
		for((user, status) in statusMap) when(status) {
			is Status.Submitted -> if(!status.isHealthy) suspicious += user
			is Status.NotSubmitted -> notSubmitted++
		}
		
		GroupStatus(notSubmittedCount = notSubmitted, suspicious = suspicious)
	}.value
	
	if(groupStatus == null && !allowInit) {
		Text(
			"자가진단 상태 불러오기(${users.size}명)",
			style = MaterialTheme.typography.h6,
			color = MaterialTheme.colors.primaryActive,
			modifier = Modifier.clickable { forceAllowInit = true }
		)
	} else {
		Text("자가진단 상태", style = MaterialTheme.typography.h6)
		
		Spacer(Modifier.height(16.dp))
		
		when {
			groupStatus == null -> {
				Text("불러오는 중...", style = MaterialTheme.typography.h3)
			}
			
			groupStatus.notSubmittedCount == 0 -> if(groupStatus.suspicious.isEmpty()) {
				Text("모두 정상", style = MaterialTheme.typography.h3)
			} else {
				Text("유증상자 있음", style = MaterialTheme.typography.h3)
				
				Spacer(Modifier.height(12.dp))
				
				Text(
					"유증상자: ${groupStatus.suspicious.joinToString { it.user.name }}",
					style = MaterialTheme.typography.body1
				)
			}
			
			else -> {
				Text(
					"자가진단 ${groupStatus.notSubmittedCount}명 미완료",
					style = MaterialTheme.typography.h4
				)
				
				if(groupStatus.suspicious.isNotEmpty()) Text(
					"유증상자: ${groupStatus.suspicious.joinToString { it.user.name }}",
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
								is Status.Submitted -> if(status.isHealthy) R.drawable.ic_check_24 else R.drawable.ic_warning_24
								Status.NotSubmitted -> R.drawable.ic_clear_24
							}
							
							Icon(painterResource(icon), contentDescription = null)
						}
					) {
						Row {
							Text(user.user.name)
							Text(" (${user.instituteName})", color = MediumContentColor)
							Text(": ")
							
							when(status) {
								is Status.Submitted -> if(status.isHealthy) {
									Text(
										"정상", color = Color(
											onLight = Color(0xff285db9),
											onDark = Color(0xffadcbff)
										)
									)
								} else {
									Text(
										"의심증상 있음", color = Color(
											onLight = Color(0xfffd2f5f), onDark = Color(0xffffa6aa)
										)
									)
								}
								Status.NotSubmitted -> Text("미제출", color = MediumContentColor)
							}
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


@OptIn(ExperimentalMaterialApi::class)
@Composable
fun UserListItem(group: DbTestGroup, onClick: () -> Unit) {
	val target = group.target
	val pref = LocalPreference.current
	val icon = painterResource(pref.db.iconFor(target))
	
	ListItem(
		icon = {
			Icon(icon, contentDescription = null)
		},
		text = {
			Text(with(pref.db) { target.name })
		},
		secondaryText = {
			if(target is DbTestTarget.Group) with(pref.db) { target.allUsers }.joinToString(
				separator = ", ", limit = 4
			) { it.user.name }
		},
		modifier = Modifier.clickable(onClick = onClick)
	)
}


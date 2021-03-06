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
import com.lhwdev.selfTestMacro.icons.ExpandMore
import com.lhwdev.selfTestMacro.icons.Icons
import com.lhwdev.selfTestMacro.model.*
import com.vanpra.composematerialdialogs.*


@Preview
@Composable
fun Main(
	repository: MainRepository = run {
		val pref = LocalPreference.current
		remember { MainRepositoryImpl(pref) }
	}
): Unit = Surface(color = MaterialTheme.colors.surface) {
	val navigator = LocalNavigator
	
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
								navigator.pushRoute { Info() }
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
				modifier = Modifier
					.padding(paddingValue)
					.padding(vertical = 28.dp)
					.fillMaxSize(),
				horizontalAlignment = Alignment.CenterHorizontally
			) {
				MainContent(repository)
			}
		}
		
		scrims.navigationBar()
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


@Composable
private fun ColumnScope.MainContent(repository: MainRepository) {
	val pref = LocalPreference.current
	val navigator = LocalNavigator
	
	val users = pref.db.users.users
	val groups = pref.db.testGroups.groups
	
	if(users.isEmpty() || groups.isEmpty()) {
		Column(
			verticalArrangement = Arrangement.Center,
			horizontalAlignment = Alignment.CenterHorizontally,
			modifier = Modifier.fillMaxSize()
		) {
			Text("등록된 사용자가 없습니다.", style = MaterialTheme.typography.h4)
			Spacer(Modifier.height(32.dp))
			RoundButton(
				onClick = {
					navigator.showRouteAsync { Setup() }
				},
				icon = { Icon(painterResource(R.drawable.ic_add_24), contentDescription = null) }
			) {
				Text("사용자 추가")
			}
		}
		return
	}
	
	var selectedTestGroup by remember {
		mutableStateOf(groups.getOrElse(pref.headUser) { groups.first() })
	}
	val selectedGroup = GroupInfo(selectedTestGroup)
	
	
	if(changed(groups)) {
		if(selectedTestGroup !in groups) selectedTestGroup = groups.first()
	}
	
	var showSelect by remember { mutableStateOf(false) }
	
	
	/// head
	
	// select test group
	// '그룹 1 (2명)'
	RoundButton(
		onClick = { showSelect = true },
		icon = { Icon(painterResource(selectedGroup.icon), contentDescription = null) },
		trailingIcon = {
			Icon(
				imageVector = Icons.Filled.ExpandMore,
				contentDescription = "더보기"
			)
		}
	) {
		Text(selectedGroup.name)
		
		Spacer(Modifier.width(4.dp))
		
		val subText = buildString {
			if(selectedGroup.instituteName != null) {
				append(selectedGroup.instituteName)
				if(selectedGroup.isGroup) append(", ")
			}
			
			if(selectedGroup.isGroup) append("${selectedGroup.group.target.allUserIds.size}명")
		}
		Text("($subText)", color = LocalContentColor.current.copy(ContentAlpha.medium))
	}
	
	// test group members hint
	// '김철수'
	if(selectedGroup.isGroup) {
		Spacer(Modifier.height(4.dp))
		
		val usersText = with(pref.db) { selectedGroup.group.target.allUsers }
			.joinToString(separator = ", ", limit = 4) { it.user.name }
		Text(usersText, style = MaterialTheme.typography.body1)
	}
	
	Spacer(Modifier.weight(1f))
	
	
	/// center
	
	// status
	// '자가진단 상태'
	//  '모두 정상'
	//  '자세히 보기'
	when(val target = selectedGroup.group.target) {
		is DbTestTarget.Group -> {
			GroupStatusView(repository, target)
		}
		is DbTestTarget.Single -> {
			SingleStatusView(repository, target)
		}
	}
	
	
	Spacer(Modifier.weight(2f))
	
	/// below
	
	// scheduling
	// '자가진단 예약: 꺼짐'
	RoundButton(
		onClick = {
			navigator.showScheduleSelfTest(selectedGroup)
		},
		icon = {
			Icon(
				painterResource(R.drawable.ic_access_alarm_24),
				contentDescription = null
			)
		},
		trailingIcon = {
			Icon(Icons.Filled.ExpandMore, contentDescription = null)
		}
	) {
		val text = buildAnnotatedString {
			withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append("자가진단 예약") }
			append(": ")
			append(selectedGroup.group.scheduleInfo())
		}
		
		Text(text)
	}
	
	Spacer(Modifier.height(16.dp))
	
	RoundButton(
		onClick = {},
		colors = ButtonDefaults.buttonColors()
	) {
		Text("지금 자가진단 제출하기")
	}
	
	Spacer(Modifier.height(36.dp))
	
	
	/// Dialogs
	if(showSelect) MaterialDialog(
		onCloseRequest = { showSelect = false }
	) {
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
				Column(
					modifier = Modifier
						.verticalScroll(rememberScrollState())
						.weight(1f)
				) {
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
						navigator.showRouteAsync { EditUsers() }
					}
				) {
					Text("편집")
				}
			}
		}
	}
	
}


@Suppress("unused")
@Composable
private fun ColumnScope.SingleStatusView(repository: MainRepository, target: DbTestTarget.Single) {
	val pref = LocalPreference.current
	
	var statusKey by remember { mutableStateOf(0) }
	val status = lazyState(null, key = statusKey) {
		with(pref.db) { repository.getCurrentStatus(target.user) }
	}.value
	
	Row(verticalAlignment = Alignment.CenterVertically) {
		Spacer(Modifier.width(44.dp))
		
		Text("자가진단 상태", style = MaterialTheme.typography.h6)
		
		Spacer(Modifier.width(8.dp))
		
		SmallIconButton(onClick = {
			statusKey++
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

@Suppress("unused")
@Composable
private fun ColumnScope.GroupStatusView(repository: MainRepository, target: DbTestTarget.Group) {
	val pref = LocalPreference.current
	val users = with(pref.db) { target.allUsers }
	
	var allStatus by remember { mutableStateOf<Map<DbUser, Status>>(emptyMap()) } // stub
	var forceAllowInit by remember { mutableStateOf(false) }
	val allowInit = users.size <= 4 || forceAllowInit
	
	var groupStatusKey by remember { mutableStateOf(0) }
	val groupStatus = lazyState(null, key = groupStatusKey, allowInit = allowInit) {
		val statusMap = users.map {
			it to repository.getCurrentStatus(it)
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
		Row(verticalAlignment = Alignment.CenterVertically) {
			Spacer(Modifier.width(44.dp))
			
			Text("자가진단 상태", style = MaterialTheme.typography.h6)
			
			Spacer(Modifier.width(8.dp))
			
			SmallIconButton(onClick = {
				groupStatusKey++
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
							val text = buildAnnotatedString {
								append(user.user.name)
								append(" ")
								withStyle(SpanStyle(color = MediumContentColor)) {
									append("(${user.instituteName})")
								}
								append(": ")
								
								when(status) {
									is Status.Submitted -> if(status.isHealthy) withStyle(
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
										append("의심증상 있음")
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


@Composable
fun UserListItem(group: DbTestGroup, onClick: () -> Unit) {
	val target = group.target
	val pref = LocalPreference.current
	val icon = painterResource(pref.db.iconFor(target))
	
	val secondary =
		if(target is DbTestTarget.Group) with(pref.db) { target.allUsers }.joinToString(
			separator = ", ", limit = 4
		) { it.user.name } else null
	
	ListItem(
		icon = { Icon(icon, contentDescription = null) },
		text = { Text(with(pref.db) { target.name }) },
		secondaryText = if(secondary == null) null else ({ Text(secondary) }),
		modifier = Modifier.clickable(onClick = onClick)
	)
}


private enum class ScheduleType { none, fixed, random }


/// Scheduling

private fun Navigator.showScheduleSelfTest(info: GroupInfo): Unit = showDialogAsync { removeRoute ->
	Scaffold(
		topBar = {
			TopAppBar(
				navigationIcon = {
					IconButton(onClick = removeRoute) {
						Icon(painterResource(R.drawable.ic_clear_24), contentDescription = "닫기")
					}
				},
				title = { Text("자가진단 예약") },
				backgroundColor = Color.Transparent,
				elevation = 0.dp
			)
		},
		modifier = Modifier.wrapContentHeight()
	) {
		val pref = LocalPreference.current
		val navigator = LocalNavigator
		val group = info.group
		val target = group.target
		
		Column {
			@Composable
			fun Header(text: String) {
				Text(
					text = text,
					style = MaterialTheme.typography.body1,
					modifier = Modifier.padding(vertical = 10.dp, horizontal = 22.dp)
				)
			}
			
			ListItem(
				icon = { Icon(painterResource(pref.db.iconFor(target)), contentDescription = null) }
			) {
				val text = with(pref.db) {
					when(target) {
						is DbTestTarget.Group -> "${target.name} (${target.userIds.size})"
						is DbTestTarget.Single -> target.name
					}
				}
				Text(text)
			}
			
			Spacer(Modifier.height(10.dp))
			
			Header("자가진단 예약")
			
			var type by remember {
				mutableStateOf(
					when(group.schedule) {
						DbTestSchedule.None -> ScheduleType.none
						is DbTestSchedule.Fixed -> ScheduleType.fixed
						is DbTestSchedule.Random -> ScheduleType.random
					}
				)
			}
			
			@Composable
			fun ScheduleTypeHead(targetType: ScheduleType, text: String) {
				ListItem(
					icon = { RadioButton(selected = type == targetType, onClick = null) },
					modifier = Modifier.clickable { type = targetType }
				) {
					Text(text)
				}
			}
			
			
			/// none
			ScheduleTypeHead(ScheduleType.none, "꺼짐")
			
			
			/// fixed
			ScheduleTypeHead(ScheduleType.fixed, "매일 특정 시간")
			
			var hour by remember { mutableStateOf(-1) }
			var minute by remember { mutableStateOf(0) }
			
			AnimateHeight(
				visible = type == ScheduleType.fixed,
				modifier = Modifier.padding(horizontal = 8.dp)
			) {
				TextFieldDecoration(
					label = { Text("시간 설정") },
					inputState = if(hour == -1) InputPhase.UnfocusedEmpty else InputPhase.UnfocusedNotEmpty,
					modifier = Modifier.padding(8.dp),
					innerModifier = Modifier.clickable {
					
					}
				) {
					if(hour != -1) {
						val text = buildAnnotatedString {
							append("$hour")
							withStyle(SpanStyle(color = MediumContentColor)) { append(":") }
							append("$minute")
						}
						Text(text)
					}
				}
			}
			
			
			/// random
		}
	}
}


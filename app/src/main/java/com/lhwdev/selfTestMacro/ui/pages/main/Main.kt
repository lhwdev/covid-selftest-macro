package com.lhwdev.selfTestMacro.ui.pages.main

import android.annotation.SuppressLint
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.lhwdev.selfTestMacro.R
import com.lhwdev.selfTestMacro.api.InstituteType
import com.lhwdev.selfTestMacro.database.*
import com.lhwdev.selfTestMacro.navigation.LocalNavigator
import com.lhwdev.selfTestMacro.navigation.Route
import com.lhwdev.selfTestMacro.navigation.pushRoute
import com.lhwdev.selfTestMacro.navigation.showRouteAsync
import com.lhwdev.selfTestMacro.repository.GroupInfo
import com.lhwdev.selfTestMacro.repository.LocalSelfTestManager
import com.lhwdev.selfTestMacro.ui.*
import com.lhwdev.selfTestMacro.ui.common.SimpleIconButton
import com.lhwdev.selfTestMacro.ui.icons.ExpandMore
import com.lhwdev.selfTestMacro.ui.icons.Icons
import com.lhwdev.selfTestMacro.ui.pages.edit.EditUsers
import com.lhwdev.selfTestMacro.ui.pages.info.Info
import com.lhwdev.selfTestMacro.ui.pages.setup.SetupRoute
import com.lhwdev.selfTestMacro.ui.utils.RoundButton
import com.vanpra.composematerialdialogs.MaterialDialog
import com.vanpra.composematerialdialogs.showFullDialogAsync
import kotlinx.coroutines.launch


val MainRoute: Route = Route("Main") { Main() }


@Composable
private fun Main(): Unit = Surface(color = MaterialTheme.colors.surface) {
	val navigator = LocalNavigator
	
	AutoSystemUi(enabled = true) { scrims ->
		val scaffoldState = rememberScaffoldState()
		Scaffold(
			scaffoldState = scaffoldState,
			topBar = {
				var showMoreActions by remember { mutableStateOf(false) }
				TopAppBar(
					title = { Text("코로나19 자가진단 매크로") },
					actions = {
						SimpleIconButton(
							icon = R.drawable.ic_more_vert_24, contentDescription = "옵션 더보기",
							onClick = { showMoreActions = true }
						)
						
						DropdownMenu(
							expanded = showMoreActions,
							onDismissRequest = { showMoreActions = false }
						) {
							DropdownMenuItem(onClick = {
								showMoreActions = false
								navigator.pushRoute { LogView() }
							}) {
								Text("자가진단 기록")
							}
							
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
					.padding(vertical = 28.dp, horizontal = 16.dp)
					.fillMaxSize(),
				horizontalAlignment = Alignment.CenterHorizontally
			) {
				MainContent(scaffoldState)
			}
		}
		
		scrims.navigationBar()
	}
	
}


@DrawableRes
fun DatabaseManager.iconFor(group: DbTestTarget): Int = when(group) {
	is DbTestTarget.Group -> R.drawable.ic_group_24
	is DbTestTarget.Single -> iconFor(group.user)
}

@DrawableRes
fun iconFor(user: DbUser): Int = when(user.institute.type) {
	InstituteType.school -> R.drawable.ic_school_24
	InstituteType.university -> TODO()
	InstituteType.academy -> TODO()
	InstituteType.office -> TODO()
}


@SuppressLint("ComposableNaming") // factory
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


private fun resolveNewGroup(last: DbTestGroup, groups: Collection<DbTestGroup>): DbTestGroup {
	// groups changed:
	if(last in groups) {
		return last
	} else {
		val target = last.target
		if(target is DbTestTarget.Single) {
			// case 1. group was made
			val toGroup = groups.find {
				val t = it.target
				t is DbTestTarget.Group && target.userId in t.userIds
			}
			if(toGroup != null) return toGroup
		}
		if(target is DbTestTarget.Group) {
			// case 2. group was demolished
			val one = target.userIds.firstOrNull()
			if(one != null) {
				val toSingle = groups.find {
					val t = it.target
					t is DbTestTarget.Single && t.userId == one
				}
				if(toSingle != null) return toSingle
			}
		}
		
		// case 3. last was removed
		return groups.first()
	}
}

@Composable
private fun ColumnScope.MainContent(scaffoldState: ScaffoldState) {
	val pref = LocalPreference.current
	val navigator = LocalNavigator
	val selfTestManager = LocalSelfTestManager.current
	val context = LocalContext.current
	val scope = rememberCoroutineScope()
	
	val users = pref.db.users.users
	val groups = pref.db.testGroups.groups
	
	if(users.isEmpty() || groups.isEmpty()) {
		Column(
			verticalArrangement = Arrangement.Center,
			horizontalAlignment = Alignment.CenterHorizontally,
			modifier = Modifier.fillMaxSize()
		) {
			Text("등록된 사용자가 없어요.", style = MaterialTheme.typography.h4)
			Spacer(Modifier.height(32.dp))
			RoundButton(
				onClick = {
					navigator.showRouteAsync(SetupRoute())
				},
				icon = { Icon(painterResource(R.drawable.ic_add_24), contentDescription = null) }
			) {
				Text("사용자 추가")
			}
		}
		return
	}
	
	var selectedTestGroup by remember {
		mutableStateOf(groups.getOrElse(pref.headUser) { groups.values.first() })
	}
	if(changed(groups)) selectedTestGroup = resolveNewGroup(selectedTestGroup, groups.values)
	
	val selectedGroup = GroupInfo(selectedTestGroup)
	
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
			
			if(selectedGroup.isGroup) append("${selectedGroup.group.target.allUsersCount}명")
		}
		Text("($subText)", color = LocalContentColor.current.copy(ContentAlpha.medium))
	}
	
	// test group members hint
	// '김철수'
	if(selectedGroup.isGroup) {
		Spacer(Modifier.height(4.dp))
		
		val usersText = with(pref.db) { selectedGroup.group.target.allUsers }
			.joinToString(separator = ", ", limit = 4) { it.name }
		Text(usersText, style = MaterialTheme.typography.body1)
	}
	
	Spacer(Modifier.weight(1f))
	
	
	/// center
	
	// status
	// '자가진단 상태'
	//  '모두 정상'
	//  '자세히 보기'
	val statusKey = remember { mutableStateOf(0) }
	
	when(val target = selectedGroup.group.target) {
		is DbTestTarget.Group -> {
			GroupStatusView(target, statusKey)
		}
		is DbTestTarget.Single -> {
			SingleStatusView(target, statusKey)
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
		onClick = {
			scope.launch {
				val uiContext = UiContext(
					context = context,
					navigator = navigator,
					showMessage = { message, action ->
						scaffoldState.snackbarHostState.showSnackbar(message, action)
					},
					scope = scope
				)
				
				navigator.showSubmitSelfTestNowDialog(selfTestManager, uiContext, selectedTestGroup)
				
				statusKey.value++
			}
		},
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
							SimpleIconButton(
								icon = R.drawable.ic_clear_24, contentDescription = "닫기",
								onClick = { showSelect = false }
							)
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
					for(group in groups.values) UserListItem(
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
						navigator.showFullDialogAsync { EditUsers() }
					}
				) {
					Text("편집 / 사용자 추가")
				}
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
		) { it.name } else null
	
	ListItem(
		icon = { Icon(icon, contentDescription = null) },
		text = { Text(with(pref.db) { target.name }) },
		secondaryText = if(secondary == null) null else ({ Text(secondary) }),
		modifier = Modifier.clickable(onClick = onClick)
	)
}


internal enum class ScheduleType { none, fixed, random }

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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.lhwdev.selfTestMacro.R
import com.lhwdev.selfTestMacro.api.InstituteType
import com.lhwdev.selfTestMacro.database.DatabaseManager
import com.lhwdev.selfTestMacro.database.DbTestGroup
import com.lhwdev.selfTestMacro.database.DbTestTarget
import com.lhwdev.selfTestMacro.database.allUserIds
import com.lhwdev.selfTestMacro.icons.ExpandMore
import com.lhwdev.selfTestMacro.icons.Icons
import com.lhwdev.selfTestMacro.repository.GroupInfo
import com.lhwdev.selfTestMacro.repository.MainRepository
import com.lhwdev.selfTestMacro.repository.MainRepositoryImpl
import com.lhwdev.selfTestMacro.scheduleInfo
import com.lhwdev.selfTestMacro.ui.*
import com.lhwdev.selfTestMacro.ui.pages.edit.EditUsers
import com.lhwdev.selfTestMacro.ui.pages.info.Info
import com.lhwdev.selfTestMacro.ui.pages.setup.Setup
import com.vanpra.composematerialdialogs.MaterialDialog


@Immutable
data class MainModel(val navigator: Navigator, val scaffoldState: ScaffoldState) {
	suspend fun showSnackbar(
		message: String,
		actionLabel: String? = null,
		duration: SnackbarDuration = SnackbarDuration.Short
	): SnackbarResult = scaffoldState.snackbarHostState.showSnackbar(message, actionLabel, duration)
}


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
	is DbTestTarget.Single -> when(group.user.institute.type) {
		InstituteType.school -> R.drawable.ic_school_24
		InstituteType.university -> TODO()
		InstituteType.academy -> TODO()
		InstituteType.office -> TODO()
	}
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


private fun resolveNewGroup(last: DbTestGroup, groups: List<DbTestGroup>): DbTestGroup {
	// groups changed:
	if(last in groups) {
		return last
	} else {
		val target = last.target
		if(target is DbTestTarget.Single) {
			// case 1. group was made
			val toGroup = groups.find { it.target is DbTestTarget.Group && target.userId in it.target.userIds }
			if(toGroup != null) return toGroup
		}
		if(target is DbTestTarget.Group) {
			// case 2. group was demolished
			val one = target.userIds.firstOrNull()
			if(one != null) {
				val toSingle = groups.find { it.target is DbTestTarget.Single && it.target.userId == one }
				if(toSingle != null) return toSingle
			}
		}
		
		// case 3. last was removed
		return groups.first()
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
			Text("등록된 사용자가 없어요.", style = MaterialTheme.typography.h4)
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
	if(changed(groups)) selectedTestGroup = resolveNewGroup(selectedTestGroup, groups)
	
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
			
			if(selectedGroup.isGroup) append("${selectedGroup.group.target.allUserIds.size}명")
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
			navigator.showScheduleSelfTest(repository, selectedGroup)
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

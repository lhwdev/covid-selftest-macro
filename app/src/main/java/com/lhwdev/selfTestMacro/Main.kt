package com.lhwdev.selfTestMacro

import androidx.annotation.DrawableRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.lhwdev.selfTestMacro.icons.ExpandMore
import com.lhwdev.selfTestMacro.icons.Icons
import com.vanpra.composematerialdialogs.MaterialDialog


@Preview
@Composable
fun Main() {
	// val context = LocalActivity.current
	val route = LocalRoute.current
	val pref = LocalPreference.current
	
	val users = pref.db.users.users
	
	if(users.isEmpty()) {
		route[0] = { Setup() }
		return
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
					title = { Text("코로나19 자가진단 매크르") },
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
							DropdownMenuItem(onClick = {}) {
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
					selectedGroup = selectedGroup,
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
			// TODO: as DbTestGroup
			for(group in groups) UserListItem(
				group = group,
				onClick = {
					selectedTestGroup = group
					showSelect = false
				}
			)
		}
	}
}


@DrawableRes
private fun DatabaseManager.iconFor(group: DbTestTarget): Int = when(group) {
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
			isGroup = group.target is DbTestTarget.Group
		)
	}
}


@Stable
private data class GroupInfo(
	@DrawableRes val icon: Int,
	val name: String,
	val instituteName: String?,
	val isGroup: Boolean
) {
	val subtitle: String
		get() = when {
			instituteName == null -> "그룹"
			isGroup -> "그룹, $instituteName"
			else -> instituteName
		}
	
	enum class Status { notChecked, healthy, problem, loading }
}


@OptIn(ExperimentalMaterialApi::class)
@Suppress("unused") // guard
@Composable
private fun ColumnScope.MainContent(
	selectedGroup: GroupInfo,
	showSelectingUser: () -> Unit
) {
	val pref = LocalPreference.current
	
	TextButton(
		onClick = showSelectingUser,
		shape = RoundedCornerShape(percent = 100),
		colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colors.onSurface)
	) {
		UserBadge(selectedGroup)
		Icon(imageVector = Icons.Filled.ExpandMore, contentDescription = "더보기")
	}
	
	Spacer(Modifier.weight(1f))
	
	
}


@Suppress("unused") // guard
@Composable
private fun RowScope.UserBadge(
	info: GroupInfo
) {
	val icon = painterResource(info.icon)
	Icon(icon, contentDescription = null, modifier = Modifier.padding(4.dp)) // not important
	
	Text(info.name, style = MaterialTheme.typography.body1, modifier = Modifier.padding(3.dp))
	Text(
		"(${info.instituteName})",
		style = MaterialTheme.typography.body1,
		color = LocalContentColor.current.copy(ContentAlpha.medium),
		modifier = Modifier.padding(3.dp)
	)
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun UserListItem(group: DbTestGroup, onClick: () -> Unit) {
	val pref = LocalPreference.current
	val icon = painterResource(pref.db.iconFor(group.target))
	
	ListItem(
		icon = {
			Icon(icon, contentDescription = null, modifier = Modifier.padding(4.dp))
		},
		text = {
			Text(
				with(pref.db) { group.target.name },
				style = MaterialTheme.typography.body1,
				modifier = Modifier.padding(4.dp)
			)
		},
		secondaryText = {
			Text(
				"(${with(pref.db) { group.target.commonInstitute?.name ?: "그룹" }})",
				style = MaterialTheme.typography.body1,
				color = LocalContentColor.current.copy(ContentAlpha.medium),
				modifier = Modifier.padding(4.dp)
			)
		},
		modifier = Modifier.clickable(onClick = onClick)
	)
}


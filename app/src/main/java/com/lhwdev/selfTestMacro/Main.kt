package com.lhwdev.selfTestMacro

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
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
	
	var selectedUser by remember {
		mutableStateOf(pref.db.users.users.let {
			it[pref.headUser] ?: it.values.first() // safe
		})
	}
	
	var showSelect by remember { mutableStateOf(false) }
	
	
	AutoSystemUi(enabled = true) { scrims ->
		Scaffold(
			topBar = {
				TopAppBar(
					title = { Text("코로나19 자가진단 매크르") },
					actions = {
						IconButton(onClick = { showSelect = true }) {
							Icon(
								painterResource(R.drawable.ic_more_vert_24),
								contentDescription = "옵션 더보기"
							)
						}
						
					},
					statusBarScrim = { scrims.statusBar() }
				)
			},
			modifier = Modifier.weight(1f)
		) { paddingValue ->
			Column(modifier = Modifier.padding(paddingValue)) {
				MainContent(
					selectedUser = selectedUser,
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
			for(user in users.values) UserListItem(
				user = user,
				onClick = { selectedUser = user }
			)
		}
	}
}


@OptIn(ExperimentalMaterialApi::class)
@Suppress("unused") // guard
@Composable
fun ColumnScope.MainContent(
	selectedUser: DbUser,
	showSelectingUser: () -> Unit
) {
	TextButton(
		onClick = showSelectingUser,
		shape = RoundedCornerShape(percent = 100),
		colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colors.onSurface)
	) {
		UserBadge(selectedUser)
		Icon(imageVector = Icons.Filled.ExpandMore, contentDescription = "더보기")
	}
}


@Suppress("unused") // guard
@Composable
fun RowScope.UserBadge(user: DbUser) {
	val icon = painterResource(userIconFor(user.instituteType))
	Icon(icon, contentDescription = null, modifier = Modifier.padding(4.dp)) // not important
	
	Text(user.user.name, style = MaterialTheme.typography.body1, modifier = Modifier.padding(4.dp))
	
	Text(
		"(${user.instituteName})",
		style = MaterialTheme.typography.body1,
		color = LocalContentColor.current.copy(ContentAlpha.medium),
		modifier = Modifier.padding(4.dp)
	)
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun UserListItem(user: DbUser, onClick: () -> Unit) {
	val icon = painterResource(userIconFor(user.instituteType))
	
	ListItem(
		icon = {
			Icon(icon, contentDescription = null, modifier = Modifier.padding(4.dp))
		},
		text = {
			Text(
				user.user.name,
				style = MaterialTheme.typography.body1,
				modifier = Modifier.padding(4.dp)
			)
		},
		secondaryText = {
			Text(
				"(${user.instituteName})",
				style = MaterialTheme.typography.body1,
				color = LocalContentColor.current.copy(ContentAlpha.medium),
				modifier = Modifier.padding(4.dp)
			)
		},
		modifier = Modifier.clickable(onClick = onClick)
	)
}


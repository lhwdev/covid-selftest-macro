package com.lhwdev.selfTestMacro.ui.pages.common

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.material.Icon
import androidx.compose.material.ListItem
import androidx.compose.material.Text
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import com.lhwdev.selfTestMacro.R
import com.lhwdev.selfTestMacro.database.DbTestGroup
import com.lhwdev.selfTestMacro.database.DbTestTarget
import com.lhwdev.selfTestMacro.database.DbUser
import com.lhwdev.selfTestMacro.navigation.Navigator
import com.lhwdev.selfTestMacro.repository.Status
import com.lhwdev.selfTestMacro.ui.Color
import com.lhwdev.selfTestMacro.ui.DisabledContentColor
import com.lhwdev.selfTestMacro.ui.LocalPreference
import com.lhwdev.selfTestMacro.ui.MediumContentColor
import com.lhwdev.selfTestMacro.ui.pages.main.OneUserDetail
import com.lhwdev.selfTestMacro.ui.pages.main.displayText
import com.vanpra.composematerialdialogs.*


fun Navigator.showGroupTestStatusDialog(
	group: DbTestGroup, statusKey: MutableState<Int>, allStatusBlock: () -> Map<DbUser, Status>
) = when(val target = group.target) {
	is DbTestTarget.Single -> showDialogAsync {
		val user = with(LocalPreference.current.db) { target.user }
		OneUserDetail(group, user, allStatusBlock()[user], statusKey)
	}
	is DbTestTarget.Group -> showDialogAsync {
		Title { Text("${target.name}의 자가진단 현황") }
		ListContent {
			val allStatus = allStatusBlock()
			for(user in with(LocalPreference.current.db) { target.allUsers }) {
				val status = allStatus[user]
				ListItem(
					icon = if(status != null) ({
						val icon = when(status) {
							is Status.Submitted -> if(status.suspicious == null) {
								R.drawable.ic_check_24
							} else {
								R.drawable.ic_warning_24
							}
							Status.NotSubmitted -> R.drawable.ic_clear_24
						}
						
						Icon(painterResource(icon), contentDescription = null)
					}) else null,
					// trailing = { Icon(painterResource(R.drawable.ic_arrow_right_24), contentDescription = null) },
					modifier = Modifier.clickable(enabled = status != null) {
						showDialogAsync {
							OneUserDetail(group, user, allStatusBlock().getValue(user), statusKey)
						}
					}
				) {
					Row {
						val text = buildAnnotatedString {
							append(user.name)
							
							withStyle(SpanStyle(color = MediumContentColor)) {
								append(": ")
							}
							
							when(status) {
								null -> withStyle(SpanStyle(DisabledContentColor)) { append("로딩 중") }
								is Status.Submitted -> {
									if(status.suspicious == null) withStyle(
										SpanStyle(
											color = Color(onLight = Color(0xff285db9), onDark = Color(0xffadcbff))
										)
									) {
										append("정상")
									} else withStyle(
										SpanStyle(
											color = Color(onLight = Color(0xfffd2f5f), onDark = Color(0xffffa6aa))
										)
									) {
										append(status.suspicious.displayText)
									}
									
									append(' ')
									append("(${status.time})")
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
			Button(onClick = { statusKey.value++ }) { Text("새로고침") }
			PositiveButton(onClick = requestClose)
		}
	}
}

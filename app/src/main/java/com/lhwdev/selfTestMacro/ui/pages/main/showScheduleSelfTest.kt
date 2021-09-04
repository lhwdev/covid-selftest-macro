package com.lhwdev.selfTestMacro.ui.pages.main

import android.app.TimePickerDialog
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
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.lhwdev.selfTestMacro.R
import com.lhwdev.selfTestMacro.database.DbTestSchedule
import com.lhwdev.selfTestMacro.database.DbTestTarget
import com.lhwdev.selfTestMacro.repository.GroupInfo
import com.lhwdev.selfTestMacro.repository.LocalSelfTestManager
import com.lhwdev.selfTestMacro.showToast
import com.lhwdev.selfTestMacro.ui.*
import com.vanpra.composematerialdialogs.Buttons
import com.vanpra.composematerialdialogs.showDialogAsync
import kotlinx.coroutines.launch


internal fun Navigator.showScheduleSelfTest(
	info: GroupInfo
): Unit = showDialogAsync(
	maxHeight = Dp.Unspecified
) { removeRoute ->
	val pref = LocalPreference.current
	val context = LocalContext.current
	val selfTestManager = LocalSelfTestManager.current
	val scope = rememberCoroutineScope()
	
	val group = info.group
	val target = group.target
	
	
	var type by remember {
		mutableStateOf(
			when(group.schedule) {
				DbTestSchedule.None -> ScheduleType.none
				is DbTestSchedule.Fixed -> ScheduleType.fixed
				is DbTestSchedule.Random -> ScheduleType.random
			}
		)
	}
	
	var hour by remember {
		mutableStateOf((group.schedule as? DbTestSchedule.Fixed)?.hour ?: -1)
	}
	var minute by remember {
		mutableStateOf((group.schedule as? DbTestSchedule.Fixed)?.minute ?: 0)
	}
	
	val random = group.schedule as? DbTestSchedule.Random
	var fromHour by remember { mutableStateOf(random?.from?.hour ?: -1) }
	var fromMinute by remember { mutableStateOf(random?.from?.minute ?: 0) }
	var toHour by remember { mutableStateOf(random?.to?.hour ?: -1) }
	var toMinute by remember { mutableStateOf(random?.to?.minute ?: 0) }
	
	var excludeWeekend by remember { mutableStateOf(group.excludeWeekend) }
	
	
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
	Divider()
	
	Column(
		modifier = Modifier
			.verticalScroll(rememberScrollState())
			.weight(1f, fill = false)
	) {
		@Composable
		fun Header(text: String) {
			Text(
				text = text,
				style = MaterialTheme.typography.subtitle1,
				modifier = Modifier.padding(top = 12.dp, bottom = 8.dp, start = 20.dp)
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
		
		Spacer(Modifier.height(12.dp))
		
		Header("자가진단 예약")
		
		@Composable
		fun ScheduleTypeHead(targetType: ScheduleType, text: String) {
			ListItem(
				icon = { RadioButton(selected = type == targetType, onClick = null) },
				modifier = Modifier.clickable { type = targetType }
			) {
				Text(text)
			}
		}
		
		@Composable
		fun ScheduleTime(
			label: String,
			hour: Int,
			minute: Int,
			setTime: (hour: Int, minute: Int) -> Unit,
			modifier: Modifier = Modifier
		) {
			TextFieldDecoration(
				label = { Text(label) },
				inputState = if(hour == -1) InputPhase.UnfocusedEmpty else InputPhase.UnfocusedNotEmpty,
				modifier = modifier.padding(8.dp),
				innerModifier = Modifier.clickable {
					val dialog = TimePickerDialog(
						context,
						R.style.AppTheme_Dialog,
						{ _, h, m ->
							setTime(h, m)
						},
						hour.coerceAtLeast(0), minute, false
					)
					dialog.show()
				}
			) {
				if(hour != -1) {
					val text = buildAnnotatedString {
						append(if(hour == 0) "12" else "$hour")
						withStyle(SpanStyle(color = MediumContentColor)) { append(":") }
						append("$minute")
					}
					Text(text)
				}
			}
		}
		
		
		/// none
		ScheduleTypeHead(ScheduleType.none, "꺼짐")
		
		/// fixed
		ScheduleTypeHead(ScheduleType.fixed, "매일 특정 시간")
		
		AnimateHeight(
			visible = type == ScheduleType.fixed,
			modifier = Modifier.padding(horizontal = 16.dp)
		) {
			ScheduleTime(label = "시간 설정", hour = hour, minute = minute, setTime = { h, m ->
				hour = h
				minute = m
			})
		}
		
		
		/// random
		ScheduleTypeHead(ScheduleType.random, "렌덤 시간")
		
		AnimateHeight(
			visible = type == ScheduleType.random,
			modifier = Modifier.padding(horizontal = 16.dp)
		) {
			Column {
				Text(
					"시작 시간과 끝 시간 사이 임의의 시간에 자가진단을 합니다.",
					style = MaterialTheme.typography.body2,
					modifier = Modifier.padding(8.dp)
				)
				
				Row(verticalAlignment = Alignment.CenterVertically) {
					ScheduleTime(
						label = "범위 시작",
						hour = fromHour, minute = fromMinute,
						modifier = Modifier.weight(1f),
						setTime = { h, m ->
							fromHour = h
							fromMinute = m
						}
					)
					
					Text("~", style = MaterialTheme.typography.body1)
					
					ScheduleTime(
						label = "범위 끝",
						hour = toHour, minute = toMinute,
						modifier = Modifier.weight(1f),
						setTime = { h, m ->
							toHour = h
							toMinute = m
						}
					)
				}
				
				Spacer(Modifier.height(8.dp))
			}
		}
	}
	
	Spacer(Modifier.height(4.dp))
	
	Buttons {
		PositiveButton(onClick = submit@{
			val schedule = when(type) {
				ScheduleType.none -> DbTestSchedule.None
				ScheduleType.fixed -> {
					if(hour == -1) {
						context.showToast("시간을 선택해주세요.")
						return@submit
					}
					DbTestSchedule.Fixed(hour = hour, minute = minute)
				}
				ScheduleType.random -> {
					if(fromHour == -1 || toHour == -1) {
						context.showToast("시간을 선택해주세요.")
						return@submit
					}
					DbTestSchedule.Random(
						from = DbTestSchedule.Fixed(hour = fromHour, minute = fromMinute),
						to = DbTestSchedule.Fixed(hour = toHour, minute = toMinute)
					)
				}
			}
			scope.launch {
				selfTestManager.updateSchedule(group, schedule)
				
				removeRoute()
			}
		}) { Text("확인") }
		NegativeButton { Text("취소") }
	}
}

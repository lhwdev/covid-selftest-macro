package com.lhwdev.selfTestMacro.repository

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import androidx.compose.foundation.clickable
import androidx.compose.material.ListItem
import androidx.compose.material.Text
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.core.content.getSystemService
import com.lhwdev.fetch.http.Session
import com.lhwdev.selfTestMacro.api.*
import com.lhwdev.selfTestMacro.database.*
import com.lhwdev.selfTestMacro.onError
import com.lhwdev.selfTestMacro.selfLog
import com.lhwdev.selfTestMacro.ui.Color
import com.lhwdev.selfTestMacro.ui.pages.main.MainModel
import com.vanpra.composematerialdialogs.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.launch
import kotlin.collections.List
import kotlin.collections.all
import kotlin.collections.emptyList
import kotlin.collections.map
import kotlin.collections.set


val Context.isNetworkAvailable: Boolean
	get() {
		val service = getSystemService<ConnectivityManager>() ?: return true // assume true
		return if(Build.VERSION.SDK_INT >= 23) {
			service.getNetworkCapabilities(service.activeNetwork)
				?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
		} else {
			@Suppress("DEPRECATION") // Deprecated in Api level 23
			service.activeNetworkInfo?.isConnectedOrConnecting == true
		}
	}


class MainRepositoryImpl(
	val pref: PreferenceState
) : MainRepository {
	override suspend fun sessionFor(group: DbUserGroup): Session {
		val info = SessionManager.sessionInfoFor(database = pref.db, group = group)
		if(!info.sessionFullyLoaded) try {
			val result = info.session.validatePassword(group.institute, group.usersIdentifier.token, group.password)
			if(result is UsersToken) {
				pref.db.apiLoginCache[group.usersIdentifier.token] = result
				info.sessionFullyLoaded = true
			}
		} catch(th: Throwable) {
			onError(th, "MainRepositoryImpl.sessionFor")
		}
		
		return info.session
	}
	
	override suspend fun getCurrentStatus(user: DbUser): Status? = with(pref.db) {
		try {
			val session = sessionFor(user.userGroup)
			Status(session.getUserInfo(user.usersInstitute, user.apiUser(session)!!))
		} catch(th: Throwable) {
			selfLog("getCurrentStatus: error")
			th.printStackTrace()
			null
		}
	}
	
	// TODO: separate random time for those in one group
	@OptIn(DangerousHcsApi::class)
	private suspend fun DatabaseManager.submitSelfTest(
		target: DbTestTarget,
		surveyData: SurveyData
	): List<SubmitResult> = try {
		target.allUsers.map { user ->
			try {
				val session = sessionFor(user.userGroup)
				val data = session.registerSurvey(
					institute = user.usersInstitute,
					user = user.apiUser(session)!!,
					surveyData = surveyData
				)
				SubmitResult.Success(user, data.registerAt)
			} catch(th: Throwable) {
				SubmitResult.Failed(user, "자가진단에 실패했습니다.", th)
			}
		}
		
	} catch(th: Throwable) {
		emptyList()
	}
	
	override suspend fun Context.submitSelfTestNow(
		manager: DatabaseManager,
		model: MainModel,
		target: DbTestTarget,
		surveyData: SurveyData
	): List<SubmitResult> {
		return try {
			if(!isNetworkAvailable) {
				CoroutineScope(currentCoroutineContext()).launch {
					model.showSnackbar("네트워크에 연결되어 있지 않습니다.", actionLabel = "확인")
				}
				return emptyList()
			}
			val result = manager.submitSelfTest(target, surveyData)
			if(result.isEmpty()) return result
			
			if(result.all { it is SubmitResult.Success }) CoroutineScope(currentCoroutineContext()).launch {
				model.showSnackbar(
					if(target is DbTestTarget.Group) "모두 자가진단을 완료했습니다." else "자가진단을 완료했습니다.",
					actionLabel = "확인"
				)
			} else model.navigator.showDialogUnit {
				Title { Text("자가진단 실패") }
				
				ListContent {
					for(resultItem in result) when(resultItem) {
						is SubmitResult.Success -> ListItem {
							Text(
								"${resultItem.target.name}: 성공",
								color = Color(
									onLight = Color(0xf4259644),
									onDark = Color(0xff99ffa0)
								)
							)
						}
						
						is SubmitResult.Failed -> ListItem(
							modifier = Modifier.clickable {
								model.navigator.showDialogAsync {
									Title { Text("${resultItem.target.name} (${resultItem.target.institute.name}): ${resultItem.message}") }
									
									Content {
										val stackTrace = remember(resultItem.error) {
											resultItem.error.stackTraceToString()
										}
										Text(stackTrace)
									}
								}
							}
						) {
							Text(
								"${resultItem.target.name}: 실패",
								color = Color(
									onLight = Color(0xffff1122),
									onDark = Color(0xffff9099)
								)
							)
						}
					}
				}
				
				Buttons { PositiveButton { Text("확인") } }
			}
			result
		} catch(th: Throwable) {
			emptyList()
		}
	}
	
	override suspend fun scheduleSelfTest(group: DbTestGroup, newSchedule: DbTestSchedule) {
		pref.db.updateSchedule(group, newSchedule)
	}
}

fun Status(info: UserInfo): Status = when {
	info.isHealthy != null && info.lastRegisterAt != null ->
		Status.Submitted(info.isHealthy!!, formatRegisterTime(info.lastRegisterAt!!))
	else -> Status.NotSubmitted
}


private fun formatRegisterTime(time: String): String = time.substring(0, time.lastIndexOf('.'))

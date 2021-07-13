package com.lhwdev.selfTestMacro.model

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
import com.lhwdev.selfTestMacro.*
import com.lhwdev.selfTestMacro.api.SurveyData
import com.lhwdev.selfTestMacro.api.UserInfo
import com.lhwdev.selfTestMacro.api.getUserInfo
import com.lhwdev.selfTestMacro.api.registerSurvey
import com.vanpra.composematerialdialogs.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.launch


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
	override suspend fun getCurrentStatus(user: DbUser): Status = with(pref.db) {
		Status(getUserInfo(user.institute, user.user))
	}
	
	private suspend fun DatabaseManager.submitSelfTest(
		target: DbTestTarget,
		surveyData: SurveyData
	): List<SubmitResult> {
		return target.allUsers.map { user ->
			try {
				val data = registerSurvey(
					institute = user.institute,
					user = user.user,
					surveyData = surveyData
				)
				SubmitResult.Success(user, data.registerAt)
			} catch(th: Throwable) {
				SubmitResult.Failed(user, "자가진단에 실패했습니다.", th)
			}
		}
		
	}
	
	override suspend fun Context.submitSelfTestNow(
		manager: DatabaseManager,
		model: MainModel,
		target: DbTestTarget,
		surveyData: SurveyData
	): List<SubmitResult> {
		if(!isNetworkAvailable) {
			CoroutineScope(currentCoroutineContext()).launch {
				model.showSnackbar("네트워크에 연결되어 있지 않습니다.", actionLabel = "확인")
			}
			return emptyList()
		}
		val result = manager.submitSelfTest(target, surveyData)
		if(result.all { it is SubmitResult.Success }) CoroutineScope(currentCoroutineContext()).launch {
			model.showSnackbar(
				if(target is DbTestTarget.Group) "모두 자가진단을 완료했습니다." else "자가진단을 완료했습니다.",
				actionLabel = "확인"
			)
		} else model.navigator.showRouteUnit { removeRoute ->
			MaterialDialog(onCloseRequest = removeRoute) {
				Title { Text("자가진단 실패") }
				
				ListContent {
					for(resultItem in result) when(resultItem) {
						is SubmitResult.Success -> ListItem {
							Text(
								"${resultItem.target.user.name}: 성공", color = Color(
									onLight = Color(0xf4259644),
									onDark = Color(0xff99ffa0)
								)
							)
						}
						
						is SubmitResult.Failed -> ListItem(
							modifier = Modifier.clickable {
								model.navigator.showRouteAsync { removeRoute ->
									MaterialDialog(onCloseRequest = removeRoute) {
										Title { Text("${resultItem.target.user.name} (${resultItem.target.instituteName}): ${resultItem.message}") }
										
										Content {
											val stackTrace = remember(resultItem.error) {
												resultItem.error.stackTraceToString()
											}
											Text(stackTrace)
										}
									}
								}
							}
						) {
							Text("${resultItem.target.user.name}: 실패", color = Color(
								onLight = Color(0xffff1122),
								onDark = Color(0xffff9099)
							))
						}
					}
				}
				
				Buttons { PositiveButton { Text("확인") } }
			}
		}
		return result
	}
	
	override suspend fun scheduleSelfTest(group: DbTestGroup) {
		TODO("Not yet implemented")
	}
}

fun Status(info: UserInfo): Status = when {
	info.isHealthy != null && info.lastRegisterAt != null ->
		Status.Submitted(info.isHealthy!!, formatRegisterTime(info.lastRegisterAt!!))
	else -> Status.NotSubmitted
}


private fun formatRegisterTime(time: String): String = time.substring(0, time.lastIndexOf('.'))

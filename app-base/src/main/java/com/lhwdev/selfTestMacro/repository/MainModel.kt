package com.lhwdev.selfTestMacro.repository

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import androidx.annotation.DrawableRes
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.core.content.getSystemService
import com.lhwdev.selfTestMacro.api.QuickTestResult
import com.lhwdev.selfTestMacro.api.UserInfo
import com.lhwdev.selfTestMacro.database.DbTestGroup
import com.lhwdev.selfTestMacro.database.DbTestTarget
import com.lhwdev.selfTestMacro.database.DbUser
import com.lhwdev.selfTestMacro.debug.DiagnosticItem
import com.lhwdev.selfTestMacro.debug.DiagnosticObject
import com.lhwdev.selfTestMacro.debug.dumpLocalized


@Immutable
sealed class SubmitResult(val target: DbUser) {
	class Success(target: DbUser, val at: String) : SubmitResult(target)
	class Failed(
		target: DbUser,
		val causes: Set<HcsAppError.ErrorCause>,
		val diagnostic: SelfTestDiagnosticInfo,
		val cause: Throwable?
	) : SubmitResult(target), DiagnosticObject {
		override fun getDiagnosticInformation(): DiagnosticItem = diagnostic
		
		val description =
			"자가진단 실패(추정 이유: ${causes.joinToString { it.description }}, 진단정보: ${diagnostic.dumpLocalized(oneLine = true)})"
	}
	
	enum class ErrorCategory {
		flag, network, bug
	}
}


@Stable
data class GroupInfo(
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


enum class SuspiciousKind(val displayName: String) { symptom("의심증상 있음"), quarantined("자가격리함") }

@Immutable
sealed class Status {
	data class Submitted(
		val suspicious: SuspiciousKind?,
		val time: String,
		
		val questionSuspicious: Boolean?,
		val questionQuickTestResult: QuickTestResult?,
		val questionWaitingResult: Boolean?,
		val questionQuarantined: Boolean?,
		val questionHousemateInfected: Boolean?
	) : Status()
	
	object NotSubmitted : Status()
}

@Immutable
data class GroupStatus(val notSubmittedCount: Int, val symptom: List<DbUser>, val quarantined: List<DbUser>)


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

val UserInfo.suspiciousKind: SuspiciousKind?
	get() = when {
		isHealthy == true -> null
		questionSuspicious == true -> SuspiciousKind.symptom
		questionWaitingResult == true ||
			questionQuarantined == true ||
			questionHouseholdInfected == true -> SuspiciousKind.quarantined
		else -> null
	}

fun Status(info: UserInfo): Status = when {
	info.isHealthy != null && info.lastRegisterAt != null ->
		Status.Submitted(
			suspicious = info.suspiciousKind,
			time = formatRegisterTime(info.lastRegisterAt!!),
			
			questionSuspicious = info.questionSuspicious,
			questionQuickTestResult = info.questionQuickTestResult,
			questionWaitingResult = info.questionWaitingResult,
			questionQuarantined = info.questionQuarantined,
			questionHousemateInfected = info.questionHouseholdInfected
		)
	else -> Status.NotSubmitted
}


private fun formatRegisterTime(time: String): String = time.substring(0, time.lastIndexOf('.'))

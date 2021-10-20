package com.lhwdev.selfTestMacro.repository

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import androidx.annotation.DrawableRes
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.core.content.getSystemService
import com.lhwdev.selfTestMacro.api.UserInfo
import com.lhwdev.selfTestMacro.database.DbTestGroup
import com.lhwdev.selfTestMacro.database.DbTestTarget
import com.lhwdev.selfTestMacro.database.DbUser


@Immutable
sealed class SubmitResult(val target: DbUser) {
	class Success(target: DbUser, val at: String) : SubmitResult(target)
	class Failed(target: DbUser, val cause: Set<ErrorCause>, val diagnostic: SelfTestDiagnosticInfo) :
		SubmitResult(target)
	
	enum class ErrorCategory {
		flag, network, bug
	}
	
	enum class ErrorCause(
		val description: String,
		val detail: String?,
		val parent: ErrorCause? = null,
		val sure: ErrorCause? = null,
		val category: ErrorCategory
	) {
		
		repeated(description = "반복해서 일어난 오류", detail = null, category = ErrorCategory.flag),
		
		noNetwork(
			description = "네트워크 연결 없음",
			detail = """
				네트워크에 연결되어 있지 않습니다.
				와이파이나 데이터 네트워크가 켜져있는지 확인하시고, 데이터만 켜져있을 경우 백그라운드 데이터 사용 제한을 해제하셨는지 확인해주세요.
			""".trimIndent(),
			category = ErrorCategory.network
		),
		
		appBug(
			description = "앱 자체 버그",
			detail = """
				자가진단 매크로 앱의 버그입니다.
				오류정보를 복사해서 개발자에게 제보해주신다면 감사하겠습니다.
			""".trimIndent(),
			category = ErrorCategory.bug
		),
		probableAppBug(
			description = "앱 자체 버그(?)",
			detail = """
				자가진단 매크로 앱의 버그일 수도 있습니다.
				만약 버그라고 생각되신다면 오류정보를 복사해서 개발자에게 제보해주신다면 감사하겠습니다.
			""".trimIndent(),
			sure = appBug,
			category = ErrorCategory.bug
		),
		
		apiChange(
			description = "교육청 건강상태 자가진단의 내부 구조 변화(?)",
			detail = """
				교육청의 건강상태 자가진단 사이트 내부구조가 바뀌었을 가능성이 있습니다.
				버그인 것 같다면 개발자에게 제보해주세요.
			""".trimIndent(),
			parent = appBug,
			category = ErrorCategory.bug
		),
		probableApiChange(
			description = "교육청 건강상태 자가진단의 내부 구조 변화",
			detail = """
				교육청의 건강상태 자가진단 사이트 내부구조가 바뀌었습니다.
				가능하다면 개발자에게 제보해주세요.
			""".trimIndent(),
			sure = apiChange,
			category = ErrorCategory.bug
		),
		
		unresponsiveNetwork(
			description = "네트워크 불안정",
			detail = """
				네트워크(와이파이, 데이터 네트워크 등)에 연결되어 있지만 인터넷에 연결할 수 없습니다.
				네트워크 연결을 다시 확인해주세요.
			""".trimIndent(),
			category = ErrorCategory.network
		),
		
		hcsUnreachable(
			description = "자가진단 사이트 접근 불가",
			detail = """
				네트워크에 연결되어 있고 인터넷에 연결할 수 있지만, 자가진단 사이트에 연결할 수 없습니다.
				교육청 건강상태 자가진단 서버가 순간적으로 불안정해서 일어났을 수도 있습니다.
				공식 자가진단 사이트나 앱에 들어가서 작동하는지 확인하고, 작동하는데도 이 에러가 뜬다면 버그를 제보해주세요.
			""".trimIndent(),
			category = ErrorCategory.network
		),
		
		vpn(
			description = "VPN 사용 중..?",
			detail = """
				VPN을 사용하고 있다면 VPN을 끄고 다시 시도해보세요.
				자가진단 서버는 해외에서 오는 연결을 싸그리 차단해버린답니다.
			""".trimIndent(),
			category = ErrorCategory.network
		)
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

enum class SuspiciousKind(val displayName: String) { symptom("유증상 있음"), quarantined("자가격리함") }

@Immutable
sealed class Status {
	data class Submitted(
		val suspicious: SuspiciousKind?,
		val time: String,
		
		val questionSuspicious: Boolean?,
		val questionWaitingResult: Boolean?,
		val questionQuarantined: Boolean?
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
		questionWaitingResult == true || questionQuarantined == true -> SuspiciousKind.quarantined
		else -> null
	}

fun Status(info: UserInfo): Status = when {
	info.isHealthy != null && info.lastRegisterAt != null ->
		Status.Submitted(
			suspicious = info.suspiciousKind,
			time = formatRegisterTime(info.lastRegisterAt!!),
			
			questionSuspicious = info.questionSuspicious,
			questionWaitingResult = info.questionWaitingResult,
			questionQuarantined = info.questionQuarantined
		)
	else -> Status.NotSubmitted
}


private fun formatRegisterTime(time: String): String = time.substring(0, time.lastIndexOf('.'))

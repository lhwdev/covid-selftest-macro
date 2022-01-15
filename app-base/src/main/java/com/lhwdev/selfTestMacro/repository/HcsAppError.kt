package com.lhwdev.selfTestMacro.repository

import com.lhwdev.selfTestMacro.debug.DiagnosticItem
import com.lhwdev.selfTestMacro.debug.DiagnosticObject
import com.lhwdev.selfTestMacro.debug.dumpLocalized

class HcsAppError(
	message: String,
	val isSerious: Boolean,
	val causes: Set<ErrorCause>,
	val target: Any? = null,
	val diagnosticItem: SelfTestDiagnosticInfo,
	cause: Throwable? = null
) : RuntimeException(null, cause), DiagnosticObject {
	override fun getDiagnosticInformation(): DiagnosticItem = diagnosticItem
	private val mMessage = message
	private var cachedMessage: String? = null
	
	private fun createMessage() = buildString {
		append(mMessage)
		
		if(target != null) {
			append(" -> ")
			append(target)
		}
		
		append('\n')
		append("원인: ")
		causes.joinTo(this) { it.description }
		
		append("\n")
		append("진단 정보: ")
		diagnosticItem.dumpLocalized(oneLine = true)
	}
	
	override val message: String
		get() = cachedMessage ?: run {
			val m = createMessage()
			cachedMessage = m
			m
		}
	
	enum class ErrorCause(
		val description: String,
		val detail: String?,
		val parent: ErrorCause? = null,
		val sure: ErrorCause? = null,
		val category: SubmitResult.ErrorCategory
	) {
		repeated(description = "반복해서 일어난 오류", detail = null, category = SubmitResult.ErrorCategory.flag),
		
		noNetwork(
			description = "네트워크 연결 없음",
			detail = """
				네트워크에 연결되어 있지 않습니다.
				와이파이나 데이터 네트워크가 켜져있는지 확인하시고, 데이터만 켜져있을 경우 백그라운드 데이터 사용 제한을 해제하셨는지 확인해주세요.
			""".trimIndent(),
			category = SubmitResult.ErrorCategory.network
		),
		
		appBug(
			description = "앱 자체 버그",
			detail = """
				자가진단 매크로 앱의 버그입니다.
				오류정보를 복사해서 개발자에게 제보해주신다면 감사하겠습니다.
			""".trimIndent(),
			category = SubmitResult.ErrorCategory.bug
		),
		probableAppBug(
			description = "앱 자체 버그(?)",
			detail = """
				자가진단 매크로 앱의 버그일 수도 있습니다.
				만약 버그라고 생각되신다면 오류정보를 복사해서 개발자에게 제보해주신다면 감사하겠습니다.
			""".trimIndent(),
			sure = appBug,
			category = SubmitResult.ErrorCategory.bug
		),
		
		apiChange(
			description = "교육청 건강상태 자가진단의 내부 구조 변화",
			detail = """
				교육청의 건강상태 자가진단 사이트 내부구조가 바뀌었습니다.
				가능하다면 개발자에게 제보해주세요.
			""".trimIndent(),
			parent = appBug,
			category = SubmitResult.ErrorCategory.bug
		),
		probableApiChange(
			description = "교육청 건강상태 자가진단의 내부 구조 변화(?)",
			detail = """
				교육청의 건강상태 자가진단 사이트 내부구조가 바뀌었을 수 있습니다.
				버그인 것 같다면 개발자에게 제보해주세요.
			""".trimIndent(),
			sure = apiChange,
			category = SubmitResult.ErrorCategory.bug
		),
		
		unresponsiveNetwork(
			description = "네트워크 불안정",
			detail = """
				네트워크(와이파이, 데이터 네트워크 등)에 연결되어 있지만 인터넷에 연결할 수 없습니다.
				네트워크 연결을 다시 확인해주세요.
			""".trimIndent(),
			category = SubmitResult.ErrorCategory.network
		),
		
		hcsUnreachable(
			description = "자가진단 사이트 접근 불가",
			detail = """
				네트워크에 연결되어 있고 인터넷에 연결할 수 있지만, 자가진단 사이트에 연결할 수 없습니다.
				교육청 건강상태 자가진단 서버가 순간적으로 불안정해서 일어났을 수도 있습니다.
				공식 자가진단 사이트나 앱에 들어가서 작동하는지 확인하고, 작동하는데도 이 에러가 뜬다면 버그를 제보해주세요.
			""".trimIndent(),
			category = SubmitResult.ErrorCategory.network
		),
		
		vpn(
			description = "VPN 사용 중..?",
			detail = """
				VPN을 사용하고 있다면 VPN을 끄고 다시 시도해보세요.
				자가진단 서버는 해외에서 오는 연결을 싸그리 차단해버린답니다.
			""".trimIndent(),
			category = SubmitResult.ErrorCategory.network
		);
		
		
		sealed class Action {
			class Notice(val message: String) : Action()
		}
	}
}

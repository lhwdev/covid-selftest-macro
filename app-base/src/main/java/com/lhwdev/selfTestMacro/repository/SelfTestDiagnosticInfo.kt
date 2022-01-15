package com.lhwdev.selfTestMacro.repository

import com.lhwdev.selfTestMacro.android.utils.NetworkCommonImpl
import com.lhwdev.selfTestMacro.debug.DiagnosticItem
import com.lhwdev.selfTestMacro.debug.DiagnosticItemGroup
import com.lhwdev.selfTestMacro.debug.diagnosticElements


class SelfTestDiagnosticInfo(var extraInfo: String? = null) : DiagnosticItemGroup {
	var networkInfo: NetworkCommonImpl? = null
	var networkResponsive = true
	var hcsAccessible = true
	
	
	override val name: String get() = "SelfTestDiagnosticInfo"
	override val localizedName: String get() = "자가진단 진단 정보"
	
	override val children: List<DiagnosticItem>
		get() = diagnosticElements {
			"networkInfo" set networkInfo localized "네트워크 정보" localizeData { it.name ?: "(없음)" }
			"networkResponsive" set networkResponsive localized "네트워크 사용 가능 여부"
			"hcsAccessible" set hcsAccessible localized "자가진단 사이트 접근 가능 여부"
		}
}

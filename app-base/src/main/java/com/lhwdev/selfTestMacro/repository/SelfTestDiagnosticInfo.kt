package com.lhwdev.selfTestMacro.repository

import com.lhwdev.selfTestMacro.android.utils.NetworkCommonImpl
import com.lhwdev.selfTestMacro.debug.DiagnosticItem
import com.lhwdev.selfTestMacro.debug.DiagnosticItemGroup
import com.lhwdev.selfTestMacro.debug.diagnosticElements


class SelfTestDiagnosticInfo : DiagnosticItemGroup {
	var networkInfo: NetworkCommonImpl? = null
	var networkResponsive = true
	var hcsAccessible = true
	
	
	override val name: String get() = "SelfTestDiagnosticInfo"
	
	override val children: List<DiagnosticItem>
		get() = diagnosticElements {
			"networkInfo" set networkInfo
			"networkResponsive" set networkResponsive
			"hcsAccessible" set hcsAccessible
		}
}

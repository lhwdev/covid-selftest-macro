package com.lhwdev.selfTestMacro.ui

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import com.lhwdev.selfTestMacro.api.QuickTestResult as QuickTestResultData


private val yesNoText = mapOf(false to "없음", true to "있음")


@Serializable
sealed class SelfTestQuestions<T>(
	val title: String,
	val content: String,
	val displayTexts: Map<T, String>
) {
	companion object {
		val all = listOf(Suspicious, QuickTestResult, WaitingResult)
	}
	
	
	fun displayText(value: T): String = displayTexts.getValue(value)
	
	
	@Serializable
	@SerialName("suspicious")
	object Suspicious : SelfTestQuestions<Boolean>(
		title = "감염 의심증상",
		content = "본인이 코로나19 감염에 의심되는 임상증상(발열(37.5℃), 기침, 호흡곤란, 오한, 근육통, 두통, 인후통, 후각·미각소실)이 있나요?",
		displayTexts = yesNoText
	)
	
	@Serializable
	@SerialName("quickTestResult")
	object QuickTestResult : SelfTestQuestions<QuickTestResultData>(
		title = "신속항원검사 결과",
		content = "오늘(어제 저녁 포함) 신속항원검사(자가진단)를 실시했나요?",
		displayTexts = enumValues<QuickTestResultData>().associateWith { it.displayLabel }
	)
	
	@Serializable
	@SerialName("waitingResult")
	object WaitingResult : SelfTestQuestions<Boolean>(
		title = "본인 또는 동거인의 PCR 검사 여부",
		content = "본인 또는 동거인이 PCR 검사를 받고 그 결과를 기다리고 있나요?",
		displayTexts = yesNoText
	)
}

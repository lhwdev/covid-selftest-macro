package com.lhwdev.selfTestMacro.ui.pages.main

import com.lhwdev.selfTestMacro.database.DbTestGroup
import com.lhwdev.selfTestMacro.navigation.Navigator
import com.lhwdev.selfTestMacro.repository.SelfTestManager
import com.lhwdev.selfTestMacro.ui.UiContext


suspend fun Navigator.showSubmitSelfTestNowDialog(
	selfTestManager: SelfTestManager,
	uiContext: UiContext,
	group: DbTestGroup
) {
	// WIP
	// selfTestManager.submitSelfTestNow(
	// 	uiContext = uiContext,
	// 	group = group
	// )
}

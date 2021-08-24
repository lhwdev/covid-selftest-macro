package com.lhwdev.selfTestMacro.repository

import android.content.Context
import com.lhwdev.fetch.http.Session
import com.lhwdev.selfTestMacro.api.SurveyData
import com.lhwdev.selfTestMacro.database.*


interface SelfTestManager {
	suspend fun sessionFor(group: DbUserGroup): Session
	
	suspend fun getCurrentStatus(user: DbUser): Status?
	
	
	suspend fun Context.submitSelfTestNow(
		manager: DatabaseManager,
		target: DbTestTarget,
		surveyData: SurveyData
	): List<SubmitResult>
	
	fun updateSchedule(target: DbTestGroup, schedule: DbTestSchedule)
	fun onScheduleUpdated(database: DatabaseManager)
}

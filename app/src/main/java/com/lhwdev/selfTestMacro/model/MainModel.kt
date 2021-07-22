package com.lhwdev.selfTestMacro.model

import android.content.Context
import androidx.annotation.DrawableRes
import androidx.compose.material.ScaffoldState
import androidx.compose.material.SnackbarDuration
import androidx.compose.material.SnackbarResult
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import com.lhwdev.selfTestMacro.*
import com.lhwdev.selfTestMacro.api.SurveyData


@Immutable
data class MainModel(val navigator: Navigator, val scaffoldState: ScaffoldState) {
	suspend fun showSnackbar(
		message: String,
		actionLabel: String? = null,
		duration: SnackbarDuration = SnackbarDuration.Short
	): SnackbarResult = scaffoldState.snackbarHostState.showSnackbar(message, actionLabel, duration)
}

@Immutable
sealed class SubmitResult(val target: DbUser) {
	class Success(target: DbUser, val at: String) : SubmitResult(target)
	class Failed(target: DbUser, val message: String, val error: Throwable) : SubmitResult(target)
}


@Immutable
interface MainRepository {
	suspend fun getCurrentStatus(user: DbUser): Status
	
	suspend fun Context.submitSelfTestNow(
		manager: DatabaseManager,
		model: MainModel,
		target: DbTestTarget,
		surveyData: SurveyData
	): List<SubmitResult>
	
	suspend fun scheduleSelfTest(group: DbTestGroup)
}


@Stable
internal data class GroupInfo(
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

@Immutable
sealed class Status {
	data class Submitted(val isHealthy: Boolean, val time: String) : Status()
	object NotSubmitted : Status()
}

@Immutable
internal data class GroupStatus(val notSubmittedCount: Int, val suspicious: List<DbUser>)

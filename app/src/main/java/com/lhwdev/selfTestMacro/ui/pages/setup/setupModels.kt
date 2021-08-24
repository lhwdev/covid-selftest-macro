package com.lhwdev.selfTestMacro.ui.pages.setup

import android.content.Context
import androidx.compose.material.*
import androidx.compose.runtime.*
import com.lhwdev.selfTestMacro.api.*
import com.lhwdev.selfTestMacro.database.DbTestGroup
import com.lhwdev.selfTestMacro.onError


@Immutable
data class SetupParameters(
	val targetTestGroup: DbTestGroup? = null,
	val endRoute: (() -> Unit)? = null
) {
	companion object {
		val Default = SetupParameters()
	}
}


@Stable
internal class SetupModel {
	var scaffoldState = ScaffoldState(DrawerState(DrawerValue.Closed), SnackbarHostState())
	
	var instituteInfo by mutableStateOf<InstituteInfoModel?>(null)
	
	var userName by mutableStateOf("")
	var userBirth by mutableStateOf("")
	
	val userList = mutableStateListOf<WizardUser>()
	
	suspend inline fun showSnackbar(
		message: String,
		actionLabel: String? = null,
		duration: SnackbarDuration = SnackbarDuration.Short
	): SnackbarResult = scaffoldState.snackbarHostState.showSnackbar(message, actionLabel, duration)
	
	suspend fun onError(context: Context, message: String, throwable: Throwable) {
		context.onError(scaffoldState.snackbarHostState, message, throwable)
	}
}

@Immutable
internal data class WizardUser(val user: User, val info: UserInfo, val master: MasterUser)

@Immutable
internal data class MasterUser(
	val identifier: UsersIdentifier,
	val birth: String,
	val password: String,
	val instituteInfo: InstituteInfo,
	val instituteType: InstituteType
)


@Suppress("CanSealedSubClassBeObject") // model: no comparison needed
@Stable
internal sealed class InstituteInfoModel {
	abstract val type: InstituteType
	abstract val notFulfilledIndex: Int
	abstract val institute: InstituteInfo?
	
	class School : InstituteInfoModel() {
		var schoolLevel by mutableStateOf(0)
		var regionCode by mutableStateOf<String?>("")
		var schoolName by mutableStateOf("")
		override var institute by mutableStateOf<InstituteInfo?>(null)
		
		override val notFulfilledIndex: Int
			get() = when {
				schoolLevel == 0 -> 0
				regionCode == "" -> 1
				schoolName.isBlank() -> 2
				else -> -1
			}
		
		override val type: InstituteType
			get() = InstituteType.school
	}
}

package com.lhwdev.selfTestMacro

import com.lhwdev.selfTestMacro.api.LoginType
import com.lhwdev.selfTestMacro.api.UserIdentifier
import com.lhwdev.selfTestMacro.api.UsersToken
import kotlinx.serialization.Serializable


@Serializable
data class UserSetting(
	val loginType: LoginType,
	val region: String,
	val level: Int,
	val schoolName: String,
	val studentName: String,
	val studentBirth: String
)

@Serializable
data class UserLoginInfo(val identifier: UserIdentifier, val token: UsersToken)


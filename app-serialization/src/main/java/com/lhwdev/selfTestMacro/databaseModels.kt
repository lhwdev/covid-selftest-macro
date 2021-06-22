package com.lhwdev.selfTestMacro

import com.lhwdev.selfTestMacro.api.InstituteInfo
import com.lhwdev.selfTestMacro.api.User
import com.lhwdev.selfTestMacro.api.UserIdentifier
import kotlinx.serialization.Serializable


@Serializable
data class DbTestGroups(
	val groups: List<DbTestGroup> = emptyList()
)

@Serializable
data class DbTestGroup(
	val users: List<Int>,
	val schedule: DbTestSchedule,
	val excludeWeekend: Boolean,
	val excludeHoliday: Boolean
)

@Serializable
sealed class DbTestSchedule {
	@Serializable
	object None
	
	@Serializable
	class Fixed(val hour: Int, val minute: Int)
	
	@Serializable
	class Random(val from: Fixed, val to: Fixed)
}


@Serializable
data class DbUsers(
	val users: Map<Int, DbUser> = emptyMap(),
	val maxId: Int = 0
)

@Serializable
data class DbUser(
	val id: Int,
	val user: User,
	val instituteName: String,
	val userGroupId: Int
)


@Serializable
data class DbUserGroups(
	val groups: Map<Int, DbUserGroup> = emptyMap(),
	val maxId: Int = 0
)

@Serializable
data class DbUserGroup(
	val id: Int,
	val userIdentifier: UserIdentifier,
	val institute: InstituteInfo
)

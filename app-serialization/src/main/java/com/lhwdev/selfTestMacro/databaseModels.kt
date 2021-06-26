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
sealed class DbTestGroup(
	val target: DbTestTarget,
	val schedule: DbTestSchedule,
	val excludeWeekend: Boolean,
	val excludeHoliday: Boolean
)

@Serializable
sealed class DbTestTarget {
	@Serializable
	data class Group(val userIds: List<Int>) : DbTestTarget()
	
	@Serializable
	data class Single(val userId: Int) : DbTestTarget()
}

val DbTestTarget.allUserIds: List<Int>
	get() = when(this) {
		is DbTestTarget.Group -> userIds
		is DbTestTarget.Single -> listOf(userId)
	}


@Serializable
sealed class DbTestSchedule {
	@Serializable
	object None : DbTestSchedule()
	
	@Serializable
	class Fixed(val hour: Int, val minute: Int) : DbTestSchedule()
	
	@Serializable
	class Random(val from: Fixed, val to: Fixed) : DbTestSchedule()
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
	val instituteType: InstituteType,
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
	val userIds: List<Int>,
	val userIdentifier: UserIdentifier,
	val instituteType: InstituteType,
	val institute: InstituteInfo
)

package com.lhwdev.selfTestMacro.database

import com.lhwdev.selfTestMacro.api.InstituteInfo
import com.lhwdev.selfTestMacro.api.InstituteType
import com.lhwdev.selfTestMacro.api.UsersIdentifier
import kotlinx.serialization.Serializable


@Serializable
data class DbTestGroups(
	val groups: List<DbTestGroup> = emptyList(),
	val maxGroupGeneratedNameIndex: Int = 0
)

@Serializable
data class DbTestGroup(
	val id: Int,
	val target: DbTestTarget,
	val schedule: DbTestSchedule = DbTestSchedule.None,
	val excludeWeekend: Boolean = false
)

@Serializable
sealed class DbTestTarget {
	@Serializable
	data class Group(val name: String, val userIds: List<Int>) : DbTestTarget()
	
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
data class DbInstitute(
	val type: InstituteType,
	val code: String,
	val name: String,
	val classifierCode: String,
	val hcsUrl: String,
	val regionCode: String? = null,
	val levelCode: String? = null,
	val sigCode: String? = null
)

@Serializable
data class DbUser(
	val id: Int,
	val name: String,
	val userCode: String,
	val userBirth: String,
	val institute: DbInstitute,
	val userGroupId: Int,
	val answer: Answer
)

@Serializable
data class Answer(
	val suspicious: Boolean,
	val waitingResult: Boolean,
	val quarantined: Boolean,
	val message: String? = null
)


@Serializable
data class DbUserGroups(
	val groups: Map<Int, DbUserGroup> = emptyMap(),
	val maxId: Int = 0
)

@Serializable
data class DbUserGroup(
	val id: Int,
	val masterName: String,
	val masterBirth: String,
	val password: String,
	val userIds: List<Int>,
	val usersIdentifier: UsersIdentifier,
	val instituteType: InstituteType,
	val institute: InstituteInfo
)

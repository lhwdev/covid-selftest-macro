package com.lhwdev.selfTestMacro

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.lhwdev.selfTestMacro.api.InstituteInfo


class DatabaseManager(pref: PreferenceHolder) {
	var testGroups: DbTestGroups by pref.preferenceSerialized(
		key = "testGroups",
		serializer = DbTestGroups.serializer(),
		defaultValue = DbTestGroups()
	)
	
	var userGroups: DbUserGroups by pref.preferenceSerialized(
		key = "userGroups",
		serializer = DbUserGroups.serializer(),
		defaultValue = DbUserGroups()
	)
	
	var users: DbUsers by pref.preferenceSerialized(
		key = "users",
		serializer = DbUsers.serializer(),
		defaultValue = DbUsers()
	)
	
	val DbTestTarget.Group.allUsers: List<DbUser> get() = object : LazyListBase<DbUser>(userIds.size) {
		override fun createAt(index: Int): DbUser = users.users.getValue(userIds[index])
		override fun contains(element: DbUser): Boolean = element.id in userIds
		
	}
	
	val DbTestTarget.Single.user: DbUser get() = users.users.getValue(userId)
	val DbTestTarget.allUsers: List<DbUser>
		get() = when(this) {
			is DbTestTarget.Group -> allUsers
			is DbTestTarget.Single -> listOf(user)
		}
	
	val DbTestTarget.name: String
		get() = when(this) {
			is DbTestTarget.Group -> name
			is DbTestTarget.Single -> user.user.name
		}
	
	val DbTestTarget.commonInstitute: InstituteInfo?
		get() = when(this) {
			is DbTestTarget.Group -> allUsers.fold<DbUser, InstituteInfo?>(allUsers.first().institute) { acc, user ->
				acc?.takeIf { user.institute.code == acc.code }
			}
			is DbTestTarget.Single -> user.institute
		}
	
	val DbTestTarget.commonInstituteType: InstituteType?
		get() = when(this) {
			is DbTestTarget.Group -> allUsers.fold<DbUser, InstituteType?>(allUsers.first().instituteType) { acc, user ->
				acc?.takeIf { user.userGroup.instituteType == acc }
			}
			is DbTestTarget.Single -> user.userGroup.instituteType
		}
	
	val DbUser.userGroup: DbUserGroup get() = userGroups.groups.getValue(userGroupId)
	val DbUser.institute: InstituteInfo get() = userGroup.institute
	
	val DbUserGroup.allUsers: List<DbUser> get() = userIds.map { users.users.getValue(it) }
}

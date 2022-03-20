package com.lhwdev.selfTestMacro.database

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.lhwdev.selfTestMacro.api.InstituteInfo
import com.lhwdev.selfTestMacro.lazyMap


class AppDatabase(val holder: PreferenceHolder) {
	var testGroups: DbTestGroups by holder.preferenceSerialized(
		key = "testGroups",
		serializer = DbTestGroups.serializer(),
		defaultValue = DbTestGroups()
	)
	
	var userGroups: DbUserGroups by holder.preferenceSerialized(
		key = "userGroups",
		serializer = DbUserGroups.serializer(),
		defaultValue = DbUserGroups()
	)
	
	var users: DbUsers by holder.preferenceSerialized(
		key = "users",
		serializer = DbUsers.serializer(),
		defaultValue = DbUsers()
	)
	
	val DbTestTarget.Group.allUsers: List<DbUser>
		get() = userIds.lazyMap { users.users.getValue(it) }
	
	val DbTestTarget.Single.user: DbUser get() = users.users.getValue(userId)
	val DbTestTarget.allUsers: List<DbUser>
		get() = when(this) {
			is DbTestTarget.Group -> allUsers
			is DbTestTarget.Single -> listOf(user)
		}
	
	val DbTestTarget.name: String
		get() = when(this) {
			is DbTestTarget.Group -> name
			is DbTestTarget.Single -> user.name
		}
	
	val DbUser.userGroup: DbUserGroup get() = userGroups.groups.getValue(userGroupId)
	val DbUser.usersInstitute: InstituteInfo get() = userGroup.institute
	
	
	val DbTestTarget.commonInstitute: DbInstitute?
		get() {
			val all = allUsers
			if(all.isEmpty()) return null
			
			return all.fold(all.first().institute as DbInstitute?) { acc, user ->
				if(user.institute == acc) acc else null
			}
		}
	
	val DbUserGroup.allUsers: List<DbUser> get() = userIds.lazyMap { users.users.getValue(it) }
	val DbUserGroup.mainUser: DbUser get() = users.users.getValue(userIds.first())
}

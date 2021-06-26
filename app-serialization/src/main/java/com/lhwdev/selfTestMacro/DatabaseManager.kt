package com.lhwdev.selfTestMacro

import android.content.SharedPreferences


class DatabaseManager(pref: SharedPreferences) {
	var testGroups by pref.preferenceSerialized(
		key = "testGroups",
		serializer = DbTestGroups.serializer(),
		defaultValue = DbTestGroups()
	)
	var userGroups by pref.preferenceSerialized(
		key = "testGroups",
		serializer = DbUserGroups.serializer(),
		defaultValue = DbUserGroups()
	)
	var users by pref.preferenceSerialized(
		key = "users",
		serializer = DbUsers.serializer(),
		defaultValue = DbUsers()
	)
	
	val DbTestTarget.Group.allUsers: List<DbUser> get() = userIds.map { users.users.getValue(it) }
	val DbTestTarget.Single.user: DbUser get() = users.users.getValue(userId)
	val DbTestTarget.allUsers: List<DbUser>
		get() = when(this) {
			is DbTestTarget.Group -> allUsers
			is DbTestTarget.Single -> listOf(user)
		}
	
	val DbUser.userGroup: DbUserGroup get() = userGroups.groups.getValue(userGroupId)
}

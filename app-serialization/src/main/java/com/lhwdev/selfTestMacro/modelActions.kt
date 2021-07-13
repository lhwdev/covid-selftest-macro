package com.lhwdev.selfTestMacro


fun DatabaseManager.removeTestGroup(group: DbTestGroup) {
	removeUsers(group.target.allUserIds)
	
	testGroups = testGroups.copy(groups = testGroups.groups - group)
}

fun DatabaseManager.removeTestGroups(groups: List<DbTestGroup>) {
	removeUsers(groups.flatMap { it.target.allUserIds })
	
	testGroups = testGroups.copy(groups = testGroups.groups - groups)
}


fun DatabaseManager.removeUsers(userIdsToRemove: List<Int>) {
	// clear user groups
	val userGroupsToModify = userIdsToRemove.map { users.users.getValue(it).userGroup.id }.toSet()
	
	// clear users
	users = users.copy(users = users.users.filterNot { (id, _) -> id in userIdsToRemove })
	
	val newGroups = userGroups.groups.mapNotNull { (id, group) ->
		if(id in userGroupsToModify) {
			if(group.userIds.containsAll(userIdsToRemove)) null
			else id to group.copy(userIds = group.userIds - userIdsToRemove)
		} else {
			id to group
		}
	}.toMap()
	userGroups = userGroups.copy(groups = newGroups)
}


fun DatabaseManager.disbandGroup(group: DbTestGroup, inheritSchedule: Boolean) {
	if(group.target !is DbTestTarget.Group) return
	
	val users = group.target.allUsers
	val newTestGroups = users.map {
		val target = DbTestTarget.Single(it.id)
		if(inheritSchedule) DbTestGroup(
			target = target,
			schedule = group.schedule,
			excludeHoliday = group.excludeHoliday,
			excludeWeekend = group.excludeWeekend
		) else DbTestGroup(target = target)
	}
	
	// do not need to touch userGroups and users, so does not use removeTestGroup
	testGroups = testGroups.copy(groups = testGroups.groups - group + newTestGroups)
}

package com.lhwdev.selfTestMacro


fun List<Int>.nextId(): Int {
	for(i in 0..Int.MAX_VALUE) {
		if(i !in this) return i
	}
	return -1
}

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
	val ids = testGroups.groups.map { it.id }.toMutableList()
	val newTestGroups = users.map {
		val id = ids.nextId()
		ids += id
		val target = DbTestTarget.Single(it.id)
		if(inheritSchedule) DbTestGroup(
			id = id,
			target = target,
			schedule = group.schedule,
			excludeHoliday = group.excludeHoliday,
			excludeWeekend = group.excludeWeekend
		) else DbTestGroup(id = id, target = target)
	}
	
	// do not need to touch userGroups and users, so does not use removeTestGroup
	testGroups = testGroups.copy(groups = testGroups.groups - group + newTestGroups)
}

fun DatabaseManager.moveToTestGroup(
	target: List<Pair<Int, DbTestGroup>>,
	toGroup: DbTestGroup
): DbTestGroup {
	check(target.all { it.second.target is DbTestTarget.Single })
	check(toGroup.target is DbTestTarget.Group)
	
	val newGroup = toGroup.copy(
		target = toGroup.target.copy(userIds = toGroup.target.userIds + target.map { it.first })
	)
	
	testGroups = testGroups.copy(
		groups = testGroups.groups - target.map { it.second } - toGroup + newGroup
	)
	
	return newGroup
}

fun DatabaseManager.replaceTestGroup(from: DbTestGroup, to: DbTestGroup) {
	testGroups = testGroups.copy(groups = testGroups.groups.map {
		if(it == from) to else it
	})
}

fun DatabaseManager.updateSchedule(group: DbTestGroup, schedule: DbTestSchedule): DbTestGroup {
	val newGroup = group.copy(schedule = schedule)
	replaceTestGroup(group, newGroup)
	return newGroup
}

fun DatabaseManager.renameTestGroup(group: DbTestGroup, newName: String): DbTestGroup {
	check(group.target is DbTestTarget.Group)
	val newGroup = group.copy(target = group.target.copy(name = newName))
	replaceTestGroup(group, newGroup)
	return newGroup
}

package com.lhwdev.selfTestMacro.database


fun List<Int>.nextTestGroupId(): Int {
	for(i in 0..Int.MAX_VALUE) {
		if(i !in this) return i
	}
	return -1
}

fun DatabaseManager.removeTestGroup(group: DbTestGroup): Unit = transactDb {
	removeUsers(group.target.allUserIds)
	
	testGroups = testGroups.copy(groups = testGroups.groups - group.id)
}

fun DatabaseManager.removeTestGroups(groups: List<DbTestGroup>): Unit = transactDb {
	removeUsers(groups.flatMap { it.target.allUserIds })
	
	testGroups = testGroups.copy(groups = testGroups.groups - groups.map { it.id })
}


fun DatabaseManager.removeUsers(userIdsToRemove: List<Int>): Unit = transactDb {
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
	val ids = testGroups.groups.keys.toMutableList()
	val newTestGroups = users.map {
		val id = ids.nextTestGroupId()
		ids += id
		val target = DbTestTarget.Single(it.id)
		if(inheritSchedule) DbTestGroup(
			id = id,
			target = target,
			schedule = group.schedule,
			excludeWeekend = group.excludeWeekend
		) else DbTestGroup(id = id, target = target)
	}.associateBy { it.id }
	
	// do not need to touch userGroups and users, so does not use removeTestGroup
	testGroups = testGroups.copy(groups = testGroups.groups - group.id + newTestGroups)
}

fun DatabaseManager.moveToTestGroup(
	target: List<DbTestGroup>,
	toGroup: DbTestGroup
): DbTestGroup {
	check(target.all { it.target is DbTestTarget.Single })
	check(toGroup.target is DbTestTarget.Group)
	
	val newGroup = toGroup.copy(
		target = toGroup.target.copy(userIds = toGroup.target.userIds +
			target.map { (it.target as DbTestTarget.Single).userId })
	)
	
	testGroups = testGroups.copy(
		groups = testGroups.groups - target.map { it.id } - toGroup.id + (newGroup.id to newGroup)
	)
	
	return newGroup
}

fun DatabaseManager.moveOutFromTestGroup(
	fromGroup: DbTestGroup,
	target: List<DbTestGroup> // not added to db yet
): DbTestGroup {
	check(target.all { it.target is DbTestTarget.Single })
	check(fromGroup.target is DbTestTarget.Group)
	
	val newGroup = fromGroup.copy(
		target = fromGroup.target.copy(userIds = fromGroup.target.userIds -
			target.map { (it.target as DbTestTarget.Single).userId }
		)
	)
	val addList = (target + newGroup).associateBy { it.id }
	
	testGroups = testGroups.copy(
		groups = testGroups.groups - fromGroup.id + addList
	)
	
	return newGroup
}

fun DatabaseManager.replaceTestGroupDangerous(from: DbTestGroup, to: DbTestGroup) {
	testGroups = testGroups.copy(groups = testGroups.groups.mapValues { (_, value) ->
		if(value == from) to else value
	})
}

fun DatabaseManager.updateSchedule(group: DbTestGroup, schedule: DbTestSchedule): DbTestGroup {
	val newGroup = group.copy(schedule = schedule)
	replaceTestGroupDangerous(group, newGroup)
	return newGroup
}

fun DatabaseManager.renameTestGroup(group: DbTestGroup, newName: String): DbTestGroup {
	check(group.target is DbTestTarget.Group)
	val newGroup = group.copy(target = group.target.copy(name = newName))
	replaceTestGroupDangerous(group, newGroup)
	return newGroup
}

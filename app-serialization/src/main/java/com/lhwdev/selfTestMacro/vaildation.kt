package com.lhwdev.selfTestMacro


private inline fun <T, K> List<T>.fixDuplicate(key: (T) -> K): Pair<Map<T, K>, List<T>> {
	val removedList = mutableMapOf<T, K>()
	val list = ArrayList<T>(size)
	val keyList = ArrayList<K>(size)
	
	for(element in this) {
		val k = key(element)
		if(k in keyList) {
			removedList[element] = k
		} else list += element
	}
	return removedList to list
}


fun DatabaseManager.validateAndFixDb() {
	// #1. validate groupUsers id (cannot fix)
	for((id, group) in userGroups.groups) {
		if(group.id != id) error("database misalignment: user $group should have id=$id")
	}
	
	// #2. validate users id (cannot fix)
	for((id, user) in users.users) {
		if(user.id != id) error("database misalignment: user $user should have id=$id")
	}
	
	run { // #3. find duplicate groupUsers
		val (removed, fixed) = userGroups.groups.map { it.value }.fixDuplicate { it.id }
		if(removed.isNotEmpty()) {
			// replace all references
			val newUsers = users.users.mapValues { (_, user) ->
				user.copy(userGroupId = removed.getOrElse(user.userGroup) { user.userGroupId })
			}
			
			// apply
			userGroups = userGroups.copy(groups = fixed.associateBy { it.id })
			users = users.copy(users = newUsers)
		}
	}
	
	run { // #4. find duplicate users
		val (removed, fixed) = users.users.map { it.value }.fixDuplicate { it.id }
		if(removed.isNotEmpty()) {
			// replace all references
			fun map(userId: Int) = removed[users.users[userId]] ?: userId
			
			fun map(target: DbTestTarget) = when(target) {
				is DbTestTarget.Group -> DbTestTarget.Group(
					target.name,
					target.userIds.map { map(it) })
				is DbTestTarget.Single -> DbTestTarget.Single(map(target.userId))
			}
			
			val newGroups = testGroups.groups.map { group ->
				group.copy(target = map(group.target))
			}
			
			// apply
			users = users.copy(users = fixed.associateBy { it.id })
			testGroups = testGroups.copy(groups = newGroups)
		}
	}
	
	// run {
	// 	// #5. find testGroups with duplicate users
	// 	// can be duplicated
	// }
}

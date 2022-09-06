package com.lhwdev.selfTestMacro.api.impl

import com.lhwdev.selfTestMacro.api.*
import com.lhwdev.selfTestMacro.api.impl.raw.HcsSession
import com.lhwdev.selfTestMacro.api.UserGroup
import com.lhwdev.selfTestMacro.api.impl.raw.getUserGroup
import com.lhwdev.selfTestMacro.api.utils.LifecycleValue
import com.lhwdev.selfTestMacro.api.utils.getOrDefault
import com.lhwdev.selfTestMacro.utils.CachedSuspendState


@OptIn(InternalHcsApi::class)
public class UserGroupImpl(
	private val data: UserGroupData,
	private val mainUserInstitute: InstituteImpl,
	private val session: HcsSession,
	private var token: LifecycleValue<UserGroup.Token>
) : UserGroup {
	override val mainUser: UserGroupModel.MainUser get() = data.mainUser
	override val users: List<User> = data.users.map {
		session.api.createUser(data = it, userGroup = this)
	}
	
	override fun toData(): UserGroupData = data
	
	
	private suspend fun getToken() = token.getOrDefault {
		val (data, token) = mainUserInstitute.getUserGroup(mainUser, forceLogin = true)
		
	}
	
	private suspend fun getApiGroup() = session.getUserGroup(getToken())
	
	
	
	override val status: CachedSuspendState<UserGroupModel.Status> = CachedSuspendState {
		
	}
}

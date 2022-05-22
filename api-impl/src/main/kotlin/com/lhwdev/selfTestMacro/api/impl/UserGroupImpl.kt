package com.lhwdev.selfTestMacro.api.impl

import com.lhwdev.selfTestMacro.api.*
import com.lhwdev.selfTestMacro.api.impl.raw.ApiUser
import com.lhwdev.selfTestMacro.api.impl.raw.HcsSession
import com.lhwdev.selfTestMacro.api.impl.raw.UsersToken
import com.lhwdev.selfTestMacro.api.impl.raw.getUserGroup


@OptIn(InternalHcsApi::class)
public class UserGroupImpl(
	private val data: UserGroupData,
	private val mainUserInstitute: InstituteImpl,
	private val session: HcsSession,
	private var token: LifecycleValue<UsersToken>
) : UserGroup {
	internal val apiGroup = DefaultExternalStateImpl<List<ApiUser>>(initialValue = update()) {}
	
	override val mainUser: UserGroupModel.MainUser get() = data.mainUser
	override val users: List<User> = apiGroup.map { apiUsers, update -> }
	
	
	private suspend fun getToken() = token.getOrDefault { mainUserInstitute. }
	
	private suspend fun getApiGroup() = session.getUserGroup(getToken())
	
	override val status: ExternalState<UserGroupModel.Status> = ExternalStateImpl {
		update()
	}
}

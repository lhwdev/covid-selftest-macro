package com.lhwdev.selfTestMacro.api.impl

import com.lhwdev.selfTestMacro.api.*
import com.lhwdev.selfTestMacro.api.impl.raw.*
import com.lhwdev.selfTestMacro.api.utils.LifecycleValue


@InternalHcsApi
internal suspend fun HcsSession.getInternalVerificationToken(data: InstituteData): InstituteData.InternalSearchKey {
	data.internalVerificationToken.value?.let { return it }
	
	println("InstituteImpl.kt/getInternalVerificationToken: time >= 2m (-> expired), retrying to get search key")
	
	val findResult = when(data) {
		is InstituteData.School -> searchSchool(level = data.level!!, region = data.region, name = data.name)
	}
	val newData = findResult
		.singleOrNull { it.name == data.name && it.identifier == data.identifier }
		?: error("학교가 여러개?!")
	return newData.internalVerificationToken.value ?: error("searchKey가 바로 만료?!")
}


@OptIn(InternalHcsApi::class)
public class InstituteImpl(
	private var data: InstituteData,
	private val session: HcsSession
) : Institute {
	private val userGroups: MutableMap<UserGroupModel.MainUser, UserGroup> = mutableMapOf()
	override val userGroupsCache: Map<UserGroupModel.MainUser, UserGroup> get() = userGroups
	
	override val identifier: String get() = data.identifier
	override val name: String get() = data.name
	override val type: InstituteModel.Type get() = data.type
	override val address: String? get() = data.address
	
	override fun toData(): InstituteData = data
	
	internal suspend fun getUserGroupData(
		mainUser: UserGroupModel.MainUser,
		token: UserGroup.Token
	): Pair<UserGroupData, List<User.Token>> {
		val group = session.getUserGroup(token)
		val users = group.map { user ->
			val info = session.getUserInfo(user.instituteCode, user.userCode, User.Token(user.token))
			
			UserData(
				identifier = user.userCode,
				name = info.userName,
				type = when {
					info.isAdmin -> UserModel.Type.admin
					info.isClassManager || info.isDepartmentManager -> UserModel.Type.manager
					info.isStudent -> UserModel.Type.user
					else -> {
						println("InstituteImpl.getUserGroupData: no else")
						UserModel.Type.user
					}
				},
				institute = when(info.instituteType) {
					ApiInstituteType.school -> InstituteData.School(
						identifier = info.instituteCode,
						name = info.instituteName,
						address = null,
						level = null,
						region = InstituteModel.School.Region.values().find { it.code == info.instituteRegionCode }
							?: run {
								println("InstituteImpl.getUserGroupData: unknown region ${info.instituteRegionCode}")
								InstituteModel.School.Region.seoul // random value?
							},
						complete = false
					)
					ApiInstituteType.university -> TODO()
					ApiInstituteType.academy -> TODO()
					ApiInstituteType.office -> TODO()
				}
			)
		}
		val tokens = group.map { User.Token(it.token) }
		return UserGroupData(mainUser = mainUser, users = users) to tokens
	}
	
	override fun createUserGroup(
		data: UserGroupData,
		token: LifecycleValue<UserGroup.Token>?
	): UserGroup = userGroups.getOrPut(data.mainUser) {
		UserGroupImpl(
			data = data.toData(),
			mainUserInstitute = this,
			session = session,
			token = token ?: LifecycleValue.empty()
		)
	}
	
	override suspend fun getUserGroup(
		mainUser: UserGroupModel.MainUser,
		forceLogin: Boolean
	): Institute.LoginResult {
		val previous = userGroups[mainUser]
		if(previous != null) {
			if(forceLogin) {
				previous.refresh()
			}
			return Institute.LoginResult.Success(previous)
		}
		
		val result = session.findUser(
			password = mainUser.password,
			instituteCode = identifier,
			name = mainUser.name,
			birthday = mainUser.birthday,
			loginType = when(type) {
				InstituteModel.Type.school -> ApiLoginType.school
				InstituteModel.Type.university -> ApiLoginType.univ
				InstituteModel.Type.academy, InstituteModel.Type.office -> ApiLoginType.office
			},
			searchKey = session.getInternalVerificationToken(data)
		)
		
		return when(result) {
			is PasswordResult.Success -> {
				val token = UserGroup.Token(result.token)
				val data = UserGroupData(
					mainUser = mainUser,
					users = fetchUserGroupUsers(mainUser, token)
				)
				val userGroup = createUserGroup(
					data = data,
					token = with(LifecycleValue) { token.expiresIn(millis = 1000000000000 /* IDK */) }
				)
				
				Institute.LoginResult.Success(userGroup)
			}
			is PasswordResult.Failed -> TODO()
		}
	}
	
	public suspend fun fetchUserGroupUsers(
		mainUser: UserGroupModel.MainUser,
		token: UserGroup.Token
	): List<UserData> {
		val result = session.getUserGroup(token = token)
		result[0]
	}
	
	override suspend fun refresh() {
		TODO()
	}
}

package com.lhwdev.selfTestMacro.api

import com.lhwdev.selfTestMacro.utils.CachedSuspendState
import kotlinx.serialization.Serializable


public interface UserGroupModel : HcsPersistentModel {
	@Serializable
	public class MainUser(
		public val name: String,
		public val birthday: String,
		public val password: String
	)
	
	
	public val mainUser: MainUser
	
	public val mainUserModel: UserModel get() = users[0]
	
	/**
	 * `users[0]` is expected to be equal to [mainUser].
	 */
	public val users: List<UserModel>
	
	public fun toData(): UserGroupData
	
	
	public interface Status : HcsTemporaryModel {
		public val agreement: Boolean
	}
}


@Serializable
public class UserGroupData(
	public override val mainUser: UserGroupModel.MainUser,
	public override val users: List<UserData>
) : UserGroupModel {
	override fun toData(): UserGroupData = this
	
	override fun equals(other: Any?): Boolean = when {
		this === other -> true
		other !is UserGroupData -> false
		else -> users == other.users
	}
	
	override fun hashCode(): Int = users.hashCode()
}


public interface UserGroup : UserGroupModel, HcsLiveModel {
	public class Token(public val token: String)
	
	
	override val mainUserModel: User get() = users[0]
	
	public override val users: List<User>
	
	
	public val status: CachedSuspendState<UserGroupModel.Status>
	
	
	public suspend fun updateAgreement(): Boolean
	
	public suspend fun registerPassword(password: String)
	
	public suspend fun changePassword(oldPassword: String, newPassword: String)
}

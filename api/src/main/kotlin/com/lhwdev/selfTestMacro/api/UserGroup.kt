package com.lhwdev.selfTestMacro.api


public interface UserGroupModel : HcsPersistentModel {
	public val mainUser: UserModel
	
	public val users: List<User>
}


public interface UserGroup

package com.lhwdev.selfTestMacro.api


public interface UserGroupModel : HcsPersistentModel {
	public val status: ExternalState<Status>
	
	public val mainUser: UserModel
	
	public val users: List<User>
	
	
	public interface Status : HcsTemporaryModel {
		public val agreement: Boolean
	}
}


public interface UserGroup

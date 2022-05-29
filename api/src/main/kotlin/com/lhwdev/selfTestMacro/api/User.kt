package com.lhwdev.selfTestMacro.api

import com.lhwdev.selfTestMacro.utils.CachedSuspendState
import kotlinx.serialization.Serializable
import java.util.Date


public interface UserModel : HcsPersistentModel {
	public enum class Type { user, manager, admin }
	
	public val identifier: String
	
	public val name: String
	
	public val type: Type
	public val institute: InstituteModel
	
	public fun toData(): UserData
	
	
	public interface Status : HcsTemporaryModel {
		public val agreement: Boolean
		public val newNotificationCount: Int
		
		public val surveyResult: SurveyResult?
		
		public class SurveyResult(
			public val answers: AnswersMap,
			public val lastSurveyAt: Date
		)
	}
}


@Serializable
public class UserData(
	public override val identifier: String,
	
	public override val name: String,
	
	public override val type: UserModel.Type,
	public override val institute: InstituteData
) : UserModel {
	override fun toData(): UserData = this
	
	override fun equals(other: Any?): Boolean = when {
		this === other -> true
		other !is UserData -> false
		else -> identifier == other.identifier && institute == other.institute
	}
	
	override fun hashCode(): Int {
		var result = identifier.hashCode()
		result = 31 * result + institute.hashCode()
		return result
	}
}


public interface User : UserModel {
	public val userGroup: UserGroup
	public override val institute: Institute
	
	public val status: CachedSuspendState<UserModel.Status>
	
	public suspend fun registerSurvey(answers: AnswersMap): UserModel.Status.SurveyResult
}

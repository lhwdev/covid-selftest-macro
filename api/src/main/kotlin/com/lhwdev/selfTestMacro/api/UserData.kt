package com.lhwdev.selfTestMacro.api

import kotlinx.serialization.Serializable
import java.util.Date


public interface UserModel {
	public val identifier: String
	public val name: String
	public val type: Type
	public val institute: InstituteData
	
	public enum class Type { user, manager }
	
	public interface Status : HcsTemporaryModel {
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
) : UserModel, HcsPersistentModel {
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

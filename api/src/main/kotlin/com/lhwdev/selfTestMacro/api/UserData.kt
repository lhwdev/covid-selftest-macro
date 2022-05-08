package com.lhwdev.selfTestMacro.api

import kotlinx.serialization.Serializable
import java.util.Date


/*
 * Most models have a concept of 'persistent' and 'temporary'.
 * All models which is temporary should be only used for limited period, and cannot be serialized.
 * This ensures that one do not put temporary model into database.
 */


@Serializable
public class UserData(
	public val identifier: String,
	public val name: String,
	public val institute: InstituteData
) : HcsPersistentModel {
	public class Status(
		public val survey: SurveyResult
	)
	
	public class SurveyResult(
		public val survey: AnswersMap,
		public val lastSurveyAt: Date
	)
	
	
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

package com.lhwdev.selfTestMacro.api


public interface User {
	public val status: ExternalState<UserModel.Status>
	
	public suspend fun registerSurvey(answers: AnswersMap): UserModel.Status.SurveyResult
}

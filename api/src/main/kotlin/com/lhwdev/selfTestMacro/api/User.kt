package com.lhwdev.selfTestMacro.api


public interface User : UserModel {
	public val status: ExternalState<UserModel.Status>
	
	public suspend fun registerSurvey(answers: AnswersMap): UserModel.Status.SurveyResult
}

package com.lhwdev.selfTestMacro.api


/**
 * An entry point to whole covid hcs api (self test api).
 * You can use default implementation `DefaultCovidHcsApi`.
 */
public interface CovidHcsApi {
	/// Creating / Restoring implementations from serialized data
	
	public fun createInstitute(data: InstituteModel): Institute
	
	public fun createUser(data: UserModel): User
	
	
	// Creating implementations
	
	public suspend fun findSchool(
		level: InstituteModel.School.Level,
		region: InstituteModel.School.Region?,
		name: String
	): List<InstituteModel.School>
	
	// TODO: university, office, academy
}

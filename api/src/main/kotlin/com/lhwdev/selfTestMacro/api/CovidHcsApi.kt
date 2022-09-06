package com.lhwdev.selfTestMacro.api


/**
 * An entry point to whole covid hcs api (self test api).
 * You can use default implementation `DefaultCovidHcsApi`.
 * 
 * All create methods like [createInstitute], [Institute.createUserGroup] always returns same instance for same entity.
 */
public interface CovidHcsApi {
	// public enum class CachePolicy { none, always }
	//
	// public fun withCachePolicy(policy: CachePolicy): CovidHcsApi
	
	/// Caches
	
	public val institutesCache: Set<Institute>
	
	
	/// Creating / Restoring implementations from serialized data
	
	public fun createInstitute(data: InstituteData): Institute
	
	/**
	 * [User] entity do not have that much power (without submitting survey), instead [UserGroup] has.
	 * We may not get [Institute] for one [User], so [createUser] is on this global api factory.
	 */
	public fun createUser(data: UserData, userGroup: UserGroup): User
	
	
	// Creating implementations
	
	public suspend fun findSchool(
		level: InstituteModel.School.Level,
		region: InstituteModel.School.Region?,
		name: String
	): List<InstituteModel.School>
	
	// TODO: university, office, academy
}

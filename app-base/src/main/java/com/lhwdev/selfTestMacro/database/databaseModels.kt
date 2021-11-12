package com.lhwdev.selfTestMacro.database

import com.lhwdev.selfTestMacro.api.InstituteInfo
import com.lhwdev.selfTestMacro.api.InstituteType
import com.lhwdev.selfTestMacro.api.UsersIdentifier
import com.lhwdev.selfTestMacro.debug.DiagnosticItem
import com.lhwdev.selfTestMacro.debug.DiagnosticObject
import com.lhwdev.selfTestMacro.debug.diagnosticGroup
import com.lhwdev.selfTestMacro.sRegions
import com.lhwdev.selfTestMacro.sSchoolLevels
import kotlinx.serialization.Serializable


@Serializable
data class DbTestGroups(
	val groups: List<DbTestGroup> = emptyList(),
	val maxGroupGeneratedNameIndex: Int = 0
)

@Serializable
data class DbTestGroup(
	val id: Int,
	val target: DbTestTarget,
	val schedule: DbTestSchedule = DbTestSchedule.None,
	val excludeWeekend: Boolean = false
)

@Serializable
sealed class DbTestTarget {
	@Serializable
	data class Group(val name: String, val userIds: List<Int>) : DbTestTarget()
	
	@Serializable
	data class Single(val userId: Int) : DbTestTarget()
}

val DbTestTarget.allUserIds: List<Int>
	get() = when(this) {
		is DbTestTarget.Group -> userIds
		is DbTestTarget.Single -> listOf(userId)
	}


@Serializable
sealed class DbTestSchedule {
	@Serializable
	object None : DbTestSchedule()
	
	@Serializable
	class Fixed(val hour: Int, val minute: Int) : DbTestSchedule()
	
	@Serializable
	class Random(val from: Fixed, val to: Fixed) : DbTestSchedule()
}


@Serializable
data class DbUsers(
	val users: Map<Int, DbUser> = emptyMap(),
	val maxId: Int = 0
)

@Serializable
data class DbInstitute(
	val type: InstituteType,
	val code: String,
	val name: String,
	val classifierCode: String,
	val hcsUrl: String,
	val regionCode: String? = null,
	val levelCode: String? = null,
	val sigCode: String? = null
) : DiagnosticObject {
	override fun getDiagnosticInformation(): DiagnosticItem = diagnosticGroup("DbInstitute", "기관 정보") {
		"type" set type localized "기관 유형" localizeData { it.displayName }
		"name" set name localized "이름"
		"regionCode" set regionCode localized "지역" localizeData { sRegions[it] ?: "?" }
		"levelCode" set levelCode localized "학교 수준" localizeData {
			val code = it.toIntOrNull()
			if(code == null) {
				"?"
			} else {
				sSchoolLevels[code] ?: "?"
			}
		}
	}
}

@Serializable
data class DbUser(
	val id: Int,
	val name: String,
	val userCode: String,
	val userBirth: String,
	val institute: DbInstitute,
	val userGroupId: Int,
	val answer: Answer
) : DiagnosticObject {
	override fun getDiagnosticInformation(): DiagnosticItem = diagnosticGroup("DbUser", "사용자 정보") {
		"id" set id
		"name" set name localized "이름"
		"userCode" set userCode
		"userBirth" set userBirth localized "생일"
		"institute" set institute
		
		"answer" set answer
	}
}

@Serializable
data class Answer(
	val suspicious: Boolean,
	val waitingResult: Boolean,
	val quarantined: Boolean,
	val message: String? = null
) : DiagnosticObject {
	override fun getDiagnosticInformation(): DiagnosticItem = diagnosticGroup("Answer", "자가진단 제출 질문") {
		"suspicious" set suspicious localized "의심증상 여부"
		"waitingResult" set waitingResult localized "검사결과 대기 중"
		"quarantined" set quarantined localized "자가격리 중"
	}
}


@Serializable
data class DbUserGroups(
	val groups: Map<Int, DbUserGroup> = emptyMap(),
	val maxId: Int = 0
)

@Serializable
data class DbUserGroup(
	val id: Int,
	val masterName: String,
	val masterBirth: String,
	val password: String,
	val userIds: List<Int>,
	val usersIdentifier: UsersIdentifier,
	val instituteType: InstituteType,
	val institute: InstituteInfo
)

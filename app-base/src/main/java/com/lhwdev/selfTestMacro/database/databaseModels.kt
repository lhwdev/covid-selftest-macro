package com.lhwdev.selfTestMacro.database

import com.lhwdev.selfTestMacro.api.*
import com.lhwdev.selfTestMacro.debug.DiagnosticItem
import com.lhwdev.selfTestMacro.debug.DiagnosticObject
import com.lhwdev.selfTestMacro.debug.diagnosticGroup
import com.lhwdev.selfTestMacro.sRegions
import com.lhwdev.selfTestMacro.ui.SelfTestQuestions
import kotlinx.serialization.Serializable


@Serializable
class DbTestGroups(
	val revision: Int,
	val groups: Map<Int, DbTestGroup> = emptyMap(),
	val maxGroupGeneratedNameIndex: Int = 0
) {
	fun copy(
		revision: Int = this.revision + 1, // +1 !!!
		groups: Map<Int, DbTestGroup> = this.groups,
		maxGroupGeneratedNameIndex: Int = this.maxGroupGeneratedNameIndex
	): DbTestGroups = DbTestGroups(revision, groups, maxGroupGeneratedNameIndex)
	
	override fun equals(other: Any?): Boolean = when {
		this === other -> true
		other !is DbTestGroups -> false
		else -> revision == other.revision &&
			groups == other.groups &&
			maxGroupGeneratedNameIndex == other.maxGroupGeneratedNameIndex
	}
	
	override fun hashCode(): Int {
		var result = revision
		result = 31 * result + groups.hashCode()
		result = 31 * result + maxGroupGeneratedNameIndex
		return result
	}
	
	override fun toString(): String =
		"DbTestGroups(revision=$revision, groups=$groups, maxGroupGeneratedNameIndex=$maxGroupGeneratedNameIndex"
}

@Serializable
data class DbTestGroup(
	val id: Int,
	val target: DbTestTarget,
	val schedule: DbTestSchedule = DbTestSchedule.None,
	val excludeWeekend: Boolean = true
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

val DbTestTarget.allUsersCount: Int
	get() = when(this) {
		is DbTestTarget.Group -> userIds.size
		is DbTestTarget.Single -> 1
	}


@Serializable
sealed class DbTestSchedule {
	abstract val stable: Boolean
	open val altogether: Boolean get() = true
	
	@Serializable
	object None : DbTestSchedule() {
		override val stable: Boolean get() = true
	}
	
	@Serializable
	class Fixed(val hour: Int, val minute: Int) : DbTestSchedule() {
		override val stable: Boolean get() = true
	}
	
	/**
	 * @param altogether if time for users in a group should be rolled separately, or altogether.
	 */
	@Serializable
	class Random(val from: Fixed, val to: Fixed, override val altogether: Boolean) : DbTestSchedule() {
		override val stable: Boolean get() = false
	}
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
	val sigCode: String? = null
) : DiagnosticObject {
	override fun getDiagnosticInformation(): DiagnosticItem = diagnosticGroup("DbInstitute", "기관 정보") {
		"type" set type localized "기관 유형" localizeData { it.displayName }
		"name" set name localized "이름"
		"regionCode" set regionCode localized "지역" localizeData { sRegions[it] ?: "?" }
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
	val answer: Answer,
	var lastScheduleAt: Long // mutable; be aware to synchronize properly
) : DiagnosticObject {
	override fun getDiagnosticInformation(): DiagnosticItem = diagnosticGroup("DbUser", "사용자 정보") {
		"id" set id
		"name" set name localized "이름"
		"userCode" set userCode
		"userBirth" set userBirth localized "생일"
		"institute" set institute
		
		"answer" set answer
		"lastScheduleAt" set lastScheduleAt
	}
}


@Serializable
data class Answer(
	val suspicious: Boolean,
	val quickTestResult: QuickTestResult,
	val waitingResult: Boolean,
	val message: String? = null
) : DiagnosticObject {
	@Suppress("UNCHECKED_CAST")
	operator fun <T> get(question: SelfTestQuestions<T>): T = when(question) {
		SelfTestQuestions.Suspicious -> suspicious
		SelfTestQuestions.QuickTestResult -> quickTestResult
		SelfTestQuestions.WaitingResult -> waitingResult
	} as T
	
	fun <T> with(question: SelfTestQuestions<T>, value: T): Answer = when(question) {
		SelfTestQuestions.Suspicious -> copy(suspicious = value as Boolean)
		SelfTestQuestions.QuickTestResult -> copy(quickTestResult = value as QuickTestResult)
		SelfTestQuestions.WaitingResult -> copy(waitingResult = value as Boolean)
	}
	
	override fun getDiagnosticInformation(): DiagnosticItem = diagnosticGroup("Answer", "자가진단 제출 질문") {
		"suspicious" set suspicious localized "의심증상 여부"
		"quickTestResult" set quickTestResult localized "신속항원검사 결과" localizeData { it.displayLabel }
		"waitingResult" set waitingResult localized "검사결과 대기 중"
	}
}

val UserInfo.answer: Answer?
	get() = if(questionSuspicious != null) {
		Answer(
			suspicious = questionSuspicious ?: false,
			quickTestResult = questionQuickTestResult ?: QuickTestResult.didNotConduct,
			waitingResult = questionWaitingResult ?: false
		)
	} else null


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

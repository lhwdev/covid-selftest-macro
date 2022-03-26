@file:Suppress("SpellCheckingInspection")
@file:OptIn(ExperimentalSerializationApi::class)

package com.lhwdev.selfTestMacro.api

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.PrimitiveKind
import java.net.URL


public enum class InstituteType(public val displayName: String, public val loginType: LoginType) {
	school("학교", LoginType.school),
	university("대학교", LoginType.univ),
	academy("학원", LoginType.office),
	office("회사", LoginType.office)
}


/**
 * The type of login that is used in [findUser].
 */
public enum class LoginType {
	/**
	 * From school by [getSchoolData].
	 */
	school,
	
	/**
	 * From university by [getUniversityData].
	 */
	univ,
	
	/**
	 * From office by [getOfficeData], and from academy by [getAcademyData].
	 */
	office
}

/**
 * The level of school.
 * 유치원, 초등학교, 중학교, 고등학교. (대학교: seperate institute type)
 */
public enum class SchoolLevel {
	pre, elementary, middle, high
}

/**
 * The information of institute that can be obtained from [getSchoolData], [getUniversityData], [getOfficeData],
 * or [getAcademyData].
 *
 * This class is also used broadly around apis to get `atptOfcdcConctUrl`. (url for Si/Do)
 */
@Serializable
public data class InstituteInfo(
	@Serializable(with = TypeSerializer::class)
	@SerialName("insttClsfCode") val type: InstituteType,
	
	/**
	 * The korean name of institute.
	 */
	@SerialName("kraOrgNm") val name: String,
	
	/**
	 * The english name of institute.
	 */
	@SerialName("engOrgNm") val englishName: String? = null,
	
	/**
	 * The code of institute.
	 * The form of `CNNNNNNNN`, where `C` is country code, like `S` for Seoul, `D` for Daegu, etc., and `N` is
	 * number.
	 */
	@SerialName("orgCode") val code: String,
	
	/**
	 * The address of institute.
	 */
	@SerialName("addres") val address: String,
	
	/**
	 * The level of the school if the [type] is [InstituteType.school].
	 */
	@Serializable(with = SchoolLevelSerializer::class)
	@SerialName("schulKndScCode") val schoolLevel: SchoolLevel? = null,
	
	/**
	 * The base url fraction for most hcs operations.
	 * This property is commonly used to get `atptOfcdcConctUrl`. (url for Si/Do)
	 * Note that this url does not include `https://`. Instead, use [requestUrl] or [requestUrlV2].
	 *
	 * Normally form of `???hcs.eduro.go.kr` where `???` comes the code of Ministry of Education, i.e., 'sen', 'dge'.
	 *
	 * @see requestUrl
	 * @see requestUrlV2
	 */
	@SerialName("atptOfcdcConctUrl") val requestUrlBody: String
) {
	public object TypeSerializer : PrimitiveMappingSerializer<InstituteType, String>(
		rawSerializer = String.serializer(),
		serialName = "com.lhwdev.selfTestMacro.api.InstituteInfo.type",
		primitiveKind = PrimitiveKind.STRING,
		
		InstituteType.school to "5",
		InstituteType.university to "7",
		InstituteType.office to "4",
		InstituteType.academy to "3"
	)
	
	public object SchoolLevelSerializer : PrimitiveMappingSerializer<SchoolLevel, String>(
		rawSerializer = String.serializer(),
		serialName = "com.lhwdev.selfTestMacro.api.InstituteInfo.schoolLevel",
		primitiveKind = PrimitiveKind.STRING,
		
		SchoolLevel.pre to "01",
		SchoolLevel.elementary to "02",
		SchoolLevel.middle to "03",
		SchoolLevel.high to "04"
	)
	
	/**
	 * v2 url for request such as [findUser], [validatePassword], [getUserGroup], [getUserInfo].
	 *
	 * Normally form of `https://???hcs.eduro.go.kr/v2` where `???` comes the code of Ministry of Education, i.e.,
	 * 'sen', 'dge'.
	 */
	public val requestUrlV2: URL get() = URL("https://$requestUrlBody/v2")
	
	/**
	 * The base url for request such as [registerSurvey], [getClassList].
	 *
	 * Normally form of `https://???hcs.eduro.go.kr` where `???` comes the code of Ministry of Education, i.e.,
	 * 'sen', 'dge'.
	 */
	public val requestUrl: URL get() = URL("https://$requestUrlBody")
}


/*
 * usersId token --(validatePassword)--> users token(temporary) --(selectUserGroup)--> user token
 */

/**
 * The identifier of the user.
 * This class can be obtained from [findUser].
 *
 * One 'main user' can register multiple 'users' into the account, so this is called 'users' identifier.
 */
@Serializable
public data class UsersIdentifier(
	/**
	 * The name of main user.
	 */
	@SerialName("userName") val mainUserName: String,
	
	/**
	 * The token of main user.
	 * Can be used to call [validatePassword].
	 */
	@SerialName("token") val token: UsersIdToken,
	
	@Serializable(with = YesNoSerializer::class)
	@SerialName("stdntYn") val isStudent: Boolean,
	
	@Serializable(with = YesNoSerializer::class)
	@SerialName("admnYn") val isAdmin: Boolean,
	
	/**
	 * Whether the user agreed with the [privacy policy](https://hcs.eduro.go.kr/agreement).
	 *
	 * If this is false, you should redirect user to link above and agree with it.
	 * You can also use [updateAgreement] but be sure to inform the user.
	 */
	@Serializable(with = YesNoSerializer::class)
	@SerialName("pInfAgrmYn") val agreement: Boolean
)


/**
 * A class which represents user group.
 * Can be acquired from [getUserGroup].
 */
public data class UserGroup(
	val users: List<User>
) {
	val mainUser: User get() = users.first()
}


/**
 * A class that is needed to interact with fundamental apis of hcs.
 * This can be obtained from [getUserGroup].
 *
 * This class contains primitive information about the user; [userCode], [instituteCode] and [token].
 * If you want to know user's information such as name or last survey status, use [UserInfo].
 */
@Serializable
public data class User(
	/**
	 * The identifier of user. This seems to unique in one [institute][InstituteInfo].
	 */
	@SerialName("userPNo") val userCode: String,
	
	/**
	 * The code of institute.
	 * @see [InstituteInfo.code]
	 */
	@SerialName("orgCode") val instituteCode: String,
	
	/**
	 * The token for one user.
	 */
	@SerialName("token") val token: UserToken,
	
	
	/**
	 * The name of user, if present.
	 * Normally you don't need to interact with this, as does `hcs.eduro.go.kr`. Instead, use [UserInfo].
	 *
	 * If [isOther] is true, this is null.
	 */
	@SerialName("userNameEncpt") val name: String? = null,
	
	/**
	 * @see [InstituteInfo.requestUrlBody]
	 */
	@SerialName("atptOfcdcConctUrl") val requestUrlBody: String,
	
	
	/**
	 * Whether this user is as same as 'main user'.
	 * If true, [name], [isStudent], [isManager] becomes null.
	 *
	 * @see UsersIdentifier
	 */
	@Serializable(with = YesNoSerializer::class)
	@SerialName("otherYn") val isOther: Boolean,
	
	/**
	 * Whether the user is student.
	 * Normally you don't need to interact with this, as does `hcs.eduro.go.kr`. Instead, use [UserInfo].
	 *
	 * If [isOther] is true, this is null.
	 */
	@Serializable(with = YesNoSerializer::class)
	@SerialName("stdntYn") val isStudent: Boolean? = null,
	
	/**
	 * Whether the user is manager.
	 * Normally you don't need to interact with this, as does `hcs.eduro.go.kr`. Instead, use [UserInfo].
	 *
	 * If [isOther] is true, this is null.
	 */
	@Serializable(with = YesNoSerializer::class)
	@SerialName("mngrYn") val isManager: Boolean? = null
)


public enum class QuickTestResult(public val displayLabel: String) {
	didNotConduct("실시하지 않음"),
	negative("음성"),
	positive("양성")
}


/**
 * The class that contains detailed information about user.
 * Can be obtained from [getUserInfo].
 *
 * This is not used to interact with other hcs apis, but contains a lot of useful information, such as [userName],
 * [isHealthy], etc.
 */
@Serializable
public data class UserInfo(
	// basic user information
	
	/**
	 * The name of user.
	 * [User] also has [name][User.name] property, but that is not ensured to exist as if [User.isOther] is true,
	 * it becomes null.
	 */
	@SerialName("userName") val userName: String,
	
	@SerialName("userPNo") val userCode: String,
	
	@SerialName("orgCode") val instituteCode: String,
	
	@SerialName("orgName") val instituteName: String,
	
	@SerialName("insttClsfCode") val instituteClassifierCode: String,
	
	// detailed information for institute
	@SerialName("atptOfcdcConctUrl") val instituteRequestUrlBody: String,
	
	@SerialName("lctnScCode") val instituteRegionCode: String? = null,
	
	@SerialName("sigCode") val instituteSigCode: String? = null,
	
	// latest survey
	@SerialName("registerYmd") val lastRegisterDate: String? = null,
	
	@SerialName("registerDtm") val lastRegisterAt: String? = null,
	
	@SerialName("isHealthy") val isHealthy: Boolean? = null,
	
	@SerialName("isIsoslated") val isIsolated: Boolean? = null,
	
	val rspns01: String? = null,
	val rspns02: String? = null,
	val rspns03: String? = null,
	val rspns07: String? = null,
	
	// etc
	@SerialName("newNoticeCount") val newNoticeCount: Int,
	
	@Serializable(with = YesNoSerializer::class)
	@SerialName("pInfAgrmYn") val agreement: Boolean,
	
	@SerialName("deviceUuid") val deviceUuid: String? = null
) {
	val questionSuspicious: Boolean?
		get() = when(rspns01) {
			"1" -> false
			"2" -> true
			else -> null
		}
	
	val questionQuickTestResult: QuickTestResult?
		get() = if(rspns03 == "1") {
			QuickTestResult.didNotConduct
		} else {
			when(rspns07) {
				"0" -> QuickTestResult.negative
				"1" -> QuickTestResult.positive
				else -> null
			}
		}
	
	val questionWaitingResult: Boolean?
		get() = when(rspns02) {
			"1" -> false
			"0" -> true
			else -> null
		}
	
	@Transient
	private var mInstituteStub: InstituteInfo? = null
	public val instituteStub: InstituteInfo
		get() = mInstituteStub ?: run {
			val stub = InstituteInfo(
				type = instituteType,
				name = instituteName,
				englishName = instituteName,
				code = instituteCode,
				address = "???",
				requestUrlBody = instituteRequestUrlBody
			)
			mInstituteStub = stub
			stub
		}
	
	// see getInstituteData.kt
	@Transient
	public val instituteType: InstituteType = when { // TODO: needs verification
		instituteClassifierCode == "5" -> InstituteType.school
		instituteClassifierCode == "7" -> InstituteType.university
		instituteRegionCode != null && instituteSigCode != null -> InstituteType.academy
		instituteRegionCode != null -> InstituteType.school
		else -> InstituteType.office
	}
	
	public fun toUserInfoString(): String = "$userName($instituteName)"
	public fun toLastRegisterInfoString(): String =
		"최근 자가진단: ${if(lastRegisterAt == null) "미참여" else ((if(isHealthy == true) "정상" else "유증상") + "($lastRegisterAt)")}"
}

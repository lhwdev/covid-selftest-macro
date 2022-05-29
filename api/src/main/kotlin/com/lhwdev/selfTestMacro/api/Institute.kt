package com.lhwdev.selfTestMacro.api

import com.lhwdev.selfTestMacro.api.utils.LifecycleValue
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient


public interface InstituteModel : HcsPersistentModel {
	public enum class Type(public val displayName: String) {
		school("학교"),
		university("대학교"),
		academy("학원"),
		office("회사")
	}
	
	
	public val identifier: String
	public val name: String
	public val type: Type
	public val address: String? // this is nullable as this cannot be got from /v2/getUserInfo
	
	public fun toData(): InstituteData
	
	
	public interface School : InstituteModel {
		public val level: Level? // this is nullable as this cannot be got from /v2/getUserInfo
		public val region: Region
		
		public enum class Level(public val code: Int, public val label: String) {
			pre(code = 1, label = "유치원"),
			primary(code = 2, label = "초등학교"),
			middle(code = 3, label = "중학교"),
			high(code = 4, label = "고등학교"),
			special(code = 5, label = "특수학교 등")
		}
		
		@Suppress("SpellCheckingInspection")
		public enum class Region(public val code: String, public val label: String) {
			seoul(code = "01", label = "서울"),
			busan(code = "02", label = "부산"),
			daegu(code = "03", label = "대구"),
			incheon(code = "04", label = "인천"),
			gwangju(code = "05", label = "광주"),
			daejeon(code = "06", label = "대전"),
			ulsan(code = "07", label = "울산"),
			sejong(code = "08", label = "세종"),
			gyeonggi(code = "10", label = "경기"),
			gangwon(code = "11", label = "강원"),
			chungbuk(code = "12", label = "충북"),
			chungnam(code = "13", label = "충남"),
			jeonbuk(code = "14", label = "전북"),
			jeonnam(code = "15", label = "전남"),
			gyeongbuk(code = "16", label = "경북"),
			gyeongnam(code = "17", label = "경남"),
			jeju(code = "18", label = "제주")
		}
	}
}


@Serializable
public sealed class InstituteData : InstituteModel {
	@InternalHcsApi
	@Serializable
	public class InternalSearchKey(public val token: String)
	
	
	@InternalHcsApi
	@Transient
	public var internalVerificationToken: LifecycleValue<InternalSearchKey> = LifecycleValue.empty()
	
	
	@Serializable
	@SerialName("school")
	public class School(
		public override val identifier: String,
		public override val name: String,
		public override val address: String?,
		
		public override val level: InstituteModel.School.Level?,
		public override val region: InstituteModel.School.Region
	) : InstituteData(), InstituteModel.School {
		override val type: InstituteModel.Type get() = InstituteModel.Type.school
		
		override fun toData(): School = this
	}
	
	
	override fun equals(other: Any?): Boolean = other is InstituteData && identifier == other.identifier
	
	override fun hashCode(): Int = identifier.hashCode()
}


public interface Institute : InstituteModel {
	public suspend fun getUserGroup(
		mainUser: UserGroupModel.MainUser,
		forceLogin: Boolean = false
	): LoginResult
	
	
	public sealed class LoginResult {
		public class Success(public val userGroup: UserGroup) : LoginResult()
		
		public sealed class Failed(
			public val throwable: Throwable
		) : LoginResult() {
			public enum class Reason { wrongUserInfo, wrongPassword, unknown }
		}
	}
}

package com.lhwdev.selfTestMacro.api.impl

import com.lhwdev.selfTestMacro.api.Institute
import com.lhwdev.selfTestMacro.api.InstituteData
import com.lhwdev.selfTestMacro.api.InstituteModel
import com.lhwdev.selfTestMacro.api.InternalHcsApi
import com.lhwdev.selfTestMacro.api.impl.raw.*


@InternalHcsApi
internal suspend fun HcsSession.getInternalVerificationToken(data: InstituteData): InstituteData.InternalSearchKey {
	data.internalVerificationToken.value?.let { return it }
	
	println("InstituteImpl.kt/getInternalVerificationToken: time >= 2m (-> expired), retrying to get search key")
	
	val findResult = when(data) {
		is InstituteData.School -> searchSchool(level = data.level, region = data.region, name = data.name)
	}
	val newData = findResult
		.singleOrNull { it.name == data.name && it.identifier == data.identifier }
		?: error("학교가 여러개?!")
	return newData.internalVerificationToken.value ?: error("searchKey가 바로 만료?!")
}


@OptIn(InternalHcsApi::class)
public class InstituteImpl(
	private val data: InstituteData,
	private val session: HcsSession
) : Institute, InstituteModel by data {
	override suspend fun login(name: String, birthday: String, password: String): Institute.LoginResult {
		val result = session.findUser(
			password = password,
			instituteCode = identifier,
			name = name,
			birthday = birthday,
			loginType = when(type) {
				InstituteModel.Type.school -> LoginType.school
				InstituteModel.Type.university -> LoginType.univ
				InstituteModel.Type.academy, InstituteModel.Type.office -> LoginType.office
			},
			searchKey = session.getInternalVerificationToken(data)
		)
		
		return when(result) {
			is PasswordResult.Success -> Institute.LoginResult.Success()
			is PasswordResult.Failed -> TODO()
		}
	}
}

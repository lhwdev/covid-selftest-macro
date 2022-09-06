package com.lhwdev.selfTestMacro

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.tooling.preview.Preview
import com.lhwdev.selfTestMacro.api.*
import com.lhwdev.selfTestMacro.model.MainRepository
import com.lhwdev.selfTestMacro.model.MainRepositoryImpl
import com.lhwdev.selfTestMacro.model.Status


// TODO: this crashes


@Composable
fun PreviewStubState(statusBar: Boolean = false, content: @Composable () -> Unit) {
	PreviewBase(statusBar = statusBar) {
		val pref = remember {
			val sharedPreferences = buildSharedPreference {}
			val state = PreferenceState(PreferenceHolder(sharedPreferences))
			state.apply {
				isDebugEnabled = true
				firstState = 1
				
				val userA = DbUser(
					id = 0,
					user = User(name = "홍길동", userCode = "D01234567", token = User.Token("userA")),
					instituteName = "어느고등학교",
					instituteType = InstituteType.school,
					userGroupId = 10
				)
				val userB = DbUser(
					id = 1,
					user = User(name = "김철수", userCode = "D01234568", token = User.Token("userB")),
					instituteName = "어느고등학교",
					instituteType = InstituteType.school,
					userGroupId = 11
				)
				
				val userGroupA = DbUserGroup(
					id = 10,
					userIds = listOf(0),
					usersIdentifier = UsersIdentifier(
						mainUserName = "홍길동",
						token = UsersIdToken("groupA"),
						isStudent = true,
						agreement = true
					),
					instituteType = InstituteType.school,
					institute = InstituteInfo(
						name = "어느고등학교",
						code = "schoolA",
						address = "서울시 대충 광화문 1번",
						requestUrlBody = "https://stubhcs.example.org"
					)
				)
				
				val userGroupB = DbUserGroup(
					id = 11,
					userIds = listOf(1),
					usersIdentifier = UsersIdentifier(
						mainUserName = "김철수",
						token = UsersIdToken("groupB"),
						isStudent = true,
						agreement = true
					),
					instituteType = InstituteType.school,
					institute = InstituteInfo(
						name = "어느고등학교",
						code = "schoolA",
						address = "서울시 대충 광화문 1번",
						requestUrlBody = "https://stubhcs.example.org"
					)
				)
				
				val testGroupA = DbTestGroup(
					target = DbTestTarget.Group(name = "그룹 1", userIds = listOf(0, 1)),
					schedule = DbTestSchedule.None,
					excludeWeekend = true
				)
				
				db.users = DbUsers(
					users = mapOf(0 to userA, 1 to userB),
					maxId = 1
				)
				db.userGroups = DbUserGroups(
					groups = mapOf(
						10 to userGroupA,
						11 to userGroupB
					),
					maxId = 11
				)
				db.testGroups = DbTestGroups(
					groups = listOf(testGroupA),
					maxGroupGeneratedNameIndex = 1
				)
			}
			
			state
		}
		
		CompositionLocalProvider(
			LocalPreference provides pref
		) {
			content()
		}
	}
}


@Preview
@Composable
fun MainPreview(): Unit = PreviewStubState {
	Main()
}

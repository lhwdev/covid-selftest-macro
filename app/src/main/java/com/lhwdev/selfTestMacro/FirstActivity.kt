package com.lhwdev.selfTestMacro

import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.text.InputFilter
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import android.widget.Filter
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.getSystemService
import androidx.core.content.res.ResourcesCompat
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import com.lhwdev.selfTestMacro.api.*
import kotlinx.android.synthetic.main.activity_first.*
import kotlinx.coroutines.launch


class FirstActivity : AppCompatActivity() {
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		
		setContentView(R.layout.activity_first)
		window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
		val pref = preferenceState
		val first = intent.hasExtra("first")
		
		setSupportActionBar(toolbar)
		
		var institute: InstituteInfo? = null
		
		fun adapter(list: List<String>) =
			object : ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, list) {
				override fun getFilter(): Filter {
					return DisabledFilter()
				}
				
				private inner class DisabledFilter : Filter() {
					override fun performFiltering(text: CharSequence): FilterResults {
						val result = FilterResults()
						result.values = list
						result.count = list.size
						return result
					}
					
					override fun publishResults(text: CharSequence, results: FilterResults) {
						notifyDataSetChanged()
					}
				}
			}
//			ArrayAdapter(this, android.R.layout.simple_list_item_1, list)
		
		input_loginType.setAdapter(adapter(listOf("학교")))
		
		input_region.setAdapter(adapter(sRegions.keys.toList()))
		input_level.setAdapter(adapter(sSchoolLevels.keys.toList()))
		
		input_schoolName.setOnEditorActionListener { _, _, _ ->
			button_checkSchoolInfo.performClick()
		}
		
		button_checkSchoolInfo.setOnClickListener {
			val schoolName = input_schoolName.text
			if(schoolName == null || schoolName.isEmpty()) {
				showToast("학교 이름을 입력해주세요")
				return@setOnClickListener
			}
			
			currentFocus?.windowToken?.let {
				getSystemService<InputMethodManager>()!!.hideSoftInputFromWindow(it, 0)
			}
			
			val nameString = schoolName.toString()
			
			val regionCode = sRegions.getValue(input_region.text.toString())
			val levelCode = sSchoolLevels.getValue(input_level.text.toString())
			
			lifecycleScope.launch {
				val snackBar =
					Snackbar.make(button_checkSchoolInfo, "잠시만 기다려주세요", Snackbar.LENGTH_INDEFINITE)
				snackBar.show()
				
				fun selectInstitute(instituteInfo: InstituteInfo) {
					institute = instituteInfo
					
					// ui interaction
					schoolName.clear()
					schoolName.append(instituteInfo.name)
					button_checkSchoolInfo.icon = ResourcesCompat.getDrawable(
						resources,
						R.drawable.ic_baseline_check_24,
						theme
					)
					
					scrollView.smoothScrollTo(0, scrollView.height)
				}
				
				val data = getSchoolData(
					regionCode = regionCode,
					schoolLevelCode = levelCode.toString(),
					name = nameString
				)
				snackBar.dismiss()
				if(data.instituteList.isEmpty()) {
					showToast("학교를 찾을 수 없습니다. 이름을 바르게 입력했는지 확인해주세요.")
					return@launch
				}
				
				if(data.instituteList.size == 1)
					selectInstitute(data.instituteList[0])
				else AlertDialog.Builder(this@FirstActivity).apply {
					setTitle("학교를 선택해주세요")
					setItems(
						data.instituteList.map { "${it.name}(${it.address})" }.toTypedArray(),
						DialogInterface.OnClickListener { _, which ->
							selectInstitute(data.instituteList[which])
						})
				}.show()
			}
		}
		
		input_studentName.doAfterTextChanged {
			input_studentName.error = null
		}
		
		input_studentBirth.doAfterTextChanged {
			input_studentBirth.error = null
		}
		
		input_studentBirth.setOnEditorActionListener { _, _, _ ->
			button_done.callOnClick()
		}
		
		button_done.setOnClickListener onClick@{
			val instituteInfo = institute ?: run {
				button_checkSchoolInfo.callOnClick()
				institute ?: return@onClick
			}
			
			if(input_studentName.isEmpty()) {
				input_studentName.requestFocus()
				input_studentName.error = "학생 이름을 입력해주세요."
				return@onClick
			}
			
			if(input_studentBirth.isEmpty()) {
				input_studentBirth.requestFocus()
				input_studentBirth.error = "학생 생년월일을 입력해주세요."
				return@onClick
			}
			
			val name = input_studentName.text!!.toString()
			val birth = input_studentBirth.text!!.toString()
			
			if(birth.length != 6) {
				input_studentBirth.requestFocus()
				input_studentBirth.error = "생년월일을 6자리로 입력해주세요. (주민등록번호 앞 6자리, YYMMDD 형식)"
				return@onClick
			}
			
			lifecycleScope.launch main@{
				// TODO: show progress
				try {
					val userIdentifier = findUser(
						instituteInfo,
						GetUserTokenRequestBody(
							institute = instituteInfo,
							name = name,
							birthday = birth,
							loginType = LoginType.school /* TODO */
						)
					)
					
					val password = promptInput { edit, _ ->
						setTitle("비밀번호를 입력해주세요.")
						edit.filters = arrayOf(InputFilter { source, start, end, dest, destStart, destEnd ->
							val result = dest.replaceRange(destStart, destEnd, source.substring(start, end))
							if(result.length > 4 || result.any { !it.isDigit() }) null
							else source
						})
					} ?: return@main
					
					val token = catchErrorThanToast {
						validatePassword(instituteInfo, userIdentifier, password)
					} ?: return@main
					
					if(token is PasswordWrong) {
						showToastSuspendAsync("잘못된 비밀번호입니다. 다시 시도해주세요. (${token.data.failedCount}회 틀림)")
						return@main
					}
					require(token is UsersToken)
					
					val groups = getUserGroup(instituteInfo, token)
					singleOfUserGroup(groups) ?: return@main // TODO: many users
					
					pref.institute = instituteInfo
					pref.user = UserLoginInfo(userIdentifier, token)
					pref.setting = UserSetting(
						loginType = LoginType.school, // TODO
						region = sRegions.getValue(input_region.text.toString()),
						level = sSchoolLevels.getValue(input_level.text.toString()),
						schoolName = instituteInfo.name,
						studentName = name,
						studentBirth = birth
					)
					
					// success
					finish()
					
					if(first) {
						startActivity(Intent(this@FirstActivity, MainActivity::class.java))
						pref.firstState = 1
					}
				} catch(e: Throwable) {
					e.printStackTrace()
					showToastSuspendAsync("잘못된 학생 정보입니다.")
				}
				
			}
		}
		
		pref.setting?.let { setting ->
			input_loginType.setText("학교", false) // TODO
			input_region.setText(sRegions.entries.first { it.value == setting.region }.key, false)
			input_level.setText(
				sSchoolLevels.entries.first { it.value == setting.level }.key,
				false
			)
			input_schoolName.setText(setting.schoolName)
			input_studentName.setText(setting.studentName)
			input_studentBirth.setText(setting.studentBirth)
		}
		
		if(first) checkNotice()
	}
}

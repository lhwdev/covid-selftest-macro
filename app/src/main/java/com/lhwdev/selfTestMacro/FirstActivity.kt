package com.lhwdev.selfTestMacro

import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Filter
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import com.lhwdev.selfTestMacro.api.*
import kotlinx.android.synthetic.main.activity_first.*
import kotlinx.coroutines.launch


// TODO list:
// * use view binding
// * use preference fragment etc.
// * use better model, like MVVM. This page is small so whole code is not complicated, but
//   on larger project, this imperative style lacks.


class FirstActivity : AppCompatActivity() {
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_first)
		window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
		val pref = PreferenceState(prefMain())
		val first = intent.hasExtra("first")
		preferenceState = pref
		
		setSupportActionBar(toolbar)
		
		var schoolInfo: SchoolInfo? = null
		
		fun adapter(list: List<String>) = object : ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, list) {
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
		
		button_checkSchoolInfo.setOnClickListener {
//			if(input_region.) {
//				showToast("시/도 정보를 입력해주세요")
//				return@setOnClickListener
//			}
//
//			if(spinner_level.selectedItemPosition == -1) {
//				showToast("시/도 정보를 입력해주세요")
//				return@setOnClickListener
//			}
			
			val schoolName = input_schoolName.text
			if(schoolName == null || schoolName.isEmpty()) {
				showToast("학교 이름을 입력해주세요")
				return@setOnClickListener
			}
			val nameString = schoolName.toString()
			
			val regionCode = sRegions.getValue(input_region.text.toString())
			val levelCode = sSchoolLevels.getValue(input_level.text.toString())
			
			lifecycleScope.launch {
				val snackbar =
					Snackbar.make(button_checkSchoolInfo, "잠시만 기다려주세요", Snackbar.LENGTH_INDEFINITE)
				snackbar.show()
				
				fun selectSchool(school: SchoolInfo) {
					schoolInfo = school
					
					// ui interaction
					schoolName.clear()
					schoolName.append(school.name)
					button_checkSchoolInfo.icon = ResourcesCompat.getDrawable(
						resources,
						R.drawable.ic_baseline_check_24,
						theme
					)
				}
				
				val data = getSchoolData(
					regionCode = regionCode,
					schoolLevelCode = levelCode.toString(),
					name = nameString,
					loginType = LoginType.school /* TODO */
				)
				snackbar.dismiss()
				if(data.schoolList.isEmpty()) {
					showToast("학교를 찾을 수 없습니다. 이름을 바르게 입력했는지 확인해주세요.")
					return@launch
				}
				
				if(data.schoolList.size == 1)
					selectSchool(data.schoolList[0])
				else AlertDialog.Builder(this@FirstActivity).apply {
					setTitle("학교를 선택해주세요")
					setItems(
						data.schoolList.map { "${it.name}(${it.address})" }.toTypedArray(),
						DialogInterface.OnClickListener { _, which ->
							selectSchool(data.schoolList[which])
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
		
		button_done.setOnClickListener onClick@{
			val school = schoolInfo ?: run {
				button_checkSchoolInfo.callOnClick()
				schoolInfo ?: return@onClick
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

//			require(input_loginType.selectedItemPosition == 0)
			
			if(birth.length != 6) {
				input_studentBirth.requestFocus()
				input_studentBirth.error = "생년월일을 6자리로 입력해주세요. (주민등록번호 앞 6자리, YYMMDD 형식)"
				return@onClick
			}
			
			lifecycleScope.launch {
				// TODO: show progress
				try {
					val token = findUser(
						school,
						GetUserTokenRequestBody(
							schoolInfo = school,
							name = name,
							birthday = birth,
							loginType = LoginType.school /* TODO */
						)
					)
					val groups = getUserGroup(school, token)
					if(groups.isEmpty()) {
						showToastSuspendAsync("해당 정보의 학생을 찾지 못했습니다.")
						return@launch
					}
					if(groups.size != 1) {
						showToastSuspendAsync("여러명의 자가진단은 아직 지원하지 않습니다.")
					}
					val userInfo = groups.single()
					
					pref.school = school
					pref.user = userInfo
					pref.setting = UserSetting(
						loginType = LoginType.school, // TODO
						region = sRegions.getValue(input_region.text.toString()),
						level = sSchoolLevels.getValue(input_level.text.toString()),
						schoolName = school.name,
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
					showToastSuspendAsync("잘못된 학생 정보입니다.")
				}
				
			}
		}
		
		pref.setting?.let { setting ->
			input_loginType.setText("학교", false) // TODO
			input_region.setText(sRegions.entries.first { it.value == setting.region}.key, false)
			input_level.setText(sSchoolLevels.entries.first { it.value == setting.level }.key, false)
			input_schoolName.setText(setting.schoolName)
			input_studentName.setText(setting.studentName)
			input_studentBirth.setText(setting.studentBirth)
		}
	}
}

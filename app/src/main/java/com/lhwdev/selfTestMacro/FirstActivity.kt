package com.lhwdev.selfTestMacro

import android.os.Bundle
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.lifecycleScope
import kotlinx.android.synthetic.main.activity_first.*
import kotlinx.coroutines.launch


class FirstActivity : AppCompatActivity() {
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_first)
		window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
		val pref = PreferenceState(prefMain())
		val first = intent.hasExtra("first")
		preferenceState = pref
		
		setSupportActionBar(toolbar)
		
		var schoolInfo: com.lhwdev.selfTestMacro.api.SchoolInfo? = null
		
		spinner_region.adapter =
			ArrayAdapter(this, android.R.layout.simple_list_item_1, sRegions.keys.toList())
		spinner_level.adapter =
			ArrayAdapter(this, android.R.layout.simple_list_item_1, sSchoolLevels.keys.toList())
		
		button_checkSchoolInfo.setOnClickListener {
			if(spinner_region.selectedItemPosition == -1) {
				showToast("시/도 정보를 입력해주세요")
				return@setOnClickListener
			}
			
			if(spinner_level.selectedItemPosition == -1) {
				showToast("시/도 정보를 입력해주세요")
				return@setOnClickListener
			}
			
			val schoolName = input_schoolName.text
			if(schoolName == null || schoolName.isEmpty()) {
				showToast("학교 이름을 입력해주세요")
				return@setOnClickListener
			}
			val nameString = schoolName.toString()
			
			val regionCode = sRegions[spinner_region.selectedItem]!!
			val levelCode = sSchoolLevels[spinner_level.selectedItem]!!
		
			lifecycleScope.launch {
				val data = com.lhwdev.selfTestMacro.api.getSchoolData(regionCode = regionCode, schoolLevelCode = levelCode.toString(), name = nameString)
				if(data.schoolList.isEmpty()) {
					showToast("학교를 찾을 수 없습니다. 이름을 바르게 입력했는지 확인해주세요.")
					return@launch
				}
				
				if(data.schoolList.size == 1) {
					val school = data.schoolList[0]
					schoolInfo = school
					
					// ui interaction
					schoolName.clear()
					schoolName.append(school.name)
					button_checkSchoolInfo.icon = ResourcesCompat.getDrawable(resources, R.drawable.ic_baseline_check_24, theme)
				}
			}
		}
		
		input_studentName.doAfterTextChanged {
			input_studentName.error = null
		}
		
		input_studentBirth.doAfterTextChanged {
			input_studentBirth.error = null
		}
		
		button_done.setOnClickListener onClick@ {
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
			if(birth.length != 6) {
				input_studentBirth.requestFocus()
				input_studentBirth.error = "생년월일을 6자리로 입력해주세요. (주민등록번호 앞 6자리, YYMMDD 형식)"
				return@onClick
			}
			
			lifecycleScope.launch {
				com.lhwdev.selfTestMacro.api.findUser()
			}
		}
	}
}
package com.lhwdev.selfTestMacro

import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.text.InputFilter
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
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
		
		val session = selfTestSession(this)
		
		setContentView(R.layout.activity_first)
		window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
		val pref = preferenceState
		val first = intent.hasExtra("first")
		
		setSupportActionBar(toolbar)
		
		var institute: InstituteResult? = null
		var searchKey: InstituteSearchKey? = null
		
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
				if(nameString.length <= 1) {
					showToast("학교 이름은 2글자 이상으로 입력해주세요.")
					return@launch
				}
				
				val snackBar =
					Snackbar.make(button_checkSchoolInfo, "잠시만 기다려주세요", Snackbar.LENGTH_INDEFINITE)
				snackBar.show()
				
				fun selectInstitute(result: InstituteResult) {
					institute = result
					
					// ui interaction
					schoolName.clear()
					schoolName.append(result.info.name)
					button_checkSchoolInfo.icon = ResourcesCompat.getDrawable(
						resources,
						R.drawable.ic_baseline_check_24,
						theme
					)
					
					scrollView.smoothScrollTo(0, scrollView.height)
				}
				
				val data = session.getSchoolData(
					regionCode = regionCode,
					schoolLevelCode = levelCode.toString(),
					name = nameString
				)
				
				snackBar.dismiss()
				if(data.list.isEmpty()) {
					showToast("학교를 찾을 수 없습니다. 이름을 바르게 입력했는지 확인해주세요.")
					return@launch
				}
				searchKey = data.searchKey
				
				if(data.list.size == 1)
					selectInstitute(data.list[0])
				else AlertDialog.Builder(this@FirstActivity).apply {
					setTitle("학교를 선택해주세요")
					setItems(
						data.list.map { "${it.info.name}(${it.info.address})" }.toTypedArray(),
						DialogInterface.OnClickListener { _, which ->
							selectInstitute(data.list[which])
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
					val password = promptInput { edit, _ ->
						setTitle("비밀번호를 입력해주세요.")
						edit.inputType = EditorInfo.TYPE_CLASS_NUMBER
						edit.filters = arrayOf(InputFilter { source, start, end, dest, destStart, destEnd ->
							val result = dest.replaceRange(destStart, destEnd, source.substring(start, end))
							if(result.length > 4 || result.any { !it.isDigit() }) null
							else source
						})
					} ?: return@main
					
					val userQuery = UserQuery(name = name, birthday = birth)
					val result = catchErrorThanToast {
						session.findUser(
							institute = instituteInfo,
							searchKey = searchKey!!,
							userQuery = userQuery,
							password = password
						)
						
					} ?: return@main
					
					if(result is FindUserResult.Failed) {
						showToastSuspendAsync(result.toString())
						return@main
					}
					require(result is FindUserResult.Success)
					val token = result.token
					
					val groups = tryAtMost(maxTrial = 3) {
						try {
							session.getUserGroup(instituteInfo.info, token)
						} catch(th: Throwable) {
							if(th.message?.contains("userNameEncpt") == true) {
								showToastSuspendAsync("아직 여러명의 자가진단은 지원하지 않습니다.")
								null
							} else throw th
						}
					} ?: return@main
					singleOfUserGroup(groups) ?: return@main // TODO: many users
					
					pref.info = UserLoginInfo(
						institute = instituteInfo,
						userQuery = userQuery,
						password = password/*, token*/
					)
					
					// success
					finish()
					
					if(first) {
						startActivity(Intent(this@FirstActivity, MainActivity::class.java))
						pref.firstState = 1
					}
				} catch(e: Throwable) {
					onError(e, "잘못된 학생 정보입니다.", forceShow = true)
					e.printStackTrace()
				}
				
			}
		}
		
		pref.info?.let { info ->
			input_loginType.setText("학교", false) // TODO
			input_region.setText(sRegions.entries.first { it.value == info.institute.regionCode }.key, false)
			val level = info.institute.schoolLevelCode!!.toInt()
			input_level.setText(
				sSchoolLevels.entries.first { it.value == level }.key,
				false
			)
			input_schoolName.setText(info.institute.info.name)
			input_studentName.setText(info.userQuery.name)
			input_studentBirth.setText(info.userQuery.birthday)
		}
	}
}

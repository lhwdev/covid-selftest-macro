package com.lhwdev.selfTestMacro

import com.lhwdev.github.repo.Repository
import com.lhwdev.github.sGithubInstanceDefault
import com.lhwdev.selfTestMacro.models.Version


object App {
	lateinit var version: Version
	lateinit var versionName: String
	var versionCode: Int = -1
	
	val githubRepo = Repository(sGithubInstanceDefault, "lhwdev", "covid-selftest-macro")
	val metaBranch = "meta"
}

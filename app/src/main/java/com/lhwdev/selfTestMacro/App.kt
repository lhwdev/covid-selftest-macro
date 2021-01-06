package com.lhwdev.selfTestMacro

import com.lhwdev.github.repo.Repository
import com.lhwdev.github.sGithubInstanceDefault


object App {
	val version = Version(BuildConfig.VERSION_NAME)
	val githubRepo = Repository(sGithubInstanceDefault, "lhwdev", "covid-selftest-macro")
}

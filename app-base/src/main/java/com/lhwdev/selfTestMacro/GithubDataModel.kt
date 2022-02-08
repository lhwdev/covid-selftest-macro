package com.lhwdev.selfTestMacro

import com.lhwdev.github.repo.Branch
import com.lhwdev.github.repo.Content
import com.lhwdev.github.repo.Repository
import com.lhwdev.github.repo.branch
import com.lhwdev.github.sGithubInstanceDefault


class GithubDataModel(
	val repository: Repository,
	val privacyPolicy: Content,
	val appMetaBranch: Branch
)


val sDefaultRepository = Repository(sGithubInstanceDefault, "lhwdev", "covid-selftest-macro")


fun defaultGithubDataModel(repository: Repository): GithubDataModel {
	val master = repository.branch("master")
	
	return GithubDataModel(
		repository = repository,
		privacyPolicy = master.contentOf("PRIVACY_POLICY.md"),
		appMetaBranch = repository.branch("app-meta")
	)
}

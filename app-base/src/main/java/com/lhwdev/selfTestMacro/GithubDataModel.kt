package com.lhwdev.selfTestMacro

import com.lhwdev.github.repo.Branch
import com.lhwdev.github.repo.Content
import com.lhwdev.github.repo.Repository
import com.lhwdev.github.repo.branch
import com.lhwdev.github.sGithubInstanceDefault


class GithubDataModel(
	val repository: Repository,
	val privacyPolicy: Content,
	val meta: Meta
) {
	class Meta(
		val branch: Branch,
		val developerInfo: Content,
		val specialThanks: Content
	)
}


val sDefaultRepository = Repository(sGithubInstanceDefault, "lhwdev", "covid-selftest-macro")


fun defaultGithubDataModel(repository: Repository): GithubDataModel {
	val master = repository.branch("master")
	val meta = repository.branch("app-meta")
	
	return GithubDataModel(
		repository = repository,
		privacyPolicy = master.contentOf("PRIVACY_POLICY.md"),
		meta = GithubDataModel.Meta(
			branch = meta,
			developerInfo = meta.contentOf("src/info/developer.json"),
			specialThanks = meta.contentOf("src/info/special-thanks.json")
		)
	)
}

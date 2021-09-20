package com.lhwdev.github.repo

import com.lhwdev.fetch.FetchResult
import com.lhwdev.fetch.fetch
import com.lhwdev.fetch.get


data class Branch(val repository: Repository, val branchName: String?) : ContentSource {
	override suspend fun getContent(path: String, accept: GithubContentType): FetchResult {
		val realPath = "contents/$path"
		
		return fetch(
			url = if(branchName == null) repository.url[realPath] else repository.url[realPath, "ref" to branchName],
			headers = mapOf("Accept" to accept.contentType)
		)
	}
}


fun Repository.branch(name: String): Branch = Branch(this, name)
fun Repository.defaultBranch(): Branch = Branch(this, null)

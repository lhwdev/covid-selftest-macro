package com.lhwdev.github.repo

import com.lhwdev.fetch.FetchResult
import com.lhwdev.fetch.fetch
import com.lhwdev.fetch.get
import java.net.URL


data class Branch(val repository: Repository, val branchName: String, val default: Boolean?) : ContentSource {
	val webBlobUrl: URL get() = repository.webUrl["blob/$branchName"]
	
	override fun contentOf(path: String): Content = GitContent(this, path)
}

private class GitContent(private val branch: Branch, private val path: String) : Content {
	override val url: URL = run {
		val realPath = "contents/$path"
		
		if(branch.default == true) {
			branch.repository.url[realPath]
		} else {
			branch.repository.url[realPath, "ref" to branch.branchName]
		}
	}
	
	override val webUrl: URL
		get() = branch.webBlobUrl[path]
	
	override suspend fun get(accept: GithubContentType): FetchResult = fetch(
		url = url,
		headers = mapOf("Accept" to accept.contentType)
	)
}


fun Repository.branch(name: String): Branch = Branch(this, name, default = null)
// fun Repository.defaultBranch(): Branch = Branch(this, null)

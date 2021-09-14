package com.lhwdev.github.repo

import com.lhwdev.fetch.FetchResult
import com.lhwdev.fetch.fetch
import com.lhwdev.fetch.get


enum class GithubContentType(val contentType: String) {
	raw("application/vnd.github.VERSION.raw"),
	html("application/vnd.github.VERSION.html")
}


suspend fun Repository.getContent(
	path: String,
	branch: String?,
	accept: GithubContentType = GithubContentType.raw
): FetchResult {
	val realPath = "contents/$path"
	return fetch(
		url = if(branch == null) url[realPath] else url[realPath, "ref" to branch],
		headers = mapOf("Accept" to accept.contentType)
	)
}

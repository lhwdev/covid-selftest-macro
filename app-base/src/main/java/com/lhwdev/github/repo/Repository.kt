package com.lhwdev.github.repo

import com.lhwdev.fetch.get
import com.lhwdev.github.GithubInstance
import java.net.URL


data class Repository(val instance: GithubInstance, val owner: String, val name: String) {
	val url = instance.url["repos/$owner/$name"]
	val webUrl: URL get() = instance.webUrl["$owner/$name"]
}

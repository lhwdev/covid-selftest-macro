package com.lhwdev.github.repo

import com.lhwdev.github.GithubInstance
import com.lhwdev.selfTestMacro.get


data class Repository(val instance: GithubInstance, val owner: String, val name: String) {
	val url = instance.url["repos/$owner/$name"]
}

/*
 * Copyright (c) 2015 Fran Montiel
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.lhwdev.selfTestMacro

import android.content.SharedPreferences
import android.util.Log
import androidx.core.content.edit
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.net.CookieStore
import java.net.HttpCookie
import java.net.URI
import java.net.URISyntaxException


@Serializable
data class SerializableHttpCookie(@Serializable(with = HttpCookieSerializer::class) val cookie: HttpCookie)



class PersistentCookieStore(private val sharedPreferences: SharedPreferences) : CookieStore {
	
	init {
		loadAllFromPersistence()
	}
	
	// In memory
	private var allCookies: MutableMap<URI, MutableSet<HttpCookie>> = mutableMapOf()
	
	private fun loadAllFromPersistence() {
		allCookies = HashMap()
		
		val allPairs: Map<String, *> = sharedPreferences.all
		for(entry: Map.Entry<String, *> in allPairs.entries) {
			val uriAndName = entry.key.split(SP_KEY_DELIMITER).toTypedArray()
			try {
				val uri = URI(uriAndName[0])
				val encodedCookie = entry.value as String
				val cookie = Json.decodeFromString(SerializableHttpCookie.serializer(), encodedCookie).cookie
				var targetCookies = allCookies[uri]
				if(targetCookies == null) {
					targetCookies = HashSet()
					allCookies[uri] = targetCookies
				}
				// Repeated cookies cannot exist in persistence
				// targetCookies.remove(cookie)
				targetCookies.add(cookie)
			} catch(e: URISyntaxException) {
				Log.w(TAG, e)
			}
		}
	}
	
	@Synchronized
	override fun add(uri: URI, cookie: HttpCookie) {
		val cookieUri = cookieUri(uri, cookie)
		val targetCookies = allCookies[cookieUri] ?: run {
			val cookies = HashSet<HttpCookie>()
			allCookies[cookieUri] = cookies
			cookies
		}
		targetCookies.remove(cookie)
		targetCookies.add(cookie)
		saveToPersistence(cookieUri, cookie)
	}
	
	private fun saveToPersistence(uri: URI, cookie: HttpCookie): Unit = sharedPreferences.edit {
		putString(
			uri.toString() + SP_KEY_DELIMITER + cookie.name,
			Json.encodeToString(SerializableHttpCookie.serializer(), SerializableHttpCookie(cookie))
		)
	}
	
	@Synchronized
	override operator fun get(uri: URI): List<HttpCookie> {
		return getValidCookies(uri)
	}
	
	@Synchronized
	override fun getCookies(): List<HttpCookie> {
		val allValidCookies: MutableList<HttpCookie> = ArrayList()
		
		for(storedUri in allCookies.keys) {
			allValidCookies.addAll(getValidCookies(storedUri))
		}
		return allValidCookies
	}
	
	private fun getValidCookies(uri: URI): List<HttpCookie> {
		val targetCookies = mutableListOf<HttpCookie>()
		// If the stored URI does not have a path then it must match any URI in
		// the same domain
		for(storedUri in allCookies.keys) {
			// Check ith the domains match according to RFC 6265
			if(checkDomainsMatch(storedUri.host, uri.host)) {
				// Check if the paths match according to RFC 6265
				if(checkPathsMatch(storedUri.path, uri.path)) {
					targetCookies.addAll(allCookies[storedUri]!!)
				}
			}
		}
		
		// Check it there are expired cookies and remove them
		if(targetCookies.isNotEmpty()) {
			val cookiesToRemoveFromPersistence = mutableListOf<HttpCookie>()
			val it: MutableIterator<HttpCookie> = targetCookies.iterator()
			while(it.hasNext()) {
				val currentCookie: HttpCookie = it.next()
				if(currentCookie.hasExpired()) {
					cookiesToRemoveFromPersistence.add(currentCookie)
					it.remove()
				}
			}
			if(cookiesToRemoveFromPersistence.isNotEmpty()) {
				removeFromPersistence(uri, cookiesToRemoveFromPersistence)
			}
		}
		return targetCookies
	}
	
	/* http://tools.ietf.org/html/rfc6265#section-5.1.3

    A string domain-matches a given domain string if at least one of the
    following conditions hold:

    o  The domain string and the string are identical.  (Note that both
    the domain string and the string will have been canonicalized to
    lower case at this point.)

    o  All of the following conditions hold:

        *  The domain string is a suffix of the string.

        *  The last character of the string that is not included in the
           domain string is a %x2E (".") character.

        *  The string is a host name (i.e., not an IP address). */
	private fun checkDomainsMatch(cookieHost: String, requestHost: String): Boolean {
		return requestHost == cookieHost || requestHost.endsWith(".$cookieHost")
	}
	
	/*  http://tools.ietf.org/html/rfc6265#section-5.1.4

        A request-path path-matches a given cookie-path if at least one of
        the following conditions holds:

        o  The cookie-path and the request-path are identical.

        o  The cookie-path is a prefix of the request-path, and the last
        character of the cookie-path is %x2F ("/").

        o  The cookie-path is a prefix of the request-path, and the first
        character of the request-path that is not included in the cookie-
        path is a %x2F ("/") character. */
	private fun checkPathsMatch(cookiePath: String, requestPath: String): Boolean {
		return requestPath == cookiePath ||
			requestPath.startsWith(cookiePath) && cookiePath[cookiePath.length - 1] == '/' ||
			requestPath.startsWith(cookiePath) && requestPath.substring(cookiePath.length)[0] == '/'
	}
	
	private fun removeFromPersistence(uri: URI, cookiesToRemove: List<HttpCookie>) {
		val editor: SharedPreferences.Editor = sharedPreferences.edit()
		for(cookieToRemove: HttpCookie in cookiesToRemove) {
			editor.remove(uri.toString() + SP_KEY_DELIMITER + cookieToRemove.name)
		}
		editor.apply()
	}
	
	@Synchronized
	override fun getURIs(): List<URI> = allCookies.keys.toList()
	
	@Synchronized
	override fun remove(uri: URI, cookie: HttpCookie): Boolean {
		val targetCookies: MutableSet<HttpCookie>? = allCookies[uri]
		val cookieRemoved = targetCookies != null && targetCookies.remove(cookie)
		if(cookieRemoved) {
			removeFromPersistence(uri, cookie)
		}
		return cookieRemoved
	}
	
	private fun removeFromPersistence(uri: URI, cookieToRemove: HttpCookie) {
		val editor: SharedPreferences.Editor = sharedPreferences.edit()
		editor.remove(
			(uri.toString() + SP_KEY_DELIMITER + cookieToRemove.name)
		)
		editor.apply()
	}
	
	@Synchronized
	override fun removeAll(): Boolean {
		allCookies.clear()
		removeAllFromPersistence()
		return true
	}
	
	private fun removeAllFromPersistence() {
		sharedPreferences.edit().clear().apply()
	}
	
	companion object {
		private val TAG = PersistentCookieStore::class.simpleName
		
		// Persistence
		private const val SP_COOKIE_STORE = "cookieStore"
		private const val SP_KEY_DELIMITER = "|" // Unusual char in URL
		
		/**
		 * Get the real URI from the cookie "domain" and "path" attributes, if they
		 * are not set then uses the URI provided (coming from the response)
		 *
		 * @param uri
		 * @param cookie
		 * @return
		 */
		private fun cookieUri(uri: URI, cookie: HttpCookie): URI {
			var cookieUri: URI = uri
			if(cookie.domain != null) {
				// Remove the starting dot character of the domain, if exists (e.g: .domain.com -> domain.com)
				var domain: String = cookie.domain
				if(domain[0] == '.') {
					domain = domain.substring(1)
				}
				try {
					cookieUri = URI(
						if(uri.scheme == null) "http" else uri.scheme, domain,
						if(cookie.path == null) "/" else cookie.path, null
					)
				} catch(e: URISyntaxException) {
					Log.w(TAG, e)
				}
			}
			return cookieUri
		}
	}
}

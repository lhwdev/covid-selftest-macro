package com.lhwdev.selfTestMacro.repository

import com.lhwdev.selfTestMacro.database.DatabaseManager
import com.lhwdev.selfTestMacro.database.DbTestGroup
import com.lhwdev.selfTestMacro.database.DbUser
import com.lhwdev.selfTestMacro.debug.log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeToSequence
import java.io.File


private val json = Json


class SelfTestLog(val logFile: File) {
	@Serializable
	sealed class Entry
	
	@Serializable
	class SelfTestGroup(
		val id: Int,
		val hash: Long,
		val name: String,
		val abbr: String,
		val success: Boolean,
		val message: String
	) : Entry() {
		companion object {
			fun hash(users: List<DbUser>): Long =
				users.fold(0L) { acc, user -> (acc shl 8) or user.userCode.hashCode().toLong() }
		}
	}
	
	@Serializable
	class SelfTestUser(val userCode: String, val name: String, val success: Boolean, val message: String) : Entry()
	
	
	private var fileEntries: MutableList<Entry>? = try {
		logFile.parentFile?.mkdirs()
		if(logFile.createNewFile()) {
			mutableListOf()
		} else {
			null
		}
	} catch(_: Throwable) {
		null
	}
	
	private var extraEntries: MutableList<Entry> = mutableListOf()
	
	
	val cachedEntries: List<Entry>
		get() {
			val cache = fileEntries
			return when {
				cache == null -> extraEntries
				extraEntries.isEmpty() -> cache
				else -> {
					val joined = mutableListOf<Entry>()
					joined += cache
					joined += extraEntries
					fileEntries = joined
					joined
				}
			}
		}
	
	suspend fun fetchEntries(): List<Entry> {
		if(fileEntries == null) try {
			val fetched = withContext(Dispatchers.IO) {
				logFile.inputStream().use {
					json.decodeToSequence(stream = it, deserializer = Entry.serializer()).toList()
				}
			}
			fileEntries = fetched
		} catch(th: Throwable) {
			log("[SelfTestLog] error while reading: $th")
		}
		
		return cachedEntries
	}
	
	private suspend fun logLine(entry: Entry) {
		val line = json.encodeToString(Entry.serializer(), entry)
		
		val cache = fileEntries
		if(cache == null || extraEntries.isNotEmpty()) {
			extraEntries += entry
		} else {
			cache += entry
		}
		
		try {
			withContext(Dispatchers.IO) { logFile.appendText("\n$line") }
		} catch(th: Throwable) {
			log("[SelfTestLog] error while writing: $th")
		}
	}
	
	suspend fun logSelfTest(database: DatabaseManager, user: DbUser, success: Boolean, message: String) {
		logLine(SelfTestUser(userCode = user.userCode, name = user.name, success = success, message = message))
	}
	
	suspend fun logSelfTest(database: DatabaseManager, group: DbTestGroup, success: Boolean, message: String) {
		val users = with(database) { group.target.allUsers }
		val hash = SelfTestGroup.hash(users)
		val abbr = StringBuilder()
		for((index, user) in users.withIndex()) {
			if(abbr.length > 10) break
			
			if(index != 0) abbr.append(", ")
			abbr.append(user)
		}
		
		logLine(
			SelfTestGroup(
				id = group.id,
				hash = hash,
				name = with(database) { group.target.name },
				abbr = abbr.toString(),
				success = success,
				message = message
			)
		)
	}
}

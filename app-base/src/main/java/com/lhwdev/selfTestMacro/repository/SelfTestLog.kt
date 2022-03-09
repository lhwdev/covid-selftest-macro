package com.lhwdev.selfTestMacro.repository

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.lhwdev.selfTestMacro.database.*
import com.lhwdev.selfTestMacro.debug.log
import kotlinx.coroutines.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeToSequence
import kotlinx.serialization.json.encodeToStream
import java.io.File
import java.io.OutputStream


private val json = Json


class SelfTestLog(val logFile: File, holder: PreferenceHolder, private val coroutineScope: CoroutineScope) {
	private inner class DbAction(val entries: MutableList<Entry>) : () -> Unit {
		var task: Job? = null
		
		override fun invoke() {
			task = coroutineScope.launch(Dispatchers.IO) {
				run()
			}
		}
		
		suspend fun run() {
			val task = task
			if(task != null) {
				task.join()
				return
			}
			logFile.outputStream().buffered().use {
				for(entry in entries) logLineTo(entry, it)
			}
		}
	}
	
	
	@Serializable
	sealed class Entry {
		abstract val id: Int
	}
	
	@SerialName("submitUser")
	@Serializable
	class SelfTestUser(
		override val id: Int,
		val userCode: String,
		val name: String,
		val success: Boolean,
		val message: String,
		val timeMillis: Long
	) : Entry()
	
	
	private var fileEntries: MutableList<Entry>? = try {
		logFile.parentFile?.mkdirs()
		if(logFile.createNewFile()) {
			mutableListOf() // new file which did not exist before
		} else {
			null
		}
	} catch(_: Throwable) {
		null
	}
	
	var nextId by holder.preferenceInt("nextId", defaultValue = 0)
	
	fun nextId(): Int = nextId++
	
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
		currentDbTransaction?.flush(this) {
			(it as DbAction).run()
		}
		
		if(fileEntries == null) try {
			val fetched = withContext(Dispatchers.IO) {
				logFile.inputStream().use {
					@OptIn(ExperimentalSerializationApi::class)
					json.decodeToSequence(stream = it, deserializer = Entry.serializer()).toList()
				}
			}
			fileEntries = fetched.toMutableList()
		} catch(th: Throwable) {
			log("[SelfTestLog] error while reading: $th")
		}
		
		return cachedEntries
	}
	
	private suspend fun logLine(entry: Entry) {
		val cache = fileEntries
		if(cache == null || extraEntries.isNotEmpty()) {
			extraEntries += entry
		} else {
			cache += entry
		}
		
		val transaction = currentDbTransaction
		if(transaction == null) {
			try {
				logFile.outputStream().buffered().use {
					logLineTo(entry, it)
				}
			} catch(th: Throwable) {
				log("[SelfTestLog] error while writing: $th")
			}
		} else {
			val previous = transaction.operations[this]
			if(previous == null) {
				transaction.operations[this] = DbAction(mutableListOf(entry))
			} else {
				previous as DbAction
				previous.entries += entry
			}
		}
	}
	
	private suspend fun logLineTo(entry: Entry, to: OutputStream) {
		try {
			withContext(Dispatchers.IO) {
				to.write('\n'.code)
				@OptIn(ExperimentalSerializationApi::class)
				json.encodeToStream(Entry.serializer(), entry, to)
			}
		} catch(th: Throwable) {
			log("[SelfTestLog] error while writing: $th")
		}
	}
	
	suspend fun logSelfTest(
		database: AppDatabase,
		user: DbUser,
		success: Boolean,
		message: String,
		timeMillis: Long = System.currentTimeMillis()
	): Int {
		val id = nextId()
		
		logLine(
			SelfTestUser(
				id = id,
				userCode = user.userCode,
				name = user.name,
				success = success,
				message = message,
				timeMillis = timeMillis
			)
		)
		return id
	}
}

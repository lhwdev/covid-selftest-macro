package com.lhwdev.selfTestMacro

import kotlinx.serialization.Serializable

@Serializable
data class NotificationObject(
	val notificationVersion: Int,
	val entries: List<NotificationEntry>
)

@Serializable
data class NotificationEntry(
	val id: String,
	val version: VersionSpec?, // null to all versions
	val priority: Priority,
	val title: String,
	val message: String
) {
	enum class Priority { once, everyWithDoNotShowAgain, every }
}

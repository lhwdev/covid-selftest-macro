package com.lhwdev.selfTestMacro.database


class Transaction {
	val operations = mutableMapOf<Any?, () -> Unit>()
	
	fun apply() {
		for(operation in operations.values) {
			operation()
		}
	}
}


@PublishedApi
internal val sDbTransaction = ThreadLocal<Transaction?>()

inline val currentDbTransaction: MutableMap<Any?, () -> Unit>? get() = sDbTransaction.get()?.operations


inline fun <R> transactDb(block: () -> R): R {
	val last = currentDbTransaction
	val transaction = sDbTransaction.get() ?: Transaction()
	if(last == null) sDbTransaction.set(transaction)
	
	return try {
		block()
	} finally {
		if(last == null) {
			transaction.apply()
			sDbTransaction.set(null)
		}
	}
}


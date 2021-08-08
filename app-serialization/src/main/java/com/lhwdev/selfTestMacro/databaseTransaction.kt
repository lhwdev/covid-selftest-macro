package com.lhwdev.selfTestMacro


class Transaction {
	val operations = mutableMapOf<Any?, () -> Unit>()
	
	fun apply() {
		for(operation in operations.values) {
			operation()
		}
	}
}


@PublishedApi
internal val sTransaction = ThreadLocal<Transaction?>()

inline val currentTransaction: MutableMap<Any?, () -> Unit>? get() = sTransaction.get()?.operations


inline fun <R> transactDb(block: () -> R): R {
	val last = currentTransaction
	val transaction = sTransaction.get() ?: Transaction()
	if(last == null) sTransaction.set(transaction)
	
	return try {
		block()
	} finally {
		if(last == null) {
			transaction.apply()
			sTransaction.set(null)
		}
	}
}


@file:OptIn(ExperimentalContracts::class)

package com.lhwdev.selfTestMacro.database

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract


class DbTransaction {
	val operations = mutableMapOf<Any?, () -> Unit>()
	
	fun apply() {
		for(operation in operations.values) {
			operation()
		}
	}
}


@PublishedApi
internal val sDbTransaction = ThreadLocal<DbTransaction?>()

inline val currentDbTransaction: DbTransaction? get() = sDbTransaction.get()


inline fun <R> transactDb(block: () -> R): R {
	contract { callsInPlace(block, InvocationKind.EXACTLY_ONCE) }
	
	val last = sDbTransaction.get()
	val transaction = last ?: DbTransaction()
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

fun pushDbOperation(key: Any?, block: () -> Unit) {
	val current = currentDbTransaction
	
	if(current == null) { // should I use transactDb { ... }?
		block()
	} else {
		current.operations[key] = block
	}
}


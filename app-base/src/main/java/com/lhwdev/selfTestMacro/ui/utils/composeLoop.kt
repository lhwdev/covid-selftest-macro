package com.lhwdev.selfTestMacro.ui.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember


@Composable
inline fun <reified T> rememberLoopLinkedList(size: Int): LoopLinkedList<T> {
	val list = remember { LoopLinkedList<T>(array = arrayOfNulls(size)) }
	if(list.array.size != size) list.array = list.array.copyOf(size)
	return list
}


// WARNING: does not fit for general purposes as it ignores recomposition entirely
class LoopLinkedList<T>(@PublishedApi internal var array: Array<T?>) {
	fun previous(index: Int): T? = array.getOrNull(index - 1)
	
	fun updateCurrent(index: Int, value: T) {
		array[index] = value
	}
}

@file:Suppress("UNUSED_EXPRESSION")

package com.lhwdev.selfTestMacro.ui.utils

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import com.lhwdev.selfTestMacro.ui.Ref
import kotlinx.collections.immutable.mutate
import kotlinx.collections.immutable.toPersistentList
import kotlin.math.max


enum class VisibilityAnimationState(
	val fromState: Boolean,
	val targetState: Boolean,
	val animating: Boolean,
	val virtual: Boolean
) {
	enter(fromState = false, targetState = true, animating = true, virtual = false),
	visible(fromState = true, targetState = true, animating = false, virtual = false),
	waitingExit(fromState = true, targetState = true, animating = false, virtual = true),
	exit(fromState = true, targetState = false, animating = true, virtual = true)
}

@Stable
class AnimationListEntry<T>(val item: T, var state: VisibilityAnimationState) {
	override fun toString() = "AnimationListEntry(item=$item, state=$state)"
}


@Stable
var sDebugAnimateListAsComposable = false


typealias AnimateListAsComposableContent<T> = @Composable (index: Int, item: T, visible: Boolean, container: @Composable (@Composable () -> Unit) -> Unit) -> Unit

// stack design; does not support diffing
// note that this implementation is very hacky and dirty, for proper diffing and performance.
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun <T> AnimateListAsComposable(
	items: List<T>,
	key: (T) -> Any? = { it },
	isOpaque: (T) -> Boolean = { true },
	animation: @Composable (
		item: T,
		state: VisibilityAnimationState,
		onAnimationEnd: () -> Unit,
		content: @Composable () -> Unit
	) -> Unit,
	content: @Composable (List<AnimationListEntry<T>>, @Composable (AnimateListAsComposableContent<T>) -> Unit) -> Unit
) {
	val scope = rememberCoroutineScope()
	
	// AnimateListAsComposable assigns list to other value, causing recomposition.
	// So uses workaround, but be aware to call recomposeId++. (here not used currentRecomposeScope as it is hard
	// to manage where to recompose)
	// also, AnimationListEntry.state is not backed by State, so you should also handle this.
	var list by remember {
		Ref(items.map { AnimationListEntry(it, VisibilityAnimationState.visible) }.toPersistentList())
	}
	var recomposeId by remember { mutableStateOf(0) }
	recomposeId
	var lastItems by remember { Ref(items.toList()) }
	
	/// Diff items
	// note that this diff is not like Mayer Diff; just checking whether it is as-is
	
	val last = list
	var firstChange = -1
	
	// 1. diff
	val filteredLast = last.filter { !it.state.virtual }
	val maxIndex = max(items.size, filteredLast.size)
	
	for(i in 0 until maxIndex) {
		val conflict =
			// list shrunk; index out of new items bound
			i >= items.size ||
				
				// list expanded; index
				i >= filteredLast.size ||
				
				// conflict
				items[i] != filteredLast[i].item
		
		if(conflict) {
			firstChange = i
			break
		}
	}
	
	val result = if(firstChange == -1) {
		// 2. fast path: no changes
		last
	} else {
		// 3. apply to list
		val newItemExist = items.size - firstChange > 0
		val removedItemExist = last.size - firstChange > 0
		
		if(!newItemExist) {
			// some routes removed
			for(i in firstChange until last.size) {
				val entry = last[i]
				entry.state = VisibilityAnimationState.exit
			}
			last
		} else {
			// mark remove if needed, or just remove
			val targetState = if(removedItemExist) {
				VisibilityAnimationState.waitingExit
			} else {
				VisibilityAnimationState.exit
			}
			
			for(i in firstChange until last.size) {
				val entry = last[i]
				entry.state = targetState
			}
			// some routes added
			val new = last.mutate { l ->
				// l.subList(firstConflict, l.lastIndex).clear() // not called here for animation
				l += items.drop(firstChange).map { AnimationListEntry(it, VisibilityAnimationState.enter) }
			}
			
			@Suppress("UNUSED_VALUE")
			list = new
			@Suppress("UNUSED_VALUE")
			lastItems = items.toList()
			new
		}
	}
	
	// just in case: replaced -> newly replaced item removed before [enter] complete
	val lastItem = result.last()
	if(lastItem.state == VisibilityAnimationState.waitingExit) {
		lastItem.state = VisibilityAnimationState.exit
	}
	
	// remove waitingExit
	var removeWaitingExit = false
	for(i in result.lastIndex downTo 0) {
		val item = result[i]
		if(item.state == VisibilityAnimationState.waitingExit && removeWaitingExit) {
			item.state = VisibilityAnimationState.exit
		}
		if(!item.state.animating) {
			removeWaitingExit = true
		}
	}
	
	if(sDebugAnimateListAsComposable) {
		println(result)
	}
	
	
	val lastOpaqueIndex = result.indexOfLast {
		val transparent = it.state.animating || !isOpaque(it.item) // inherently transparent like dialog
		!transparent
	}.coerceAtLeast(0)
	
	content(result) { realContent ->
		Box {
			recomposeId
			for((index, entry) in result.withIndex()) key(key(entry.item)) {
				val visible = index >= lastOpaqueIndex
				realContent(index, entry.item, visible) { contentInner ->
					Box(Modifier.graphicsLayer {
						alpha = if(visible) 1f else 0f
					}) {
						recomposeId
						
						animation(
							entry.item,
							entry.state,
							{ // onAnimationEnd
								val newIndex = list.indexOf(entry)
								if(newIndex == -1) return@animation
								when(entry.state) {
									VisibilityAnimationState.enter -> {
										entry.state = VisibilityAnimationState.visible
										recomposeId++
									}
									VisibilityAnimationState.visible -> Unit // no-op
									VisibilityAnimationState.waitingExit -> Unit // never called with this
									VisibilityAnimationState.exit -> {
										list = list.removeAt(newIndex)
										recomposeId++
									}
								}
							}
						) { contentInner() }
					}
				}
			}
		}
	}
}

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
	val animating: Boolean
) {
	enter(fromState = false, targetState = true, animating = true),
	visible(fromState = true, targetState = true, animating = false),
	exit(fromState = true, targetState = false, animating = true)
}

@Stable
private class AnimationListEntry<T>(val item: T, state: VisibilityAnimationState) {
	var state by mutableStateOf(state)
	
	override fun toString() = "AnimationListEntry(item=$item, state=$state)"
}

// stack design; does not support diffing
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
	content: @Composable (index: Int, T) -> Unit
) {
	val scope = rememberCoroutineScope()
	
	// AnimateListAsComposable assigns list to other value, causing recomposition.
	// So uses workaround, but be aware to call recompositionScope.invalidate()
	var list by remember {
		Ref(items.map { AnimationListEntry(it, VisibilityAnimationState.visible) }.toPersistentList())
	}
	val recomposeScope = currentRecomposeScope
	var lastItems by remember { Ref(items.toList()) }
	
	// diffing goes here
	@Suppress("UnnecessaryVariable")
	val last = list
	
	val result = if(lastItems == items) {
		// 1. fast path
		last
	} else {
		// 2. diff
		// note that this diff is not like Mayer Diff; just checking whether it is as-is
		
		var firstChange = -1
		
		val maxIndex = max(items.size, last.size)
		for(i in 0 until maxIndex) {
			val conflict =
				// list shrunk; index out of new items bound
				i >= items.size ||
					
					// list expanded; index
					i >= last.size ||
					
					// conflict
					items[i] != last[i].item
			
			if(conflict) {
				firstChange = i
				break
			}
		}
		
		if(firstChange == -1) {
			// 3. fast path: no changes
			list
		} else {
			// 4. apply to list
			for(i in firstChange until last.size) {
				val entry = last[i]
				entry.state = VisibilityAnimationState.exit
			}
			
			val new = last.mutate { l ->
				// l.subList(firstConflict, l.lastIndex).clear() // preserved for animation
				l += items.drop(firstChange).map { AnimationListEntry(it, VisibilityAnimationState.enter) }
			}
			
			@Suppress("UNUSED_VALUE")
			list = new
			@Suppress("UNUSED_VALUE")
			lastItems = items.toList()
			new
		}
	}
	
	
	val lastOpaqueIndex = result.indexOfLast {
		val transparent = it.state.animating || !isOpaque(it.item) // inherently transparent like dialog
		!transparent
	}.coerceAtLeast(0)
	
	Box {
		for((index, entry) in result.withIndex()) key(key(entry.item)) {
			Box(Modifier.graphicsLayer {
				alpha = if(index >= lastOpaqueIndex) 1f else 0f
			}) {
				animation(
					entry.item,
					entry.state,
					{
						val newIndex = list.indexOf(entry)
						if(newIndex == -1) return@animation
						when(entry.state) {
							VisibilityAnimationState.enter ->
								entry.state = VisibilityAnimationState.visible
							VisibilityAnimationState.visible -> Unit // no-op
							VisibilityAnimationState.exit -> {
								list = list.removeAt(newIndex)
								recomposeScope.invalidate()
							}
						}
					}
				) { content(index, entry.item) }
			}
		}
	}
}

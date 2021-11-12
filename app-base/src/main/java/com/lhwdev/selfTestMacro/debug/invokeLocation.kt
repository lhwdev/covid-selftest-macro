package com.lhwdev.selfTestMacro.debug

import java.lang.reflect.Method


private val sTraceItemAnnotation = TraceItem::class.java
private val sTraceItemsAnnotation = TraceItems::class.java


/**
 * Getting invocation location is a quite slow job, getting all stacktrace and figuring out the original method.
 * Should be only used in error.
 */
fun invokeLocationDescription(depth: Int): String {
	val method = invokeLocation(depth = depth + 1)
	
	return if(method == null) {
		"?"
	} else {
		"${method.declaringClass.name}.${method.name}"
	}
}

fun invokeLocation(depth: Int): Method? {
	val realDepth = depth + 1
	val trace = Throwable().stackTrace
	
	for(index in realDepth until trace.size) {
		val element = trace[index] ?: continue
		
		val method = methodFromStackTrace(element, checkAnnotation = true)
		if(method != null) {
			return method
		}
	}
	
	return methodFromStackTrace(trace[realDepth], checkAnnotation = false)
}

private fun methodFromStackTrace(element: StackTraceElement, checkAnnotation: Boolean): Method? = try {
	val targetClass = DebugContext::class.java.classLoader.loadClass(element.className)
	
	if(checkAnnotation) {
		val classAnnotation = targetClass.getAnnotation(sTraceItemsAnnotation)
		
		if(classAnnotation == null) {
			targetClass.declaredMethods.find {
				it.name == element.methodName &&
					it.isAnnotationPresent(sTraceItemAnnotation)
			}
		} else {
			targetClass.declaredMethods.find {
				it.name == element.methodName &&
					classAnnotation.matches(it)
			}
		}
	} else {
		targetClass.declaredMethods.find { it.name == element.methodName }
	}
} catch(th: Throwable) {
	null
}

private fun TraceItems.matches(method: Method) =
	requiredModifier == (requiredModifier and method.modifiers)

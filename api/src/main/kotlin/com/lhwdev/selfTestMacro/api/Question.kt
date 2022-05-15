package com.lhwdev.selfTestMacro.api

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.nullable
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.*


private val yesNoText = mapOf(false to "없음", true to "있음")


// null is treated as nothing
@Serializable
public sealed class Question<T>(
	public val name: String,
	public val title: String,
	public val content: String,
	public val displayTexts: Map<T, String>,
	public val valueSerializer: KSerializer<T>,
	public val defaultValue: T
) {
	public companion object {
		public val all: List<Question<*>> =
			@OptIn(UnstableHcsApi::class) listOf(Suspicious, QuickTest, WaitingResult)
	}
	
	public class Answer<T>(public val question: Question<T>, public val value: T)
	
	
	public infix fun to(value: T): Answer<T> = Answer(this, value)
	
	public fun displayText(value: T): String = displayTexts.getValue(value)
	
	
	@UnstableHcsApi
	@Serializable
	@SerialName("suspicious")
	public object Suspicious : Question<Boolean>(
		name = "suspicious",
		title = "감염 의심증상",
		content = "본인이 코로나19 감염에 의심되는 임상증상(발열(37.5℃), 기침, 호흡곤란, 오한, 근육통, 두통, 인후통, 후각·미각소실)이 있나요?",
		displayTexts = yesNoText,
		valueSerializer = Boolean.serializer(),
		defaultValue = false
	)
	
	@UnstableHcsApi
	@Serializable
	@SerialName("quickTest")
	public object QuickTest : Question<QuickTest.Data>(
		name = "quickTest",
		title = "신속항원검사 결과",
		content = "오늘(어제 저녁 포함) 신속항원검사(자가진단)를 실시했나요?",
		displayTexts = enumValues<Data>().associateWith { it.displayLabel },
		valueSerializer = Data.serializer(),
		defaultValue = Data.didNotConduct
	) {
		@UnstableHcsApi
		@Serializable // useful for using QuickTestResult.serializer() explicitly
		public enum class Data(public val displayLabel: String) {
			didNotConduct("실시하지 않음"),
			negative("음성"),
			positive("양성")
		}
	}
	
	@UnstableHcsApi
	@Serializable
	@SerialName("waitingResult")
	public object WaitingResult : Question<Boolean>(
		name = "waitingResult",
		title = "본인 또는 동거인의 PCR 검사 여부",
		content = "본인 또는 동거인이 PCR 검사를 받고 그 결과를 기다리고 있나요?",
		displayTexts = yesNoText,
		valueSerializer = Boolean.serializer(),
		defaultValue = false
	)
}


@Serializable(with = AnswersMap.Serializer::class)
public class AnswersMap(private val data: List<Question.Answer<*>>) {
	@UnstableHcsApi
	public val suspicious: Boolean
		get() = this[Question.Suspicious]
	
	@UnstableHcsApi
	public val quickTest: Question.QuickTest.Data
		get() = this[Question.QuickTest]
	
	@UnstableHcsApi
	public val waitingResult: Boolean
		get() = this[Question.WaitingResult]
	
	@OptIn(UnstableHcsApi::class)
	public val isHealthy: Boolean
		get() = !suspicious && quickTest != Question.QuickTest.Data.positive && !waitingResult
	
	public object Serializer : KSerializer<AnswersMap> {
		override val descriptor: SerialDescriptor = buildClassSerialDescriptor(
			serialName = "com.lhwdev.selfTestMacro.api.QuestionsMap"
		) {
			for(question in Question.all) {
				element(elementName = question.name, descriptor = question.valueSerializer.descriptor)
			}
		}
		
		override fun deserialize(decoder: Decoder): AnswersMap = decoder.decodeStructure(descriptor) {
			// decodeSequentially() // Json cannot use this; skip for brevity
			val data = mutableListOf<Question.Answer<*>>()
			while(true) {
				when(val index = decodeElementIndex(descriptor)) {
					CompositeDecoder.DECODE_DONE -> break
					CompositeDecoder.UNKNOWN_NAME -> {
						// in case answer was changed? IDK if I can continue silently here
						// NOTE: with Json { ignoreUnknownKeys = false }, this never hits
						continue
					}
					else -> {
						@Suppress("UNCHECKED_CAST")
						val question = Question.all[index] as Question<Any>
						
						@OptIn(ExperimentalSerializationApi::class)
						val value = decodeNullableSerializableElement(
							descriptor = descriptor,
							index = index,
							deserializer = question.valueSerializer.nullable
						)
						if(value != null) {
							data += question to value
						}
					}
				}
			}
			
			AnswersMap(data)
		}
		
		
		override fun serialize(encoder: Encoder, value: AnswersMap): Unit = encoder.encodeStructure(descriptor) {
			for((index, question) in Question.all.withIndex()) {
				@Suppress("UNCHECKED_CAST")
				question as Question<Any>
				
				@OptIn(ExperimentalSerializationApi::class)
				encodeNullableSerializableElement(
					descriptor = descriptor,
					index = index,
					serializer = question.valueSerializer,
					value = value.getEntry(question)?.value
				)
			}
		}
	}
	
	public constructor(vararg data: Question.Answer<*>) : this(data.toList())
	
	init {
		// if keys(questions) do not duplicate
		check(data.groupBy { it.question }.all { it.value.size == 1 })
	}
	
	
	@Suppress("UNCHECKED_CAST")
	public fun <T> getEntry(key: Question<T>): Question.Answer<T>? =
		data.find { it.question == key } as Question.Answer<T>?
	
	public operator fun <T> get(key: Question<T>): T {
		val entry = getEntry(key)
		return if(entry != null) {
			entry.value
		} else {
			key.defaultValue
		}
	}
}

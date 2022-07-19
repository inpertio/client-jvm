package tech.harmonysoft.oss.inpertio.client.factory.impl

import tech.harmonysoft.oss.common.collection.mapFirstNotNull
import tech.harmonysoft.oss.inpertio.client.context.Context
import tech.harmonysoft.oss.inpertio.client.exception.InpertioException
import tech.harmonysoft.oss.inpertio.client.factory.impl.InpertioClientKotlinUtil.parseConstructors
import kotlin.reflect.KClass
import kotlin.reflect.KType

@Suppress("UNCHECKED_CAST")
class ClassSpecificCreator<T : Any>(private val type: KType) {

    private val klass: KClass<T> = type.classifier as? KClass<T> ?: throw InpertioException(
            "Can't instantiate type '$type' - its classifier is not a class"
    )
    private val instantiators: Collection<Instantiator<T>> = parseConstructors(klass).map { Instantiator(it) }

    fun create(prefix: String, creator: KotlinCreator, context: Context): T {
        if (type.classifier == Any::class) {
            return createAny(prefix, context)
        }

        val failedResults = mutableMapOf<Instantiator<T>, String>()

        return instantiators.mapFirstNotNull {
            val candidate = it.mayBeCreate(prefix, creator, context)
            if (candidate.success) {
                candidate.successValue
            } else {
                failedResults[it] = candidate.failureValue
                null
            }
        } ?: throw InpertioException(
                "Failed instantiating a ${klass.qualifiedName ?: klass.simpleName} instance. "
                + "None of ${instantiators.size} constructors match:\n  "
                + failedResults.entries.joinToString(separator = "\n  ") {
                    "${it.key} - ${it.value}"
                }
        )
    }

    @Suppress("ALWAYS_NULL")
    private fun createAny(prefix: String, context: Context): T {
        val rawValue = context.getPropertyValue(prefix)
        return if (rawValue == null) {
            if (type.isMarkedNullable) {
                rawValue as T
            } else {
                throw InpertioException(
                        "Failed finding value for property '$prefix'"
                )
            }
        } else {
            val klass = type.classifier as? KClass<T>
            if (klass == null) {
                return rawValue as T
            } else {
                context.convertIfNecessary(rawValue, klass) as T
            }
        }
    }

    override fun toString(): String {
        return "$type creator"
    }
}
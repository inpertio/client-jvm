package tech.harmonysoft.oss.inpertio.client.factory.impl

import tech.harmonysoft.oss.common.collection.mapFirstNotNull
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KType
import kotlin.reflect.KVisibility

object InpertioClientKotlinUtil {

    fun combineConverters(vararg converters: (Any) -> Any?): (Any) -> Any? {
        return { arg ->
            converters.toList().mapFirstNotNull {
                it(arg)
            }
        }
    }

    fun <I, O> combineMapKeyProducers(vararg producers: (I) -> Iterable<O>): (I) -> Iterable<O> {
        return { input ->
            producers.flatMap { it(input) }
        }
    }

    fun <T : Any> parseConstructors(klass: KClass<T>): Collection<KFunction<T>> {
        return klass.constructors.filter {
            it.visibility == KVisibility.PUBLIC
        }
    }
}

inline fun <reified T : Enum<T>> enumKeyProducer(): (KType) -> Set<String> {
    return { type ->
        if (type.classifier == T::class) {
            enumValues<T>().map { it.name }.toSet()
        } else {
            emptySet()
        }
    }
}

inline fun <reified T : Enum<T>> enumConverter(values: Array<T>): (Any, KClass<*>) -> Any? {
    return { value, klass ->
        when {
            klass != T::class -> null
            value::class == T::class -> value
            else -> {
                val stringValue = value.toString()
                values.find {
                    it.name == stringValue
                }
            }
        }
    }
}
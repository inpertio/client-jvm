package tech.harmonysoft.oss.inpertio.client.context.impl

import tech.harmonysoft.oss.common.string.util.isBlankEffective
import tech.harmonysoft.oss.inpertio.client.context.Context
import java.time.ZoneId
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.full.isSuperclassOf

class ContextBuilderImpl(private val dataProvider: (String) -> Any?) : Context.Builder {

    private val simpleTypes = DEFAULT_SIMPLE_TYPES.toMutableSet()
    private val collectionTypes = DEFAULT_COLLECTION_TYPES.toMutableSet()

    private var collectionCreator = wrapCollectionCreator(DEFAULT_COLLECTION_CREATOR)
    private var regularPropertyNameStrategy = DEFAULT_REGULAR_PROPERTY_NAME_STRATEGY
    private var collectionPropertyNameStrategy = DEFAULT_COLLECTION_ELEMENT_PROPERTY_NAME_STRATEGY
    private var typeConverter = wrapTypeConverter(DEFAULT_TYPE_CONVERTER)
    private var mapCreator: () -> MutableMap<Any, Any> = DEFAULT_MAP_CREATOR
    private var mapKeyStrategy = DEFAULT_MAP_KEY_STRATEGY
    private var mapValuePropertyNameStrategy = DEFAULT_REGULAR_PROPERTY_NAME_STRATEGY

    override fun withSimpleTypes(types: Set<KClass<*>>, replace: Boolean): Context.Builder {
        return apply {
            if (replace) {
                simpleTypes.clear()
            }
            simpleTypes.addAll(types)
        }
    }

    override fun withCollectionTypes(types: Set<KClass<*>>, replace: Boolean): Context.Builder {
        return apply {
            if (replace) {
                collectionTypes.clear()
            }
            collectionTypes += types
        }
    }

    override fun withTypeConverter(replace: Boolean, converter: (Any, KClass<*>) -> Any?): Context.Builder {
        return apply {
            typeConverter = wrapTypeConverter(if (replace) {
                converter
            } else {
                { value, targetType ->
                    DEFAULT_TYPE_CONVERTER(value, targetType) ?: converter(value, targetType)
                }
            })
        }
    }

    private fun wrapTypeConverter(converter: (Any, KClass<*>) -> Any?): (Any, KClass<*>) -> Any {
        return { value, targetType ->
            val result = converter(value, targetType)
            if (result != null && targetType.isInstance(result)) {
                result
            } else if (targetType.isInstance(value)) {
                value
            } else {
                throw IllegalArgumentException(
                        "can't convert value '$value' of type '${value::class.qualifiedName}' "
                        + "to type '${targetType.qualifiedName}'"
                )
            }
        }
    }

    override fun withRegularPropertyNameStrategy(strategy: (String, String) -> String): Context.Builder {
        return apply {
            regularPropertyNameStrategy = strategy
        }
    }

    override fun withCollectionCreator(
            replace: Boolean,
            creator: (KClass<*>) -> MutableCollection<Any>?
    ): Context.Builder {
        return apply {
            collectionCreator = if (replace) {
                wrapCollectionCreator(creator)
            } else {
                wrapCollectionCreator {
                    DEFAULT_COLLECTION_CREATOR(it) ?: creator(it)
                }
            }
        }
    }

    private fun wrapCollectionCreator(
            creator: (KClass<*>) -> MutableCollection<Any>?
    ): (KClass<*>) -> MutableCollection<Any> {
        return {
            val result = creator(it)
            if (result != null && it.isInstance(result)) {
                result
            } else {
                throw IllegalArgumentException(
                        "Failed creating a collection of type '${it.qualifiedName}'"
                )
            }
        }
    }

    override fun withCollectionElementPropertyNameStrategy(strategy: (String, Int) -> String): Context.Builder {
        return apply {
            collectionPropertyNameStrategy = strategy
        }
    }

    override fun withMapCreator(creator: () -> MutableMap<Any, Any>): Context.Builder {
        return apply {
            mapCreator = creator
        }
    }

    override fun withMapKeyStrategy(replace: Boolean, strategy: (String, KType) -> Set<String>): Context.Builder {
        return apply {
            if (replace) {
                mapKeyStrategy = strategy
            } else {
                val initial = mapKeyStrategy
                mapKeyStrategy = { key, type ->
                    val result = initial(key, type)
                    if (result.isEmpty()) {
                        strategy(key, type)
                    } else {
                        result
                    }
                }
            }
        }
    }

    override fun withMapValuePropertyNameStrategy(strategy: (String, String) -> String): Context.Builder {
        return apply {
            mapValuePropertyNameStrategy = strategy
        }
    }

    override fun withMapKeys(allKeys: Set<String>): Context.Builder {
        return apply {
            withMapKeyStrategy { baseName, _ ->
                buildMapKeys(baseName, allKeys)
            }
            withMapValuePropertyNameStrategy { baseName, propertyName ->
                when {
                    baseName.isBlankEffective() -> propertyName
                    allKeys.any { it.startsWith("$baseName[$propertyName]") } -> "$baseName[$propertyName]"
                    else -> "$baseName.$propertyName"
                }
            }
        }
    }

    private fun buildMapKeys(baseName: String, allKeys: Set<String>): Set<String> {
        val commonPrefix = "$baseName."
        val specialPrefix = "$baseName["
        return allKeys.mapNotNull {  fullKey ->
            when {
                fullKey.startsWith(commonPrefix) -> {
                    nextKey(commonPrefix.length, fullKey)?.let { keyCandidate ->
                        val j = keyCandidate.indexOf("[")
                        if (j < 0) {
                            keyCandidate
                        } else {
                            keyCandidate.substring(0, j)
                        }
                    }
                }

                fullKey.startsWith(specialPrefix) -> {
                    fullKey.substring(specialPrefix.length, fullKey.indexOf(']', specialPrefix.length)).takeIf {
                        !it.matches(NUMBERS_REGEX)
                    }
                }

                else -> null
            }
        }.toSet()
    }

    private fun nextKey(keyStartOffset: Int, fullKey: String): String? {
        if (keyStartOffset >= fullKey.length) {
            return null
        }
        val i = fullKey.indexOf(".", keyStartOffset)
        return if (i > 0) {
            fullKey.substring(keyStartOffset, i)
        } else {
            fullKey.substring(keyStartOffset)
        }
    }

    override fun build(): Context {
        return ContextImpl(
                dataProvider = dataProvider,
                typeConverter = typeConverter,
                regularPropertyNameStrategy = regularPropertyNameStrategy,
                collectionCreator = collectionCreator,
                collectionPropertyNameStrategy = collectionPropertyNameStrategy,
                simpleTypes = simpleTypes,
                collectionTypes = collectionTypes,
                mapCreator = mapCreator,
                mapKeyStrategy = mapKeyStrategy,
                mapPropertyNameStrategy = mapValuePropertyNameStrategy
        )
    }

    companion object {

        private val NUMBERS_REGEX = """\d+""".toRegex()

        val DEFAULT_SIMPLE_TYPES = setOf<KClass<*>>(
                Boolean::class, Short::class, Char::class, Int::class, Long::class, Float::class, Double::class,
                String::class, ZoneId::class
        )

        val DEFAULT_COLLECTION_TYPES: Set<KClass<*>> = setOf(
                List::class, Set::class, Collection::class, Iterable::class
        )

        val DEFAULT_COLLECTION_CREATOR: (KClass<*>) -> MutableCollection<Any>? = { klass ->
            when {
                List::class.isSuperclassOf(klass) -> mutableListOf()
                Set::class.isSuperclassOf(klass) -> mutableSetOf()
                Collection::class.isSuperclassOf(klass) -> mutableListOf()
                Iterable::class.isSuperclassOf(klass) -> mutableListOf()
                else -> null
            }
        }

        val DEFAULT_COLLECTION_ELEMENT_PROPERTY_NAME_STRATEGY: (String, Int) -> String = { baseName, index ->
            "$baseName[$index]"
        }

        val DEFAULT_REGULAR_PROPERTY_NAME_STRATEGY: (String, String) -> String = { baseName, propertyName ->
            if (baseName.isBlank()) {
                propertyName
            } else {
                "$baseName.$propertyName"
            }
        }

        val DEFAULT_TYPE_CONVERTER: (Any, KClass<*>) -> Any? = { rawValue, targetClass ->
            if (targetClass.isInstance(rawValue)) {
                rawValue
            } else {
                val trimmedValue = rawValue.toString().trim()
                @Suppress("UNCHECKED_CAST")
                when (targetClass) {
                    Boolean::class -> when {
                        trimmedValue.equals("true", true) -> true
                        trimmedValue.equals("false", true) -> false
                        else -> throw IllegalArgumentException(
                                "can't convert value '$trimmedValue' of type '${trimmedValue::class.qualifiedName}' "
                                + "to type '${targetClass.qualifiedName}'"
                        )
                    }
                    Short::class -> trimmedValue.toShort()
                    Char::class -> if (trimmedValue.length == 1) {
                        trimmedValue[0]
                    } else {
                        throw IllegalArgumentException(
                                "can't convert value '$trimmedValue' of type '${trimmedValue::class.qualifiedName}' "
                                + "to type '${targetClass.qualifiedName}'"
                        )
                    }
                    Int::class -> trimmedValue.toInt()
                    Long::class -> trimmedValue.toLong()
                    Float::class -> trimmedValue.toFloat()
                    Double::class -> trimmedValue.toDouble()
                    ZoneId::class -> ZoneId.of(rawValue.toString())
                    else -> {
                        if (Enum::class.isSuperclassOf(targetClass)) {
                            (targetClass.java.enumConstants as Array<Enum<*>>).find { it.name == rawValue }
                        } else {
                            null
                        }
                    }
                }
            }
        }

        val DEFAULT_MAP_CREATOR: () -> MutableMap<Any, Any> = {
            mutableMapOf()
        }

        val DEFAULT_MAP_KEY_STRATEGY: (String, KType) -> Set<String> = { _, type ->
            (type.classifier as? KClass<*>)?.let { klass ->
                if (Enum::class.isSuperclassOf(klass)) {
                    @Suppress("UNCHECKED_CAST")
                    (klass.java.enumConstants as Array<Enum<*>>).map {
                        it.name
                    }.toSet()
                } else {
                    emptySet()
                }
            } ?: emptySet()
        }
    }
}
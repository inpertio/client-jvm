package tech.harmonysoft.oss.inpertio.client.factory.impl

import tech.harmonysoft.oss.common.ProcessingResult
import tech.harmonysoft.oss.inpertio.client.context.Context
import tech.harmonysoft.oss.inpertio.client.exception.InpertioException
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KType
import kotlin.reflect.jvm.javaConstructor

class Instantiator<T>(private val constructor: KFunction<T>) {

    private val retrievers = constructor.parameters.map { ParameterValueRetriever(it) }
    private val error = retrievers.mapNotNull { it.error }.joinToString()

    fun mayBeCreate(prefix: String, creator: KotlinCreator, context: Context): ProcessingResult<T, String> {
        if (error.isNotBlank()) {
            return ProcessingResult.failure(error)
        }

        val hasMandatoryParameter = retrievers.any {
            !it.parameter.isOptional && !it.parameter.type.isMarkedNullable
        }
        val paramLookupResults = context.withMandatoryParameter(hasMandatoryParameter) {
            retrievers.mapNotNull { retriever ->
                try {
                    val propertyValueResult = retriever.retrieve(prefix, creator, context)
                    retriever to mayBeRemap(propertyValueResult, retriever.parameter.type, context)
                } catch (e: InpertioException) {
                    if (!context.tolerateEmptyCollection) {
                        throw e
                    }
                    when {
                        retriever.parameter.type.isMarkedNullable -> retriever to ProcessingResult.success<Any?, String>(null)
                        retriever.parameter.isOptional -> null
                        else -> throw e
                    }
                }
            }.toMap()
        }

        val error = paramLookupResults.values.mapNotNull {
            if (it == null || it.success) {
                null
            } else {
                it.failureValue
            }
        }.joinToString()

        if (error.isNotBlank()) {
            return ProcessingResult.failure(error)
        }

        val arguments = paramLookupResults.mapNotNull { (retriever, result) ->
            if (result == null) {
                if (retriever.parameter.isOptional) {
                    null
                } else {
                    retriever.parameter to null
                }
            } else {
                retriever.parameter to result.successValue
            }
        }
        return try {
            ProcessingResult.success(constructor.callBy(arguments.toMap()))
        } catch (e: Exception) {
            ProcessingResult.failure("${e.javaClass.name}: ${e.message} for parameters ${arguments.joinToString {
                "${it.first.name}=${it.second}"
            }}")
        }
    }

    private fun mayBeRemap(
        result: ProcessingResult<Any?, String>?,
        type: KType, context: Context
    ): ProcessingResult<Any?, String>? {
        if (context.tolerateEmptyCollection
            || (result != null
                && (!result.success
                    || (result.successValue == null && type.isMarkedNullable && context.hasMandatoryParameter)))
        ) {
            return result
        }
        val klass = type.classifier as? KClass<*> ?: return result
        if (!context.isCollection(klass)) {
            return result
        }

        return if (result == null || (result.successValue as Collection<*>).isEmpty()) {
            ProcessingResult.failure("found an empty collection parameter but current context disallows that")
        } else {
            result
        }
    }

    override fun toString(): String {
        val declaringClass = constructor.javaConstructor?.declaringClass
        return "${declaringClass?.simpleName ?: constructor.name}(" +
               constructor.parameters.joinToString { it.name.toString() } +
               ")"
    }
}
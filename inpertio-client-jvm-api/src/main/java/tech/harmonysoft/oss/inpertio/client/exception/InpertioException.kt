package tech.harmonysoft.oss.inpertio.client.exception

class InpertioException @JvmOverloads constructor(
    message: String? = null,
    cause: Throwable? = null
) : RuntimeException(message, cause)
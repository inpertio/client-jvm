package tech.harmonysoft.oss.inpertio.client.context

import org.springframework.core.env.*
import javax.inject.Named

@Named
class SpringContextProvider(
    private val environment: Environment
) : ContextProvider {

    override val context = buildContext()

    private fun buildContext(): Context {
        val allKeys = getAllPropertyKeys(environment)
        return Context.builder {
            environment[it]
        }.withMapKeys(allKeys).build()
    }

    private fun getAllPropertyKeys(environment: Environment): Set<String> {
        return when (environment) {
            is StandardEnvironment -> environment.propertySources.flatMap {
                getAllPropertyKeys(it).toSet()
            }.toSet()
            else -> emptySet()
        }
    }

    private fun getAllPropertyKeys(source: PropertySource<*>): Set<String> {
        return when (source) {
            is CompositePropertySource -> source.propertySources.flatMap { getAllPropertyKeys(it) }.toSet()
            is EnumerablePropertySource -> source.propertyNames.toSet()
            else -> (source.source as? Map<*, *>)?.keys?.map { it.toString() }?.toSet() ?: emptySet()
        }
    }
}
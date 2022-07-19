package tech.harmonysoft.oss.inpertio.client.factory.impl

import org.slf4j.LoggerFactory
import tech.harmonysoft.oss.inpertio.client.ConfigProvider
import tech.harmonysoft.oss.inpertio.client.context.ContextProvider
import tech.harmonysoft.oss.inpertio.client.event.ConfigChangedEvent
import tech.harmonysoft.oss.inpertio.client.event.ConfigChangedEventAware
import tech.harmonysoft.oss.inpertio.client.event.ConfigEventManager
import tech.harmonysoft.oss.inpertio.client.util.ClientUtil
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Named
import kotlin.reflect.full.createType

@Named
class KotlinConfigProviderFactory(
    private val contextProvider: ContextProvider,
    private val eventManager: ConfigEventManager
) : BaseConfigProviderFactory(eventManager) {

    private val logger = LoggerFactory.getLogger(KotlinConfigProviderFactory::class.java)

    private val creator = KotlinCreatorImpl()

    override fun <T : Any?> build(clazz: Class<T>): ConfigProvider<T> {
        return build(ClientUtil.maybeGetPrefixFromAnnotation(clazz) ?: "", clazz)
    }

    override fun <T : Any?> build(configurationPrefix: String, clazz: Class<T>): ConfigProvider<T> {
        return object : ConfigProvider<T>, ConfigChangedEventAware {

            private val cached = AtomicReference<T?>()

            override fun getData(): T & Any {
                val c = cached.get()
                if (c != null) {
                    return c
                }

                return probe().apply {
                    cached.set(this)
                }
            }

            override fun refresh() {
                val current = data
                val latest = probe()
                if (current != latest) {
                    cached.set(latest)
                    logger.info(
                        "Configuration change detected firing an event about that, previous: {}, current: {}",
                        current, latest
                    )
                    eventManager.fire(ConfigChangedEvent(current, latest))
                }
            }

            override fun probe(): T & Any {
                return creator.create(
                    prefix = configurationPrefix,
                    type = (clazz as Class<*>).kotlin.createType(),
                    context = contextProvider.context
                )
            }

            override fun onConfigChanged(event: ConfigChangedEvent) {
            }

            override fun onRefreshEvent() {
                refresh()
            }

        }.apply {
            eventManager.subscribe(this)
        }
    }
}
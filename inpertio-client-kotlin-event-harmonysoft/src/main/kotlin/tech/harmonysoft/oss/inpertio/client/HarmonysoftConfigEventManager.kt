package tech.harmonysoft.oss.inpertio.client

import tech.harmonysoft.oss.event.bus.AutoSubscribe
import tech.harmonysoft.oss.event.bus.EventBus
import tech.harmonysoft.oss.inpertio.client.event.ConfigChangedEvent
import tech.harmonysoft.oss.inpertio.client.event.ConfigChangedEventAware
import tech.harmonysoft.oss.inpertio.client.event.ConfigEventManager
import tech.harmonysoft.oss.inpertio.client.event.RefreshConfigsEvent
import javax.inject.Named

@Named
class HarmonysoftConfigEventManager(
    private val eventBus: EventBus
) : ConfigEventManager {

    override fun fire(event: ConfigChangedEvent) {
        eventBus.post(event)
    }

    override fun fire(event: RefreshConfigsEvent) {
        eventBus.post(event)
    }

    override fun subscribe(callback: ConfigChangedEventAware) {
        eventBus.register(SubscriberDecorator(callback))
    }

    class SubscriberDecorator(
        private val callback: ConfigChangedEventAware
    ) : ConfigChangedEventAware {

        @AutoSubscribe
        override fun onConfigChanged(event: ConfigChangedEvent) {
            callback.onConfigChanged(event)
        }

        @AutoSubscribe
        override fun onRefreshEvent() {
            callback.onRefreshEvent()
        }
    }
}
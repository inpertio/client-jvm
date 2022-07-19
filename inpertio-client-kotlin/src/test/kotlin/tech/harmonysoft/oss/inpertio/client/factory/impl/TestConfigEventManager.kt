package tech.harmonysoft.oss.inpertio.client.factory.impl

import tech.harmonysoft.oss.inpertio.client.event.ConfigChangedEvent
import tech.harmonysoft.oss.inpertio.client.event.ConfigChangedEventAware
import tech.harmonysoft.oss.inpertio.client.event.ConfigEventManager
import tech.harmonysoft.oss.inpertio.client.event.RefreshConfigsEvent
import tech.harmonysoft.oss.test.TestAware
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CopyOnWriteArraySet

class TestConfigEventManager : ConfigEventManager, TestAware {

    val firedEvents = CopyOnWriteArraySet<Any>()
    private val callbacks = CopyOnWriteArrayList<ConfigChangedEventAware>()

    override fun fire(event: ConfigChangedEvent) {
        firedEvents += event
        for (callback in callbacks) {
            callback.onConfigChanged(event)
        }
    }

    override fun fire(event: RefreshConfigsEvent) {
        firedEvents += event
        for (callback in callbacks) {
            callback.onRefreshEvent()
        }
    }

    override fun subscribe(callback: ConfigChangedEventAware) {
        callbacks += callback
    }

    override fun onTestEnd() {
        callbacks.clear()
        firedEvents.clear()
    }
}
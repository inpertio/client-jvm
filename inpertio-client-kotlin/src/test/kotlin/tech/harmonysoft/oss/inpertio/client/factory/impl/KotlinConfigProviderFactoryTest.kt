package tech.harmonysoft.oss.inpertio.client.factory.impl

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import tech.harmonysoft.oss.inpertio.client.context.Context
import tech.harmonysoft.oss.inpertio.client.context.ContextProvider
import tech.harmonysoft.oss.inpertio.client.event.ConfigChangedEvent
import tech.harmonysoft.oss.inpertio.client.factory.ConfigProviderFactory

internal class KotlinConfigProviderFactoryTest {

    private lateinit var data: MutableMap<String, Any>
    private lateinit var eventManager: TestConfigEventManager
    private lateinit var factory: ConfigProviderFactory

    @BeforeEach
    fun setUp() {
        data = mutableMapOf()
        eventManager = TestConfigEventManager()
        factory = KotlinConfigProviderFactory(object : ContextProvider {
            override val context = Context.builder {
                data[it]
            }.build()
        }, eventManager)
    }

    @Test
    fun `when raw2public config is built then it is cached`() {
        data["data1"] = "first"
        val configProvider = factory.build(Config1::class.java) {
            Config2(it.data1)
        }
        assertThat(configProvider.data.data2).isEqualTo("first")

        data["data"] = "second"
        assertThat(configProvider.data.data2).isEqualTo("first")
    }

    @Test
    fun `when raw2public config is built then probe shows the actual data`() {
        data["data1"] = "first"
        val configProvider = factory.build(Config1::class.java) {
            Config2(it.data1)
        }
        assertThat(configProvider.data.data2).isEqualTo("first")

        data["data1"] = "second"
        assertThat(configProvider.probe().data2).isEqualTo("second")
        assertThat(configProvider.data.data2).isEqualTo("first")
    }

    @Test
    fun `when refresh is performed on raw2public config provider then it works as expected`() {
        data["data1"] = "first"
        val configProvider = factory.build(Config1::class.java) {
            Config2(it.data1)
        }
        assertThat(configProvider.data.data2).isEqualTo("first")

        data["data1"] = "second"
        configProvider.refresh()
        assertThat(configProvider.data.data2).isEqualTo("second")
    }

    @Test
    fun `when row2public config is changed then the change event is fired`() {
        data["data1"] = "1"

        val configProvider = factory.build(Config1::class.java) {
            Config2(it.data1 + (it.data1.toInt() + 1))
        }
        assertThat(configProvider.data.data2).isEqualTo("12")

        data["data1"] = "2"
        configProvider.refresh()
        assertThat(eventManager.firedEvents).containsOnly(ConfigChangedEvent(Config2("12"), Config2("23")))
    }

    @Test
    fun `when low level config is changed then its derived configs are changed`() {
        data["data1"] = "1"
        val configProvider1 = factory.build(Config1::class.java) {
            Config2(it.data1 + (it.data1.toInt() + 1))
        }
        val configProvider2 = factory.build(listOf(configProvider1)) { source ->
            val data2 = source.get(Config2::class.java)
            Config3(data2.data2 + (data2.data2.toInt() + 1))
        }

        data["data1"] = "2"
        configProvider1.refresh()
        assertThat(configProvider2.data.data3).isEqualTo("2324")
    }
}

data class Config1(
    val data1: String
)

data class Config2(
    val data2: String
)

data class Config3(
    val data3: String
)
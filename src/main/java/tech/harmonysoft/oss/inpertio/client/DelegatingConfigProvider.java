package tech.harmonysoft.oss.inpertio.client;

import org.jetbrains.annotations.NotNull;
import tech.harmonysoft.oss.inpertio.client.factory.ConfigProviderFactory;

/**
 * {@link ConfigProviderFactory} provides convenient ways to create {@link ConfigProvider} instances, however,
 * it might be inconvenient to expose them in DI context. Consider, for example, a config provider implementation
 * which spans, say, half a screen. Consider that we have many such config providers. It would be inconvenient
 * to put them into Spring setup like below:
 *
 * {@code
 *     @Configuration
 *     public class ConfigProvidersConfiguration {
 *
 *         @Bean
 *         public ConfigProvider1 configProvider1(ConfigProviderFactory factory) {
 *             return factory.build(MyRawClass1.class, raw -> {
 *                 // ... many lines of code
 *             });
 *         }
 *
 *         @Bean
 *         public ConfigProvider2 configProvider2(ConfigProviderFactory factory) {
 *             return factory.build(MyRawClass2.class, raw -> {
 *                 // ... many lines of code
 *             });
 *         }
 *
 *         // ...
 *     }
 * }
 *
 * It would be more convenient to set up every config provider as a separate class instead. This class facilitates
 * that:
 *
 * {@code
 *     @Named
 *     public class ConfigProvider1Impl<T> extends DelegatingConfigProvider<T> implements ConfigProvider1<T> {
 *
 *         public ConfigProvider1Impl(ConfigProviderFactory factory) {
 *             super(factory.build(MyRawClass1.class, raw -> {
 *                 // ...
 *             });
 *         }
 *     }
 * }
 */
public class DelegatingConfigProvider<T> implements ConfigProvider<T> {

    private final ConfigProvider<T> delegate;

    public DelegatingConfigProvider(ConfigProvider<T> delegate) {
        this.delegate = delegate;
    }

    @NotNull
    @Override
    public T getData() {
        return delegate.getData();
    }

    @Override
    public void refresh() {
        delegate.refresh();
    }

    @NotNull
    @Override
    public T probe() {
        return delegate.probe();
    }
}

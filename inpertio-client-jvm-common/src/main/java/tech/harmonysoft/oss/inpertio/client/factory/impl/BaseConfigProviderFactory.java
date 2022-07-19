package tech.harmonysoft.oss.inpertio.client.factory.impl;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.harmonysoft.oss.inpertio.client.ConfigProvider;
import tech.harmonysoft.oss.inpertio.client.event.ConfigChangedEvent;
import tech.harmonysoft.oss.inpertio.client.event.ConfigChangedEventAware;
import tech.harmonysoft.oss.inpertio.client.event.ConfigEventManager;
import tech.harmonysoft.oss.inpertio.client.event.GenericConfigChangedEvent;
import tech.harmonysoft.oss.inpertio.client.factory.ConfigProviderFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

public abstract class BaseConfigProviderFactory implements ConfigProviderFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(BaseConfigProviderFactory.class);

    private final ConfigEventManager eventManager;

    public BaseConfigProviderFactory(@NotNull ConfigEventManager eventManager) {
        this.eventManager = eventManager;
    }

    @Override
    public @NotNull <PUBLIC, RAW> ConfigProvider<PUBLIC> build(
            @NotNull Class<RAW> rawClass,
            @Nullable String configurationPrefix,
            @NotNull Function<RAW, PUBLIC> builder
    ) {
        class Raw2PublicConfigProvider implements ConfigProvider<PUBLIC>, ConfigChangedEventAware {

            private final AtomicReference<PUBLIC> cached = new AtomicReference<>();

            @NotNull
            @Override
            public PUBLIC getData() {
                PUBLIC c = cached.get();
                if (c != null) {
                    return c;
                }

                PUBLIC result = probe();
                cached.set(result);
                LOGGER.info("Cached public config based on raw class {}: {}", rawClass.getName(), result);
                return result;
            }

            @Override
            public void refresh() {
                PUBLIC current = getData();
                PUBLIC latest = probe();
                if (!current.equals(latest)) {
                    cached.set(latest);
                    LOGGER.info("Configuration change detected firing an event about that, previous: {}, current: {}",
                                current, latest);
                    eventManager.fire(new ConfigChangedEvent(current, latest));
                }
            }

            @NotNull
            @Override
            public PUBLIC probe() {
                if (configurationPrefix == null) {
                    return builder.apply(build(rawClass).probe());
                } else {
                    return builder.apply(build(configurationPrefix, rawClass).probe());
                }
            }

            @Override
            public void onConfigChanged(@NotNull ConfigChangedEvent event) {
                GenericConfigChangedEvent<RAW> typed = event.typed(rawClass);
                if (typed != null) {
                    refresh();
                }
            }

            @Override
            public void onRefreshEvent() {
                refresh();
            }
        }
        return new Raw2PublicConfigProvider();
    }

    @NotNull
    @Override
    public <T> ConfigProvider<T> build(
            @NotNull Collection<ConfigProvider<?>> providers,
            @NotNull Function<Source, T> builder
    ) {
        class CompositeConfigProvider implements ConfigProvider<T>, ConfigChangedEventAware {

            private final AtomicReference<T> cached = new AtomicReference<>();

            @Override
            public @NotNull T getData() {
                T c = cached.get();
                if (c != null) {
                    return c;
                }

                T result = probe();
                cached.set(result);
                LOGGER.info("Cached public config based on underlying config providers: {}", result);
                return result;
            }

            @Override
            public void refresh() {
                T current = getData();
                T latest = probe();
                if (!current.equals(latest)) {
                    cached.set(latest);
                    LOGGER.info("Configuration change detected firing an event about that, previous: {}, current: {}",
                                current, latest);
                    eventManager.fire(new ConfigChangedEvent(current, latest));
                }
            }

            @NotNull
            @Override
            public T probe() {
                Source source = new Source() {
                    @SuppressWarnings("unchecked")
                    @NotNull
                    @Override
                    public <L> L get(@NotNull Class<L> clazz) {
                        Collection<String> available = new ArrayList<>();
                        for (ConfigProvider<?> provider : providers) {
                            Object data = provider.probe();
                            if (clazz.isAssignableFrom(data.getClass())) {
                                return (L) data;
                            } else {
                                available.add(data.getClass().getName());
                            }
                        }
                        throw new IllegalArgumentException(
                                "no data provider is registered for class " + clazz.getName() + ", available: "
                                + String.join(", ", available)
                        );
                    }
                };
                return builder.apply(source);
            }

            @Override
            public void onConfigChanged(@NotNull ConfigChangedEvent event) {
                for (ConfigProvider<?> provider : providers) {
                    if (provider.getData().getClass().isInstance(event.getPrevious())) {
                        refresh();
                        return;
                    }
                }
            }

            @Override
            public void onRefreshEvent() {
                refresh();
            }
        }
        return new CompositeConfigProvider();
    }
}

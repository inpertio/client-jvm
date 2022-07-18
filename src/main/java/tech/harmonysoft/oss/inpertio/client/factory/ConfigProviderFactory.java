package tech.harmonysoft.oss.inpertio.client.factory;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import tech.harmonysoft.oss.inpertio.client.ConfigPrefix;
import tech.harmonysoft.oss.inpertio.client.ConfigProvider;
import tech.harmonysoft.oss.inpertio.client.event.ConfigEventManager;

import java.util.Collection;
import java.util.Map;
import java.util.function.Function;

/**
 * Defines contract for building various {@link ConfigProvider config providers}.
 */
public interface ConfigProviderFactory {

    /**
     * Similar to {@link #build(String, Class)} but uses prefix extracted from config type's {@link ConfigPrefix}
     * annotation (if any) or no prefix at all.
     *
     * @param clazz     target config type
     * @param <T>       target config type
     * @return          {@link ConfigProvider} for the target config type
     */
    @NotNull
    <T> ConfigProvider<T> build(@NotNull Class<T> clazz);

    /**
     * <p>
     *     Builds {@link ConfigProvider} for the target config type.
     * </p>
     * <pre>
     *     class MyDbConfig {
     *         String address;
     *         String login;
     *         String password;
     *     }
     * </pre>
     * <pre>
     *     factory.build("my-app.db", MyDbConfig.class) builds a ConfigProvider&lt;MyDbConfig&gt; where
     *     'address' property value is picked up by key 'my-app.db.address', 'login' is picked up by key
     *     'my-app.db.login' and 'password' is picked up by key 'my-app.db.password'
     * </pre>
     *
     * @param configurationPrefix   configuration prefix to use for looking up config properties
     * @param clazz                 target config class
     * @param <T>                   target config class
     * @return                      {@link ConfigProvider} for the target config class
     */
    @NotNull
    <T> ConfigProvider<T> build(@NotNull String configurationPrefix, @NotNull Class<T> clazz);

    /**
     * <p>
     *     We might have a situation when it's necessary to create a {@link ConfigProvider} from, say,
     *     some {@code RAW} class and some other existing {@link ConfigProvider}.
     * </p>
     * <p>
     *     If we need to react on changes either in {@code RAW} class or other {@link ConfigProvider},
     *     {@link #build(Collection, Function)} can be used for that. However, it's necessary to wrap
     *     target {@code RAW} type into {@link ConfigProvider}. This method allows to do that.
     * </p>
     *
     * @param clazz     target config class
     * @param <T>       target config class
     * @return          {@link ConfigProvider} for that target config class
     */
    @NotNull
    default <T> ConfigProvider<T> raw(@NotNull Class<T> clazz) {
        return build(clazz, Function.identity());
    }

    @NotNull
    default <PUBLIC, RAW> ConfigProvider<PUBLIC> build(
            @NotNull Class<RAW> rawClass,
            @NotNull Function<RAW, PUBLIC> builder
    ) {
        return build(rawClass, null, builder);
    }

    /**
     * <p>
     *     It's a common situation when we have {@code 'raw config classes'} which reflect the structure
     *     of actual key/value config values. However, when application needs to work with the configs, it might
     *     be more convenient to define dedicated domain specific {@code 'public config classes'} and use them
     *     in business logic implementation instead.
     * </p>
     * <p>
     *     This method allows building a {@link ConfigProvider} for {@code PUBLIC} config class based on
     *     {@code RAW} config class
     * </p>
     * <p>
     *     If {@link ConfigEventManager} is available, resulting {@link ConfigProvider} automatically refreshes
     *     its state every time underlying {@code RAW} config is changed.
     * </p>
     *
     * @param rawClass            target raw class
     * @param configurationPrefix configuration prefix to use for {@code RAW} config class
     * @param builder             a strategy to build {@code PUBLIC} config object instance based on the given
     *                            {@code RAW} config object
     * @param <PUBLIC>            public config class
     * @param <RAW>               raw config class
     * @return                    {@link ConfigProvider} for the {@code PUBLIC} class
     */
    @NotNull
    <PUBLIC, RAW> ConfigProvider<PUBLIC> build(
            @NotNull Class<RAW> rawClass,
            @Nullable String configurationPrefix,
            @NotNull Function<RAW, PUBLIC> builder
    );

    /**
     * <p>
     *     Allows creating a {@link ConfigProvider} which is based on multiple other underlying {@link ConfigProvider}:
     * </p>
     * <pre>
     *     ConfigProvider&lt;MyCompositeConfig&gt; buildMyCompositeConfigProvider(
     *         ConfigProviderFactory factory,
     *         ConfigProvider&lt;MyFirstConfig&gt; firstProvider,
     *         ConfigProvider&lt;MySecondConfig&gt; secondProvider
     *     ) {
     *         return factory.build(
     *                 source -&gt; new MyCompositeConfig(source.get(MyFirstConfig.class).getFirstProperty(),
     *                                                 source.get(MySecondConfig.class).getSecondProperty()),
     *                 firstProvider, secondProvider
     *         );
     *     }
     * </pre>
     * <p>
     *     <b>Note:</b> it's important to use {@link Source#get(Class)} instead of accessing
     *     {@link ConfigProvider#getData()} directly. The reason is that if particular underlying config
     *     provider is not provided among the {@code providers} and used directly, then config provider
     *     returned from this method is not automatically refreshed on underlying config change. However,
     *     a call to {@link Source#get(Class)} for such lost config provider's config class results in immediate
     *     exception in runtime
     * </p>
     *
     * @param providers   underlying config providers
     * @param builder     target config builder
     * @param <T>         resulting config type
     * @return            config provider for the target type
     */
    @NotNull
    <T> ConfigProvider<T> build(@NotNull Collection<ConfigProvider<?>> providers, @NotNull Function<Source, T> builder);

    /**
     * <p>
     *     Sometimes configuration classes are built from configuration properties located under different
     *     hierarchies. This method allows to handle such case.
     * </p>
     * <pre>
     *     configs:
     *     root.level1.prop1 = value1
     *     root.level2.level3.prop2 = value2
     * </pre>
     * <pre>
     *     class MyConfig {
     *         String prop1;
     *         String prop2;
     *     }
     * </pre>
     * <pre>
     *     Map&lt;String, String&gt; mappings = new HashMap&lt;&gt;();
     *     mappings.put("prop1", "root.level.prop1");
     *     mappings.put("prop2", "root.level2.level3.prop2");
     *     factory.build(mappings, MyConfig.class);
     * </pre>
     *
     * @param propertyName2configKey    mappings between config class properties and low level config property keys
     * @param configClass               target config class
     * @param <T>                       target config class
     * @return                          {@link ConfigProvider} for the target class
     */
    @NotNull
    <T> ConfigProvider<T> build(@NotNull Map<String, String> propertyName2configKey, @NotNull Class<T> configClass);

    interface Source {

        @NotNull
        <T> T get(@NotNull Class<T> clazz);
    }
}
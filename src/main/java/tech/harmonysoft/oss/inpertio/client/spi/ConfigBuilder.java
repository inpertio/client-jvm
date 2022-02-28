package tech.harmonysoft.oss.inpertio.client.spi;

import org.jetbrains.annotations.NotNull;

/**
 * Stands for a strategy which allows building config classes from low level key/value configs.
 */
public interface ConfigBuilder {

    /**
     * Allows building target config object.
     *
     * @param prefix    prefix to use for the given keys. For example, suppose that target config type has a single
     *                  property named {@code key}. If given prefix is equal to, say, {@code 'my.prefix'}, then
     *                  current builder must lookup target value using name {@code 'my.prefix.key'}
     * @param type      target config type
     * @param context   config context to use
     * @param <T>       target config type
     * @return          config object built from the given data
     */
    @NotNull
    <T> T create(@NotNull String prefix, @NotNull Class<T> type, @NotNull Context context);
}
package tech.harmonysoft.oss.inpertio.client.event;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import tech.harmonysoft.oss.inpertio.client.ConfigProvider;

import java.util.Objects;

/**
 * <p>
 *     Every time particular domain config is changed (its low level config is changed and corresponding
 *     {@link ConfigProvider} detects that), corresponding event (an instance of the current class)
 *     {@link ConfigEventManager#fire(ConfigChangedEvent) is fired} for that.
 * </p>
 * <p>
 *     There is a possible case that we have low level config (list of key-value), then {@code ConfigProvider<Config1>}
 *     and {@code ConfigProvider<Config2>} which are based on low level configs; then {@code ConfigProvider<Config3>}
 *     which is based on {@code ConfigProvider<Config1>} and {@code ConfigProvider<Config2>}. Following situation
 *     is possible:
 * </p>
 * <ol>
 *     <li>low-level config used by {@code ConfigProvider<Config2>} is changed</li>
 *     <li>
 *         {@code ConfigProvider<Config2>} is refreshed, it detects the change and fires a change event
 *         for {@code Config2}
 *     </li>
 *     <li>
 *         {@code ConfigProvider<Config3>} detects that there is a change in the underlying {@code Config2},
 *         refreshed {@code Config3} and fires a change event for {@code Config3}
 *     </li>
 * </ol>
 */
public class ConfigChangedEvent implements GenericConfigChangedEvent<Object> {

    @NotNull private final Object previous;
    @NotNull private final Object current;


    public ConfigChangedEvent(@NotNull Object previous, @NotNull Object current) {
        this.previous = previous;
        this.current = current;
    }

    @NotNull
    @Override
    public Object getPrevious() {
        return previous;
    }

    @NotNull
    @Override
    public Object getCurrent() {
        return current;
    }

    /**
     * Utility method for working with configs, should be used by below:
     *
     * <pre>
     *     public void onConfigChanged(@NotNull ConfigChangedEvent event) {
     *         GenericConfigChangedEvent&lt;MyConfig&gt; typedEvent = event.typed(MyConfig.class);
     *         if (typedEvent != null) {
     *             // process the change
     *         }
     *     }
     * </pre>
     *
     * @param clazz     interested config type
     * @param <T>       interested config type
     * @return          type-safe change event for the target config type (if any)
     */
    @SuppressWarnings("unchecked")
    @Nullable
    public <T> GenericConfigChangedEvent<T> typed(Class<T> clazz) {
        if (clazz.isInstance(previous) && clazz.isInstance(current)) {
            return (GenericConfigChangedEvent<T>) this;
        } else {
            return null;
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(previous, current);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ConfigChangedEvent that = (ConfigChangedEvent) o;
        return previous.equals(that.previous) && current.equals(that.current);
    }

    @Override
    public String toString() {
        return "previous=" + previous + ", current=" + current;
    }
}
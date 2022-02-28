package tech.harmonysoft.oss.inpertio.client;

import tech.harmonysoft.oss.inpertio.client.event.ConfigChangedEvent;
import tech.harmonysoft.oss.inpertio.client.event.ConfigEventManager;
import tech.harmonysoft.oss.inpertio.client.event.ConfigChangedEventAware;
import org.jetbrains.annotations.NotNull;

/**
 * <p>
 *     As all configurations are external and might be changed any time, it's recommended to access them via this
 *     interface ({@link #getData()}). That way business logic can be sure that it works with the last known
 *     configs state.
 * </p>
 * <p>
 *     It's possible that particular settings are used in external setup, e.g. we might define server socket's port
 *     in configs, then an application opens a listening socket on that port on startup and even if the port value
 *     is changed later, the socket is already open. In this situation the application can
 *     {@link ConfigEventManager#subscribe(ConfigChangedEventAware) subscribe} to {@link ConfigChangedEvent},
 *     when an event for the target config class is received, close previous socket and open new one on the new port.
 * </p>
 *
 * @param <T>   target config type. Is expected to properly implement {@link Object#equals(Object)}
 */
public interface ConfigProvider<T> {

    /**
     * @return      config object for the last known configs state. This method is fast, so, it's safe to call it
     *              as frequently as necessary
     */
    @NotNull
    T getData();

    /**
     * Normally underlying implementation caches target config and updates it automatically during
     * {@link ConfigChangedEvent} processing.
     *
     * However, there are situations when we might want to force refresh configs, this method allows to do that.
     * Sample processing sequence:
     * <ol>
     *     <li>Particular config state {@code state1} is cached and returned from all calls to {@link #getData()}</li>
     *     <li>Underlying config value is changed</li>
     *     <li>This method is called, new config state {@code state2} is created and cached</li>
     *     <li>Subsequent calls to {@link #getData()} return {@code state2}</li>
     * </ol>
     */
    void refresh();

    /**
     * <p>
     *     Allows creating target config object from the last known low level configs without caching it.
     * </p>
     * Example:
     * <ol>
     *     <li>Particular config state {@code state1} is cached and returned from all calls to {@link #getData()}</li>
     *     <li>
     *         Underlying low level configs are changed, but current {@link ConfigProvider} is not refreshed and its
     *         {@link #getData()} still returns {@code state1}
     *     </li>
     *     <li>This method is called, it builds {@code state2} from the latest configs and returns it</li>
     *     <li>Subsequent calls to {@link #getData()} still return {@code state1}</li>
     * </ol>
     *
     * @return      config object built from the last known low level configs
     */
    @NotNull
    T probe();
}
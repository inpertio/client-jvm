package tech.harmonysoft.oss.inpertio.client.event;

import org.jetbrains.annotations.NotNull;

/**
 * Stands for a callback for entity interested in config change events.
 *
 * @see ConfigChangedEvent
 */
public interface ConfigChangedEventAware {

    void onConfigChanged(@NotNull ConfigChangedEvent event);
}
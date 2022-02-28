package tech.harmonysoft.oss.inpertio.client;

import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * Facades low level key/value configs retrieved from config service.
 */
public interface ConfigClient {

    /**
     * @return  last know low level configs
     */
    @NotNull
    Map<String, String> getConfigs();

    /**
     * Asks to refresh low level configs retrieved from config service
     */
    void refresh();
}
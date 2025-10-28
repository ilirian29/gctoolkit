package com.yourorg.gcdesk.plugins;

/**
 * Lifecycle state of a discovered plug-in.
 */
public enum PluginStatus {
    /** Plug-in loaded successfully and will be used for analysis. */
    LOADED,
    /** Plug-in was discovered but rejected due to compatibility issues. */
    INCOMPATIBLE,
    /** Plug-in failed to load because of an unrecoverable error. */
    FAILED
}

package com.yourorg.gcdesk.plugins;

import com.microsoft.gctoolkit.GCToolKit;

/**
 * Service interface implemented by GC Desk plug-ins. Plug-ins are discovered from
 * {@code META-INF/services/com.yourorg.gcdesk.plugins.GCDeskPlugin} resources
 * packaged inside JAR files located under the application's {@code plugins/}
 * directory.
 *
 * <p>The contract is intentionally small to minimise coupling between the host
 * application and extensions. Implementations should expose stable identifiers
 * and version information so the host can validate compatibility before
 * activation.</p>
 */
public interface GCDeskPlugin {

    /**
     * Current plug-in API version expected by the application.
     */
    String API_VERSION = "1.0";

    /**
     * @return a unique identifier for the plug-in. This should remain stable
     * across releases.
     */
    String getId();

    /**
     * @return human-readable name for display purposes.
     */
    String getName();

    /**
     * @return semantic version of the plug-in implementation.
     */
    String getVersion();

    /**
     * @return optional description surfaced in the UI.
     */
    default String getDescription() {
        return "";
    }

    /**
     * @return API version the plug-in was compiled against.
     */
    String getTargetApiVersion();

    /**
     * @return the GCToolKit version the plug-in requires. Use {@code "*"}
     * or {@code "any"} to opt out of strict version checks.
     */
    String getRequiredGCToolKitVersion();

    /**
     * Optional initialisation callback invoked after the plug-in has been
     * loaded but before any aggregations are registered with a {@link GCToolKit}
     * instance. Implementations may perform lightweight validation here.
     *
     * @throws Exception if the plug-in cannot be initialised
     */
    default void initialize() throws Exception {
        // no-op
    }

    /**
     * Optional shutdown hook invoked when the plug-in manager is disposed.
     *
     * @throws Exception if the plug-in cannot be closed cleanly
     */
    default void close() throws Exception {
        // no-op
    }
}

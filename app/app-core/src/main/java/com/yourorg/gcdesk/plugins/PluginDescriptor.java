package com.yourorg.gcdesk.plugins;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

/**
 * Metadata describing the outcome of loading a plug-in JAR.
 */
public record PluginDescriptor(
        String id,
        String name,
        String version,
        String description,
        String targetApiVersion,
        String requiredGCToolKitVersion,
        PluginStatus status,
        List<String> providedAggregations,
        List<String> warnings,
        List<String> errors,
        Path source
) {
    public PluginDescriptor {
        Objects.requireNonNull(status, "status");
        providedAggregations = List.copyOf(providedAggregations == null ? List.of() : providedAggregations);
        warnings = List.copyOf(warnings == null ? List.of() : warnings);
        errors = List.copyOf(errors == null ? List.of() : errors);
    }

    public boolean isActive() {
        return status == PluginStatus.LOADED;
    }
}

package com.yourorg.gcdesk.plugins;

import java.util.Collections;
import java.util.List;

/**
 * Immutable view of the plug-ins discovered on start-up.
 */
public final class PluginRegistry {

    private final List<PluginDescriptor> descriptors;
    private final String gctoolkitVersion;
    private final String apiVersion;

    public PluginRegistry(List<PluginDescriptor> descriptors, String gctoolkitVersion, String apiVersion) {
        this.descriptors = List.copyOf(descriptors);
        this.gctoolkitVersion = gctoolkitVersion;
        this.apiVersion = apiVersion;
    }

    public List<PluginDescriptor> descriptors() {
        return Collections.unmodifiableList(descriptors);
    }

    public long activeCount() {
        return descriptors.stream().filter(PluginDescriptor::isActive).count();
    }

    public long failureCount() {
        return descriptors.stream().filter(d -> d.status() == PluginStatus.FAILED).count();
    }

    public String gctoolkitVersion() {
        return gctoolkitVersion;
    }

    public String apiVersion() {
        return apiVersion;
    }
}

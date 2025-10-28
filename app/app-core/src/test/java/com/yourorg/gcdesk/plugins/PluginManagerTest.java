package com.yourorg.gcdesk.plugins;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class PluginManagerTest {

    @Test
    void emptyRegistryWhenDirectoryDoesNotExist(@TempDir Path tempDir) {
        Path pluginsDir = tempDir.resolve("plugins");
        try (PluginManager manager = new PluginManager(pluginsDir)) {
            PluginRegistry registry = manager.getRegistry();
            assertThat(registry.descriptors()).isEmpty();
            assertThat(registry.activeCount()).isZero();
            assertThat(registry.failureCount()).isZero();
        }
    }

    @Test
    void emptyRegistryWhenDirectoryExistsButIsEmpty(@TempDir Path pluginsDir) {
        try (PluginManager manager = new PluginManager(pluginsDir)) {
            PluginRegistry registry = manager.getRegistry();
            assertThat(registry.descriptors()).isEmpty();
            assertThat(registry.activeCount()).isZero();
            assertThat(registry.failureCount()).isZero();
        }
    }
}

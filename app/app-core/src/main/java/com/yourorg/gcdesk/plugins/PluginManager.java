package com.yourorg.gcdesk.plugins;

import com.example.app.core.logging.Logging;
import com.microsoft.gctoolkit.GCToolKit;
import com.microsoft.gctoolkit.aggregator.Aggregation;
import org.slf4j.Logger;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;
import java.util.stream.Collectors;

/**
 * Discovers and loads plug-ins packaged as JARs under a {@code plugins/}
 * directory. Each plug-in is isolated using a dedicated {@link URLClassLoader}
 * to prevent classpath interference.
 */
public final class PluginManager implements AutoCloseable {

    private static final Logger LOGGER = Logging.getLogger(PluginManager.class);
    private static final String DEFAULT_DIRECTORY = "plugins";

    private final Path pluginDirectory;
    private final List<LoadedPlugin> activePlugins = new ArrayList<>();
    private PluginRegistry registry;
    private boolean loaded;

    public PluginManager() {
        this(Path.of(DEFAULT_DIRECTORY));
    }

    public PluginManager(Path pluginDirectory) {
        this.pluginDirectory = Objects.requireNonNull(pluginDirectory, "pluginDirectory");
    }

    /**
     * @return immutable view of discovered plug-ins. Loading occurs lazily on the first call.
     */
    public synchronized PluginRegistry getRegistry() {
        ensureLoaded();
        return registry;
    }

    /**
     * Register aggregations from active plug-ins with the provided {@link GCToolKit} instance.
     *
     * @param toolKit analysis toolkit receiving plug-in aggregations
     */
    public synchronized void registerWith(GCToolKit toolKit) {
        Objects.requireNonNull(toolKit, "toolKit");
        ensureLoaded();
        for (LoadedPlugin plugin : activePlugins) {
            plugin.registerWith(toolKit);
        }
    }

    private void ensureLoaded() {
        if (!loaded) {
            loadPlugins();
        }
    }

    private void loadPlugins() {
        loaded = true;
        List<PluginDescriptor> descriptors = new ArrayList<>();
        String gctoolkitVersion = resolveGCToolKitVersion();
        if (!Files.isDirectory(pluginDirectory)) {
            LOGGER.debug("Plug-in directory {} does not exist; continuing without extensions", pluginDirectory);
            registry = new PluginRegistry(descriptors, gctoolkitVersion, GCDeskPlugin.API_VERSION);
            return;
        }

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(pluginDirectory, "*.jar")) {
            for (Path jar : stream) {
                descriptors.addAll(loadJar(jar, gctoolkitVersion));
            }
        } catch (IOException ex) {
            LOGGER.error("Unable to scan plug-in directory {}", pluginDirectory, ex);
            descriptors.add(new PluginDescriptor(
                    "directory",
                    "Plug-in directory",
                    "-",
                    "",
                    GCDeskPlugin.API_VERSION,
                    gctoolkitVersion,
                    PluginStatus.FAILED,
                    List.of(),
                    List.of(),
                    List.of("Unable to scan plug-in directory: " + ex.getMessage()),
                    pluginDirectory));
        }

        registry = new PluginRegistry(descriptors, gctoolkitVersion, GCDeskPlugin.API_VERSION);
    }

    private List<PluginDescriptor> loadJar(Path jar, String gctoolkitVersion) {
        URLClassLoader classLoader = createClassLoader(jar);
        if (classLoader == null) {
            return List.of(new PluginDescriptor(
                    jar.getFileName().toString(),
                    jar.getFileName().toString(),
                    "-",
                    "",
                    GCDeskPlugin.API_VERSION,
                    gctoolkitVersion,
                    PluginStatus.FAILED,
                    List.of(),
                    List.of(),
                    List.of("Unable to create class loader for plug-in"),
                    jar));
        }

        List<PluginDescriptor> descriptors = new ArrayList<>();
        boolean foundAny = false;
        boolean keepClassLoader = false;
        try {
            for (ServiceLoader.Provider<GCDeskPlugin> provider : ServiceLoader.load(GCDeskPlugin.class, classLoader)) {
                foundAny = true;
                PluginProcessingResult result = processPlugin(provider, classLoader, jar, gctoolkitVersion);
                descriptors.add(result.descriptor());
                keepClassLoader |= result.keepClassLoader();
            }
        } catch (ServiceConfigurationError error) {
            LOGGER.error("Invalid plug-in configuration in {}", jar, error);
            descriptors.add(new PluginDescriptor(
                    jar.getFileName().toString(),
                    jar.getFileName().toString(),
                    "-",
                    "",
                    GCDeskPlugin.API_VERSION,
                    gctoolkitVersion,
                    PluginStatus.FAILED,
                    List.of(),
                    List.of(),
                    List.of("Invalid service configuration: " + error.getMessage()),
                    jar));
            closeQuietly(classLoader);
            return descriptors;
        }

        if (!foundAny) {
            LOGGER.warn("Plug-in JAR {} does not declare any {} services", jar, GCDeskPlugin.class.getName());
            descriptors.add(new PluginDescriptor(
                    jar.getFileName().toString(),
                    jar.getFileName().toString(),
                    "-",
                    "",
                    GCDeskPlugin.API_VERSION,
                    gctoolkitVersion,
                    PluginStatus.FAILED,
                    List.of(),
                    List.of(),
                    List.of("No GCDeskPlugin services were found in the archive."),
                    jar));
            closeQuietly(classLoader);
            return descriptors;
        }

        if (!keepClassLoader) {
            closeQuietly(classLoader);
        }
        return descriptors;
    }

    private PluginProcessingResult processPlugin(ServiceLoader.Provider<GCDeskPlugin> provider,
                                                 URLClassLoader classLoader,
                                                 Path jar,
                                                 String gctoolkitVersion) {
        GCDeskPlugin plugin;
        try {
            plugin = provider.get();
        } catch (Throwable ex) {
            LOGGER.error("Failed to instantiate plug-in from {}", jar, ex);
            return new PluginProcessingResult(new PluginDescriptor(
                    jar.getFileName().toString(),
                    jar.getFileName().toString(),
                    "-",
                    "",
                    GCDeskPlugin.API_VERSION,
                    gctoolkitVersion,
                    PluginStatus.FAILED,
                    List.of(),
                    List.of(),
                    List.of("Unable to instantiate plug-in: " + ex.getMessage()),
                    jar), false);
        }

        List<String> warnings = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        PluginStatus status = PluginStatus.LOADED;

        String id = sanitise(plugin.getId(), "Unnamed plug-in");
        String name = sanitise(plugin.getName(), id);
        String version = sanitise(plugin.getVersion(), "-");
        String description = plugin.getDescription() != null ? plugin.getDescription() : "";
        String targetApi = sanitise(plugin.getTargetApiVersion(), "");
        String requiredGCToolkit = sanitise(plugin.getRequiredGCToolKitVersion(), "");

        if (targetApi.isBlank()) {
            status = PluginStatus.FAILED;
            errors.add("Plug-in did not declare a target API version.");
        } else if (!GCDeskPlugin.API_VERSION.equals(targetApi)) {
            status = PluginStatus.INCOMPATIBLE;
            warnings.add(String.format(Locale.ROOT,
                    "Plug-in targets API %s but host expects %s.", targetApi, GCDeskPlugin.API_VERSION));
        }

        if (!requiredGCToolkit.isBlank() && !isWildcard(requiredGCToolkit)) {
            String actual = gctoolkitVersion;
            if (actual == null || actual.isBlank() || "unknown".equalsIgnoreCase(actual)) {
                warnings.add("Running GCToolKit version is unknown; compatibility cannot be verified.");
            } else if (!versionMatches(requiredGCToolkit, actual)) {
                status = PluginStatus.INCOMPATIBLE;
                warnings.add(String.format(Locale.ROOT,
                        "Requires GCToolKit %s but running %s.", requiredGCToolkit, actual));
            }
        }

        List<String> aggregationTypes = Collections.emptyList();
        if (status == PluginStatus.LOADED) {
            try {
                plugin.initialize();
            } catch (Exception ex) {
                status = PluginStatus.FAILED;
                errors.add("Initialisation failed: " + ex.getMessage());
                LOGGER.error("Plug-in {} failed to initialise", id, ex);
            }
        }

        if (status == PluginStatus.LOADED) {
            aggregationTypes = discoverAggregations(classLoader, warnings, errors);
            if (aggregationTypes.isEmpty()) {
                warnings.add("No Aggregation services were exported by the plug-in.");
            }
            activePlugins.add(new LoadedPlugin(plugin, classLoader, aggregationTypes, jar));
            LOGGER.info("Loaded plug-in {} v{} from {}", id, version, jar);
        }

        return new PluginProcessingResult(new PluginDescriptor(
                id,
                name,
                version,
                description,
                targetApi,
                requiredGCToolkit,
                status,
                aggregationTypes,
                warnings,
                errors,
                jar), status == PluginStatus.LOADED);
    }

    private List<String> discoverAggregations(ClassLoader classLoader, List<String> warnings, List<String> errors) {
        try {
            return ServiceLoader.load(Aggregation.class, classLoader)
                    .stream()
                    .map(ServiceLoader.Provider::type)
                    .map(Class::getName)
                    .distinct()
                    .collect(Collectors.toList());
        } catch (ServiceConfigurationError error) {
            errors.add("Failed to inspect aggregations: " + error.getMessage());
            LOGGER.error("Aggregation service lookup failed", error);
        }
        return List.of();
    }

    private URLClassLoader createClassLoader(Path jar) {
        try {
            URL url = jar.toUri().toURL();
            return new URLClassLoader(new URL[]{url}, PluginManager.class.getClassLoader());
        } catch (MalformedURLException ex) {
            LOGGER.error("Invalid plug-in path {}", jar, ex);
            return null;
        }
    }

    private static boolean isWildcard(String value) {
        return "*".equals(value) || "any".equalsIgnoreCase(value);
    }

    private static boolean versionMatches(String required, String actual) {
        return required.equals(actual);
    }

    private static String sanitise(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.trim();
    }

    private String resolveGCToolKitVersion() {
        Package pkg = GCToolKit.class.getPackage();
        if (pkg != null) {
            String version = pkg.getImplementationVersion();
            if (version != null && !version.isBlank()) {
                return version;
            }
        }
        return "unknown";
    }

    private void closeQuietly(URLClassLoader classLoader) {
        if (classLoader == null) {
            return;
        }
        try {
            classLoader.close();
        } catch (IOException ex) {
            LOGGER.debug("Failed to close class loader", ex);
        }
    }

    @Override
    public synchronized void close() {
        for (LoadedPlugin plugin : activePlugins) {
            plugin.close();
        }
        activePlugins.clear();
    }

    private static final class LoadedPlugin {
        private final GCDeskPlugin plugin;
        private final URLClassLoader classLoader;
        private final List<String> aggregationTypes;
        private final Path source;

        private LoadedPlugin(GCDeskPlugin plugin, URLClassLoader classLoader, List<String> aggregationTypes, Path source) {
            this.plugin = plugin;
            this.classLoader = classLoader;
            this.aggregationTypes = List.copyOf(aggregationTypes);
            this.source = source;
        }

        private void registerWith(GCToolKit toolKit) {
            for (String typeName : aggregationTypes) {
                try {
                    Aggregation aggregation = instantiate(typeName);
                    toolKit.loadAggregation(aggregation);
                } catch (Exception ex) {
                    LOGGER.error("Failed to register aggregation {} from {}", typeName, source, ex);
                }
            }
        }

        private Aggregation instantiate(String typeName) throws ClassNotFoundException, NoSuchMethodException,
                InvocationTargetException, InstantiationException, IllegalAccessException {
            Class<?> type = Class.forName(typeName, true, classLoader);
            if (!Aggregation.class.isAssignableFrom(type)) {
                throw new IllegalArgumentException(typeName + " does not implement Aggregation");
            }
            @SuppressWarnings("unchecked")
            Class<? extends Aggregation> aggregationClass = (Class<? extends Aggregation>) type;
            Constructor<? extends Aggregation> ctor = aggregationClass.getDeclaredConstructor();
            if (!ctor.canAccess(null)) {
                ctor.setAccessible(true);
            }
            return ctor.newInstance();
        }

        private void close() {
            try {
                plugin.close();
            } catch (Exception ex) {
                LOGGER.warn("Plug-in {} threw while closing", plugin.getId(), ex);
            }
            try {
                classLoader.close();
            } catch (IOException ex) {
                LOGGER.debug("Failed to close class loader for plug-in {}", plugin.getId(), ex);
            }
        }
    }

    private record PluginProcessingResult(PluginDescriptor descriptor, boolean keepClassLoader) {
    }
}

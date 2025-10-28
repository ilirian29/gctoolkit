# GC Toolkit Desktop User Manual

The GC Toolkit desktop application helps you analyze Java garbage collection (GC) activity quickly and consistently. This guide walks through installation, importing logs, navigating dashboards, and exporting results.

## 1. Installation

### System requirements

- Java 17 or later (JRE or JDK)
- macOS 12+, Windows 10+, or a modern Linux distribution with JavaFX support
- 8 GB RAM recommended for analyzing large log sets
- Internet access for downloading packaged releases (optional when installing from offline media)

### Install from packaged release

1. Download the latest installer or ZIP from the project releases page.
2. Unzip or run the installer in a directory where you have write permissions.
3. Launch the application:
   - **macOS/Linux:** `./gcdesk` (or double-click the launcher)
   - **Windows:** `gcdesk.exe`
4. On first launch, confirm the default workspace and export directories.

### Install from source (developers)

1. Ensure Java 17+ and Maven 3.9+ are installed.
2. Clone the repository and change into the project directory.
3. Run `./mvnw -pl app/app-ui javafx:run` to start the UI directly, or `./mvnw -pl app/app-ui -am package` to produce platform-specific bundles.
4. The packaged application appears under `app/app-ui/target`, ready to distribute.

## 2. Import GC logs

1. Start the application and select **Add GC Log** on the log selection pane.
2. Browse to one or more GC log files (plain text or `.gz`) and open them.
3. Configure optional analysis parameters:
   - Time zone handling (use JVM timestamps or convert to local time).
   - Sampling window size for summary metrics.
4. Click **Analyze**. The progress dialog lists parsing status and any warnings.
5. When analysis completes, results load in the dashboard and the log appears in **Recent Analyses**.

### Tips

- Drag-and-drop files onto the log selection pane to queue them instantly.
- Multi-select logs to build composite timelines for clustered JVMs.
- Use the **Clear** action to reset the queue before starting a new investigation.

## 3. Navigate the dashboards

The workspace is split into three regions: the log selection header, the dashboard center panel, and the workspace/sidebar on the left.

### Dashboard overview

- **Summary** — key KPIs such as throughput, pause counts, and longest pause.
- **Timeline** — interactive chart showing GC events. Hover to see detailed metadata and pause durations.
- **Heap & Metaspace** — area charts for memory usage before/after collection events.
- **JVM Traces** — tables listing GC events, triggers, and contributing subsystems.

### Interaction patterns

- **Zoom and pan:** Scroll to zoom the timeline, drag to pan, or use the toolbar range presets (1h/6h/24h).
- **Filter:** Toggle collectors (Young, Mixed, Full) and event severities to focus on relevant data.
- **Correlate:** Selecting an event in the table highlights it in the charts and reveals detailed stats in the inspector panel.
- **Workspace history:** The left sidebar tracks previous analyses; double-click an entry to reload it.

## 4. Export workflows

Use the **Export** controls on the dashboard to create sharable artifacts.

### Reports

1. Click **Export ➜ HTML Report**.
2. Choose the destination directory (defaults to your preferences).
3. Enter optional analyst notes to embed in the report header.
4. The generated report includes summary statistics, key charts, and recommendations.

### Charts and raw data

- **Charts:** Select **Export ➜ Charts (PNG)** to download high-resolution images of the currently visible charts.
- **CSV:** Choose **Export ➜ Event Data (CSV)** to capture tabular GC events for spreadsheet analysis.

### Crash context bundles

If a log contains fatal errors, **Export ➜ Crash Bundle** packages the log segments, JVM metadata, and timeline snapshots into a ZIP ready for incident tracking systems.

## 5. Troubleshooting

- **Analysis fails immediately:** Confirm the log format is supported (Unified, G1, ZGC). See the console for parsing errors.
- **Charts are blank:** Ensure the log uses timestamps. Turn off filtering options that might hide all events.
- **Long exports:** Large logs may require additional heap. Increase the **Analysis concurrency** preference or allocate more memory.

## 6. Additional resources

- Visit the [Guided Tutorials](./guided-tutorials.md) for scenario-based walkthroughs.
- Check the [project README](../README.md) for release notes and support channels.
- File issues or feature requests via the issue tracker if you encounter unexpected behavior.

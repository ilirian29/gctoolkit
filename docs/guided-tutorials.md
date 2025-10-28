# GC Toolkit Guided Tutorials

Use these scripts to record narrated walkthroughs (screen captures, GIFs, or screenshots) that demonstrate common GC analysis workflows. Each scenario includes the recommended flow, callouts for visuals, and narration prompts.

## Tutorial 1 – Diagnose long GC pauses

**Goal:** Pinpoint the root cause of multi-second pauses affecting end-user latency.

1. **Intro slide / title card**
   - Display: "Diagnosing Long GC Pauses".
   - Narration: Introduce the affected JVM, time range, and impact.
2. **Import the problematic logs**
   - Action: Click **Add GC Log**, select the log file, and press **Analyze**.
   - Narration: Explain the log source (production cluster, timeframe) and mention concurrent analyses if relevant.
   - Capture: Show the progress dialog highlighting parsing status.
3. **Highlight the pause timeline**
   - Action: In the dashboard, zoom into the timeframe with spikes.
   - Narration: Point out the longest pause duration and how it correlates with collector type (Young vs Full GC).
   - Capture: Use a callout or cursor highlight on the timeline peaks.
4. **Inspect heap usage trends**
   - Action: Switch to the **Heap & Metaspace** view and hover over points before/after pauses.
   - Narration: Describe memory pressure, promotion failures, or metaspace growth.
   - Capture: Freeze-frame the tooltip showing pre/post heap values.
5. **Summarize findings**
   - Action: Open **Export ➜ HTML Report** to show the summary.
   - Narration: Recap key metrics and recommend tuning actions (e.g., adjust heap regions or pause goals).
   - Capture: End on the generated report or a closing slide with takeaways.

## Tutorial 2 – Validate tuning changes

**Goal:** Compare GC performance before and after a configuration change.

1. **Intro**
   - Display: "Validating GC Tuning Changes" title card.
   - Narration: Explain the tuning hypothesis (e.g., enabling G1 adaptive IHOP).
2. **Import baseline and experiment logs**
   - Action: Queue both logs and analyze sequentially.
   - Narration: Note that both runs appear in **Recent Analyses** for quick switching.
3. **Use workspace history**
   - Action: Show how to reopen the baseline session from the sidebar.
   - Narration: Emphasize side-by-side comparison without re-parsing.
4. **Compare dashboard metrics**
   - Action: Highlight throughput, pause quantiles, and heap occupancy for each run.
   - Narration: Describe improvements or regressions observed.
5. **Export comparison artifacts**
   - Action: Export charts for both runs and assemble them in presentation slides.
   - Narration: Encourage viewers to include analyst notes using the export dialog.

## Tutorial 3 – Share crash diagnostics with support teams

**Goal:** Capture critical evidence for a JVM crash or fatal error.

1. **Intro**
   - Display: "Sharing Crash Diagnostics".
   - Narration: Set context—production outage, need for rapid triage.
2. **Import the crash log**
   - Action: Analyze the log; spotlight warning banners that appear in the progress dialog.
   - Narration: Describe how the parser flags truncated logs or unsupported sections.
3. **Review crash markers**
   - Action: On the dashboard, select the event flagged as "Fatal".
   - Narration: Explain metadata fields (thread, trigger, timestamp).
4. **Export crash bundle**
   - Action: Use **Export ➜ Crash Bundle** and choose a shared directory.
   - Narration: Walk through the generated ZIP contents (log excerpts, timeline snapshots, JVM info).
5. **Closing guidance**
   - Narration: Encourage attaching the bundle to support tickets and linking to the User Manual for follow-up steps.

## Recording checklist

- Use a consistent theme (light or dark) for all captures.
- Zoom into UI elements before highlighting them.
- Provide captions or callouts for silent playback versions.
- Store final media under `docs/media/` and link them from the online documentation when ready.

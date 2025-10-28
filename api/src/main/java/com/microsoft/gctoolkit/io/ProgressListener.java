package com.microsoft.gctoolkit.io;

/**
 * Listener notified as the toolkit streams a GC log.
 */
@FunctionalInterface
public interface ProgressListener {

    ProgressListener NO_OP = update -> { };

    void onProgress(ProgressUpdate update);
}

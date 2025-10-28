package com.yourorg.gcdesk;

import com.yourorg.gcdesk.ui.GCDeskApplication;

/**
 * Entry point used when running the shaded desktop application JAR.
 */
public final class DesktopLauncher {

    private DesktopLauncher() {
        // Prevent instantiation
    }

    public static void main(String[] args) {
        GCDeskApplication.main(args);
    }
}

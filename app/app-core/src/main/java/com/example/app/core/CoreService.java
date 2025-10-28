package com.example.app.core;

import com.microsoft.gctoolkit.GCToolKit;

/**
 * Simple facade that demonstrates linking against GCToolKit libraries.
 */
public class CoreService {

    public String getToolkitName() {
        return "Loaded toolkit: " + GCToolKit.class.getSimpleName();
    }
}

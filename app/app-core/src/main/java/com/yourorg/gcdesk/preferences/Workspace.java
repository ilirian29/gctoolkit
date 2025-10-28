package com.yourorg.gcdesk.preferences;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Captures workspace-specific preferences, including the recent analyses list.
 */
public record Workspace(@JsonProperty("recentAnalyses") List<WorkspaceEntry> recentAnalyses) {

    @JsonCreator
    public Workspace {
        List<WorkspaceEntry> entries = recentAnalyses == null
                ? Collections.emptyList()
                : List.copyOf(new ArrayList<>(recentAnalyses));
        recentAnalyses = entries;
    }

    public static Workspace empty() {
        return new Workspace(List.of());
    }

    public Workspace withEntries(List<WorkspaceEntry> entries) {
        Objects.requireNonNull(entries, "entries");
        return new Workspace(entries);
    }
}

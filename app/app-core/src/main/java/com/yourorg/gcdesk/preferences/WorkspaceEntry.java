package com.yourorg.gcdesk.preferences;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Representation of a recently analysed GC log within the workspace.
 */
public record WorkspaceEntry(
        @JsonProperty("path") String path,
        @JsonProperty("displayName") String displayName,
        @JsonProperty("group") String group,
        @JsonProperty("tags") List<String> tags,
        @JsonProperty("lastOpened") Instant lastOpened) {

    private static final List<String> EMPTY_TAGS = List.of();

    @JsonCreator
    public WorkspaceEntry {
        path = Objects.requireNonNull(path, "path");
        displayName = displayName == null || displayName.isBlank()
                ? Path.of(path).getFileName().toString()
                : displayName;
        group = group != null && !group.isBlank() ? group : null;
        tags = tags == null || tags.isEmpty() ? EMPTY_TAGS : List.copyOf(new ArrayList<>(normalizeTags(tags)));
        lastOpened = Objects.requireNonNull(lastOpened, "lastOpened");
    }

    private static List<String> normalizeTags(List<String> tags) {
        List<String> normalized = new ArrayList<>();
        for (String tag : tags) {
            if (tag == null) {
                continue;
            }
            String trimmed = tag.trim();
            if (!trimmed.isEmpty() && !normalized.contains(trimmed)) {
                normalized.add(trimmed);
            }
        }
        return normalized;
    }

    /**
     * Convenience factory for creating an entry from a log path.
     */
    public static WorkspaceEntry create(Path path, Instant lastOpened) {
        Objects.requireNonNull(path, "path");
        return new WorkspaceEntry(path.toString(), deriveDisplayName(path), null, EMPTY_TAGS, lastOpened);
    }

    private static String deriveDisplayName(Path path) {
        Path fileName = path.getFileName();
        return fileName != null ? fileName.toString() : path.toString();
    }

    public WorkspaceEntry withTags(List<String> tags) {
        return new WorkspaceEntry(path, displayName, group, tags, lastOpened);
    }

    public WorkspaceEntry withGroup(String group) {
        return new WorkspaceEntry(path, displayName, group, tags, lastOpened);
    }

    public WorkspaceEntry withLastOpened(Instant lastOpened) {
        return new WorkspaceEntry(path, displayName, group, tags, lastOpened);
    }
}

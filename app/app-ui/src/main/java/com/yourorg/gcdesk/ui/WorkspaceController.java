package com.yourorg.gcdesk.ui;

import com.yourorg.gcdesk.preferences.PreferencesService;
import com.yourorg.gcdesk.preferences.UserPreferences;
import com.yourorg.gcdesk.preferences.Workspace;
import com.yourorg.gcdesk.preferences.WorkspaceEntry;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;

import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Controller backing the workspace sidebar which surfaces recent analyses with grouping and tagging support.
 */
public class WorkspaceController {

    private static final String ALL_GROUPS = "All groups";
    private static final String UNGROUPED = "Ungrouped";

    @FXML
    private TreeView<Object> workspaceTree;

    @FXML
    private ComboBox<String> groupFilter;

    @FXML
    private TextField tagFilterField;

    @FXML
    private TextField groupField;

    @FXML
    private TextField tagsField;

    @FXML
    private Button openButton;

    @FXML
    private Label statusLabel;

    private final ObservableList<WorkspaceEntry> entries = FXCollections.observableArrayList();
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
            .withLocale(Locale.getDefault());

    private PreferencesService preferencesService;
    private UserPreferences preferences;
    private Consumer<Path> onOpenRecent;

    @FXML
    private void initialize() {
        workspaceTree.setShowRoot(false);
        workspaceTree.setPlaceholder(new Label("No analyses yet. Run an analysis to build your workspace."));
        workspaceTree.setCellFactory(this::createCell);
        workspaceTree.getSelectionModel().selectedItemProperty().addListener((obs, oldItem, newItem) -> handleSelection(newItem));

        groupFilter.getItems().setAll(ALL_GROUPS);
        groupFilter.getSelectionModel().select(ALL_GROUPS);
        groupFilter.valueProperty().addListener((obs, oldVal, newVal) -> refreshTree());

        tagFilterField.textProperty().addListener((obs, oldVal, newVal) -> refreshTree());

        statusLabel.setText("Select an analysis to view its details.");
        openButton.setDisable(true);
    }

    public void setPreferencesService(PreferencesService preferencesService) {
        this.preferencesService = Objects.requireNonNull(preferencesService, "preferencesService");
    }

    public void setPreferences(UserPreferences preferences) {
        this.preferences = Objects.requireNonNull(preferences, "preferences");
        Workspace workspace = preferences.workspace();
        if (workspace != null) {
            entries.setAll(workspace.recentAnalyses());
        } else {
            entries.clear();
        }
        refreshTree();
    }

    public void setOnOpenRecent(Consumer<Path> onOpenRecent) {
        this.onOpenRecent = onOpenRecent;
    }

    public void recordAnalysis(Path path, Instant openedAt) {
        Objects.requireNonNull(path, "path");
        Instant timestamp = openedAt != null ? openedAt : Instant.now();
        Path absolute = path.toAbsolutePath().normalize();
        String normalized = absolute.toString();
        Optional<WorkspaceEntry> existing = entries.stream()
                .filter(entry -> entry.path().equals(normalized))
                .findFirst();

        WorkspaceEntry updated = existing
                .map(entry -> entry.withLastOpened(timestamp))
                .orElseGet(() -> WorkspaceEntry.create(absolute, timestamp));

        entries.removeIf(entry -> entry.path().equals(normalized));
        entries.add(updated);
        persistAndRefresh(updated.path());
    }

    @FXML
    private void onOpenSelected() {
        WorkspaceEntry entry = getSelectedEntry();
        if (entry == null) {
            statusLabel.setText("Select an analysis to open it.");
            return;
        }
        entries.remove(entry);
        WorkspaceEntry updated = entry.withLastOpened(Instant.now());
        entries.add(updated);
        persistAndRefresh(updated.path());
        if (onOpenRecent != null) {
            onOpenRecent.accept(Path.of(updated.path()));
        }
        statusLabel.setText("Prepared " + updated.displayName() + " for analysis.");
    }

    @FXML
    private void onUpdateMetadata() {
        WorkspaceEntry entry = getSelectedEntry();
        if (entry == null) {
            statusLabel.setText("Select an analysis to update metadata.");
            return;
        }
        List<String> tags = parseTags(tagsField.getText());
        String group = normalizeGroup(groupField.getText());
        WorkspaceEntry updated = new WorkspaceEntry(entry.path(), entry.displayName(), group, tags, entry.lastOpened());
        entries.remove(entry);
        entries.add(updated);
        persistAndRefresh(updated.path());
        statusLabel.setText("Updated metadata for " + updated.displayName() + ".");
    }

    @FXML
    private void onClearFilters() {
        groupFilter.getSelectionModel().select(ALL_GROUPS);
        tagFilterField.clear();
    }

    private void refreshTree() {
        FXCollections.sort(entries, Comparator.comparing(WorkspaceEntry::lastOpened).reversed());

        TreeItem<Object> root = new TreeItem<>();
        root.setExpanded(true);

        Map<String, TreeItem<Object>> groups = new TreeMap<>();
        for (WorkspaceEntry entry : entries) {
            if (!matchesFilters(entry)) {
                continue;
            }
            String groupName = entry.group() != null ? entry.group() : UNGROUPED;
            TreeItem<Object> groupNode = groups.computeIfAbsent(groupName, name -> {
                TreeItem<Object> node = new TreeItem<>(name);
                node.setExpanded(true);
                root.getChildren().add(node);
                return node;
            });
            groupNode.getChildren().add(new TreeItem<>(entry));
        }

        workspaceTree.setRoot(root);
        updateGroupFilterOptions();

        if (entries.isEmpty()) {
            statusLabel.setText("Run an analysis to populate your workspace.");
        } else if (root.getChildren().isEmpty()) {
            statusLabel.setText("No analyses match the current filters.");
        }
    }

    private void updateGroupFilterOptions() {
        String previousSelection = groupFilter.getSelectionModel().getSelectedItem();
        List<String> groupNames = entries.stream()
                .map(entry -> entry.group() != null ? entry.group() : UNGROUPED)
                .distinct()
                .sorted(String::compareToIgnoreCase)
                .collect(Collectors.toCollection(ArrayList::new));

        groupFilter.getItems().setAll(ALL_GROUPS);
        groupFilter.getItems().addAll(groupNames);

        if (previousSelection != null && groupFilter.getItems().contains(previousSelection)) {
            groupFilter.getSelectionModel().select(previousSelection);
        } else {
            groupFilter.getSelectionModel().select(ALL_GROUPS);
        }
    }

    private TreeCell<Object> createCell(TreeView<Object> treeView) {
        return new TreeCell<>() {
            @Override
            protected void updateItem(Object item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    getStyleClass().remove("workspace-group-cell");
                } else if (item instanceof String groupName) {
                    setText(groupName);
                    if (!getStyleClass().contains("workspace-group-cell")) {
                        getStyleClass().add("workspace-group-cell");
                    }
                } else if (item instanceof WorkspaceEntry entry) {
                    setText(formatEntry(entry));
                    getStyleClass().remove("workspace-group-cell");
                }
            }
        };
    }

    private String formatEntry(WorkspaceEntry entry) {
        StringBuilder builder = new StringBuilder(entry.displayName());
        builder.append(" • ");
        builder.append(formatter.format(entry.lastOpened().atZone(ZoneId.systemDefault())));
        if (!entry.tags().isEmpty()) {
            builder.append(" • ");
            builder.append(String.join(", ", entry.tags()));
        }
        return builder.toString();
    }

    private boolean matchesFilters(WorkspaceEntry entry) {
        String selectedGroup = groupFilter.getSelectionModel().getSelectedItem();
        if (selectedGroup != null && !ALL_GROUPS.equals(selectedGroup)) {
            String entryGroup = entry.group() != null ? entry.group() : UNGROUPED;
            if (!selectedGroup.equals(entryGroup)) {
                return false;
            }
        }

        List<String> requiredTags = parseTags(tagFilterField.getText());
        if (!requiredTags.isEmpty()) {
            for (String tag : requiredTags) {
                boolean present = entry.tags().stream().anyMatch(existing -> existing.equalsIgnoreCase(tag));
                if (!present) {
                    return false;
                }
            }
        }
        return true;
    }

    private WorkspaceEntry getSelectedEntry() {
        TreeItem<Object> selectedItem = workspaceTree.getSelectionModel().getSelectedItem();
        if (selectedItem != null && selectedItem.getValue() instanceof WorkspaceEntry entry) {
            return entry;
        }
        return null;
    }

    private void handleSelection(TreeItem<Object> selectedItem) {
        if (selectedItem == null || !(selectedItem.getValue() instanceof WorkspaceEntry entry)) {
            openButton.setDisable(true);
            tagsField.clear();
            groupField.clear();
            return;
        }
        openButton.setDisable(false);
        groupField.setText(entry.group() != null ? entry.group() : "");
        tagsField.setText(String.join(", ", entry.tags()));
        statusLabel.setText("Last opened " + formatter.format(entry.lastOpened().atZone(ZoneId.systemDefault())));
    }

    private void persistAndRefresh(String pathToSelect) {
        if (preferences != null) {
            Workspace workspace = new Workspace(List.copyOf(entries));
            preferences = preferences.withWorkspace(workspace);
            if (preferencesService != null) {
                preferencesService.save(preferences);
            }
        }
        refreshTree();
        selectEntry(pathToSelect);
    }

    private void selectEntry(String path) {
        TreeItem<Object> root = workspaceTree.getRoot();
        if (root == null) {
            return;
        }
        for (TreeItem<Object> group : root.getChildren()) {
            for (TreeItem<Object> child : group.getChildren()) {
                Object value = child.getValue();
                if (value instanceof WorkspaceEntry entry && entry.path().equals(path)) {
                    workspaceTree.getSelectionModel().select(child);
                    return;
                }
            }
        }
    }

    private List<String> parseTags(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        return Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(token -> !token.isEmpty())
                .map(token -> token.replaceAll("\\s+", " "))
                .distinct()
                .collect(Collectors.toList());
    }

    private String normalizeGroup(String raw) {
        if (raw == null) {
            return null;
        }
        String trimmed = raw.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}

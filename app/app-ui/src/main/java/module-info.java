module com.yourorg.gcdesk.ui {
    requires javafx.base;
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;

    requires com.yourorg.gcdesk.core;
    requires com.microsoft.gctoolkit.api;

    exports com.yourorg.gcdesk.ui;
    opens com.yourorg.gcdesk.ui to javafx.fxml;
}

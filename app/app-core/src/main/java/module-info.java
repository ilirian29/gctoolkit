module com.yourorg.gcdesk.core {
    requires com.microsoft.gctoolkit.api;
    requires com.microsoft.gctoolkit.parser;
    requires com.microsoft.gctoolkit.vertx;

    exports com.yourorg.gcdesk;
    exports com.yourorg.gcdesk.model;
    exports com.example.app.core;
    exports com.example.app.core.aggregations;
    exports com.example.app.core.collections;

    provides com.microsoft.gctoolkit.aggregator.Aggregation with
            com.example.app.core.aggregations.HeapOccupancyAfterCollectionSummary,
            com.example.app.core.aggregations.PauseTimeSummary,
            com.example.app.core.aggregations.CollectionCycleCountsSummary,
            com.example.app.core.aggregations.DesktopPausePercentileSummary,
            com.example.app.core.aggregations.DesktopGCCauseFrequencySummary;
}

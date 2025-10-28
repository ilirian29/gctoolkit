package com.yourorg.gcdesk.model;

import com.microsoft.gctoolkit.event.GarbageCollectionTypes;

import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Immutable representation of heap occupancy series grouped by garbage collection type.
 */
public class HeapOccupancySummary {

    private final Map<GarbageCollectionTypes, List<XYPoint>> seriesByType;

    public HeapOccupancySummary(Map<GarbageCollectionTypes, List<XYPoint>> seriesByType) {
        if (seriesByType == null || seriesByType.isEmpty()) {
            this.seriesByType = Collections.emptyMap();
        } else {
            Map<GarbageCollectionTypes, List<XYPoint>> copy = new EnumMap<>(GarbageCollectionTypes.class);
            seriesByType.forEach((type, points) -> copy.put(type, List.copyOf(points)));
            this.seriesByType = Collections.unmodifiableMap(copy);
        }
    }

    public static HeapOccupancySummary empty() {
        return new HeapOccupancySummary(Collections.emptyMap());
    }

    public Map<GarbageCollectionTypes, List<XYPoint>> getSeriesByType() {
        return seriesByType;
    }

    /**
     * Simple immutable representation of a coordinate pair from the GC log.
     */
    public static class XYPoint {
        private final double x;
        private final double y;

        public XYPoint(double x, double y) {
            this.x = x;
            this.y = y;
        }

        public double getX() {
            return x;
        }

        public double getY() {
            return y;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof XYPoint)) {
                return false;
            }
            XYPoint xyPoint = (XYPoint) o;
            return Double.compare(xyPoint.x, x) == 0 && Double.compare(xyPoint.y, y) == 0;
        }

        @Override
        public int hashCode() {
            return Objects.hash(x, y);
        }

        @Override
        public String toString() {
            return "XYPoint{" + "x=" + x + ", y=" + y + '}';
        }
    }
}

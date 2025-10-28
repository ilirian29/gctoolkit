package com.example.app.core.collections;

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalDouble;
import java.util.stream.Stream;

/**
 * Simple XY dataset implementation copied from the sample module so
 * aggregations can accumulate time series data without introducing
 * additional dependencies.
 */
public class XYDataSet {
    private final List<Point> dataSeries;

    public XYDataSet() {
        dataSeries = new ArrayList<>();
    }

    public XYDataSet(XYDataSet series) {
        dataSeries = new ArrayList<>(series.getItems());
    }

    public void add(double x, double y) {
        dataSeries.add(new Point(x, y));
    }

    public void add(Number x, Number y) {
        add(x.doubleValue(), y.doubleValue());
    }

    public void add(Point item) {
        dataSeries.add(item);
    }

    protected void addAll(List<Point> items) {
        dataSeries.addAll(items);
    }

    public boolean isEmpty() {
        return dataSeries.isEmpty();
    }

    /**
     * Returns an immutable List of the items in this DataSet.
     */
    public List<Point> getItems() {
        return List.copyOf(dataSeries);
    }

    public XYDataSet scaleSeries(double scaleFactor) {
        XYDataSet scaled = new XYDataSet();
        for (Point item : dataSeries) {
            scaled.add(item.getX(), item.getY() * scaleFactor);
        }
        return scaled;
    }

    /**
     * Returns the largest Y value in the XYDataSet as an OptionalDouble,
     * with an empty optional if the dataset is empty.
     */
    public OptionalDouble maxOfY() {
        return dataSeries.stream()
                .mapToDouble(Point::getY)
                .max();
    }

    public XYDataSet scaleAndTranslateXAxis(double scale, double offset) {
        XYDataSet translatedSeries = new XYDataSet();
        for (Point dataPoint : dataSeries) {
            double scaledXCoordinate = (scale * dataPoint.getX()) + offset;
            translatedSeries.add(scaledXCoordinate, dataPoint.getY());
        }
        return translatedSeries;
    }

    public int size() {
        return dataSeries.size();
    }

    public Stream<Point> stream() {
        return dataSeries.stream();
    }

    public static class Point {
        private final double x;
        private final double y;

        public Point(Number x, Number y) {
            this(x.doubleValue(), y.doubleValue());
        }

        public Point(double x, double y) {
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
        public String toString() {
            return x + "," + y;
        }
    }
}

package com.longshanks.visualization;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.time.Day;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.util.Map;

/**
 * Handles plotting of cumulative returns and drawdowns using JFreeChart.
 */
public class Plotter {

    /**
     * Plots cumulative returns for a single strategy and displays it.
     *
     * @param strategyReturns Map of date to return.
     * @param strategyName    Name of the strategy.
     */
    public static void plotCumulativeReturns(Map<LocalDate, Double> strategyReturns, String strategyName) {
        TimeSeries series = new TimeSeries(strategyName + " Cumulative Returns");
        double cumulative = 1.0;
        for (Map.Entry<LocalDate, Double> entry : strategyReturns.entrySet()) {
            cumulative *= (1 + entry.getValue());
            LocalDate date = entry.getKey();
            Day day = new Day(date.getDayOfMonth(), date.getMonthValue(), date.getYear());
            series.addOrUpdate(day, cumulative);
        }

        TimeSeriesCollection dataset = new TimeSeriesCollection();
        dataset.addSeries(series);

        JFreeChart chart = ChartFactory.createTimeSeriesChart(
                strategyName + " - Cumulative Returns",
                "Date",
                "Cumulative Return",
                dataset,
                true,  // Legend
                true,  // Tooltips
                false  // URLs
        );

        try {
            String fileName = strategyName.replaceAll(" ", "") + "_CumulativeReturns.png";
            ChartUtils.saveChartAsPNG(new File(fileName), chart, 800, 600);
            System.out.println(strategyName + " cumulative returns chart saved as " + fileName);
        } catch (IOException e) {
            System.err.println("Error saving " + strategyName + " cumulative returns chart: " + e.getMessage());
        }

        // Display the chart in a Swing window
        displayChart(chart, strategyName + " Cumulative Returns");
    }

    /**
     * Plots drawdowns for a single strategy and displays it.
     *
     * @param strategyReturns Map of date to return.
     * @param strategyName    Name of the strategy.
     */
    public static void plotDrawdowns(Map<LocalDate, Double> strategyReturns, String strategyName) {
        TimeSeries series = new TimeSeries(strategyName + " Drawdown");
        double peak = Double.NEGATIVE_INFINITY;
        double cumulative = 1.0;

        for (Map.Entry<LocalDate, Double> entry : strategyReturns.entrySet()) {
            LocalDate date = entry.getKey();
            double ret = entry.getValue();

            cumulative *= (1 + ret);
            if (cumulative > peak) {
                peak = cumulative;
            }
            double drawdown = (cumulative - peak) / peak;
            series.addOrUpdate(new Day(date.getDayOfMonth(), date.getMonthValue(), date.getYear()), drawdown);
        }

        TimeSeriesCollection dataset = new TimeSeriesCollection();
        dataset.addSeries(series);

        JFreeChart chart = ChartFactory.createTimeSeriesChart(
                strategyName + " - Drawdowns",
                "Date",
                "Drawdown",
                dataset,
                true,  // Legend
                true,  // Tooltips
                false  // URLs
        );

        // Adjust Y-axis to range from -1 to 0 for better visibility
        XYPlot plot = chart.getXYPlot();
        NumberAxis range = (NumberAxis) plot.getRangeAxis();
        range.setRange(-1.0, 0.0);
        range.setTickUnit(new org.jfree.chart.axis.NumberTickUnit(0.1));

        try {
            String fileName = strategyName.replaceAll(" ", "") + "_Drawdowns.png";
            ChartUtils.saveChartAsPNG(new File(fileName), chart, 800, 600);
            System.out.println(strategyName + " drawdowns chart saved as " + fileName);
        } catch (IOException e) {
            System.err.println("Error saving " + strategyName + " drawdowns chart: " + e.getMessage());
        }

        // Display the chart in a Swing window
        displayChart(chart, strategyName + " Drawdowns");
    }

    /**
     * Plots cumulative returns for two strategies on the same chart and saves it.
     *
     * @param strategy1Returns Map of date to return for strategy 1.
     * @param strategy2Returns Map of date to return for strategy 2.
     * @param outputPath       Path to save the PNG file.
     */
    public static void plotCombinedCumulativeReturns(Map<LocalDate, Double> strategy1Returns, Map<LocalDate, Double> strategy2Returns, String outputPath) {
        TimeSeries series1 = new TimeSeries("Fundamental Strategy Cumulative Returns");
        double cumulative1 = 1.0;
        for (Map.Entry<LocalDate, Double> entry : strategy1Returns.entrySet()) {
            cumulative1 *= (1 + entry.getValue());
            LocalDate date = entry.getKey();
            Day day = new Day(date.getDayOfMonth(), date.getMonthValue(), date.getYear());
            series1.addOrUpdate(day, cumulative1);
        }

        TimeSeries series2 = new TimeSeries("EMA Strategy Cumulative Returns");
        double cumulative2 = 1.0;
        for (Map.Entry<LocalDate, Double> entry : strategy2Returns.entrySet()) {
            cumulative2 *= (1 + entry.getValue());
            LocalDate date = entry.getKey();
            Day day = new Day(date.getDayOfMonth(), date.getMonthValue(), date.getYear());
            series2.addOrUpdate(day, cumulative2);
        }

        TimeSeriesCollection dataset = new TimeSeriesCollection();
        dataset.addSeries(series1);
        dataset.addSeries(series2);

        JFreeChart chart = ChartFactory.createTimeSeriesChart(
                "Cumulative Returns Comparison",
                "Date",
                "Cumulative Return",
                dataset,
                true,  // Legend
                true,  // Tooltips
                false  // URLs
        );

        try {
            ChartUtils.saveChartAsPNG(new File(outputPath), chart, 1000, 600);
            System.out.println("Combined cumulative returns chart saved to " + outputPath);
        } catch (IOException e) {
            System.err.println("Error saving combined cumulative returns chart: " + e.getMessage());
        }

        // Display the chart in a Swing window
        displayChart(chart, "Cumulative Returns Comparison");
    }

    /**
     * Plots drawdowns for two strategies on the same chart and saves it.
     *
     * @param strategy1Returns Map of date to return for strategy 1.
     * @param strategy2Returns Map of date to return for strategy 2.
     * @param outputPath       Path to save the PNG file.
     */
    public static void plotCombinedDrawdowns(Map<LocalDate, Double> strategy1Returns, Map<LocalDate, Double> strategy2Returns, String outputPath) {
        TimeSeries series1 = new TimeSeries("Fundamental Strategy Drawdown");
        double peak1 = Double.NEGATIVE_INFINITY;
        double cumulative1 = 1.0;

        for (Map.Entry<LocalDate, Double> entry : strategy1Returns.entrySet()) {
            LocalDate date = entry.getKey();
            double ret = entry.getValue();

            cumulative1 *= (1 + ret);
            if (cumulative1 > peak1) {
                peak1 = cumulative1;
            }
            double drawdown = (cumulative1 - peak1) / peak1;
            series1.addOrUpdate(new Day(date.getDayOfMonth(), date.getMonthValue(), date.getYear()), drawdown);
        }

        TimeSeries series2 = new TimeSeries("EMA Strategy Drawdown");
        double peak2 = Double.NEGATIVE_INFINITY;
        double cumulative2 = 1.0;

        for (Map.Entry<LocalDate, Double> entry : strategy2Returns.entrySet()) {
            LocalDate date = entry.getKey();
            double ret = entry.getValue();

            cumulative2 *= (1 + ret);
            if (cumulative2 > peak2) {
                peak2 = cumulative2;
            }
            double drawdown = (cumulative2 - peak2) / peak2;
            series2.addOrUpdate(new Day(date.getDayOfMonth(), date.getMonthValue(), date.getYear()), drawdown);
        }

        TimeSeriesCollection dataset = new TimeSeriesCollection();
        dataset.addSeries(series1);
        dataset.addSeries(series2);

        JFreeChart chart = ChartFactory.createTimeSeriesChart(
                "Drawdowns Comparison",
                "Date",
                "Drawdown",
                dataset,
                true,  // Legend
                true,  // Tooltips
                false  // URLs
        );

        // Adjust Y-axis to range from -1 to 0 for better visibility
        XYPlot plot = chart.getXYPlot();
        NumberAxis range = (NumberAxis) plot.getRangeAxis();
        range.setRange(-1.0, 0.0);
        range.setTickUnit(new org.jfree.chart.axis.NumberTickUnit(0.1));

        try {
            ChartUtils.saveChartAsPNG(new File(outputPath), chart, 1000, 600);
            System.out.println("Combined drawdowns chart saved to " + outputPath);
        } catch (IOException e) {
            System.err.println("Error saving combined drawdowns chart: " + e.getMessage());
        }

        // Display the chart in a Swing window
        displayChart(chart, "Drawdowns Comparison");
    }

    /**
     * Displays the given chart in a Swing window.
     *
     * @param chart JFreeChart object to display.
     * @param title Title of the window.
     */
    private static void displayChart(JFreeChart chart, String title) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame(title);
            frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            ChartPanel chartPanel = new ChartPanel(chart);
            frame.add(chartPanel);
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}

package com.longshanks;

import com.longshanks.backtest.Backtester;
import com.longshanks.backtest.PortfolioManager;
import com.longshanks.data.DataLoader;
import com.longshanks.evaluation.FactorRegression;
import com.longshanks.evaluation.SignalAccuracy;
import com.longshanks.model.StockData;
import com.longshanks.performance.PerformanceMetrics;
import com.longshanks.strategy.FundamentalStrategy;
import com.longshanks.strategy.EMAStrategy;
import com.longshanks.visualization.Plotter;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;



/**
 * Entry point for the Backtesting Application.
 */

public class Main {

    public static void main(String[] args) {
        /**
         * these are file paths. make sure to provide correct path of these files from your system.(if we try to run this project on different system the we need to update this path)
         */
        String stockCsvPath = "Longshanks Capital - Take Home Exam - Part 2\\daily_stock_data.csv";
        String marketCsvPath = "Longshanks Capital - Take Home Exam - Part 2\\market_returns.csv";

        // Initializes DataLoader
        DataLoader dataLoader = new DataLoader(stockCsvPath, marketCsvPath);

        try {

            dataLoader.loadAllData();
        } catch (IOException | com.opencsv.exceptions.CsvValidationException e) {
            System.err.println("Error loading data: " + e.getMessage());
            return;
        }

        // Retrieves data
        List<StockData> allStockData = dataLoader.getStockDataList();
        Map<java.time.LocalDate, Double> marketReturns = dataLoader.getMarketReturns();

        // Splits data into training (70%) and testing (30%)
        double trainRatio = 0.7;
        DataLoader.Pair<List<StockData>, List<StockData>> splitData = dataLoader.splitData(trainRatio);
        List<StockData> trainingData = splitData.first;
        List<StockData> testingData = splitData.second;

        // Initialize and run Fundamental Strategy
        System.out.println("=== Running Fundamental Strategy ===");
        double fundamentalPercentile = 10.0; // Top and bottom 10%
        FundamentalStrategy fundamentalStrategy = new FundamentalStrategy(fundamentalPercentile);
        fundamentalStrategy.prepareData(trainingData);
        fundamentalStrategy.generateSignals();

        // Initialize and run EMA Strategy
        System.out.println("=== Running EMA Strategy ===");
        int shortWindow = 5; // e.g., 5-day EMA
        int longWindow = 20;  // e.g., 20-day EMA
        EMAStrategy emaStrategy = new EMAStrategy(shortWindow, longWindow);
        emaStrategy.prepareData(trainingData);
        emaStrategy.generateSignals();

        // Initialize and run Backtester for Fundamental Strategy with rebalancing
        Backtester fundamentalBacktester = new Backtester(
                fundamentalStrategy.getLongs(),
                fundamentalStrategy.getShorts(),
                fundamentalStrategy.getPriceMap(),
                allStockData
        );
        fundamentalBacktester.runBacktest();
        Map<java.time.LocalDate, Double> fundamentalStrategyReturns = fundamentalBacktester.getStrategyReturns();

        // Initialize and run Backtester for EMA Strategy with rebalancing
        Backtester emaBacktester = new Backtester(
                emaStrategy.getLongs(),
                emaStrategy.getShorts(),
                emaStrategy.getPriceMap(),
                allStockData
        );
        emaBacktester.runBacktest();
        Map<java.time.LocalDate, Double> emaStrategyReturns = emaBacktester.getStrategyReturns();

        // Performs Factor Regression for Fundamental Strategy
        System.out.println("\n=== Fundamental Strategy Factor Regression ===");
        FactorRegression fundamentalRegression = new FactorRegression(fundamentalStrategyReturns, marketReturns);
        fundamentalRegression.runRegression();
        double fundamentalBeta = fundamentalRegression.getBeta();

        // Performs Factor Regression for EMA Strategy
        System.out.println("\n=== EMA Strategy Factor Regression ===");
        FactorRegression emaRegression = new FactorRegression(emaStrategyReturns, marketReturns);
        emaRegression.runRegression();
        double emaBeta = emaRegression.getBeta();

        // Calculates Performance Metrics for Fundamental Strategy
        PerformanceMetrics fundamentalMetrics = new PerformanceMetrics(fundamentalStrategyReturns);
        fundamentalMetrics.setBeta(fundamentalBeta);
        System.out.println("\n=== Fundamental Strategy Performance Metrics ===");
        System.out.printf("Average Annual Return: %.2f%%\n", fundamentalMetrics.getAverageAnnualReturnPercent());
        System.out.printf("Annualized Volatility: %.2f%%\n", fundamentalMetrics.getVolatilityPercent());
        System.out.printf("Sharpe Ratio: %.4f\n", fundamentalMetrics.getSharpeRatioPercent());
        System.out.printf("Sortino Ratio: %.4f\n", fundamentalMetrics.getSortinoRatioPercent());
        System.out.printf("Calmar Ratio: %.4f\n", fundamentalMetrics.getCalmarRatioPercent());
        System.out.printf("Maximum Drawdown: %.2f%%\n", fundamentalMetrics.getMaxDrawdown() * 100);
        System.out.printf("Treynor Ratio: %.4f\n", fundamentalMetrics.getTreynorRatioPercent());

        // Calculates Annual Returns for Fundamental Strategy
        Map<Integer, Double> fundamentalAnnualReturns = fundamentalMetrics.getYearlyReturns();
        System.out.println("\n=== Fundamental Strategy Annual Returns ===");
        for (Map.Entry<Integer, Double> entry : fundamentalAnnualReturns.entrySet()) {
            System.out.printf("Year %d: %.2f%%\n", entry.getKey(), entry.getValue());
        }

        // Calculates Performance Metrics for EMA Strategy
        PerformanceMetrics emaMetrics = new PerformanceMetrics(emaStrategyReturns);
        emaMetrics.setBeta(emaBeta);
        System.out.println("\n=== EMA Strategy Performance Metrics ===");
        System.out.printf("Average Annual Return: %.2f%%\n", emaMetrics.getAverageAnnualReturnPercent());
        System.out.printf("Annualized Volatility: %.2f%%\n", emaMetrics.getVolatilityPercent());
        System.out.printf("Sharpe Ratio: %.4f\n", emaMetrics.getSharpeRatioPercent());
        System.out.printf("Sortino Ratio: %.4f\n", emaMetrics.getSortinoRatioPercent());
        System.out.printf("Calmar Ratio: %.4f\n", emaMetrics.getCalmarRatioPercent());
        System.out.printf("Maximum Drawdown: %.2f%%\n", emaMetrics.getMaxDrawdown() * 100);
        System.out.printf("Treynor Ratio: %.4f\n", emaMetrics.getTreynorRatioPercent());

        // Calculate Annual Returns for EMA Strategy
        Map<Integer, Double> emaAnnualReturns = emaMetrics.getYearlyReturns();
        System.out.println("\n=== EMA Strategy Annual Returns ===");
        for (Map.Entry<Integer, Double> entry : emaAnnualReturns.entrySet()) {
            System.out.printf("Year %d: %.2f%%\n", entry.getKey(), entry.getValue());
        }

        // Evaluate Signal Accuracy for Fundamental Strategy
        SignalAccuracy fundamentalAccuracy = new SignalAccuracy(fundamentalStrategy.getLongs(), fundamentalStrategy.getShorts(), fundamentalStrategy.getPriceMap());
        double fundamentalAccuracyPct = fundamentalAccuracy.calculateAccuracy() * 100;
        System.out.printf("\n=== Fundamental Strategy Signal Accuracy ===\nAccuracy: %.2f%%\n", fundamentalAccuracyPct);

        // Analyze Yearly Signal Accuracy for Fundamental Strategy
        Map<Integer, Double> fundamentalYearlyAccuracy = fundamentalAccuracy.analyzeYearlyAccuracy();
        System.out.println("\n=== Fundamental Strategy Yearly Signal Accuracy ===");
        for (Map.Entry<Integer, Double> entry : fundamentalYearlyAccuracy.entrySet()) {
            System.out.printf("Year %d: %.2f%%\n", entry.getKey(), entry.getValue() * 100);
        }

        // Evaluate Signal Accuracy for EMA Strategy
        SignalAccuracy emaAccuracy = new SignalAccuracy(emaStrategy.getLongs(), emaStrategy.getShorts(), emaStrategy.getPriceMap());
        double emaAccuracyPct = emaAccuracy.calculateAccuracy() * 100;
        System.out.printf("\n=== EMA Strategy Signal Accuracy ===\nAccuracy: %.2f%%\n", emaAccuracyPct);

        // Analyze Yearly Signal Accuracy for EMA Strategy
        Map<Integer, Double> emaYearlyAccuracy = emaAccuracy.analyzeYearlyAccuracy();
        System.out.println("\n=== EMA Strategy Yearly Signal Accuracy ===");
        for (Map.Entry<Integer, Double> entry : emaYearlyAccuracy.entrySet()) {
            System.out.printf("Year %d: %.2f%%\n", entry.getKey(), entry.getValue() * 100);
        }

        // Generates Separate Visualizations for Fundamental Strategy
        System.out.println("\n=== Generating Fundamental Strategy Visualizations ===");
        Plotter.plotCumulativeReturns(fundamentalStrategyReturns, "Fundamental Strategy");
        Plotter.plotDrawdowns(fundamentalStrategyReturns, "Fundamental Strategy");

        // Generates Separate Visualizations for EMA Strategy
        System.out.println("\n=== Generating EMA Strategy Visualizations ===");
        Plotter.plotCumulativeReturns(emaStrategyReturns, "EMA Strategy");
        Plotter.plotDrawdowns(emaStrategyReturns, "EMA Strategy");

        // Generates Combined Visualizations
        System.out.println("\n=== Generating Combined Visualizations ===");
        String combinedCumulativeChartPath = "CumulativeReturns_Comparison.png";
        String combinedDrawdownChartPath = "Drawdowns_Comparison.png";
        Plotter.plotCombinedCumulativeReturns(fundamentalStrategyReturns, emaStrategyReturns, combinedCumulativeChartPath);
        Plotter.plotCombinedDrawdowns(fundamentalStrategyReturns, emaStrategyReturns, combinedDrawdownChartPath);

        // Generates Swing Reports
        generateSwingReports(fundamentalBacktester.getPortfolioManager(), emaBacktester.getPortfolioManager());

        // Displays Performance Metrics in Swing Window
        displayMetricsInSwing(fundamentalMetrics, emaMetrics, fundamentalAccuracyPct, emaAccuracyPct);
    }

    /**
     * Generates Swing reports for portfolio rebalancing.
     *
     * @param fundamentalPortfolio Fundamental Strategy's PortfolioManager.
     * @param emaPortfolio         EMA Strategy's PortfolioManager.
     */
    private static void generateSwingReports(PortfolioManager fundamentalPortfolio, PortfolioManager emaPortfolio) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Portfolio Rebalancing Report");
            frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            frame.setSize(1200, 600);
            frame.setLayout(new GridLayout(1, 2));

            // Fundamental Strategy Report
            JPanel fundamentalPanel = new JPanel();
            fundamentalPanel.setLayout(new BorderLayout());

            JLabel fundamentalTitle = new JLabel("=== Fundamental Strategy Sector Allocation ===", SwingConstants.CENTER);
            fundamentalTitle.setFont(fundamentalTitle.getFont().deriveFont(16.0f));
            fundamentalPanel.add(fundamentalTitle, BorderLayout.NORTH);

            JTextArea fundamentalTextArea = new JTextArea();
            fundamentalTextArea.setEditable(false);
            StringBuilder fundamentalReport = new StringBuilder();

            for (Map.Entry<YearMonth, Map<String, Double>> entry : fundamentalPortfolio.getSectorAllocation().entrySet()) {
                YearMonth month = entry.getKey();
                Map<String, Double> sectorPercents = entry.getValue();
                fundamentalReport.append(String.format("Month: %s\n", month.toString()));
                if (sectorPercents.isEmpty()) {
                    fundamentalReport.append("  No Holdings\n\n");
                    continue;
                }
                for (Map.Entry<String, Double> sectorEntry : sectorPercents.entrySet()) {
                    fundamentalReport.append(String.format("  Sector: %s - %.2f%%\n", sectorEntry.getKey(), sectorEntry.getValue()));
                }
                fundamentalReport.append("\n");
            }

            fundamentalTextArea.setText(fundamentalReport.toString());
            JScrollPane fundamentalScrollPane = new JScrollPane(fundamentalTextArea);
            fundamentalPanel.add(fundamentalScrollPane, BorderLayout.CENTER);

            frame.add(fundamentalPanel);

            // EMA Strategy Report
            JPanel emaPanel = new JPanel();
            emaPanel.setLayout(new BorderLayout());

            JLabel emaTitle = new JLabel("=== EMA Strategy Sector Allocation ===", SwingConstants.CENTER);
            emaTitle.setFont(emaTitle.getFont().deriveFont(16.0f));
            emaPanel.add(emaTitle, BorderLayout.NORTH);

            JTextArea emaTextArea = new JTextArea();
            emaTextArea.setEditable(false);
            StringBuilder emaReport = new StringBuilder();

            for (Map.Entry<YearMonth, Map<String, Double>> entry : emaPortfolio.getSectorAllocation().entrySet()) {
                YearMonth month = entry.getKey();
                Map<String, Double> sectorPercents = entry.getValue();
                emaReport.append(String.format("Month: %s\n", month.toString()));
                if (sectorPercents.isEmpty()) {
                    emaReport.append("  No Holdings\n\n");
                    continue;
                }
                for (Map.Entry<String, Double> sectorEntry : sectorPercents.entrySet()) {
                    emaReport.append(String.format("  Sector: %s - %.2f%%\n", sectorEntry.getKey(), sectorEntry.getValue()));
                }
                emaReport.append("\n");
            }

            emaTextArea.setText(emaReport.toString());
            JScrollPane emaScrollPane = new JScrollPane(emaTextArea);
            emaPanel.add(emaScrollPane, BorderLayout.CENTER);

            frame.add(emaPanel);

            frame.setVisible(true);
        });
    }

    /**
     * Displays key metrics in a Swing window using tabs for each strategy.
     *
     * @param fundamentalMetrics    PerformanceMetrics for Fundamental Strategy.
     * @param emaMetrics            PerformanceMetrics for EMA Strategy.
     * @param fundamentalAccuracyPct Signal accuracy percentage for Fundamental Strategy.
     * @param emaAccuracyPct         Signal accuracy percentage for EMA Strategy.
     */
    private static void displayMetricsInSwing(PerformanceMetrics fundamentalMetrics, PerformanceMetrics emaMetrics, double fundamentalAccuracyPct, double emaAccuracyPct) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Strategy Performance Metrics");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(800, 600);
            frame.setLayout(new BorderLayout());

            JTabbedPane tabbedPane = new JTabbedPane();

            // Fundamental Strategy Metrics
            JPanel fundamentalPanel = new JPanel();
            fundamentalPanel.setLayout(new BoxLayout(fundamentalPanel, BoxLayout.Y_AXIS));
            fundamentalPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

            JLabel fundamentalTitle = new JLabel("=== Fundamental Strategy Performance Metrics ===");
            fundamentalTitle.setFont(fundamentalTitle.getFont().deriveFont(16.0f));
            fundamentalTitle.setAlignmentX(JLabel.CENTER_ALIGNMENT);
            fundamentalPanel.add(fundamentalTitle);
            fundamentalPanel.add(Box.createRigidArea(new java.awt.Dimension(0, 10)));

            JLabel fAvgReturn = new JLabel(String.format("Average Annual Return: %.2f%%", fundamentalMetrics.getAverageAnnualReturnPercent()));
            JLabel fVolatility = new JLabel(String.format("Annualized Volatility: %.2f%%", fundamentalMetrics.getVolatilityPercent()));
            JLabel fSharpe = new JLabel(String.format("Sharpe Ratio: %.4f", fundamentalMetrics.getSharpeRatioPercent()));
            JLabel fSortino = new JLabel(String.format("Sortino Ratio: %.4f", fundamentalMetrics.getSortinoRatioPercent()));
            JLabel fCalmar = new JLabel(String.format("Calmar Ratio: %.4f", fundamentalMetrics.getCalmarRatioPercent()));
            JLabel fMaxDrawdown = new JLabel(String.format("Maximum Drawdown: %.2f%%", fundamentalMetrics.getMaxDrawdown() * 100));
            JLabel fTreynor = new JLabel(String.format("Treynor Ratio: %.4f", fundamentalMetrics.getTreynorRatioPercent()));
            JLabel fAccuracy = new JLabel(String.format("Signal Accuracy: %.2f%%", fundamentalAccuracyPct));

            fundamentalPanel.add(fAvgReturn);
            fundamentalPanel.add(fVolatility);
            fundamentalPanel.add(fSharpe);
            fundamentalPanel.add(fSortino);
            fundamentalPanel.add(fCalmar);
            fundamentalPanel.add(fMaxDrawdown);
            fundamentalPanel.add(fTreynor);
            fundamentalPanel.add(fAccuracy);

            tabbedPane.addTab("Fundamental Strategy", fundamentalPanel);

            // EMA Strategy Metrics
            JPanel emaPanel = new JPanel();
            emaPanel.setLayout(new BoxLayout(emaPanel, BoxLayout.Y_AXIS));
            emaPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

            JLabel emaTitle = new JLabel("=== EMA Strategy Performance Metrics ===");
            emaTitle.setFont(emaTitle.getFont().deriveFont(16.0f));
            emaTitle.setAlignmentX(JLabel.CENTER_ALIGNMENT);
            emaPanel.add(emaTitle);
            emaPanel.add(Box.createRigidArea(new java.awt.Dimension(0, 10)));

            JLabel eAvgReturn = new JLabel(String.format("Average Annual Return: %.2f%%", emaMetrics.getAverageAnnualReturnPercent()));
            JLabel eVolatility = new JLabel(String.format("Annualized Volatility: %.2f%%", emaMetrics.getVolatilityPercent()));
            JLabel eSharpe = new JLabel(String.format("Sharpe Ratio: %.4f", emaMetrics.getSharpeRatioPercent()));
            JLabel eSortino = new JLabel(String.format("Sortino Ratio: %.4f", emaMetrics.getSortinoRatioPercent()));
            JLabel eCalmar = new JLabel(String.format("Calmar Ratio: %.4f", emaMetrics.getCalmarRatioPercent()));
            JLabel eMaxDrawdown = new JLabel(String.format("Maximum Drawdown: %.2f%%", emaMetrics.getMaxDrawdown() * 100));
            JLabel eTreynor = new JLabel(String.format("Treynor Ratio: %.4f", emaMetrics.getTreynorRatioPercent()));
            JLabel eAccuracy = new JLabel(String.format("Signal Accuracy: %.2f%%", emaAccuracyPct));

            emaPanel.add(eAvgReturn);
            emaPanel.add(eVolatility);
            emaPanel.add(eSharpe);
            emaPanel.add(eSortino);
            emaPanel.add(eCalmar);
            emaPanel.add(eMaxDrawdown);
            emaPanel.add(eTreynor);
            emaPanel.add(eAccuracy);

            tabbedPane.addTab("EMA Strategy", emaPanel);

            frame.add(tabbedPane, BorderLayout.CENTER);

            frame.setVisible(true);
        });
    }
}

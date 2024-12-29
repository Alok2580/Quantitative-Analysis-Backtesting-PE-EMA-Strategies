package com.longshanks.backtest;

import com.longshanks.model.StockData;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;

/**
 * Simulates the trading strategy based on long and short signals.
 * Incorporates position sizing, risk management, and portfolio rebalancing.
 */
public class Backtester {
    private Map<LocalDate, List<String>> longs;
    private Map<LocalDate, List<String>> shorts;
    private Map<String, TreeMap<LocalDate, Double>> priceMap;
    private Set<LocalDate> allDates;
    private Map<LocalDate, Double> strategyReturns; // Date -> Return
    private PortfolioManager portfolioManager;

    // Risk management parameters
    private double positionSize = 0.02; // 2% per position
    private double stopLoss = -0.05;    // 5% stop-loss
    private double takeProfit = 0.10;   // 10% take-profit
    private double transactionCost = 0.001; // 0.1% per trade

    /**
     * Constructor to initialize Backtester.
     *
     * @param longs        Map of dates to symbols to long.
     * @param shorts       Map of dates to symbols to short.
     * @param priceMap     Map of symbols to their price history.
     * @param allStockData List of all stock data for sector mapping.
     */
    public Backtester(Map<LocalDate, List<String>> longs,
                      Map<LocalDate, List<String>> shorts,
                      Map<String, TreeMap<LocalDate, Double>> priceMap,
                      List<StockData> allStockData) {
        this.longs = longs;
        this.shorts = shorts;
        this.priceMap = priceMap;
        this.allDates = new TreeSet<>();
        this.strategyReturns = new TreeMap<>();
        collectAllDates();

        // Initialize PortfolioManager with initial capital, e.g., $1,000,000
        double initialCapital = 1_000_000.0;
        this.portfolioManager = new PortfolioManager(initialCapital, allStockData);
    }

    /**
     * Collects all unique dates from price data.
     */
    private void collectAllDates() {
        for (TreeMap<LocalDate, Double> symbolPrices : priceMap.values()) {
            allDates.addAll(symbolPrices.keySet());
        }
    }

    /**
     * Runs the backtest simulation.
     */
    public void runBacktest() {
        List<LocalDate> sortedDates = new ArrayList<>(allDates);
        Collections.sort(sortedDates);

        // Initialize variables for tracking
        YearMonth currentMonth = null;

        for (LocalDate date : sortedDates) {
            // Check if it's time to rebalance
            YearMonth ym = YearMonth.from(date);
            if (!ym.equals(currentMonth)) {
                currentMonth = ym;
                List<String> todayLongs = longs.getOrDefault(date, Collections.emptyList());
                List<String> todayShorts = shorts.getOrDefault(date, Collections.emptyList());

                // Rebalance portfolio
                portfolioManager.rebalance(todayLongs, todayShorts, getPricesOnDate(date), transactionCost);

                // Record sector allocation
                portfolioManager.recordSectorAllocation(currentMonth);

                // Log rebalancing action
                System.out.println("Rebalanced on " + date + ". Current Portfolio Value: $" + String.format("%.2f", portfolioManager.getPortfolioValue()));
            }

            // Calculate daily portfolio return based on holdings
            double dailyReturn = calculateDailyReturn(date);

            // Update portfolio value
            double previousValue = portfolioManager.getPortfolioValue();
            double newValue = previousValue * (1 + dailyReturn);
            double actualReturn = (newValue - previousValue) / previousValue;
            strategyReturns.put(date, actualReturn);

            // Update portfolio value in PortfolioManager
            portfolioManager.updatePortfolioValue(newValue);

            // Log daily return
            System.out.println("Date: " + date + " | Daily Return: " + String.format("%.4f%%", actualReturn * 100) + " | Portfolio Value: $" + String.format("%.2f", newValue));
        }

        // Close all open positions at the end of backtest
        if (!sortedDates.isEmpty()) {
            closeAllPositions(sortedDates.get(sortedDates.size() - 1));
        }
    }

    /**
     * Retrieves the closing prices for all symbols on a specific date.
     *
     * @param date Date for which to retrieve prices.
     * @return Map of symbol to closing price.
     */
    private Map<String, Double> getPricesOnDate(LocalDate date) {
        Map<String, Double> prices = new HashMap<>();
        for (String symbol : priceMap.keySet()) {
            Double price = priceMap.get(symbol).get(date);
            if (price != null) {
                prices.put(symbol, price);
            }
        }
        return prices;
    }

    /**
     * Calculates the daily return of the portfolio.
     *
     * @param date Current date.
     * @return Daily return as a decimal.
     */
    private double calculateDailyReturn(LocalDate date) {
        double dailyReturn = 0.0;
        double portfolioValue = portfolioManager.getPortfolioValue();
        if (portfolioValue == 0) return 0.0;

        // Calculate return from long holdings
        for (Map.Entry<String, Integer> entry : portfolioManager.getLongHoldings().entrySet()) {
            String symbol = entry.getKey();
            int shares = entry.getValue();
            Double price = priceMap.get(symbol).get(date);
            if (price != null) {
                // Get previous day's price
                TreeMap<LocalDate, Double> symbolPrices = priceMap.get(symbol);
                Map.Entry<LocalDate, Double> prevEntry = symbolPrices.lowerEntry(date);
                if (prevEntry != null) {
                    double prevPrice = prevEntry.getValue();
                    double ret = (price - prevPrice) / prevPrice;
                    dailyReturn += ret * (shares * prevPrice) / portfolioValue;
                }
            }
        }

        // Calculate return from short holdings
        for (Map.Entry<String, Integer> entry : portfolioManager.getShortHoldings().entrySet()) {
            String symbol = entry.getKey();
            int shares = entry.getValue();
            Double price = priceMap.get(symbol).get(date);
            if (price != null) {
                // Get previous day's price
                TreeMap<LocalDate, Double> symbolPrices = priceMap.get(symbol);
                Map.Entry<LocalDate, Double> prevEntry = symbolPrices.lowerEntry(date);
                if (prevEntry != null) {
                    double prevPrice = prevEntry.getValue();
                    double ret = (prevPrice - price) / prevPrice; // Profit from short
                    dailyReturn += ret * (shares * prevPrice) / portfolioValue;
                }
            }
        }

        return dailyReturn;
    }

    /**
     * Closes all open positions at the end of the backtest.
     *
     * @param date The last date in the backtest period.
     */
    private void closeAllPositions(LocalDate date) {
        // Close all long positions
        Map<String, Integer> longHoldings = new HashMap<>(portfolioManager.getLongHoldings());
        for (Map.Entry<String, Integer> entry : longHoldings.entrySet()) {
            String symbol = entry.getKey();
            int shares = entry.getValue();
            Double price = priceMap.get(symbol).get(date);
            if (price != null) {
                portfolioManager.sell(symbol, shares, price, transactionCost);
                // Record the transaction as a return (e.g., zero return adjusted for transaction cost)
                strategyReturns.put(date, -transactionCost);
                System.out.println("Closed long position for " + symbol + " on " + date + " | Transaction Cost Applied: " + String.format("%.4f%%", transactionCost * 100));
            }
        }

        // Cover all short positions
        Map<String, Integer> shortHoldings = new HashMap<>(portfolioManager.getShortHoldings());
        for (Map.Entry<String, Integer> entry : shortHoldings.entrySet()) {
            String symbol = entry.getKey();
            int shares = entry.getValue();
            Double price = priceMap.get(symbol).get(date);
            if (price != null) {
                portfolioManager.coverShort(symbol, shares, price, transactionCost);
                // Record the transaction as a return (e.g., zero return adjusted for transaction cost)
                strategyReturns.put(date, -transactionCost);
                System.out.println("Closed short position for " + symbol + " on " + date + " | Transaction Cost Applied: " + String.format("%.4f%%", transactionCost * 100));
            }
        }
    }

    public Map<LocalDate, Double> getStrategyReturns() {
        return strategyReturns;
    }

    public PortfolioManager getPortfolioManager() {
        return portfolioManager;
    }
}

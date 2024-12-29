package com.longshanks.backtest;

import com.longshanks.model.StockData;

import java.time.YearMonth;
import java.util.*;

/**
 * Manages the portfolio, including long and short holdings and rebalancing.
 */
public class PortfolioManager {
    private double portfolioValue;
    private Map<String, Integer> longHoldings;  // Symbol -> Number of Shares
    private Map<String, Integer> shortHoldings; // Symbol -> Number of Shares
    private Map<YearMonth, Map<String, Double>> sectorAllocation; // YearMonth -> Sector -> Percentage
    private Map<String, String> symbolSectorMap; // Symbol -> Sector

    // Risk management parameters
    private double positionSize = 0.02; // 2% per position
    // for now, I haven't used this
    private double stopLoss = -0.05;    // 5% stop-loss
    private double takeProfit = 0.10;   // 10% take-profit
    private double transactionCost = 0.001; // 0.1% per trade

    /**
     * Constructor to initialize PortfolioManager.
     *
     * @param initialCapital Initial capital for the portfolio.
     * @param allStockData   List of all StockData entries for sector mapping.
     */
    public PortfolioManager(double initialCapital, List<StockData> allStockData) {
        this.portfolioValue = initialCapital;
        this.longHoldings = new HashMap<>();
        this.shortHoldings = new HashMap<>();
        this.sectorAllocation = new TreeMap<>();
        this.symbolSectorMap = new HashMap<>();

        // Initialize symbol to sector mapping
        for (StockData data : allStockData) {
            symbolSectorMap.put(data.getSymbol(), data.getSector());
        }
    }

    /**
     * Buys a specified number of shares for a symbol (Long Position).
     *
     * @param symbol          Stock symbol.
     * @param shares          Number of shares to buy.
     * @param price           Price per share.
     * @param transactionCost Transaction cost per trade (as a percentage).
     */
    public void buy(String symbol, int shares, double price, double transactionCost) {
        double cost = shares * price;
        double totalCost = cost * (1 + transactionCost);
        if (totalCost > portfolioValue) {
            System.err.println("Insufficient funds to buy " + shares + " shares of " + symbol);
            return;
        }
        portfolioValue -= totalCost;
        longHoldings.put(symbol, longHoldings.getOrDefault(symbol, 0) + shares);
        System.out.println("Bought " + shares + " shares of " + symbol + " at $" + price + " per share. Total Cost (incl. fees): $" + String.format("%.2f", totalCost));
    }

    /**
     * Sells a specified number of shares for a symbol (Closing Long Position).
     *
     * @param symbol          Stock symbol.
     * @param shares          Number of shares to sell.
     * @param price           Price per share.
     * @param transactionCost Transaction cost per trade (as a percentage).
     */
    public void sell(String symbol, int shares, double price, double transactionCost) {
        int currentShares = longHoldings.getOrDefault(symbol, 0);
        if (shares > currentShares) {
            System.err.println("Insufficient shares to sell " + shares + " shares of " + symbol);
            return;
        }
        double proceeds = shares * price;
        double totalProceeds = proceeds * (1 - transactionCost);
        portfolioValue += totalProceeds;
        if (shares == currentShares) {
            longHoldings.remove(symbol);
        } else {
            longHoldings.put(symbol, currentShares - shares);
        }
        System.out.println("Sold " + shares + " shares of " + symbol + " at $" + price + " per share. Total Proceeds (after fees): $" + String.format("%.2f", totalProceeds));
    }

    /**
     * Short sells a specified number of shares for a symbol (Short Position).
     *
     * @param symbol          Stock symbol.
     * @param shares          Number of shares to short.
     * @param price           Price per share.
     * @param transactionCost Transaction cost per trade (as a percentage).
     */
    public void shortSell(String symbol, int shares, double price, double transactionCost) {
        double proceeds = shares * price;
        double totalProceeds = proceeds * (1 - transactionCost);
        portfolioValue += totalProceeds;
        shortHoldings.put(symbol, shortHoldings.getOrDefault(symbol, 0) + shares);
        System.out.println("Shorted " + shares + " shares of " + symbol + " at $" + price + " per share. Total Proceeds (after fees): $" + String.format("%.2f", totalProceeds));
    }

    /**
     * Covers a specified number of shares for a symbol (Closing Short Position).
     *
     * @param symbol          Stock symbol.
     * @param shares          Number of shares to cover.
     * @param price           Price per share.
     * @param transactionCost Transaction cost per trade (as a percentage).
     */
    public void coverShort(String symbol, int shares, double price, double transactionCost) {
        int currentShares = shortHoldings.getOrDefault(symbol, 0);
        if (shares > currentShares) {
            System.err.println("Insufficient short shares to cover " + shares + " shares of " + symbol);
            return;
        }
        double cost = shares * price;
        double totalCost = cost * (1 + transactionCost);
        if (totalCost > portfolioValue) {
            System.err.println("Insufficient funds to cover short of " + shares + " shares of " + symbol);
            return;
        }
        portfolioValue -= totalCost;
        if (shares == currentShares) {
            shortHoldings.remove(symbol);
        } else {
            shortHoldings.put(symbol, currentShares - shares);
        }
        System.out.println("Covered short position of " + shares + " shares of " + symbol + " at $" + price + " per share. Total Cost (incl. fees): $" + String.format("%.2f", totalCost));
    }

    /**
     * Rebalances the portfolio based on new long and short signals.
     *
     * @param newLongs         List of symbols to long.
     * @param newShorts        List of symbols to short.
     * @param currentPrices    Current prices for symbols.
     * @param transactionCost  Transaction cost per trade (as a percentage).
     */
    public void rebalance(List<String> newLongs, List<String> newShorts, Map<String, Double> currentPrices, double transactionCost) {
        // Close existing long positions not in newLongs
        for (String symbol : new ArrayList<>(longHoldings.keySet())) {
            if (!newLongs.contains(symbol)) {
                int shares = longHoldings.get(symbol);
                double price = currentPrices.getOrDefault(symbol, 0.0);
                if (price > 0) {
                    sell(symbol, shares, price, transactionCost);
                }
            }
        }

        // Close existing short positions not in newShorts
        for (String symbol : new ArrayList<>(shortHoldings.keySet())) {
            if (!newShorts.contains(symbol)) {
                int shares = shortHoldings.get(symbol);
                double price = currentPrices.getOrDefault(symbol, 0.0);
                if (price > 0) {
                    coverShort(symbol, shares, price, transactionCost);
                }
            }
        }

        // Open new long positions
        if (!newLongs.isEmpty()) {
            double allocationPerLong = portfolioValue * positionSize; // 2% per position
            for (String symbol : newLongs) {
                double price = currentPrices.getOrDefault(symbol, 0.0);
                if (price > 0) {
                    int sharesToBuy = (int) (allocationPerLong / price);
                    if (sharesToBuy > 0) {
                        buy(symbol, sharesToBuy, price, transactionCost);
                    }
                }
            }
        }

        // Open new short positions
        if (!newShorts.isEmpty()) {
            double allocationPerShort = portfolioValue * positionSize; // 2% per position
            for (String symbol : newShorts) {
                double price = currentPrices.getOrDefault(symbol, 0.0);
                if (price > 0) {
                    int sharesToShort = (int) (allocationPerShort / price);
                    if (sharesToShort > 0) {
                        shortSell(symbol, sharesToShort, price, transactionCost);
                    }
                }
            }
        }
    }

    /**
     * Records sector allocation for the given month, accounting for both longs and shorts.
     *
     * @param month Current YearMonth.
     */
    public void recordSectorAllocation(YearMonth month) {
        Map<String, Double> sectorCount = new HashMap<>();

        // Count long positions
        for (String symbol : longHoldings.keySet()) {
            String sector = symbolSectorMap.get(symbol);
            sectorCount.put(sector, sectorCount.getOrDefault(sector, 0.0) + 1.0);
        }

        // Count short positions
        for (String symbol : shortHoldings.keySet()) {
            String sector = symbolSectorMap.get(symbol);
            sectorCount.put(sector, sectorCount.getOrDefault(sector, 0.0) + 1.0);
        }

        if (sectorCount.isEmpty()) {
            sectorAllocation.put(month, new HashMap<>());
            return;
        }

        Map<String, Double> sectorPercentage = new HashMap<>();
        int total = longHoldings.size() + shortHoldings.size();
        for (Map.Entry<String, Double> entry : sectorCount.entrySet()) {
            double percentage = (entry.getValue() * 100.0) / total;
            sectorPercentage.put(entry.getKey(), percentage);
        }

        sectorAllocation.put(month, sectorPercentage);
    }

    /**
     * Updates the portfolio value.
     *
     * @param newValue The new portfolio value.
     */
    public void updatePortfolioValue(double newValue) {
        this.portfolioValue = newValue;
    }

    public double getPortfolioValue() {
        return portfolioValue;
    }

    public Map<String, Integer> getLongHoldings() {
        return longHoldings;
    }

    public Map<String, Integer> getShortHoldings() {
        return shortHoldings;
    }

    public Map<YearMonth, Map<String, Double>> getSectorAllocation() {
        return sectorAllocation;
    }
}

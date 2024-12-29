package com.longshanks.strategy;

import com.longshanks.model.StockData;

import java.time.LocalDate;
import java.util.*;

/**
 * Implements an EMA Crossover Strategy.
 * Buys when short-term EMA crosses above long-term EMA.
 * Sells when short-term EMA crosses below long-term EMA.
 */
public class EMAStrategy {
    private int shortWindow;
    private int longWindow;
    private Map<String, TreeMap<LocalDate, Double>> priceMap; // Symbol -> Date -> Close Price
    private Map<LocalDate, List<String>> longs; // Date -> Symbols to Long
    private Map<LocalDate, List<String>> shorts; // Date -> Symbols to Short

    // Internal storage for EMAs
    private Map<String, TreeMap<LocalDate, Double>> shortEMA;
    private Map<String, TreeMap<LocalDate, Double>> longEMA;

    /**
     * Constructor to initialize EMA Strategy with specified window sizes.
     *
     * @param shortWindow The short-term EMA window (e.g., 5 days).
     * @param longWindow  The long-term EMA window (e.g., 20 days).
     */
    public EMAStrategy(int shortWindow, int longWindow) {
        if (shortWindow >= longWindow) {
            throw new IllegalArgumentException("Short window must be less than long window.");
        }
        this.shortWindow = shortWindow;
        this.longWindow = longWindow;
        this.priceMap = new HashMap<>();
        this.longs = new TreeMap<>();
        this.shorts = new TreeMap<>();
        this.shortEMA = new HashMap<>();
        this.longEMA = new HashMap<>();
    }

    /**
     * Prepares data by organizing prices by symbol and date.
     *
     * @param dataList List of StockData entries.
     */
    public void prepareData(List<StockData> dataList) {
        for (StockData data : dataList) {
            priceMap.putIfAbsent(data.getSymbol(), new TreeMap<>());
            priceMap.get(data.getSymbol()).put(data.getDate(), data.getClosePrice());
        }
    }

    /**
     * Generates buy/sell signals based on EMA crossovers.
     */
    public void generateSignals() {
        for (String symbol : priceMap.keySet()) {
            TreeMap<LocalDate, Double> symbolPrices = priceMap.get(symbol);
            if (symbolPrices.size() < longWindow) continue; // Not enough data

            TreeMap<LocalDate, Double> symbolShortEMA = calculateEMA(symbolPrices, shortWindow);
            TreeMap<LocalDate, Double> symbolLongEMA = calculateEMA(symbolPrices, longWindow);

            shortEMA.put(symbol, symbolShortEMA);
            longEMA.put(symbol, symbolLongEMA);

            // Iterate over dates to generate signals
            LocalDate previousDate = null;
            int buySignals = 0;
            int sellSignals = 0;
            for (LocalDate date : symbolPrices.keySet()) {
                if (!symbolShortEMA.containsKey(date) || !symbolLongEMA.containsKey(date)) continue;

                if (previousDate == null || !symbolShortEMA.containsKey(previousDate) || !symbolLongEMA.containsKey(previousDate)) {
                    previousDate = date;
                    continue;
                }

                double prevShort = symbolShortEMA.get(previousDate);
                double prevLong = symbolLongEMA.get(previousDate);
                double currentShort = symbolShortEMA.get(date);
                double currentLong = symbolLongEMA.get(date);

                // Buy signal: short EMA crosses above long EMA
                if (prevShort <= prevLong && currentShort > currentLong) {
                    longs.putIfAbsent(date, new ArrayList<>());
                    longs.get(date).add(symbol);
                    buySignals++;
                }

                // Sell signal: short EMA crosses below long EMA
                if (prevShort >= prevLong && currentShort < currentLong) {
                    shorts.putIfAbsent(date, new ArrayList<>());
                    shorts.get(date).add(symbol);
                    sellSignals++;
                }

                previousDate = date;
            }
            System.out.println("Symbol: " + symbol + " - Buy Signals: " + buySignals + ", Sell Signals: " + sellSignals);
        }
    }

    /**
     * Calculates the EMA for a given symbol.
     *
     * @param symbolPrices TreeMap of dates to close prices.
     * @param window       The window size for EMA.
     * @return TreeMap of dates to EMA values.
     */
    private TreeMap<LocalDate, Double> calculateEMA(TreeMap<LocalDate, Double> symbolPrices, int window) {
        TreeMap<LocalDate, Double> emaMap = new TreeMap<>();
        double multiplier = 2.0 / (window + 1);
        double ema = 0.0;
        int count = 0;

        for (Map.Entry<LocalDate, Double> entry : symbolPrices.entrySet()) {
            LocalDate date = entry.getKey();
            double price = entry.getValue();
            if (count < window) {
                ema += price;
                count++;
                if (count == window) {
                    ema /= window;
                    emaMap.put(date, ema);
                }
                continue;
            }
            ema = ((price - ema) * multiplier) + ema;
            emaMap.put(date, ema);
        }
        return emaMap;
    }

    public Map<LocalDate, List<String>> getLongs() {
        return longs;
    }

    public Map<LocalDate, List<String>> getShorts() {
        return shorts;
    }

    public Map<String, TreeMap<LocalDate, Double>> getPriceMap() {
        return priceMap;
    }
}

package com.longshanks.strategy;

import com.longshanks.model.StockData;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Implements a Fundamental Strategy.
 * Buys top and shorts bottom percentiles based on available metrics (e.g., closePrice).
 */
public class FundamentalStrategy {
    private double percentile; // e.g., top 10% and bottom 10%
    private Map<LocalDate, List<String>> longs; // Date -> Symbols to Long
    private Map<LocalDate, List<String>> shorts; // Date -> Symbols to Short
    private Map<String, TreeMap<LocalDate, Double>> priceMap; // Symbol -> Date -> Close Price

    /**
     * Constructor to initialize Fundamental Strategy.
     *
     * @param percentile The percentile for top and bottom selections (e.g., 10 for top and bottom 10%).
     */
    public FundamentalStrategy(double percentile) {
        if (percentile <= 0 || percentile >= 50) {
            throw new IllegalArgumentException("Percentile should be between 0 and 50.");
        }
        this.percentile = percentile;
        this.longs = new TreeMap<>();
        this.shorts = new TreeMap<>();
        this.priceMap = new HashMap<>();
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
     * Generates buy/sell signals based on percentiles of closePrice.
     * in real strategies, more meaningful metrics should be used.
     */
    public void generateSignals() {
        // Iterate over each date
        Set<LocalDate> dates = new TreeSet<>();
        for (TreeMap<LocalDate, Double> prices : priceMap.values()) {
            dates.addAll(prices.keySet());
        }

        for (LocalDate date : dates) {
            List<StockPrice> todaysPrices = new ArrayList<>();
            for (String symbol : priceMap.keySet()) {
                Double price = priceMap.get(symbol).get(date);
                if (price != null) {
                    todaysPrices.add(new StockPrice(symbol, price));
                }
            }

            if (todaysPrices.size() < 1) continue;

            // Sort by closePrice
            todaysPrices.sort(Comparator.comparingDouble(StockPrice::getPrice));

            int total = todaysPrices.size();
            int topN = (int) Math.ceil((percentile / 100.0) * total);
            int bottomN = topN;

            // Select bottom N for shorts
            List<String> shortSymbols = todaysPrices.stream()
                    .limit(bottomN)
                    .map(StockPrice::getSymbol)
                    .collect(Collectors.toList());

            // Select top N for longs
            List<String> longSymbols = todaysPrices.stream()
                    .skip(total - topN)
                    .map(StockPrice::getSymbol)
                    .collect(Collectors.toList());

            if (!shortSymbols.isEmpty()) {
                shorts.put(date, shortSymbols);
            }

            if (!longSymbols.isEmpty()) {
                longs.put(date, longSymbols);
            }
        }
    }

    /**
     * Helper class to store symbol and its price.
     */
    private static class StockPrice {
        private String symbol;
        private double price;

        public StockPrice(String symbol, double price) {
            this.symbol = symbol;
            this.price = price;
        }

        public String getSymbol() {
            return symbol;
        }

        public double getPrice() {
            return price;
        }
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

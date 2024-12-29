package com.longshanks.strategy;

import com.longshanks.model.StockData;
import java.time.LocalDate;
import java.util.*;

/**
 * Implements an SMA Crossover Strategy.
 * Buys when the short-term SMA crosses above the long-term SMA.
 * Sells when the short-term SMA crosses below the long-term SMA.
 */


// I have not used it currently instead of used fundamental strategy based on p/e ratio , because I had previously done in my Quant Trading bot Project

public class SmaCrossoverStrategy {
    private int shortWindow;
    private int longWindow;
    private Map<String, TreeMap<LocalDate, Double>> priceMap; // Symbol -> Date -> Close Price
    private Map<LocalDate, List<String>> longs; // Date -> Symbols to Long
    private Map<LocalDate, List<String>> shorts; // Date -> Symbols to Short

    // For each symbol, maintain a queue for short and long SMAs
    private Map<String, LinkedList<Double>> shortSMAQueues;
    private Map<String, LinkedList<Double>> longSMAQueues;

    // For each symbol, track previous SMA values to detect crossovers
    private Map<String, Double> prevShortSMAs;
    private Map<String, Double> prevLongSMAs;

    /**
     * Constructor to initialize the SMA Crossover Strategy.
     *
     * @param shortWindow Short-term SMA window size (e.g., 50 days).
     * @param longWindow  Long-term SMA window size (e.g., 200 days).
     */
    public SmaCrossoverStrategy(int shortWindow, int longWindow) {
        this.shortWindow = shortWindow;
        this.longWindow = longWindow;
        this.priceMap = new HashMap<>();
        this.longs = new TreeMap<>();
        this.shorts = new TreeMap<>();

        this.shortSMAQueues = new HashMap<>();
        this.longSMAQueues = new HashMap<>();
        this.prevShortSMAs = new HashMap<>();
        this.prevLongSMAs = new HashMap<>();
    }

    /**
     * Processes a single StockData entry.
     *
     * @param data        StockData entry.
     * @param isTraining  Indicates if the data is from the training set. Signals are only generated if false.
     */
    public void processData(StockData data, boolean isTraining) {
        String symbol = data.getSymbol();
        LocalDate date = data.getDate();
        double price = data.getClosePrice();

        // Initialize data structures for the symbol if not present
        priceMap.putIfAbsent(symbol, new TreeMap<>());
        shortSMAQueues.putIfAbsent(symbol, new LinkedList<>());
        longSMAQueues.putIfAbsent(symbol, new LinkedList<>());
        prevShortSMAs.putIfAbsent(symbol, Double.NaN);
        prevLongSMAs.putIfAbsent(symbol, Double.NaN);

        // Update price map
        priceMap.get(symbol).put(date, price);

        // Update SMA queues
        LinkedList<Double> shortQueue = shortSMAQueues.get(symbol);
        LinkedList<Double> longQueue = longSMAQueues.get(symbol);

        shortQueue.addLast(price);
        if (shortQueue.size() > shortWindow) {
            shortQueue.removeFirst();
        }

        longQueue.addLast(price);
        if (longQueue.size() > longWindow) {
            longQueue.removeFirst();
        }

        // Calculate current SMAs if enough data
        if (shortQueue.size() == shortWindow && longQueue.size() == longWindow) {
            double currentShortSMA = shortQueue.stream().mapToDouble(Double::doubleValue).average().orElse(Double.NaN);
            double currentLongSMA = longQueue.stream().mapToDouble(Double::doubleValue).average().orElse(Double.NaN);

            double prevShortSMA = prevShortSMAs.get(symbol);
            double prevLongSMA = prevLongSMAs.get(symbol);

            // Detect SMA crossovers only if not in training
            if (!isTraining) {
                // Golden Cross: Short SMA crosses above Long SMA
                if (prevShortSMA <= prevLongSMA && currentShortSMA > currentLongSMA) {
                    longs.putIfAbsent(date, new ArrayList<>());
                    longs.get(date).add(symbol);
                }
                // Death Cross: Short SMA crosses below Long SMA
                else if (prevShortSMA >= prevLongSMA && currentShortSMA < currentLongSMA) {
                    shorts.putIfAbsent(date, new ArrayList<>());
                    shorts.get(date).add(symbol);
                }
            }

            // Update previous SMAs
            prevShortSMAs.put(symbol, currentShortSMA);
            prevLongSMAs.put(symbol, currentLongSMA);
        }
    }

    /**
     * Processes a list of StockData entries.
     *
     * @param dataList    List of StockData entries.
     * @param isTraining  Indicates if the data is from the training set. Signals are only generated if false.
     */
    public void processDataList(List<StockData> dataList, boolean isTraining) {
        // Sort data by date
        dataList.sort(Comparator.comparing(StockData::getDate));
        for (StockData data : dataList) {
            processData(data, isTraining);
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

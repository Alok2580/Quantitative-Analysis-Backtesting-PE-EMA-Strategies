package com.longshanks.evaluation;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Evaluates the accuracy of buy/sell signals.
 */
public class SignalAccuracy {
    private Map<LocalDate, List<String>> longs;
    private Map<LocalDate, List<String>> shorts;
    private Map<String, TreeMap<LocalDate, Double>> priceMap;

    public SignalAccuracy(Map<LocalDate, List<String>> longs,
                          Map<LocalDate, List<String>> shorts,
                          Map<String, TreeMap<LocalDate, Double>> priceMap) {
        this.longs = longs;
        this.shorts = shorts;
        this.priceMap = priceMap;
    }

    /**
     * Calculates overall signal accuracy.
     *
     * @return Accuracy as a decimal.
     */
    public double calculateAccuracy() {
        int correctSignals = 0;
        int totalSignals = 0;

        // For simplicity, assume a correct long signal if the next day's return is positive
        for (Map.Entry<LocalDate, List<String>> entry : longs.entrySet()) {
            LocalDate date = entry.getKey();
            List<String> symbols = entry.getValue();
            for (String symbol : symbols) {
                Double todayPrice = priceMap.get(symbol).get(date);
                LocalDate nextDate = priceMap.get(symbol).higherKey(date);
                if (nextDate != null) {
                    Double nextPrice = priceMap.get(symbol).get(nextDate);
                    if (nextPrice != null && nextPrice > todayPrice) {
                        correctSignals++;
                    }
                    totalSignals++;
                }
            }
        }

        // Similarly, assume a correct short signal if the next day's return is negative
        for (Map.Entry<LocalDate, List<String>> entry : shorts.entrySet()) {
            LocalDate date = entry.getKey();
            List<String> symbols = entry.getValue();
            for (String symbol : symbols) {
                Double todayPrice = priceMap.get(symbol).get(date);
                LocalDate nextDate = priceMap.get(symbol).higherKey(date);
                if (nextDate != null) {
                    Double nextPrice = priceMap.get(symbol).get(nextDate);
                    if (nextPrice != null && nextPrice < todayPrice) {
                        correctSignals++;
                    }
                    totalSignals++;
                }
            }
        }

        if (totalSignals == 0) return 0.0;
        return (double) correctSignals / totalSignals;
    }

    /**
     * Calculates yearly signal accuracy.
     *
     * @return Map of Year to accuracy as a decimal.
     */
    public Map<Integer, Double> analyzeYearlyAccuracy() {
        Map<Integer, Integer> correctSignalsPerYear = new TreeMap<>();
        Map<Integer, Integer> totalSignalsPerYear = new TreeMap<>();

        // Long signals
        for (Map.Entry<LocalDate, List<String>> entry : longs.entrySet()) {
            LocalDate date = entry.getKey();
            int year = date.getYear();
            List<String> symbols = entry.getValue();
            for (String symbol : symbols) {
                Double todayPrice = priceMap.get(symbol).get(date);
                LocalDate nextDate = priceMap.get(symbol).higherKey(date);
                if (nextDate != null) {
                    Double nextPrice = priceMap.get(symbol).get(nextDate);
                    if (nextPrice != null) {
                        if (nextPrice > todayPrice) {
                            correctSignalsPerYear.put(year, correctSignalsPerYear.getOrDefault(year, 0) + 1);
                        }
                        totalSignalsPerYear.put(year, totalSignalsPerYear.getOrDefault(year, 0) + 1);
                    }
                }
            }
        }

        // Short signals
        for (Map.Entry<LocalDate, List<String>> entry : shorts.entrySet()) {
            LocalDate date = entry.getKey();
            int year = date.getYear();
            List<String> symbols = entry.getValue();
            for (String symbol : symbols) {
                Double todayPrice = priceMap.get(symbol).get(date);
                LocalDate nextDate = priceMap.get(symbol).higherKey(date);
                if (nextDate != null) {
                    Double nextPrice = priceMap.get(symbol).get(nextDate);
                    if (nextPrice != null) {
                        if (nextPrice < todayPrice) {
                            correctSignalsPerYear.put(year, correctSignalsPerYear.getOrDefault(year, 0) + 1);
                        }
                        totalSignalsPerYear.put(year, totalSignalsPerYear.getOrDefault(year, 0) + 1);
                    }
                }
            }
        }

        // Calculate accuracy per year
        Map<Integer, Double> accuracyPerYear = new TreeMap<>();
        for (int year : totalSignalsPerYear.keySet()) {
            int correct = correctSignalsPerYear.getOrDefault(year, 0);
            int total = totalSignalsPerYear.get(year);
            accuracyPerYear.put(year, total == 0 ? 0.0 : (double) correct / total);
        }

        return accuracyPerYear;
    }
}

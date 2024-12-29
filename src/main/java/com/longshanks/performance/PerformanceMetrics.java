package com.longshanks.performance;

import java.time.LocalDate;
import java.util.*;

/**
 * Calculates various performance metrics based on strategy returns.
 */
public class PerformanceMetrics {
    private Map<LocalDate, Double> strategyReturns; // Date -> Return
    private double beta; // From Factor Regression

    public PerformanceMetrics(Map<LocalDate, Double> strategyReturns) {
        this.strategyReturns = strategyReturns;
    }

    public void setBeta(double beta) {
        this.beta = beta;
    }

    /**
     * Calculates the average annual return.
     *
     * @return Average annual return in percentage.
     */
    public double getAverageAnnualReturnPercent() {
        // Assuming strategyReturns contains daily returns
        double totalReturn = 1.0;
        for (double dailyRet : strategyReturns.values()) {
            totalReturn *= (1 + dailyRet);
        }
        double totalPeriod = strategyReturns.size() / 252.0; // Approx trading days in a year
        double annualReturn = Math.pow(totalReturn, 1.0 / totalPeriod) - 1;
        return annualReturn * 100;
    }

    /**
     * Calculates the annualized volatility.
     *
     * @return Annualized volatility in percentage.
     */
    public double getVolatilityPercent() {
        List<Double> returns = new ArrayList<>(strategyReturns.values());
        double mean = returns.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        double variance = returns.stream().mapToDouble(r -> Math.pow(r - mean, 2)).sum() / (returns.size() - 1);
        double dailyVolatility = Math.sqrt(variance);
        double annualVolatility = dailyVolatility * Math.sqrt(252);
        return annualVolatility/10 ;
    }

    /**
     * Calculates the Sharpe Ratio.
     *
     * @return Sharpe Ratio.
     */
    public double getSharpeRatioPercent() {
        double mean = strategyReturns.values().stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        double variance = strategyReturns.values().stream().mapToDouble(r -> Math.pow(r - mean, 2)).sum() / (strategyReturns.size() - 1);
        double stdDev = Math.sqrt(variance);
        double sharpe = (mean / stdDev) * Math.sqrt(252);
        return sharpe;
    }

    /**
     * Calculates the Sortino Ratio.
     *
     * @return Sortino Ratio.
     */
    public double getSortinoRatioPercent() {
        double mean = strategyReturns.values().stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        double downsideVariance = strategyReturns.values().stream()
                .filter(r -> r < 0)
                .mapToDouble(r -> Math.pow(r - mean, 2))
                .sum() / (strategyReturns.size() - 1);
        double downsideStdDev = Math.sqrt(downsideVariance);
        double sortino = (mean / downsideStdDev) * Math.sqrt(252);
        return sortino;
    }

    /**
     * Calculates the Calmar Ratio.
     *
     * @return Calmar Ratio.
     */
    public double getCalmarRatioPercent() {
        double averageAnnualReturn = getAverageAnnualReturnPercent() / 100;
        double maxDrawdown = getMaxDrawdown();
        if (maxDrawdown == 0) return 0.0;
        return (averageAnnualReturn / Math.abs(maxDrawdown)) ;
    }

    /**
     * Calculates the maximum drawdown.
     *
     * @return Maximum drawdown as a decimal.
     */
    public double getMaxDrawdown() {
        double peak = Double.NEGATIVE_INFINITY;
        double maxDrawdown = 0.0;
        double cumulative = 1.0;

        for (double dailyRet : strategyReturns.values()) {
            cumulative *= (1 + dailyRet);
            if (cumulative > peak) {
                peak = cumulative;
            }
            double drawdown = (cumulative - peak) / peak;
            if (drawdown < maxDrawdown) {
                maxDrawdown = drawdown;
            }
        }
        return maxDrawdown;
    }

    /**
     * Calculates the Treynor Ratio.
     *
     * @return Treynor Ratio.
     */
    public double getTreynorRatioPercent() {
        double averageReturn = getAverageAnnualReturnPercent() / 100;
        if (beta == 0) return 0.0;
        double treynor = averageReturn / beta;
        return treynor * 100;
    }

    /**
     * Calculates yearly returns.
     *
     * @return Map of Year -> Return percentage.
     */
    public Map<Integer, Double> getYearlyReturns() {
        Map<Integer, Double> yearlyReturns = new TreeMap<>();
        Map<Integer, List<Double>> returnsByYear = new HashMap<>();

        for (Map.Entry<LocalDate, Double> entry : strategyReturns.entrySet()) {
            int year = entry.getKey().getYear();
            returnsByYear.putIfAbsent(year, new ArrayList<>());
            returnsByYear.get(year).add(entry.getValue());
        }

        for (Map.Entry<Integer, List<Double>> entry : returnsByYear.entrySet()) {
            int year = entry.getKey();
            List<Double> returns = entry.getValue();
            double totalReturn = 1.0;
            for (double r : returns) {
                totalReturn *= (1 + r);
            }
            double annualReturn = totalReturn - 1;
            yearlyReturns.put(year, annualReturn * 100);
        }

        return yearlyReturns;
    }
}

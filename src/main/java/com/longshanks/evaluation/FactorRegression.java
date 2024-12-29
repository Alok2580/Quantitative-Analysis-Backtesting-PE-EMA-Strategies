package com.longshanks.evaluation;

import org.apache.commons.math3.stat.regression.OLSMultipleLinearRegression;

import javax.swing.*;
import java.time.LocalDate;
import java.util.Map;

/**
 * Performs regression analysis of strategy returns against market returns.
 * Calculates alpha, beta, and R-squared.
 */
public class FactorRegression {
    private Map<LocalDate, Double> strategyReturns;
    private Map<LocalDate, Double> marketReturns; // e.g., S&P 500 returns

    private double alpha;
    private double beta;
    private double rSquared;

    /**
     * Constructor for FactorRegression.
     *
     * @param strategyReturns Map of strategy returns indexed by date.
     * @param marketReturns   Map of market returns indexed by date.
     */
    public FactorRegression(Map<LocalDate, Double> strategyReturns, Map<LocalDate, Double> marketReturns) {
        this.strategyReturns = strategyReturns;
        this.marketReturns = marketReturns;
    }

    /**
     * Runs an OLS regression of strategy returns on market returns.
     * Calculates alpha, beta, and R-squared.
     */
    public void runRegression() {
        // Collect aligned returns
        int n = 0;
        for (LocalDate date : strategyReturns.keySet()) {
            if (marketReturns.containsKey(date)) {
                n++;
            }
        }

        if (n < 2) {
            JOptionPane.showMessageDialog(null, "Not enough data points for regression.", "Regression Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        double[] y = new double[n]; // Strategy returns
        double[][] x = new double[n][1]; // Market returns (single factor)

        int index = 0;
        for (LocalDate date : strategyReturns.keySet()) {
            if (marketReturns.containsKey(date)) {
                y[index] = strategyReturns.get(date);
                x[index][0] = marketReturns.get(date);
                index++;
            }
        }

        OLSMultipleLinearRegression regression = new OLSMultipleLinearRegression();
        regression.setNoIntercept(false); // Include intercept for alpha
        regression.newSampleData(y, x);

        double[] parameters;
        double rSquared;
        double[] standardErrors;
        try {
            parameters = regression.estimateRegressionParameters();
            rSquared = regression.calculateRSquared();
            standardErrors = regression.estimateRegressionParametersStandardErrors();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Regression failed: " + e.getMessage(), "Regression Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Set the values
        this.alpha = parameters[0];
        this.beta = parameters[1];
        this.rSquared = rSquared;

        // Prepare regression results
        StringBuilder resultBuilder = new StringBuilder();
        resultBuilder.append("=== Factor Regression Results ===\n");
        resultBuilder.append(String.format("Alpha (Intercept): %.6f\n", this.alpha));
        resultBuilder.append(String.format("Beta (Market Exposure): %.6f\n", this.beta));
        resultBuilder.append(String.format("R-squared: %.6f\n", this.rSquared));
        resultBuilder.append(String.format("Standard Error of Alpha: %.6f\n", standardErrors[0]));
        resultBuilder.append(String.format("Standard Error of Beta: %.6f\n", standardErrors[1]));

        // Display in Swing window
        JTextArea textArea = new JTextArea(resultBuilder.toString());
        textArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(textArea);

        JOptionPane.showMessageDialog(null, scrollPane, "Factor Regression Results", JOptionPane.INFORMATION_MESSAGE);
    }

    /**
     * Retrieves the Alpha value from the regression.
     *
     * @return Alpha value.
     */
    public double getAlpha() {
        return alpha;
    }

    /**
     * Retrieves the Beta value from the regression.
     *
     * @return Beta value.
     */
    public double getBeta() {
        return beta;
    }

    /**
     * Retrieves the R-squared value from the regression.
     *
     * @return R-squared value.
     */
    public double getRSquared() {
        return rSquared;
    }
}

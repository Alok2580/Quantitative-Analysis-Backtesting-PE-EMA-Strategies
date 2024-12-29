package com.longshanks.model;

import java.time.LocalDate;

/**
 * Represents stock data for a specific date.
 */
public class StockData {
    private LocalDate date;
    private String symbol;
    private String sector;
    private double closePrice;
    private double eps;
    private double peRatio;

    /**
     * Constructor with six arguments.
     *
     * @param date       The date of the stock data.
     * @param symbol     The stock symbol.
     * @param sector     The sector of the stock.
     * @param closePrice The closing price of the stock.
     * @param eps        Earnings per share.
     * @param peRatio    Price-to-Earnings ratio.
     */
    public StockData(LocalDate date, String symbol, String sector, double closePrice, double eps, double peRatio) {
        this.date = date;
        this.symbol = symbol;
        this.sector = sector;
        this.closePrice = closePrice;
        this.eps = eps;
        this.peRatio = peRatio;
    }

    /**
     * Constructor with four arguments.
     * Useful if eps and peRatio are not available.
     *
     * @param date       The date of the stock data.
     * @param symbol     The stock symbol.
     * @param sector     The sector of the stock.
     * @param closePrice The closing price of the stock.
     */
    public StockData(LocalDate date, String symbol, String sector, double closePrice) {
        this.date = date;
        this.symbol = symbol;
        this.sector = sector;
        this.closePrice = closePrice;
        this.eps = 0.0;      // Default value if not provided
        this.peRatio = 0.0;  // Default value if not provided
    }

    // Getters

    public LocalDate getDate() {
        return date;
    }

    public String getSymbol() {
        return symbol;
    }

    public String getSector() {
        return sector;
    }

    public double getClosePrice() {
        return closePrice;
    }

    public double getEps() {
        return eps;
    }

    public double getPeRatio() {
        return peRatio;
    }

    @Override
    public String toString() {
        return "StockData{" +
                "date=" + date +
                ", symbol='" + symbol + '\'' +
                ", sector='" + sector + '\'' +
                ", closePrice=" + closePrice +
                ", eps=" + eps +
                ", peRatio=" + peRatio +
                '}';
    }
}

package com.longshanks.data;

import com.longshanks.model.StockData;
import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;

import java.io.FileReader;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Loads stock data and market returns from CSV files.
 */
public class DataLoader {
    private String stockDataPath;
    private String marketReturnsPath;
    private List<StockData> stockDataList;
    private Map<LocalDate, Double> marketReturns;

    public DataLoader(String stockDataPath, String marketReturnsPath) {
        this.stockDataPath = stockDataPath;
        this.marketReturnsPath = marketReturnsPath;
        this.stockDataList = new ArrayList<>();
        this.marketReturns = new TreeMap<>();
    }

    /**
     * Loads all data from CSV files.
     */
    public void loadAllData() throws IOException, CsvValidationException {
        loadStockData();
        loadMarketReturns();
    }

    /**
     * Loads stock data from CSV.
     */
    private void loadStockData() throws IOException, CsvValidationException {
        try (CSVReader reader = new CSVReader(new FileReader(stockDataPath))) {
            String[] line;
            reader.readNext(); // Skip header
            while ((line = reader.readNext()) != null) {
                // Adjusted column indices based on CSV structure.
                // Assuming CSV columns are: Sector, Symbol, Date, ClosePrice, Eps, PeRatio
                if (line.length < 4) {
                    System.err.println("Invalid line (less than 4 fields): " + String.join(",", line));
                    continue;
                }

                String sector = line[0];
                String symbol = line[1];
                LocalDate date;
                try {
                    date = LocalDate.parse(line[2]);
                } catch (Exception e) {
                    System.err.println("Invalid date format for symbol " + symbol + ": " + line[2]);
                    continue;
                }

                double closePrice;
                try {
                    closePrice = Double.parseDouble(line[3]);
                } catch (NumberFormatException e) {
                    System.err.println("Invalid close price for symbol " + symbol + " on " + date + ": " + line[3]);
                    continue;
                }

                // Check if CSV has more fields for eps and peRatio
                if (line.length >= 6) {
                    double eps;
                    double peRatio;
                    try {
                        eps = Double.parseDouble(line[4]);
                        peRatio = Double.parseDouble(line[5]);
                    } catch (NumberFormatException e) {
                        System.err.println("Invalid EPS or PE Ratio for symbol " + symbol + " on " + date + ": " + line[4] + ", " + line[5]);
                        continue;
                    }
                    stockDataList.add(new StockData(date, symbol, sector, closePrice, eps, peRatio));
                } else {
                    stockDataList.add(new StockData(date, symbol, sector, closePrice));
                }
            }
        }
    }

    /**
     * Loads market returns from CSV.
     */
    private void loadMarketReturns() throws IOException, CsvValidationException {
        // Define the date format used in market_returns.csv
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy"); // e.g., 09-12-2021

        try (CSVReader reader = new CSVReader(new FileReader(marketReturnsPath))) {
            String[] line;
            reader.readNext(); // Skip header
            while ((line = reader.readNext()) != null) {
                // Assuming CSV has two fields: Date, Return
                if (line.length < 2) {
                    System.err.println("Invalid line (less than 2 fields): " + String.join(",", line));
                    continue;
                }

                LocalDate date;
                try {
                    date = LocalDate.parse(line[0], formatter);
                } catch (Exception e) {
                    System.err.println("Invalid date format in market returns: " + line[0]);
                    continue;
                }

                double returnPct;
                try {
                    returnPct = Double.parseDouble(line[1]) / 100.0; // Convert percentage to decimal
                } catch (NumberFormatException e) {
                    System.err.println("Invalid return percentage on " + date + ": " + line[1]);
                    continue;
                }

                marketReturns.put(date, returnPct);
            }
        }
    }

    public List<StockData> getStockDataList() {
        return stockDataList;
    }

    public Map<LocalDate, Double> getMarketReturns() {
        return marketReturns;
    }

    /**
     * Splits data into training and testing sets based on the given ratio.
     *
     * @param trainRatio Ratio of data to use for training.
     * @return Pair of training and testing data lists.
     */
    public Pair<List<StockData>, List<StockData>> splitData(double trainRatio) {
        List<StockData> training = new ArrayList<>();
        List<StockData> testing = new ArrayList<>();

        // Sort data by date
        stockDataList.sort((a, b) -> a.getDate().compareTo(b.getDate()));

        int total = stockDataList.size();
        int trainSize = (int) (total * trainRatio);

        training.addAll(stockDataList.subList(0, trainSize));
        testing.addAll(stockDataList.subList(trainSize, total));

        return new Pair<>(training, testing);
    }

    /**
     * Simple Pair class.
     */
    public static class Pair<F, S> {
        public F first;
        public S second;

        public Pair(F first, S second) {
            this.first = first;
            this.second = second;
        }
    }
}

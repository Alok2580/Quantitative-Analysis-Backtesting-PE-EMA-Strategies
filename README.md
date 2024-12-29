---
# Quantitative-Analysis-Backtesting-PE-EMA-Strategies

## Overview

This project implements and compares two financial strategies:  
1. **Fundamental Strategy (P/E Ratio)**  
2. **EMA Crossover Strategy**

The goal is to assess the effectiveness of these strategies through backtesting, performance metrics evaluation, and visual analysis. The project is built using **Java** for implementation, **OpenCSV** for data handling, and **JFreeChart** for visualizations.

## Key Features

- **Part 1: Fundamental Strategy (P/E Ratio)**
  - Selects stocks based on their P/E ratio.
  - Monthly rebalancing based on updated P/E values.
  - Backtests strategy and evaluates performance metrics like Sharpe Ratio and Sortino Ratio.

- **Part 2: EMA Crossover Strategy**
  - Implements the EMA Crossover strategy with short-term and long-term EMAs.
  - Compares performance with the Fundamental Strategy.

## Tools & Libraries

- **OpenCSV**: For CSV data handling.
- **JFreeChart**: For generating charts and visualizations.
- **Swing**: For creating user interfaces.

## Data

Stock data and market returns are loaded from CSV files:
- **daily_stock_data.csv**: Contains stock data (symbol, date, close price, P/E ratio).
- **market_returns.csv**: Historical S&P 500 returns for benchmarking.

## How to Run

1. Clone the repository:
   ```bash
   git clone https://github.com/yourusername/Quantitative-Analysis-Backtesting-PE-EMA-Strategies.git
   ```

2. Compile and run the project in your Java environment.

3. Visualizations and performance metrics will be displayed after backtesting both strategies.

## Future Enhancements

- Incorporate additional fundamental metrics (e.g., Debt-to-Equity, ROE).
- Experiment with different EMA window sizes and add more technical indicators.
- Implement risk management features such as stop-loss and take-profit.

---

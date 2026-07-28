package com.ledger.salesmanager.controller;

import com.ledger.salesmanager.model.Product;
import com.ledger.salesmanager.model.Role;
import com.ledger.salesmanager.model.User;
import com.ledger.salesmanager.service.AuthService;
import com.ledger.salesmanager.service.DashboardService;
import com.ledger.salesmanager.util.SessionManager;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class DashboardViewController {

    @FXML private FlowPane kpiRow;
    @FXML private LineChart<String, Number> trendChart;
    @FXML private PieChart mixChart;
    @FXML private BarChart<String, Number> topProductsChart;
    @FXML private ListView<String> lowStockList;

    private final DashboardService dashboardService = new DashboardService();
    private static final DateTimeFormatter DAY_FMT = DateTimeFormatter.ofPattern("MMM d");

    @FXML
    public void initialize() {
        boolean canSeeMoney = AuthService.canViewWholesale(); // Owner + Salesperson, not Viewer (viewer never reaches this screen)

        DashboardService.Kpis kpis = dashboardService.loadKpis();
        renderKpis(kpis, canSeeMoney);
        renderTrendChart(canSeeMoney);
        renderMixChart();
        renderTopProductsChart();
        renderLowStockList();
    }

    private void renderKpis(DashboardService.Kpis k, boolean canSeeMoney) {
        kpiRow.getChildren().clear();
        kpiRow.getChildren().add(kpiCard("Total Products", String.valueOf(k.totalProducts)));
        if (canSeeMoney) {
            kpiRow.getChildren().add(kpiCard("Revenue Today", money(k.revenueToday)));
            kpiRow.getChildren().add(kpiCard("Profit Today", money(k.profitToday)));
            kpiRow.getChildren().add(kpiCard("Revenue This Month", money(k.revenueThisMonth)));
            kpiRow.getChildren().add(kpiCard("Stock Value (Wholesale)", money(k.totalStockValueWholesale)));
        }
        kpiRow.getChildren().add(kpiCard("Low Stock", String.valueOf(k.lowStockCount)));
        kpiRow.getChildren().add(kpiCard("Out of Stock", String.valueOf(k.outOfStockCount)));
    }

    private VBox kpiCard(String label, String value) {
        VBox card = new VBox(4);
        card.getStyleClass().add("kpi-card");
        card.setPrefWidth(200);
        Label valueLabel = new Label(value);
        valueLabel.getStyleClass().add("kpi-value");
        Label nameLabel = new Label(label);
        nameLabel.getStyleClass().add("muted-text");
        card.getChildren().addAll(valueLabel, nameLabel);
        return card;
    }

    private void renderTrendChart(boolean canSeeMoney) {
        trendChart.getData().clear();
        List<Object[]> rows = dashboardService.revenueTrend(14);

        XYChart.Series<String, Number> revenueSeries = new XYChart.Series<>();
        revenueSeries.setName("Revenue");
        XYChart.Series<String, Number> profitSeries = new XYChart.Series<>();
        profitSeries.setName("Profit");

        for (Object[] row : rows) {
            LocalDate date = (LocalDate) row[0];
            BigDecimal revenue = (BigDecimal) row[1];
            BigDecimal profit = (BigDecimal) row[2];
            String label = date.format(DAY_FMT);
            revenueSeries.getData().add(new XYChart.Data<>(label, revenue));
            if (canSeeMoney) profitSeries.getData().add(new XYChart.Data<>(label, profit));
        }

        trendChart.getData().add(revenueSeries);
        if (canSeeMoney) trendChart.getData().add(profitSeries);
    }

    private void renderMixChart() {
        List<Object[]> rows = dashboardService.topProducts(30, 6);
        mixChart.getData().clear();
        for (Object[] row : rows) {
            String name = (String) row[0];
            BigDecimal revenue = (BigDecimal) row[2];
            mixChart.getData().add(new PieChart.Data(name, revenue.doubleValue()));
        }
    }

    private void renderTopProductsChart() {
        topProductsChart.getData().clear();
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        for (Object[] row : dashboardService.topProducts(30, 8)) {
            String name = (String) row[0];
            Integer qty = (Integer) row[1];
            series.getData().add(new XYChart.Data<>(name, qty));
        }
        topProductsChart.getData().add(series);
    }

    private void renderLowStockList() {
        List<Product> lowStock = dashboardService.lowStockWatchlist();
        List<String> lines = lowStock.stream()
                .map(p -> p.getName() + "  —  " + p.getAvailableQuantity() + " left")
                .toList();
        lowStockList.setItems(FXCollections.observableArrayList(
                lines.isEmpty() ? List.of("All stock levels are healthy.") : lines));
    }

    private String money(BigDecimal amount) {
        return String.format("Rs %,.0f", amount);
    }
}

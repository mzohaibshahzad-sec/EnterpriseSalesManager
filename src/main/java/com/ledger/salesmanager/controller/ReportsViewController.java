package com.ledger.salesmanager.controller;

import com.ledger.salesmanager.dao.ProductDAO;
import com.ledger.salesmanager.dao.SaleDAO;
import com.ledger.salesmanager.model.Product;
import com.ledger.salesmanager.model.Sale;
import com.ledger.salesmanager.service.BackupService;
import com.ledger.salesmanager.service.ReportService;
import com.ledger.salesmanager.util.AlertUtil;
import javafx.fxml.FXML;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;

import java.io.File;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

public class ReportsViewController {

    @FXML private DatePicker fromDatePicker, toDatePicker;
    @FXML private Label backupStatusLabel;

    private final ProductDAO productDAO = new ProductDAO();
    private final SaleDAO saleDAO = new SaleDAO();
    private final ReportService reportService = new ReportService();
    private final BackupService backupService = new BackupService();

    @FXML
    public void initialize() {
        toDatePicker.setValue(LocalDate.now());
        fromDatePicker.setValue(LocalDate.now().minusDays(29));
    }

    // ---- Products ----
    @FXML private void onExportProductsCsv()   { exportProducts("csv"); }
    @FXML private void onExportProductsExcel() { exportProducts("xlsx"); }
    @FXML private void onExportProductsPdf()   { exportProducts("pdf"); }

    private void exportProducts(String format) {
        File target = chooseSaveFile("products." + format, format);
        if (target == null) return;
        try {
            List<Product> products = productDAO.findAllActive();
            Path path = Path.of(target.getAbsolutePath());
            switch (format) {
                case "csv" -> reportService.exportProductsCsv(products, path);
                case "xlsx" -> reportService.exportProductsExcel(products, path);
                case "pdf" -> reportService.exportProductsPdf(products, "Store", path);
            }
            AlertUtil.info("Exported", "Product report save ho gayi: " + target.getName());
        } catch (Exception e) {
            AlertUtil.error("Export Failed", e.getMessage());
        }
    }

    // ---- Sales ----
    @FXML private void onExportSalesCsv()   { exportSales("csv"); }
    @FXML private void onExportSalesExcel() { exportSales("xlsx"); }
    @FXML private void onExportSalesPdf()   { exportSales("pdf"); }

    private void exportSales(String format) {
        File target = chooseSaveFile("sales." + format, format);
        if (target == null) return;
        try {
            List<Sale> sales = saleDAO.findAll(5000);
            Path path = Path.of(target.getAbsolutePath());
            switch (format) {
                case "csv" -> reportService.exportSalesCsv(sales, path);
                case "xlsx" -> reportService.exportSalesExcel(sales, path);
                case "pdf" -> reportService.exportSalesPdf(sales, "Store", path);
            }
            AlertUtil.info("Exported", "Sales report save ho gayi: " + target.getName());
        } catch (Exception e) {
            AlertUtil.error("Export Failed", e.getMessage());
        }
    }

    private File chooseSaveFile(String defaultName, String extension) {
        FileChooser chooser = new FileChooser();
        chooser.setInitialFileName(defaultName);
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(extension.toUpperCase(), "*." + extension));
        return chooser.showSaveDialog(backupStatusLabel.getScene().getWindow());
    }

    // ---- Backup / Restore ----
    @FXML
    private void onBackup() {
        DirectoryChooser chooser = new DirectoryChooser();
        File dir = chooser.showDialog(backupStatusLabel.getScene().getWindow());
        if (dir == null) return;
        try {
            Path result = backupService.backup(dir.toPath());
            backupStatusLabel.setText("Backup saved: " + result.getFileName());
        } catch (Exception e) {
            backupStatusLabel.setText("Backup failed: " + e.getMessage());
        }
    }

    @FXML
    private void onRestore() {
        boolean confirmed = AlertUtil.confirm("Restore Database?",
                "Ye current database ka data overwrite kar dega. Confirm karein?");
        if (!confirmed) return;

        FileChooser chooser = new FileChooser();
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("SQL Dump", "*.sql"));
        File file = chooser.showOpenDialog(backupStatusLabel.getScene().getWindow());
        if (file == null) return;
        try {
            backupService.restore(file.toPath());
            backupStatusLabel.setText("Database restore ho gaya. App ko restart karein.");
        } catch (Exception e) {
            backupStatusLabel.setText("Restore failed: " + e.getMessage());
        }
    }
}

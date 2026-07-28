package com.ledger.salesmanager.controller;

import com.ledger.salesmanager.model.Role;
import com.ledger.salesmanager.model.Sale;
import com.ledger.salesmanager.model.User;
import com.ledger.salesmanager.service.AuthService;
import com.ledger.salesmanager.service.ReportService;
import com.ledger.salesmanager.service.SalesService;
import com.ledger.salesmanager.util.AlertUtil;
import com.ledger.salesmanager.util.SceneManager;
import com.ledger.salesmanager.util.SessionManager;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.File;
import java.net.URL;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;

public class SalesViewController {

    @FXML private Button newSaleButton;
    @FXML private TableView<Sale> salesTable;
    @FXML private TableColumn<Sale, String> colInvoice, colDate, colSalesperson, colCustomer, colTotal, colProfit, colMethod, colStatus;
    @FXML private TableColumn<Sale, Void> colActions;

    private final SalesService salesService = new SalesService();
    private final ReportService reportService = new ReportService();
    private final ObservableList<Sale> data = FXCollections.observableArrayList();
    private static final DateTimeFormatter DT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private boolean canVoid;
    private boolean canSeeMoney;

    @FXML
    public void initialize() {
        canVoid = AuthService.canManageProducts(); // Owner only
        canSeeMoney = AuthService.canViewWholesale();

        colProfit.setVisible(canSeeMoney);
        colActions.setVisible(canVoid);
        newSaleButton.setVisible(AuthService.canRecordSales());
        newSaleButton.setManaged(AuthService.canRecordSales());

        colInvoice.setCellValueFactory(new PropertyValueFactory<>("invoiceNumber"));
        colDate.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(
                c.getValue().getSaleDateTime() != null ? c.getValue().getSaleDateTime().format(DT) : ""));
        colSalesperson.setCellValueFactory(new PropertyValueFactory<>("salespersonName"));
        colCustomer.setCellValueFactory(new PropertyValueFactory<>("customerName"));
        colTotal.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(
                String.format("Rs %,.0f", c.getValue().getTotalAmount())));
        colProfit.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(
                String.format("Rs %,.0f", c.getValue().getTotalProfit())));
        colMethod.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(
                c.getValue().getPaymentMethod().name()));
        colStatus.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(
                c.getValue().getPaymentStatus().name()));

        colActions.setCellFactory(col -> new TableCell<>() {
            private final Button invoiceBtn = new Button("Invoice PDF");
            private final Button voidBtn = new Button("Void");
            private final HBox box = new HBox(6, invoiceBtn, voidBtn);
            {
                invoiceBtn.setOnAction(e -> onExportInvoice(getTableView().getItems().get(getIndex())));
                voidBtn.getStyleClass().add("danger-button");
                voidBtn.setOnAction(e -> onVoidSale(getTableView().getItems().get(getIndex())));
                voidBtn.setVisible(canVoid);
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : box);
            }
        });

        salesTable.setItems(data);
        refresh();
    }

    private void refresh() {
        User user = SessionManager.getInstance().getCurrentUser();
        if (user.getRole() == Role.SALESPERSON) {
            data.setAll(salesService.mySales(user.getId(), 200));
        } else {
            data.setAll(salesService.recentSales(200));
        }
    }

    @FXML
    private void onNewSale() {
        try {
            URL url = getClass().getResource("/fxml/new-sale-dialog.fxml");
            FXMLLoader loader = new FXMLLoader(url);
            Parent root = loader.load();
            NewSaleController controller = loader.getController();
            controller.setOnCompleted(this::refresh);

            Stage dialog = new Stage();
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.setTitle("Record a Sale");
            javafx.scene.Scene scene = new javafx.scene.Scene(root);
            SceneManager.applyTheme(scene);
            dialog.setScene(scene);
            dialog.showAndWait();
        } catch (Exception e) {
            AlertUtil.error("Error", "Sale form open nahi ho saka: " + e.getMessage());
        }
    }

    private void onExportInvoice(Sale sale) {
        FileChooser chooser = new FileChooser();
        chooser.setInitialFileName(sale.getInvoiceNumber() + ".pdf");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF", "*.pdf"));
        File file = chooser.showSaveDialog(salesTable.getScene().getWindow());
        if (file == null) return;
        try {
            var items = salesService.itemsFor(sale.getId());
            reportService.generateInvoicePdf(sale, items, "Store", null, Path.of(file.getAbsolutePath()));
            AlertUtil.info("Exported", "Invoice save ho gaya: " + file.getName());
        } catch (Exception e) {
            AlertUtil.error("Export Failed", e.getMessage());
        }
    }

    private void onVoidSale(Sale sale) {
        boolean confirmed = AlertUtil.confirm("Void Sale?",
                "Invoice " + sale.getInvoiceNumber() + " void karne se stock wapis add ho jayega. Confirm karein?");
        if (confirmed) {
            salesService.voidSale(sale.getId());
            refresh();
        }
    }
}

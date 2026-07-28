package com.ledger.salesmanager.controller;

import com.ledger.salesmanager.model.PaymentMethod;
import com.ledger.salesmanager.model.PaymentStatus;
import com.ledger.salesmanager.model.Product;
import com.ledger.salesmanager.model.SaleItem;
import com.ledger.salesmanager.service.ProductService;
import com.ledger.salesmanager.service.SalesService;
import com.ledger.salesmanager.util.ValidationUtil;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;

import java.math.BigDecimal;
import java.util.List;

public class NewSaleController {

    @FXML private ComboBox<Product> productCombo;
    @FXML private TextField qtyField, priceField, customerField, discountField, notesField;
    @FXML private ComboBox<PaymentMethod> paymentMethodCombo;
    @FXML private ComboBox<PaymentStatus> paymentStatusCombo;
    @FXML private TableView<SaleItem> cartTable;
    @FXML private TableColumn<SaleItem, String> colProduct, colUnitPrice, colLineTotal;
    @FXML private TableColumn<SaleItem, Number> colQty;
    @FXML private TableColumn<SaleItem, Void> colRemove;
    @FXML private Label totalLabel, errorLabel;

    private final ProductService productService = new ProductService();
    private final SalesService salesService = new SalesService();
    private final ObservableList<SaleItem> cart = FXCollections.observableArrayList();
    private List<Product> availableProducts;
    private Runnable onCompleted;

    public void setOnCompleted(Runnable callback) { this.onCompleted = callback; }

    @FXML
    public void initialize() {
        availableProducts = productService.listAll().stream()
                .filter(p -> p.getAvailableQuantity() > 0).toList();
        productCombo.setItems(FXCollections.observableArrayList(availableProducts));
        productCombo.setConverter(new javafx.util.StringConverter<>() {
            @Override public String toString(Product p) { return p == null ? "" : p.getName() + " (" + p.getAvailableQuantity() + " available)"; }
            @Override public Product fromString(String s) { return null; }
        });
        productCombo.getSelectionModel().selectedItemProperty().addListener((obs, old, product) -> {
            if (product != null) priceField.setText(product.getSellingPrice().toPlainString());
        });

        paymentMethodCombo.setItems(FXCollections.observableArrayList(PaymentMethod.values()));
        paymentMethodCombo.getSelectionModel().select(PaymentMethod.CASH);
        paymentStatusCombo.setItems(FXCollections.observableArrayList(PaymentStatus.values()));
        paymentStatusCombo.getSelectionModel().select(PaymentStatus.PAID);

        colProduct.setCellValueFactory(new PropertyValueFactory<>("productName"));
        colQty.setCellValueFactory(c -> new javafx.beans.property.SimpleIntegerProperty(c.getValue().getQuantity()));
        colUnitPrice.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(
                String.format("Rs %,.0f", c.getValue().getUnitPrice())));
        colLineTotal.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(
                String.format("Rs %,.0f", c.getValue().getLineTotal())));
        colRemove.setCellFactory(col -> new TableCell<>() {
            private final Button removeBtn = new Button("Remove");
            { removeBtn.setOnAction(e -> { cart.remove(getIndex()); recalcTotal(); }); }
            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : removeBtn);
            }
        });

        cartTable.setItems(cart);
        discountField.textProperty().addListener((obs, old, val) -> recalcTotal());
        recalcTotal();
    }

    @FXML
    private void onAddToCart() {
        errorLabel.setText("");
        Product product = productCombo.getValue();
        if (product == null) { errorLabel.setText("Pehle product select karein."); return; }
        if (!ValidationUtil.isNonNegativeNumber(qtyField.getText()) || Double.parseDouble(qtyField.getText()) <= 0) {
            errorLabel.setText("Valid quantity darj karein.");
            return;
        }
        if (!ValidationUtil.isNonNegativeNumber(priceField.getText())) {
            errorLabel.setText("Valid unit price darj karein.");
            return;
        }

        int qty = (int) Double.parseDouble(qtyField.getText());
        int alreadyInCart = cart.stream().filter(i -> i.getProductId() == product.getId())
                .mapToInt(SaleItem::getQuantity).sum();
        if (qty + alreadyInCart > product.getAvailableQuantity()) {
            errorLabel.setText(product.getName() + " mein sirf " + product.getAvailableQuantity() + " units available hain.");
            return;
        }

        BigDecimal unitPrice = new BigDecimal(priceField.getText());
        cart.add(new SaleItem(product, qty, unitPrice));
        qtyField.clear();
        recalcTotal();
    }

    private void recalcTotal() {
        BigDecimal subtotal = cart.stream().map(SaleItem::getLineTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal discount = ValidationUtil.isNonNegativeNumber(discountField.getText())
                ? new BigDecimal(discountField.getText()) : BigDecimal.ZERO;
        totalLabel.setText("Total: Rs " + String.format("%,.0f", subtotal.subtract(discount)));
    }

    @FXML
    private void onCheckout() {
        errorLabel.setText("");
        if (cart.isEmpty()) { errorLabel.setText("Cart khali hai — pehle product add karein."); return; }

        try {
            BigDecimal discount = ValidationUtil.isNonNegativeNumber(discountField.getText())
                    ? new BigDecimal(discountField.getText()) : BigDecimal.ZERO;

            salesService.checkout(
                    List.copyOf(cart),
                    discount,
                    paymentMethodCombo.getValue(),
                    paymentStatusCombo.getValue(),
                    customerField.getText(),
                    notesField.getText()
            );

            if (onCompleted != null) onCompleted.run();
            currentStage().close();
        } catch (SalesService.InsufficientStockException ex) {
            errorLabel.setText(ex.getMessage());
        } catch (Exception e) {
            errorLabel.setText("Sale save nahi ho saki: " + e.getMessage());
        }
    }

    @FXML
    private void onCancel() {
        currentStage().close();
    }

    private Stage currentStage() {
        return (Stage) cartTable.getScene().getWindow();
    }
}

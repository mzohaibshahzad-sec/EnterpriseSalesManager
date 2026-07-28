package com.ledger.salesmanager.controller;

import com.ledger.salesmanager.model.Product;
import com.ledger.salesmanager.service.AuthService;
import com.ledger.salesmanager.service.ProductService;
import com.ledger.salesmanager.util.AlertUtil;
import com.ledger.salesmanager.util.SceneManager;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.net.URL;
import java.util.List;

public class ProductsViewController {

    @FXML private TextField searchField;
    @FXML private Button addButton;
    @FXML private TableView<Product> productTable;
    @FXML private TableColumn<Product, String> colName, colSku, colCategory;
    @FXML private TableColumn<Product, String> colWholesale, colSelling, colProfit;
    @FXML private TableColumn<Product, Number> colTotal, colSold, colAvailable;
    @FXML private TableColumn<Product, Void> colActions;

    private final ProductService productService = new ProductService();
    private final ObservableList<Product> data = FXCollections.observableArrayList();
    private boolean canManage;
    private boolean canSeeMoney;

    @FXML
    public void initialize() {
        canManage = AuthService.canManageProducts();
        canSeeMoney = AuthService.canViewWholesale();

        addButton.setVisible(canManage);
        addButton.setManaged(canManage);
        colWholesale.setVisible(canSeeMoney);
        colProfit.setVisible(canSeeMoney);
        colActions.setVisible(canManage);

        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colSku.setCellValueFactory(new PropertyValueFactory<>("sku"));
        colCategory.setCellValueFactory(new PropertyValueFactory<>("categoryName"));
        colWholesale.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(
                money(c.getValue().getWholesalePrice())));
        colSelling.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(
                money(c.getValue().getSellingPrice())));
        colTotal.setCellValueFactory(c -> new javafx.beans.property.SimpleIntegerProperty(c.getValue().getTotalQuantity()));
        colSold.setCellValueFactory(c -> new javafx.beans.property.SimpleIntegerProperty(c.getValue().getSoldQuantity()));
        colAvailable.setCellValueFactory(c -> new javafx.beans.property.SimpleIntegerProperty(c.getValue().getAvailableQuantity()));
        colProfit.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(
                money(c.getValue().getProfitPerItem())));

        if (canManage) {
            colActions.setCellFactory(col -> new TableCell<>() {
                private final Button editBtn = new Button("Edit");
                private final Button deleteBtn = new Button("Delete");
                private final HBox box = new HBox(6, editBtn, deleteBtn);
                {
                    editBtn.setOnAction(e -> openForm(getTableView().getItems().get(getIndex())));
                    deleteBtn.getStyleClass().add("danger-button");
                    deleteBtn.setOnAction(e -> onDelete(getTableView().getItems().get(getIndex())));
                }
                @Override
                protected void updateItem(Void item, boolean empty) {
                    super.updateItem(item, empty);
                    setGraphic(empty ? null : box);
                }
            });
        }

        productTable.setItems(data);
        refresh();

        searchField.textProperty().addListener((obs, oldV, newV) -> {
            data.setAll(productService.search(newV));
        });
    }

    private void refresh() {
        data.setAll(productService.listAll());
    }

    @FXML
    private void onAddProduct() {
        openForm(null);
    }

    private void openForm(Product existing) {
        try {
            URL url = getClass().getResource("/fxml/product-form-dialog.fxml");
            FXMLLoader loader = new FXMLLoader(url);
            Parent root = loader.load();
            ProductFormController controller = loader.getController();
            controller.setProduct(existing);

            Stage dialog = new Stage();
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.setTitle(existing == null ? "Add Product" : "Edit Product");
            javafx.scene.Scene scene = new javafx.scene.Scene(root);
            SceneManager.applyTheme(scene);
            dialog.setScene(scene);
            controller.setOnSaved(this::refresh);
            dialog.showAndWait();
        } catch (Exception e) {
            AlertUtil.error("Error", "Product form open nahi ho saka: " + e.getMessage());
        }
    }

    private void onDelete(Product product) {
        boolean confirmed = AlertUtil.confirm("Delete Product?",
                "Kya aap '" + product.getName() + "' delete karna chahte hain?");
        if (confirmed) {
            productService.deleteProduct(product.getId());
            refresh();
        }
    }

    private String money(java.math.BigDecimal amount) {
        return String.format("Rs %,.0f", amount);
    }
}

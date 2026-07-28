package com.ledger.salesmanager.controller;

import com.ledger.salesmanager.dao.StoreDAO;
import com.ledger.salesmanager.model.Product;
import com.ledger.salesmanager.model.StoreInfo;
import com.ledger.salesmanager.service.ProductService;
import com.ledger.salesmanager.util.SceneManager;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;

import java.io.File;
import java.util.List;

/**
 * The default, public, no-login screen (per spec). Shows the catalog
 * with selling prices and stock only — no wholesale price, no profit,
 * no management actions. "Admin Login" and "Salesperson Login" both
 * route to the same login.fxml; the login screen itself performs
 * role-appropriate routing after authentication.
 */
public class ViewerDashboardController {

    @FXML private ImageView storeLogoView;
    @FXML private Label storeNameLabel;
    @FXML private TextField searchField;
    @FXML private FlowPane productGrid;

    private final ProductService productService = new ProductService();
    private List<Product> allProducts;

    @FXML
    public void initialize() {
        loadStoreBranding();
        allProducts = safeLoadProducts();
        renderProducts(allProducts);

        searchField.textProperty().addListener((obs, oldV, newV) ->
                renderProducts(productService.search(newV)));
    }

    private void loadStoreBranding() {
        try {
            StoreInfo store = new StoreDAO().getStoreInfo();
            if (store != null) {
                storeNameLabel.setText(store.getStoreName());
                if (store.getStoreLogoPath() != null) {
                    File logo = new File(store.getStoreLogoPath());
                    if (logo.exists()) storeLogoView.setImage(new Image(logo.toURI().toString()));
                }
            }
        } catch (Exception e) {
            storeNameLabel.setText("Store");
        }
    }

    private List<Product> safeLoadProducts() {
        try {
            return productService.listAll();
        } catch (Exception e) {
            return List.of();
        }
    }

    private void renderProducts(List<Product> products) {
        productGrid.getChildren().clear();
        for (Product p : products) {
            productGrid.getChildren().add(buildCard(p));
        }
    }

    private VBox buildCard(Product p) {
        VBox card = new VBox(6);
        card.setPrefWidth(220);
        card.setPadding(new Insets(14));
        card.getStyleClass().add("product-card");

        ImageView image = new ImageView();
        image.setFitWidth(190);
        image.setFitHeight(120);
        image.setPreserveRatio(true);
        if (p.getImagePath() != null) {
            File img = new File(p.getImagePath());
            if (img.exists()) image.setImage(new Image(img.toURI().toString()));
        }

        Label name = new Label(p.getName());
        name.getStyleClass().add("card-title");
        Label category = new Label(p.getCategoryName() != null ? p.getCategoryName() : "");
        category.getStyleClass().add("muted-text");
        Label price = new Label(String.format("Rs %,.0f", p.getSellingPrice()));
        price.getStyleClass().add("price-text");
        Label stock = new Label(p.isOutOfStock() ? "Out of stock" : p.getAvailableQuantity() + " in stock");
        stock.getStyleClass().add(p.isOutOfStock() ? "stock-out" : "stock-ok");

        card.getChildren().addAll(image, name, category, price, stock);
        return card;
    }

    @FXML
    private void onAdminLogin() {
        Platform.runLater(() -> SceneManager.switchScene("/fxml/login.fxml", "Admin Login", 480, 560));
    }

    @FXML
    private void onSalespersonLogin() {
        Platform.runLater(() -> SceneManager.switchScene("/fxml/login.fxml", "Salesperson Login", 480, 560));
    }
}

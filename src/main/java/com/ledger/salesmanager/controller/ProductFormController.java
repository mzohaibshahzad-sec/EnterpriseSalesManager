package com.ledger.salesmanager.controller;

import com.ledger.salesmanager.dao.CategoryDAO;
import com.ledger.salesmanager.dao.SupplierDAO;
import com.ledger.salesmanager.model.Category;
import com.ledger.salesmanager.model.Product;
import com.ledger.salesmanager.model.Supplier;
import com.ledger.salesmanager.service.ProductService;
import com.ledger.salesmanager.util.ValidationUtil;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;

public class ProductFormController {

    @FXML private Label titleLabel, imagePathLabel, profitPreviewLabel, errorLabel;
    @FXML private TextField nameField, brandField, skuField, wholesaleField, sellingField, totalQtyField, minStockField;
    @FXML private ComboBox<Category> categoryCombo;
    @FXML private ComboBox<Supplier> supplierCombo;
    @FXML private TextArea descriptionField;
    @FXML private ImageView imagePreview;

    private final ProductService productService = new ProductService();
    private final CategoryDAO categoryDAO = new CategoryDAO();
    private final SupplierDAO supplierDAO = new SupplierDAO();

    private Product editingProduct;
    private String chosenImagePath;
    private Runnable onSaved;

    public void setOnSaved(Runnable callback) { this.onSaved = callback; }

    @FXML
    public void initialize() {
        List<Category> categories = safe(categoryDAO::findAll);
        categoryCombo.setItems(FXCollections.observableArrayList(categories));

        List<Supplier> suppliers = safe(supplierDAO::findAll);
        supplierCombo.setItems(FXCollections.observableArrayList(suppliers));

        minStockField.setText("5");

        wholesaleField.textProperty().addListener((o, a, b) -> updateProfitPreview());
        sellingField.textProperty().addListener((o, a, b) -> updateProfitPreview());
    }

    private <T> List<T> safe(java.util.function.Supplier<List<T>> supplier) {
        try { return supplier.get(); } catch (Exception e) { return List.of(); }
    }

    public void setProduct(Product product) {
        this.editingProduct = product;
        if (product == null) return;

        titleLabel.setText("Edit Product");
        nameField.setText(product.getName());
        brandField.setText(product.getBrand());
        skuField.setText(product.getSku());
        wholesaleField.setText(product.getWholesalePrice().toPlainString());
        sellingField.setText(product.getSellingPrice().toPlainString());
        totalQtyField.setText(String.valueOf(product.getTotalQuantity()));
        minStockField.setText(String.valueOf(product.getMinStockLevel()));
        descriptionField.setText(product.getDescription());
        chosenImagePath = product.getImagePath();
        if (chosenImagePath != null) {
            imagePathLabel.setText(new File(chosenImagePath).getName());
            loadPreview(chosenImagePath);
        }

        categoryCombo.getItems().stream()
                .filter(c -> c.getId() == product.getCategoryId()).findFirst()
                .ifPresent(categoryCombo.getSelectionModel()::select);
        if (product.getSupplierId() != null) {
            supplierCombo.getItems().stream()
                    .filter(s -> s.getId() == product.getSupplierId()).findFirst()
                    .ifPresent(supplierCombo.getSelectionModel()::select);
        }
        updateProfitPreview();
    }

    @FXML
    private void onGenerateSku() {
        String prefix = nameField.getText() == null || nameField.getText().isBlank()
                ? "SKU" : nameField.getText().substring(0, Math.min(3, nameField.getText().length())).toUpperCase();
        skuField.setText(prefix + "-" + System.currentTimeMillis() % 100000);
    }

    @FXML
    private void onChooseImage() {
        FileChooser chooser = new FileChooser();
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg"));
        File file = chooser.showOpenDialog(currentStage());
        if (file != null) {
            chosenImagePath = file.getAbsolutePath();
            imagePathLabel.setText(file.getName());
            loadPreview(chosenImagePath);
        }
    }

    /** Image is entirely optional — this clears any selection back to "no image". */
    @FXML
    private void onRemoveImage() {
        chosenImagePath = null;
        imagePreview.setImage(null);
        imagePathLabel.setText("No image selected — this field is optional");
    }

    private void loadPreview(String path) {
        try {
            File f = new File(path);
            if (f.exists()) imagePreview.setImage(new Image(f.toURI().toString(), 64, 64, true, true));
        } catch (Exception e) {
            imagePreview.setImage(null);
        }
    }

    private void updateProfitPreview() {
        try {
            BigDecimal wholesale = new BigDecimal(wholesaleField.getText().isBlank() ? "0" : wholesaleField.getText());
            BigDecimal selling = new BigDecimal(sellingField.getText().isBlank() ? "0" : sellingField.getText());
            profitPreviewLabel.setText("Profit per item: Rs " + selling.subtract(wholesale).toPlainString());
        } catch (NumberFormatException e) {
            profitPreviewLabel.setText("");
        }
    }

    @FXML
    private void onSave() {
        errorLabel.setText("");
        if (ValidationUtil.isBlank(nameField.getText())) { errorLabel.setText("Product name likhein."); return; }
        if (!ValidationUtil.isNonNegativeNumber(wholesaleField.getText())) { errorLabel.setText("Wholesale price valid number ho."); return; }
        if (!ValidationUtil.isNonNegativeNumber(sellingField.getText())) { errorLabel.setText("Selling price valid number ho."); return; }
        if (!ValidationUtil.isNonNegativeNumber(totalQtyField.getText())) { errorLabel.setText("Total quantity valid number ho."); return; }

        try {
            Product p = editingProduct != null ? editingProduct : new Product();
            p.setName(nameField.getText().trim());
            p.setBrand(brandField.getText());
            p.setSku(ValidationUtil.isBlank(skuField.getText()) ? null : skuField.getText().trim());
            p.setWholesalePrice(new BigDecimal(wholesaleField.getText()));
            p.setSellingPrice(new BigDecimal(sellingField.getText()));
            p.setTotalQuantity((int) Double.parseDouble(totalQtyField.getText()));
            p.setMinStockLevel(minStockField.getText().isBlank() ? 5 : (int) Double.parseDouble(minStockField.getText()));
            p.setDescription(descriptionField.getText());
            p.setImagePath(persistImageIfNeeded());

            Category cat = categoryCombo.getValue();
            if (cat != null) p.setCategoryId(cat.getId());

            Supplier sup = supplierCombo.getValue();
            p.setSupplierId(sup != null ? sup.getId() : null);

            if (editingProduct != null) {
                productService.updateProduct(p);
            } else {
                productService.createProduct(p);
            }

            if (onSaved != null) onSaved.run();
            currentStage().close();
        } catch (ProductService.ValidationException ve) {
            errorLabel.setText(ve.getMessage());
        } catch (Exception e) {
            errorLabel.setText("Save nahi ho saka: " + e.getMessage());
        }
    }

    private String persistImageIfNeeded() {
        if (chosenImagePath == null) return null;
        // If it's already inside our app-data assets folder (editing an existing product), keep as-is.
        if (chosenImagePath.contains(".enterprise-sales-manager")) return chosenImagePath;
        try {
            Path source = Path.of(chosenImagePath);
            Path targetDir = Path.of(System.getProperty("user.home"), ".enterprise-sales-manager", "product-images");
            Files.createDirectories(targetDir);
            Path target = targetDir.resolve(System.currentTimeMillis() + "_" + source.getFileName());
            Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
            return target.toString();
        } catch (Exception e) {
            return chosenImagePath; // fall back to original path rather than losing the reference
        }
    }

    @FXML
    private void onCancel() {
        currentStage().close();
    }

    private Stage currentStage() {
        return (Stage) nameField.getScene().getWindow();
    }
}

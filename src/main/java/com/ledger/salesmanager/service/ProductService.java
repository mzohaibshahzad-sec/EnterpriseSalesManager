package com.ledger.salesmanager.service;

import com.ledger.salesmanager.dao.ActivityLogDAO;
import com.ledger.salesmanager.dao.ProductDAO;
import com.ledger.salesmanager.model.Product;
import com.ledger.salesmanager.model.User;
import com.ledger.salesmanager.util.SessionManager;
import com.ledger.salesmanager.util.ValidationUtil;

import java.math.BigDecimal;
import java.util.List;

public class ProductService {

    private final ProductDAO productDAO = new ProductDAO();
    private final ActivityLogDAO activityLogDAO = new ActivityLogDAO();

    public static class ValidationException extends RuntimeException {
        public ValidationException(String message) { super(message); }
    }

    public Product createProduct(Product p) {
        validate(p);
        Product saved = productDAO.insert(p);
        log("PRODUCT_CREATED", saved.getName() + " (SKU: " + saved.getSku() + ")");
        return saved;
    }

    public void updateProduct(Product p) {
        validate(p);
        productDAO.update(p);
        log("PRODUCT_UPDATED", p.getName());
    }

    public void deleteProduct(int id) {
        productDAO.findById(id).ifPresent(p -> log("PRODUCT_DELETED", p.getName()));
        productDAO.delete(id);
    }

    public List<Product> listAll() { return productDAO.findAllActive(); }

    public List<Product> search(String keyword) {
        return ValidationUtil.isBlank(keyword) ? listAll() : productDAO.search(keyword);
    }

    public List<Product> lowStockAlerts() { return productDAO.findLowStock(); }

    private void validate(Product p) {
        if (ValidationUtil.isBlank(p.getName())) throw new ValidationException("Product name zaroori hai.");
        if (p.getTotalQuantity() < 0) throw new ValidationException("Total quantity negative nahi ho sakti.");
        if (p.getWholesalePrice().compareTo(BigDecimal.ZERO) < 0)
            throw new ValidationException("Wholesale price negative nahi ho sakti.");
        if (p.getSellingPrice().compareTo(BigDecimal.ZERO) < 0)
            throw new ValidationException("Selling price negative nahi ho sakti.");
        if (p.getSoldQuantity() > p.getTotalQuantity())
            throw new ValidationException("Sold quantity total quantity se zyada nahi ho sakti.");
    }

    private void log(String action, String details) {
        User u = SessionManager.getInstance().getCurrentUser();
        activityLogDAO.log(u != null ? u.getId() : null, action, details);
    }
}

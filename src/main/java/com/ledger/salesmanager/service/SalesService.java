package com.ledger.salesmanager.service;

import com.ledger.salesmanager.dao.ActivityLogDAO;
import com.ledger.salesmanager.dao.ProductDAO;
import com.ledger.salesmanager.dao.SaleDAO;
import com.ledger.salesmanager.model.*;
import com.ledger.salesmanager.util.InvoiceNumberGenerator;
import com.ledger.salesmanager.util.SessionManager;

import java.math.BigDecimal;
import java.util.List;

public class SalesService {

    private final SaleDAO saleDAO = new SaleDAO();
    private final ProductDAO productDAO = new ProductDAO();
    private final ActivityLogDAO activityLogDAO = new ActivityLogDAO();

    public static class InsufficientStockException extends RuntimeException {
        public InsufficientStockException(String message) { super(message); }
    }

    /**
     * Validates every cart line against live available stock (defense in
     * depth — SaleDAO.recordSale also enforces this atomically at the DB
     * level), computes totals, stamps the invoice number and current
     * salesperson, then persists the whole transaction.
     */
    public Sale checkout(List<SaleItem> cartItems, BigDecimal discount, PaymentMethod method,
                          PaymentStatus status, String customerName, String notes) {
        if (cartItems == null || cartItems.isEmpty()) {
            throw new IllegalArgumentException("Cart khali hai — pehle products add karein.");
        }

        for (SaleItem item : cartItems) {
            Product product = productDAO.findById(item.getProductId())
                    .orElseThrow(() -> new IllegalStateException("Product no longer exists."));
            if (item.getQuantity() > product.getAvailableQuantity()) {
                throw new InsufficientStockException(
                        product.getName() + " mein sirf " + product.getAvailableQuantity() + " units available hain.");
            }
        }

        User current = SessionManager.getInstance().getCurrentUser();
        Sale sale = new Sale();
        sale.setInvoiceNumber(InvoiceNumberGenerator.generate());
        sale.setSalespersonId(current.getId());
        sale.setSalespersonName(current.getFullName());
        sale.setCustomerName(customerName);
        sale.setDiscount(discount == null ? BigDecimal.ZERO : discount);
        sale.setPaymentMethod(method);
        sale.setPaymentStatus(status);
        sale.setNotes(notes);
        sale.setItems(cartItems);
        sale.recalculateTotals();

        Sale saved = saleDAO.recordSale(sale);
        activityLogDAO.log(current.getId(), "SALE_RECORDED",
                "Invoice " + saved.getInvoiceNumber() + " — " + cartItems.size() + " item(s)");
        return saved;
    }

    public void voidSale(int saleId) {
        saleDAO.voidSale(saleId);
        User current = SessionManager.getInstance().getCurrentUser();
        activityLogDAO.log(current != null ? current.getId() : null, "SALE_VOIDED", "Sale id " + saleId);
    }

    public List<Sale> recentSales(int limit) { return saleDAO.findAll(limit); }

    public List<Sale> mySales(int salespersonId, int limit) { return saleDAO.findBySalesperson(salespersonId, limit); }

    public List<SaleItem> itemsFor(int saleId) { return saleDAO.findItems(saleId); }
}

package com.ledger.salesmanager.service;

import com.ledger.salesmanager.dao.ProductDAO;
import com.ledger.salesmanager.dao.SaleDAO;
import com.ledger.salesmanager.model.Product;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Pulls together everything the Dashboard screen needs in one place. */
public class DashboardService {

    private final ProductDAO productDAO = new ProductDAO();
    private final SaleDAO saleDAO = new SaleDAO();

    public static class Kpis {
        public int totalProducts;
        public long totalSalesToday;
        public BigDecimal revenueToday = BigDecimal.ZERO;
        public BigDecimal profitToday = BigDecimal.ZERO;
        public BigDecimal revenueThisMonth = BigDecimal.ZERO;
        public BigDecimal profitThisMonth = BigDecimal.ZERO;
        public int lowStockCount;
        public int outOfStockCount;
        public BigDecimal totalStockValueWholesale = BigDecimal.ZERO;
    }

    public Kpis loadKpis() {
        Kpis k = new Kpis();
        List<Product> products = productDAO.findAllActive();
        k.totalProducts = products.size();

        for (Product p : products) {
            if (p.isOutOfStock()) k.outOfStockCount++;
            else if (p.isLowStock()) k.lowStockCount++;
            k.totalStockValueWholesale = k.totalStockValueWholesale.add(
                    p.getWholesalePrice().multiply(BigDecimal.valueOf(p.getAvailableQuantity())));
        }

        LocalDate today = LocalDate.now();
        LocalDate monthStart = today.withDayOfMonth(1);

        k.revenueToday = saleDAO.sumRevenue(today, today);
        k.profitToday = saleDAO.sumProfit(today, today);
        k.revenueThisMonth = saleDAO.sumRevenue(monthStart, today);
        k.profitThisMonth = saleDAO.sumProfit(monthStart, today);

        return k;
    }

    public List<Object[]> revenueTrend(int days) {
        return saleDAO.revenueByDay(LocalDate.now().minusDays(days - 1L), LocalDate.now());
    }

    public List<Object[]> topProducts(int days, int limit) {
        return saleDAO.topProducts(LocalDate.now().minusDays(days - 1L), LocalDate.now(), limit);
    }

    public List<Object[]> salesBySalesperson(int days) {
        return saleDAO.salesBySalesperson(LocalDate.now().minusDays(days - 1L), LocalDate.now());
    }

    public List<Product> lowStockWatchlist() { return productDAO.findLowStock(); }
}

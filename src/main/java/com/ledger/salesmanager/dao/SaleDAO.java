package com.ledger.salesmanager.dao;

import com.ledger.salesmanager.config.DatabaseConnection;
import com.ledger.salesmanager.model.*;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class SaleDAO {

    private final ProductDAO productDAO = new ProductDAO();

    /**
     * Records a full sale transaction: inserts the sale header, all line
     * items, and decrements available stock — all inside one JDBC
     * transaction so a mid-way failure (e.g. insufficient stock on a
     * later line item) rolls everything back cleanly.
     */
    public Sale recordSale(Sale sale) {
        String saleSql = "INSERT INTO sales (invoice_number, salesperson_id, customer_id, customer_name, " +
                "subtotal, discount, total_amount, total_profit, payment_method, payment_status, notes) " +
                "VALUES (?,?,?,?,?,?,?,?,?,?,?)";
        String itemSql = "INSERT INTO sale_items (sale_id, product_id, product_name, quantity, unit_price, " +
                "unit_cost, line_total, line_profit) VALUES (?,?,?,?,?,?,?,?)";

        Connection con = null;
        try {
            con = DatabaseConnection.getConnection();
            con.setAutoCommit(false);

            int saleId;
            try (PreparedStatement ps = con.prepareStatement(saleSql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, sale.getInvoiceNumber());
                ps.setInt(2, sale.getSalespersonId());
                if (sale.getCustomerId() != null) ps.setInt(3, sale.getCustomerId()); else ps.setNull(3, Types.INTEGER);
                ps.setString(4, sale.getCustomerName());
                ps.setBigDecimal(5, sale.getSubtotal());
                ps.setBigDecimal(6, sale.getDiscount());
                ps.setBigDecimal(7, sale.getTotalAmount());
                ps.setBigDecimal(8, sale.getTotalProfit());
                ps.setString(9, sale.getPaymentMethod().name());
                ps.setString(10, sale.getPaymentStatus().name());
                ps.setString(11, sale.getNotes());
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    keys.next();
                    saleId = keys.getInt(1);
                }
            }
            sale.setId(saleId);

            for (SaleItem item : sale.getItems()) {
                // Enforce stock availability atomically for every line item.
                productDAO.applySale(con, item.getProductId(), item.getQuantity());

                try (PreparedStatement ps = con.prepareStatement(itemSql)) {
                    ps.setInt(1, saleId);
                    ps.setInt(2, item.getProductId());
                    ps.setString(3, item.getProductName());
                    ps.setInt(4, item.getQuantity());
                    ps.setBigDecimal(5, item.getUnitPrice());
                    ps.setBigDecimal(6, item.getUnitCost());
                    ps.setBigDecimal(7, item.getLineTotal());
                    ps.setBigDecimal(8, item.getLineProfit());
                    ps.executeUpdate();
                }
            }

            con.commit();
            return sale;
        } catch (SQLException e) {
            if (con != null) try { con.rollback(); } catch (SQLException ignored) {}
            throw new RuntimeException("Failed to record sale: " + e.getMessage(), e);
        } finally {
            if (con != null) try { con.setAutoCommit(true); con.close(); } catch (SQLException ignored) {}
        }
    }

    /** Voids a sale: restores stock for every line item, then deletes the sale (items cascade). */
    public void voidSale(int saleId) {
        Connection con = null;
        try {
            con = DatabaseConnection.getConnection();
            con.setAutoCommit(false);

            try (PreparedStatement ps = con.prepareStatement(
                    "SELECT product_id, quantity FROM sale_items WHERE sale_id=?")) {
                ps.setInt(1, saleId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        productDAO.revertSale(con, rs.getInt("product_id"), rs.getInt("quantity"));
                    }
                }
            }
            try (PreparedStatement ps = con.prepareStatement("DELETE FROM sales WHERE id=?")) {
                ps.setInt(1, saleId);
                ps.executeUpdate();
            }
            con.commit();
        } catch (SQLException e) {
            if (con != null) try { con.rollback(); } catch (SQLException ignored) {}
            throw new RuntimeException("Failed to void sale", e);
        } finally {
            if (con != null) try { con.setAutoCommit(true); con.close(); } catch (SQLException ignored) {}
        }
    }

    public List<Sale> findAll(int limit) {
        List<Sale> sales = new ArrayList<>();
        String sql = "SELECT s.*, u.full_name AS salesperson_name FROM sales s " +
                "JOIN users u ON s.salesperson_id = u.id ORDER BY s.sale_datetime DESC LIMIT ?";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) sales.add(mapHeader(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to list sales", e);
        }
        return sales;
    }

    public List<Sale> findBySalesperson(int salespersonId, int limit) {
        List<Sale> sales = new ArrayList<>();
        String sql = "SELECT s.*, u.full_name AS salesperson_name FROM sales s " +
                "JOIN users u ON s.salesperson_id = u.id WHERE s.salesperson_id=? " +
                "ORDER BY s.sale_datetime DESC LIMIT ?";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, salespersonId);
            ps.setInt(2, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) sales.add(mapHeader(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to list salesperson sales", e);
        }
        return sales;
    }

    public List<SaleItem> findItems(int saleId) {
        List<SaleItem> items = new ArrayList<>();
        String sql = "SELECT * FROM sale_items WHERE sale_id=?";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, saleId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    SaleItem item = new SaleItem();
                    item.setId(rs.getInt("id"));
                    item.setSaleId(rs.getInt("sale_id"));
                    item.setProductId(rs.getInt("product_id"));
                    item.setProductName(rs.getString("product_name"));
                    item.setQuantity(rs.getInt("quantity"));
                    item.setUnitPrice(rs.getBigDecimal("unit_price"));
                    item.setUnitCost(rs.getBigDecimal("unit_cost"));
                    item.setLineTotal(rs.getBigDecimal("line_total"));
                    item.setLineProfit(rs.getBigDecimal("line_profit"));
                    items.add(item);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load sale items", e);
        }
        return items;
    }

    // ---- Analytics aggregate queries ----

    public BigDecimal sumRevenue(LocalDate from, LocalDate to) {
        return sumColumn("total_amount", from, to);
    }

    public BigDecimal sumProfit(LocalDate from, LocalDate to) {
        return sumColumn("total_profit", from, to);
    }

    private BigDecimal sumColumn(String column, LocalDate from, LocalDate to) {
        String sql = "SELECT COALESCE(SUM(" + column + "),0) FROM sales " +
                "WHERE DATE(sale_datetime) BETWEEN ? AND ?";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(from));
            ps.setDate(2, Date.valueOf(to));
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getBigDecimal(1);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to aggregate sales", e);
        }
        return BigDecimal.ZERO;
    }

    /** Revenue and profit grouped by day, for the trend chart. */
    public List<Object[]> revenueByDay(LocalDate from, LocalDate to) {
        List<Object[]> rows = new ArrayList<>();
        String sql = "SELECT DATE(sale_datetime) d, SUM(total_amount) rev, SUM(total_profit) prof " +
                "FROM sales WHERE DATE(sale_datetime) BETWEEN ? AND ? GROUP BY DATE(sale_datetime) ORDER BY d";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(from));
            ps.setDate(2, Date.valueOf(to));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    rows.add(new Object[]{ rs.getDate("d").toLocalDate(), rs.getBigDecimal("rev"), rs.getBigDecimal("prof") });
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to build revenue trend", e);
        }
        return rows;
    }

    /** Top-selling products by quantity within a date range. */
    public List<Object[]> topProducts(LocalDate from, LocalDate to, int limit) {
        List<Object[]> rows = new ArrayList<>();
        String sql = "SELECT si.product_name, SUM(si.quantity) qty, SUM(si.line_total) revenue " +
                "FROM sale_items si JOIN sales s ON si.sale_id = s.id " +
                "WHERE DATE(s.sale_datetime) BETWEEN ? AND ? " +
                "GROUP BY si.product_name ORDER BY qty DESC LIMIT ?";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(from));
            ps.setDate(2, Date.valueOf(to));
            ps.setInt(3, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    rows.add(new Object[]{ rs.getString("product_name"), rs.getInt("qty"), rs.getBigDecimal("revenue") });
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to compute top products", e);
        }
        return rows;
    }

    /** Sales grouped by salesperson (for the "Sales by Salesperson" report). */
    public List<Object[]> salesBySalesperson(LocalDate from, LocalDate to) {
        List<Object[]> rows = new ArrayList<>();
        String sql = "SELECT u.full_name, COUNT(s.id) cnt, SUM(s.total_amount) rev, SUM(s.total_profit) prof " +
                "FROM sales s JOIN users u ON s.salesperson_id = u.id " +
                "WHERE DATE(s.sale_datetime) BETWEEN ? AND ? GROUP BY u.full_name ORDER BY rev DESC";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(from));
            ps.setDate(2, Date.valueOf(to));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    rows.add(new Object[]{ rs.getString("full_name"), rs.getInt("cnt"), rs.getBigDecimal("rev"), rs.getBigDecimal("prof") });
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to group sales by salesperson", e);
        }
        return rows;
    }

    private Sale mapHeader(ResultSet rs) throws SQLException {
        Sale s = new Sale();
        s.setId(rs.getInt("id"));
        s.setInvoiceNumber(rs.getString("invoice_number"));
        s.setSalespersonId(rs.getInt("salesperson_id"));
        s.setSalespersonName(rs.getString("salesperson_name"));
        int custId = rs.getInt("customer_id");
        s.setCustomerId(rs.wasNull() ? null : custId);
        s.setCustomerName(rs.getString("customer_name"));
        Timestamp dt = rs.getTimestamp("sale_datetime");
        if (dt != null) s.setSaleDateTime(dt.toLocalDateTime());
        s.setSubtotal(rs.getBigDecimal("subtotal"));
        s.setDiscount(rs.getBigDecimal("discount"));
        s.setTotalAmount(rs.getBigDecimal("total_amount"));
        s.setTotalProfit(rs.getBigDecimal("total_profit"));
        s.setPaymentMethod(PaymentMethod.valueOf(rs.getString("payment_method")));
        s.setPaymentStatus(PaymentStatus.valueOf(rs.getString("payment_status")));
        s.setNotes(rs.getString("notes"));
        return s;
    }
}

package com.ledger.salesmanager.dao;

import com.ledger.salesmanager.config.DatabaseConnection;
import com.ledger.salesmanager.model.Product;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ProductDAO {

    private static final String SELECT_BASE =
            "SELECT p.*, c.name AS category_name, s.name AS supplier_name " +
            "FROM products p " +
            "LEFT JOIN categories c ON p.category_id = c.id " +
            "LEFT JOIN suppliers s ON p.supplier_id = s.id ";

    public Product insert(Product p) {
        String sql = "INSERT INTO products (name, category_id, brand, sku, wholesale_price, " +
                "selling_price, total_quantity, sold_quantity, min_stock_level, supplier_id, " +
                "image_path, description, is_active) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bindUpsertParams(ps, p);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) p.setId(keys.getInt(1));
            }
            return p;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to create product", e);
        }
    }

    public void update(Product p) {
        String sql = "UPDATE products SET name=?, category_id=?, brand=?, sku=?, wholesale_price=?, " +
                "selling_price=?, total_quantity=?, sold_quantity=?, min_stock_level=?, supplier_id=?, " +
                "image_path=?, description=?, is_active=? WHERE id=?";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            bindUpsertParams(ps, p);
            ps.setInt(14, p.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update product", e);
        }
    }

    private void bindUpsertParams(PreparedStatement ps, Product p) throws SQLException {
        ps.setString(1, p.getName());
        if (p.getCategoryId() > 0) ps.setInt(2, p.getCategoryId()); else ps.setNull(2, Types.INTEGER);
        ps.setString(3, p.getBrand());
        ps.setString(4, p.getSku());
        ps.setBigDecimal(5, p.getWholesalePrice());
        ps.setBigDecimal(6, p.getSellingPrice());
        ps.setInt(7, p.getTotalQuantity());
        ps.setInt(8, p.getSoldQuantity());
        ps.setInt(9, p.getMinStockLevel());
        if (p.getSupplierId() != null) ps.setInt(10, p.getSupplierId()); else ps.setNull(10, Types.INTEGER);
        ps.setString(11, p.getImagePath());
        ps.setString(12, p.getDescription());
        ps.setBoolean(13, p.isActive());
    }

    /** Adds quantitySold to sold_quantity atomically — call inside the sale transaction. */
    public void applySale(Connection con, int productId, int quantitySold) throws SQLException {
        String sql = "UPDATE products SET sold_quantity = sold_quantity + ? WHERE id = ? " +
                "AND (total_quantity - sold_quantity) >= ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, quantitySold);
            ps.setInt(2, productId);
            ps.setInt(3, quantitySold);
            int rows = ps.executeUpdate();
            if (rows == 0) {
                throw new SQLException("Insufficient stock for product id " + productId);
            }
        }
    }

    /** Reverts sold_quantity — used when a sale is voided/deleted. */
    public void revertSale(Connection con, int productId, int quantity) throws SQLException {
        String sql = "UPDATE products SET sold_quantity = GREATEST(0, sold_quantity - ?) WHERE id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, quantity);
            ps.setInt(2, productId);
            ps.executeUpdate();
        }
    }

    public void delete(int id) {
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement("UPDATE products SET is_active=FALSE WHERE id=?")) {
            // Soft-delete: keeps sale_items foreign key history intact for reports.
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete product", e);
        }
    }

    public void hardDelete(int id) {
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement("DELETE FROM products WHERE id=?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to permanently delete product", e);
        }
    }

    public Optional<Product> findById(int id) {
        String sql = SELECT_BASE + "WHERE p.id = ?";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(map(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find product", e);
        }
        return Optional.empty();
    }

    public List<Product> findAllActive() {
        List<Product> list = new ArrayList<>();
        String sql = SELECT_BASE + "WHERE p.is_active = TRUE ORDER BY p.name";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(map(rs));
        } catch (SQLException e) {
            throw new RuntimeException("Failed to list products", e);
        }
        return list;
    }

    public List<Product> search(String keyword) {
        List<Product> list = new ArrayList<>();
        String sql = SELECT_BASE + "WHERE p.is_active = TRUE AND (p.name LIKE ? OR p.sku LIKE ? " +
                "OR p.brand LIKE ?) ORDER BY p.name";
        String like = "%" + keyword + "%";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, like);
            ps.setString(2, like);
            ps.setString(3, like);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to search products", e);
        }
        return list;
    }

    public List<Product> findLowStock() {
        List<Product> list = new ArrayList<>();
        String sql = SELECT_BASE + "WHERE p.is_active = TRUE AND (p.total_quantity - p.sold_quantity) <= p.min_stock_level " +
                "ORDER BY (p.total_quantity - p.sold_quantity) ASC";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(map(rs));
        } catch (SQLException e) {
            throw new RuntimeException("Failed to list low stock products", e);
        }
        return list;
    }

    private Product map(ResultSet rs) throws SQLException {
        Product p = new Product();
        p.setId(rs.getInt("id"));
        p.setName(rs.getString("name"));
        int catId = rs.getInt("category_id");
        p.setCategoryId(rs.wasNull() ? 0 : catId);
        p.setCategoryName(rs.getString("category_name"));
        p.setBrand(rs.getString("brand"));
        p.setSku(rs.getString("sku"));
        p.setWholesalePrice(nz(rs.getBigDecimal("wholesale_price")));
        p.setSellingPrice(nz(rs.getBigDecimal("selling_price")));
        p.setTotalQuantity(rs.getInt("total_quantity"));
        p.setSoldQuantity(rs.getInt("sold_quantity"));
        p.setMinStockLevel(rs.getInt("min_stock_level"));
        int supId = rs.getInt("supplier_id");
        p.setSupplierId(rs.wasNull() ? null : supId);
        p.setSupplierName(rs.getString("supplier_name"));
        p.setImagePath(rs.getString("image_path"));
        p.setDescription(rs.getString("description"));
        p.setActive(rs.getBoolean("is_active"));
        Timestamp created = rs.getTimestamp("created_at");
        if (created != null) p.setCreatedAt(created.toLocalDateTime());
        Timestamp updated = rs.getTimestamp("updated_at");
        if (updated != null) p.setUpdatedAt(updated.toLocalDateTime());
        return p;
    }

    private BigDecimal nz(BigDecimal v) { return v == null ? BigDecimal.ZERO : v; }
}

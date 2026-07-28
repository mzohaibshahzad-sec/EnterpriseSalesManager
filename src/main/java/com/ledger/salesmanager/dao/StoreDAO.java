package com.ledger.salesmanager.dao;

import com.ledger.salesmanager.config.DatabaseConnection;
import com.ledger.salesmanager.model.StoreInfo;

import java.sql.*;

public class StoreDAO {

    public StoreInfo getStoreInfo() {
        String sql = "SELECT * FROM store_settings ORDER BY id LIMIT 1";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return map(rs);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load store settings", e);
        }
        return null;
    }

    public boolean isSetupCompleted() {
        StoreInfo info = getStoreInfo();
        return info != null && info.isSetupCompleted();
    }

    public void saveStoreInfo(StoreInfo info) {
        String sql = "INSERT INTO store_settings (store_name, store_logo_path, store_address, " +
                "store_contact, store_email, currency_symbol, theme, setup_completed) " +
                "VALUES (?,?,?,?,?,?,?,TRUE)";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, info.getStoreName());
            ps.setString(2, info.getStoreLogoPath());
            ps.setString(3, info.getStoreAddress());
            ps.setString(4, info.getStoreContact());
            ps.setString(5, info.getStoreEmail());
            ps.setString(6, info.getCurrencySymbol());
            ps.setString(7, info.getTheme());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save store settings", e);
        }
    }

    public void updateStoreInfo(StoreInfo info) {
        String sql = "UPDATE store_settings SET store_name=?, store_logo_path=?, store_address=?, " +
                "store_contact=?, store_email=?, currency_symbol=?, theme=? WHERE id=?";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, info.getStoreName());
            ps.setString(2, info.getStoreLogoPath());
            ps.setString(3, info.getStoreAddress());
            ps.setString(4, info.getStoreContact());
            ps.setString(5, info.getStoreEmail());
            ps.setString(6, info.getCurrencySymbol());
            ps.setString(7, info.getTheme());
            ps.setInt(8, info.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update store settings", e);
        }
    }

    private StoreInfo map(ResultSet rs) throws SQLException {
        StoreInfo s = new StoreInfo();
        s.setId(rs.getInt("id"));
        s.setStoreName(rs.getString("store_name"));
        s.setStoreLogoPath(rs.getString("store_logo_path"));
        s.setStoreAddress(rs.getString("store_address"));
        s.setStoreContact(rs.getString("store_contact"));
        s.setStoreEmail(rs.getString("store_email"));
        s.setCurrencySymbol(rs.getString("currency_symbol"));
        s.setTheme(rs.getString("theme"));
        s.setSetupCompleted(rs.getBoolean("setup_completed"));
        return s;
    }
}

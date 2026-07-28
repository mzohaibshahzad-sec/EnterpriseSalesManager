package com.ledger.salesmanager.controller;

import com.ledger.salesmanager.model.Role;
import com.ledger.salesmanager.model.User;
import com.ledger.salesmanager.service.AuthService;
import com.ledger.salesmanager.util.SceneManager;
import com.ledger.salesmanager.util.SessionManager;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;

import java.io.IOException;
import java.net.URL;

/**
 * Persistent shell for the authenticated area. The sidebar is filtered
 * by role per the spec's permission matrix; sub-views are swapped in
 * and out of {@code contentArea} rather than opening new windows, which
 * keeps navigation instant and lets the sidebar stay put.
 */
public class MainShellController {

    @FXML private StackPane contentArea;
    @FXML private Button navDashboard, navProducts, navSales, navUsers, navReports, navAuditLog, navSettings, navProfile;
    @FXML private Label currentUserLabel, currentRoleLabel;

    private final AuthService authService = new AuthService();

    @FXML
    public void initialize() {
        User user = SessionManager.getInstance().getCurrentUser();
        if (user == null) {
            // Defensive: shouldn't happen, but never show the shell unauthenticated.
            SceneManager.switchScene("/fxml/viewer-dashboard.fxml", "Enterprise Sales Manager", 1200, 780);
            return;
        }

        currentUserLabel.setText(user.getFullName());
        currentRoleLabel.setText(user.getRole() == Role.OWNER ? "Owner" : "Salesperson");

        boolean isOwner = user.getRole() == Role.OWNER;
        navUsers.setVisible(isOwner); navUsers.setManaged(isOwner);
        navReports.setVisible(isOwner); navReports.setManaged(isOwner);
        navAuditLog.setVisible(isOwner); navAuditLog.setManaged(isOwner);
        navSettings.setVisible(isOwner); navSettings.setManaged(isOwner);

        showDashboard();
    }

    @FXML private void showDashboard() { loadView("/fxml/dashboard-view.fxml"); }
    @FXML private void showProducts()  { loadView("/fxml/products-view.fxml"); }
    @FXML private void showSales()     { loadView("/fxml/sales-view.fxml"); }
    @FXML private void showUsers()     { if (AuthService.canManageUsers()) loadView("/fxml/users-view.fxml"); }
    @FXML private void showReports()   { if (AuthService.canAccessReports()) loadView("/fxml/reports-view.fxml"); }
    @FXML private void showAuditLog()  { if (AuthService.canViewAuditLog()) loadView("/fxml/audit-log-view.fxml"); }
    @FXML private void showSettings()  { if (AuthService.canAccessSettings()) loadView("/fxml/settings-view.fxml"); }
    @FXML private void showProfile()   { loadView("/fxml/profile-view.fxml"); }

    @FXML
    private void onSignOut() {
        authService.logout();
        SceneManager.switchScene("/fxml/viewer-dashboard.fxml", "Enterprise Sales Manager", 1200, 780);
    }

    private void loadView(String fxmlPath) {
        try {
            URL url = getClass().getResource(fxmlPath);
            if (url == null) throw new IOException("Missing FXML: " + fxmlPath);
            FXMLLoader loader = new FXMLLoader(url);
            Parent view = loader.load();
            contentArea.getChildren().setAll(view);
        } catch (IOException e) {
            throw new RuntimeException("Could not load view: " + fxmlPath, e);
        }
    }
}

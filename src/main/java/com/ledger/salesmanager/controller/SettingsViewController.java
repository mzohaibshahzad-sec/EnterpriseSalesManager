package com.ledger.salesmanager.controller;

import com.ledger.salesmanager.config.AppConfig;
import com.ledger.salesmanager.dao.StoreDAO;
import com.ledger.salesmanager.model.StoreInfo;
import com.ledger.salesmanager.util.SceneManager;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;

public class SettingsViewController {

    @FXML private TextField storeNameField, storeAddressField, storeContactField, storeEmailField;
    @FXML private TextField smtpUsernameField;
    @FXML private PasswordField smtpPasswordField;
    @FXML private ToggleButton darkModeToggle;
    @FXML private Label statusLabel;

    private final StoreDAO storeDAO = new StoreDAO();
    private StoreInfo storeInfo;

    @FXML
    public void initialize() {
        storeInfo = storeDAO.getStoreInfo();
        if (storeInfo != null) {
            storeNameField.setText(storeInfo.getStoreName());
            storeAddressField.setText(storeInfo.getStoreAddress());
            storeContactField.setText(storeInfo.getStoreContact());
            storeEmailField.setText(storeInfo.getStoreEmail());
        }

        AppConfig cfg = AppConfig.getInstance();
        smtpUsernameField.setText(cfg.getSmtpUsername());
        darkModeToggle.setSelected("DARK".equalsIgnoreCase(cfg.getTheme()));
    }

    @FXML
    private void onSaveStoreInfo() {
        if (storeInfo == null) storeInfo = new StoreInfo();
        storeInfo.setStoreName(storeNameField.getText());
        storeInfo.setStoreAddress(storeAddressField.getText());
        storeInfo.setStoreContact(storeContactField.getText());
        storeInfo.setStoreEmail(storeEmailField.getText());
        storeDAO.updateStoreInfo(storeInfo);
        statusLabel.setText("Store info save ho gayi.");
    }

    @FXML
    private void onSaveSmtp() {
        AppConfig.getInstance().setSmtpCredentials(smtpUsernameField.getText().trim(), smtpPasswordField.getText());
        statusLabel.setText("Email settings save ho gayi.");
    }

    @FXML
    private void onToggleTheme() {
        String theme = darkModeToggle.isSelected() ? "DARK" : "LIGHT";
        AppConfig.getInstance().setTheme(theme);
        SceneManager.applyTheme(storeNameField.getScene());
        statusLabel.setText("Theme change ho gayi.");
    }
}

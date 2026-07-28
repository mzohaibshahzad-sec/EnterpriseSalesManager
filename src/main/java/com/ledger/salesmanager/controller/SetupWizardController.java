package com.ledger.salesmanager.controller;

import com.ledger.salesmanager.config.AppConfig;
import com.ledger.salesmanager.config.DatabaseConnection;
import com.ledger.salesmanager.dao.StoreDAO;
import com.ledger.salesmanager.dao.UserDAO;
import com.ledger.salesmanager.model.Role;
import com.ledger.salesmanager.model.StoreInfo;
import com.ledger.salesmanager.model.User;
import com.ledger.salesmanager.util.PasswordUtil;
import com.ledger.salesmanager.util.SceneManager;
import com.ledger.salesmanager.util.ValidationUtil;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.FileChooser;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public class SetupWizardController {

    @FXML private Label stepLabel;
    @FXML private Label errorLabel;
    @FXML private Button backButton, nextButton, finishButton;

    // Step 1
    @FXML private javafx.scene.layout.VBox step1Pane, step2Pane, step3Pane;
    @FXML private TextField storeNameField, storeAddressField, storeContactField, storeEmailField;
    @FXML private Label logoPathLabel;
    private String chosenLogoPath;

    // Step 2
    @FXML private TextField ownerNameField, ownerUsernameField, ownerGmailField;
    @FXML private PasswordField ownerPasswordField, ownerConfirmPasswordField;

    // Step 3
    @FXML private TextField dbHostField, dbPortField, dbNameField, dbUserField, smtpUsernameField;
    @FXML private PasswordField dbPasswordField, smtpPasswordField;
    @FXML private Label dbTestResultLabel;

    private int currentStep = 1;
    private boolean dbVerified = false;

    @FXML
    public void initialize() {
        dbHostField.setText("localhost");
        dbPortField.setText("3306");
        dbNameField.setText("sales_management");
        dbUserField.setText("root");
    }

    @FXML
    private void onChooseLogo() {
        FileChooser chooser = new FileChooser();
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg"));
        File file = chooser.showOpenDialog(SceneManager.getPrimaryStage());
        if (file != null) {
            chosenLogoPath = file.getAbsolutePath();
            logoPathLabel.setText(file.getName());
        }
    }

    @FXML
    private void onNext() {
        clearError();
        if (currentStep == 1) {
            if (ValidationUtil.isBlank(storeNameField.getText())) {
                showError("Store name zaroori hai.");
                return;
            }
            goToStep(2);
        } else if (currentStep == 2) {
            if (!validateOwnerStep()) return;
            goToStep(3);
        }
    }

    @FXML
    private void onBack() {
        clearError();
        if (currentStep > 1) goToStep(currentStep - 1);
    }

    private boolean validateOwnerStep() {
        if (ValidationUtil.isBlank(ownerNameField.getText())) { showError("Owner ka naam likhein."); return false; }
        if (!ValidationUtil.isValidUsername(ownerUsernameField.getText())) {
            showError("Username 3-30 characters ka ho, sirf letters/numbers/underscore.");
            return false;
        }
        if (!ValidationUtil.isGmailAddress(ownerGmailField.getText())) {
            showError("2FA ke liye ek valid Gmail address zaroori hai.");
            return false;
        }
        if (!ValidationUtil.isStrongPassword(ownerPasswordField.getText())) {
            showError("Password kam az kam 8 characters ka ho.");
            return false;
        }
        if (!ownerPasswordField.getText().equals(ownerConfirmPasswordField.getText())) {
            showError("Password match nahi ho raha.");
            return false;
        }
        return true;
    }

    @FXML
    private void onTestDbConnection() {
        boolean ok = DatabaseConnection.testConnection(
                dbHostField.getText().trim(), dbPortField.getText().trim(),
                dbNameField.getText().trim(), dbUserField.getText().trim(), dbPasswordField.getText());
        dbVerified = ok;
        dbTestResultLabel.setText(ok
                ? "Connection successful."
                : "Connection failed - check that MySQL is running and schema.sql has been imported.");
        dbTestResultLabel.setStyle(ok ? "-fx-text-fill: #34D3A6;" : "-fx-text-fill: #F0665B;");
    }

    @FXML
    private void onFinish() {
        clearError();
        if (!dbVerified) {
            showError("Pehle 'Test Connection' se database verify karein.");
            return;
        }

        AppConfig cfg = AppConfig.getInstance();
        cfg.setDatabaseCredentials(
                dbHostField.getText().trim(), dbPortField.getText().trim(),
                dbNameField.getText().trim(), dbUserField.getText().trim(), dbPasswordField.getText());

        if (!ValidationUtil.isBlank(smtpUsernameField.getText())) {
            cfg.setSmtpCredentials(smtpUsernameField.getText().trim(), smtpPasswordField.getText());
        }

        try {
            String savedLogoPath = copyLogoIntoAppData();

            StoreInfo store = new StoreInfo();
            store.setStoreName(storeNameField.getText().trim());
            store.setStoreAddress(storeAddressField.getText().trim());
            store.setStoreContact(storeContactField.getText().trim());
            store.setStoreEmail(storeEmailField.getText().trim());
            store.setStoreLogoPath(savedLogoPath);
            new StoreDAO().saveStoreInfo(store);

            User owner = new User(
                    ownerNameField.getText().trim(),
                    ownerUsernameField.getText().trim().toLowerCase(),
                    ownerGmailField.getText().trim(),
                    null,
                    PasswordUtil.hash(ownerPasswordField.getText()),
                    Role.OWNER
            );
            new UserDAO().insert(owner);

            cfg.markSetupCompleted();

            SceneManager.switchScene("/fxml/viewer-dashboard.fxml", "Enterprise Sales Manager", 1200, 780);
        } catch (Exception e) {
            showError("Setup save nahi ho saka: " + e.getMessage());
        }
    }

    private String copyLogoIntoAppData() {
        if (chosenLogoPath == null) return null;
        try {
            Path source = Path.of(chosenLogoPath);
            Path targetDir = Path.of(System.getProperty("user.home"), ".enterprise-sales-manager", "assets");
            Files.createDirectories(targetDir);
            Path target = targetDir.resolve("store-logo" + extensionOf(source.toString()));
            Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
            return target.toString();
        } catch (Exception e) {
            return null;
        }
    }

    private String extensionOf(String path) {
        int dot = path.lastIndexOf('.');
        return dot >= 0 ? path.substring(dot) : "";
    }

    private void goToStep(int step) {
        currentStep = step;
        step1Pane.setVisible(step == 1); step1Pane.setManaged(step == 1);
        step2Pane.setVisible(step == 2); step2Pane.setManaged(step == 2);
        step3Pane.setVisible(step == 3); step3Pane.setManaged(step == 3);

        backButton.setVisible(step > 1);
        nextButton.setVisible(step < 3);
        finishButton.setVisible(step == 3);
        finishButton.setManaged(step == 3);

        String[] titles = {"Store Information", "Owner Account", "Database & Email Setup"};
        stepLabel.setText("Step " + step + " of 3 - " + titles[step - 1]);
    }

    private void showError(String msg) { errorLabel.setText(msg); }
    private void clearError() { errorLabel.setText(""); }
}

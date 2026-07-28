package com.ledger.salesmanager.controller;

import com.ledger.salesmanager.dao.ActivityLogDAO;
import com.ledger.salesmanager.dao.UserDAO;
import com.ledger.salesmanager.model.Role;
import com.ledger.salesmanager.model.User;
import com.ledger.salesmanager.util.PasswordUtil;
import com.ledger.salesmanager.util.SessionManager;
import com.ledger.salesmanager.util.ValidationUtil;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

public class UserFormController {

    @FXML private TextField nameField, usernameField, gmailField, phoneField;
    @FXML private PasswordField passwordField;
    @FXML private ComboBox<Role> roleCombo;
    @FXML private Label errorLabel;

    private final UserDAO userDAO = new UserDAO();
    private final ActivityLogDAO activityLogDAO = new ActivityLogDAO();
    private Runnable onSaved;

    public void setOnSaved(Runnable callback) { this.onSaved = callback; }

    @FXML
    public void initialize() {
        roleCombo.setItems(FXCollections.observableArrayList(Role.values()));
        roleCombo.getSelectionModel().select(Role.SALESPERSON);
    }

    @FXML
    private void onSave() {
        errorLabel.setText("");
        if (ValidationUtil.isBlank(nameField.getText())) { errorLabel.setText("Naam likhein."); return; }
        if (!ValidationUtil.isValidUsername(usernameField.getText())) {
            errorLabel.setText("Username 3-30 characters ka ho.");
            return;
        }
        if (userDAO.findByUsername(usernameField.getText().trim()).isPresent()) {
            errorLabel.setText("Ye username pehle se mojood hai.");
            return;
        }
        if (!ValidationUtil.isGmailAddress(gmailField.getText())) {
            errorLabel.setText("Valid Gmail address zaroori hai.");
            return;
        }
        if (!ValidationUtil.isStrongPassword(passwordField.getText())) {
            errorLabel.setText("Password kam az kam 8 characters ka ho.");
            return;
        }

        User user = new User(
                nameField.getText().trim(),
                usernameField.getText().trim().toLowerCase(),
                gmailField.getText().trim(),
                phoneField.getText(),
                PasswordUtil.hash(passwordField.getText()),
                roleCombo.getValue()
        );

        try {
            userDAO.insert(user);
            activityLogDAO.log(SessionManager.getInstance().getCurrentUser().getId(),
                    "USER_CREATED", user.getUsername() + " (" + user.getRole() + ")");
            if (onSaved != null) onSaved.run();
            currentStage().close();
        } catch (Exception e) {
            errorLabel.setText("User create nahi ho saka: " + e.getMessage());
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

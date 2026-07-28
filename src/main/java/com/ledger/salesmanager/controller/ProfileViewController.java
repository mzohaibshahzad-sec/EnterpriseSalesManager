package com.ledger.salesmanager.controller;

import com.ledger.salesmanager.dao.UserDAO;
import com.ledger.salesmanager.model.User;
import com.ledger.salesmanager.util.PasswordUtil;
import com.ledger.salesmanager.util.SessionManager;
import com.ledger.salesmanager.util.ValidationUtil;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

import java.time.format.DateTimeFormatter;

public class ProfileViewController {

    @FXML private Label initialsLabel, nameLabel, usernameLabel, roleLabel, lastLoginLabel;
    @FXML private TextField nameField, gmailField, phoneField;
    @FXML private PasswordField newPasswordField, confirmPasswordField;
    @FXML private Label errorLabel, successLabel;

    private final UserDAO userDAO = new UserDAO();
    private static final DateTimeFormatter DT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @FXML
    public void initialize() {
        User user = SessionManager.getInstance().getCurrentUser();
        initialsLabel.setText(user.initials());
        nameLabel.setText(user.getFullName());
        usernameLabel.setText("@" + user.getUsername());
        roleLabel.setText(user.getRole().name());
        lastLoginLabel.setText(user.getLastLogin() != null ? "Last login: " + user.getLastLogin().format(DT) : "First login");

        nameField.setText(user.getFullName());
        gmailField.setText(user.getGmail());
        phoneField.setText(user.getPhone());
    }

    @FXML
    private void onSave() {
        errorLabel.setText("");
        successLabel.setText("");
        User user = SessionManager.getInstance().getCurrentUser();

        if (ValidationUtil.isBlank(nameField.getText())) { errorLabel.setText("Naam khali nahi ho sakta."); return; }
        if (!ValidationUtil.isGmailAddress(gmailField.getText())) { errorLabel.setText("Valid Gmail address likhein."); return; }

        String newPassword = newPasswordField.getText();
        if (!newPassword.isBlank()) {
            if (!ValidationUtil.isStrongPassword(newPassword)) {
                errorLabel.setText("Naya password kam az kam 8 characters ka ho.");
                return;
            }
            if (!newPassword.equals(confirmPasswordField.getText())) {
                errorLabel.setText("Password match nahi kar raha.");
                return;
            }
        }

        user.setFullName(nameField.getText().trim());
        user.setGmail(gmailField.getText().trim());
        user.setPhone(phoneField.getText());
        userDAO.update(user);

        if (!newPassword.isBlank()) {
            userDAO.updatePassword(user.getId(), PasswordUtil.hash(newPassword));
        }

        nameLabel.setText(user.getFullName());
        newPasswordField.clear();
        confirmPasswordField.clear();
        successLabel.setText("Profile update ho gayi.");
    }
}

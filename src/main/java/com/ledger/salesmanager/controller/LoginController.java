package com.ledger.salesmanager.controller;

import com.ledger.salesmanager.model.Role;
import com.ledger.salesmanager.model.User;
import com.ledger.salesmanager.service.AuthService;
import com.ledger.salesmanager.service.OtpService;
import com.ledger.salesmanager.util.SceneManager;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class LoginController {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label errorLabel;

    private final AuthService authService = new AuthService();
    private final OtpService otpService = new OtpService();

    @FXML
    private void onLogin() {
        errorLabel.setText("");
        String username = usernameField.getText();
        String password = passwordField.getText();

        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            errorLabel.setText("Username aur password dono zaroori hain.");
            return;
        }

        AuthService.AuthResult result = authService.verifyCredentials(username, password);
        if (!result.success) {
            errorLabel.setText(result.errorMessage);
            return;
        }

        User user = result.user;
        if (result.requiresOtp) {
            // Owner path: mandatory 2FA email OTP before session actually starts.
            OtpService.SendResult sendResult = otpService.sendLoginOtp(user);
            switch (sendResult) {
                case SENT -> {
                    OtpVerificationController.setPendingUser(user);
                    SceneManager.switchScene("/fxml/otp-verification.fxml", "Verify Your Identity", 440, 420);
                }
                case RATE_LIMITED -> errorLabel.setText("Bohot zyada OTP requests ho gayi hain. Thodi der baad try karein.");
                case SMTP_NOT_CONFIGURED -> errorLabel.setText(
                        "Email OTP configure nahi hai. Settings mein Gmail App Password add karein.");
                case SEND_FAILED -> errorLabel.setText("OTP email send nahi ho saka. Internet/SMTP settings check karein.");
            }
        } else {
            // Salesperson path: no 2FA required.
            authService.finalizeLogin(user);
            SceneManager.switchScene("/fxml/main-shell.fxml", "Enterprise Sales Manager", 1280, 800);
        }
    }

    @FXML
    private void onBackToViewer() {
        SceneManager.switchScene("/fxml/viewer-dashboard.fxml", "Enterprise Sales Manager", 1200, 780);
    }
}

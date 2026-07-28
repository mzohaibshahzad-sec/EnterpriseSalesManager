package com.ledger.salesmanager.controller;

import com.ledger.salesmanager.model.User;
import com.ledger.salesmanager.service.AuthService;
import com.ledger.salesmanager.service.OtpService;
import com.ledger.salesmanager.util.SceneManager;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.util.Duration;

public class OtpVerificationController {

    // Simple static hand-off since JavaFX FXML controllers are instantiated
    // by the loader with no-arg constructors — a small in-memory pending
    // state is the pragmatic way to pass the "who is verifying" context.
    private static User pendingUser;
    public static void setPendingUser(User user) { pendingUser = user; }

    @FXML private Label infoLabel;
    @FXML private Label errorLabel;
    @FXML private Label timerLabel;
    @FXML private TextField otpField;
    @FXML private Button resendButton;

    private final AuthService authService = new AuthService();
    private final OtpService otpService = new OtpService();

    private Timeline countdown;
    private int secondsRemaining = 300; // 5 minutes, matches OtpService expiry

    @FXML
    public void initialize() {
        if (pendingUser != null) {
            infoLabel.setText("A 6-digit code was sent to " + maskEmail(pendingUser.getGmail()));
        }
        resendButton.setDisable(true);
        startCountdown();
    }

    private void startCountdown() {
        countdown = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            secondsRemaining--;
            int m = secondsRemaining / 60, s = secondsRemaining % 60;
            timerLabel.setText(String.format("Code expires in %02d:%02d", Math.max(m, 0), Math.max(s, 0)));
            if (secondsRemaining <= 0) {
                countdown.stop();
                resendButton.setDisable(false);
                timerLabel.setText("Code expired — request a new one.");
            }
        }));
        countdown.setCycleCount(300);
        countdown.play();
    }

    @FXML
    private void onVerify() {
        errorLabel.setText("");
        if (pendingUser == null) {
            errorLabel.setText("Session expired, dobara login karein.");
            return;
        }
        String code = otpField.getText();
        if (code == null || code.isBlank()) {
            errorLabel.setText("OTP code darj karein.");
            return;
        }
        boolean valid = otpService.verifyOtp(pendingUser, code);
        if (!valid) {
            errorLabel.setText("Code ghalat ya expire ho chuka hai.");
            return;
        }
        authService.finalizeLogin(pendingUser);
        if (countdown != null) countdown.stop();
        pendingUser = null;
        SceneManager.switchScene("/fxml/main-shell.fxml", "Enterprise Sales Manager", 1280, 800);
    }

    @FXML
    private void onResend() {
        if (pendingUser == null) return;
        OtpService.SendResult result = otpService.sendLoginOtp(pendingUser);
        if (result == OtpService.SendResult.SENT) {
            secondsRemaining = 300;
            resendButton.setDisable(true);
            errorLabel.setText("");
            startCountdown();
        } else {
            errorLabel.setText("Naya code send nahi ho saka. Thodi der baad try karein.");
        }
    }

    @FXML
    private void onCancel() {
        pendingUser = null;
        if (countdown != null) countdown.stop();
        SceneManager.switchScene("/fxml/viewer-dashboard.fxml", "Enterprise Sales Manager", 1200, 780);
    }

    private String maskEmail(String email) {
        int at = email.indexOf('@');
        if (at <= 1) return email;
        return email.charAt(0) + "***" + email.substring(at - 1);
    }
}

package com.ledger.salesmanager.util;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Popup;
import javafx.stage.Window;
import javafx.util.Duration;

/** Small helpers for confirmation dialogs and animated toast notifications. */
public class AlertUtil {

    public static void error(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message);
        alert.setHeaderText(title);
        alert.showAndWait();
    }

    public static void info(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, message);
        alert.setHeaderText(title);
        alert.showAndWait();
    }

    public static boolean confirm(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, message, ButtonType.YES, ButtonType.NO);
        alert.setHeaderText(title);
        return alert.showAndWait().filter(b -> b == ButtonType.YES).isPresent();
    }

    /** Non-blocking bottom-right toast, auto-dismisses after ~2.6s. Matches the app's dark theme. */
    public static void toast(Window owner, String message, boolean isError) {
        Popup popup = new Popup();
        Label label = new Label(message);
        label.getStyleClass().add(isError ? "toast-error" : "toast-success");
        label.setStyle("-fx-background-color: #1B2440; -fx-text-fill: white; -fx-padding: 12 18; " +
                "-fx-background-radius: 10; -fx-border-color: " + (isError ? "#F0665B" : "#34D3A6") +
                "; -fx-border-radius: 10; -fx-font-size: 13px;");
        StackPane pane = new StackPane(label);
        pane.setAlignment(Pos.CENTER);
        popup.getContent().add(pane);
        popup.setAutoFix(true);
        popup.show(owner, owner.getX() + owner.getWidth() - 340, owner.getY() + owner.getHeight() - 100);

        Timeline timeline = new Timeline(new KeyFrame(Duration.seconds(2.6), e -> popup.hide()));
        timeline.play();
    }
}

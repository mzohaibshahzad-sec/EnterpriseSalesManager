package com.ledger.salesmanager.util;

import com.ledger.salesmanager.config.AppConfig;
import javafx.animation.FadeTransition;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;
import java.net.URL;

/** Centralizes FXML loading, scene switching, and theme (dark/light) application. */
public class SceneManager {

    private static Stage primaryStage;

    public static void setPrimaryStage(Stage stage) { primaryStage = stage; }
    public static Stage getPrimaryStage() { return primaryStage; }

    public static void switchScene(String fxmlPath, String title, int width, int height) {
        try {
            URL url = SceneManager.class.getResource(fxmlPath);
            if (url == null) throw new IOException("FXML not found on classpath: " + fxmlPath);
            FXMLLoader loader = new FXMLLoader(url);
            Parent root = loader.load();
            Scene scene = new Scene(root, width, height);
            applyTheme(scene);
            primaryStage.setTitle(title);
            primaryStage.setScene(scene);
            fadeIn(root);
            primaryStage.show();
        } catch (IOException e) {
            throw new RuntimeException("Failed to load screen: " + fxmlPath, e);
        }
    }

    public static void applyTheme(Scene scene) {
        String theme = AppConfig.getInstance().getTheme();
        String cssFile = "DARK".equalsIgnoreCase(theme) ? "/css/theme-dark.css" : "/css/theme-light.css";
        URL css = SceneManager.class.getResource(cssFile);
        if (css != null) {
            scene.getStylesheets().clear();
            scene.getStylesheets().add(css.toExternalForm());
        }
    }

    private static void fadeIn(Node node) {
        FadeTransition ft = new FadeTransition(Duration.millis(280), node);
        ft.setFromValue(0);
        ft.setToValue(1);
        ft.play();
    }
}

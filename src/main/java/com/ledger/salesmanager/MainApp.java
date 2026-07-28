package com.ledger.salesmanager;

import com.ledger.salesmanager.config.AppConfig;
import com.ledger.salesmanager.dao.StoreDAO;
import com.ledger.salesmanager.util.SceneManager;
import com.ledger.salesmanager.util.SessionManager;
import javafx.animation.PauseTransition;
import javafx.application.Application;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;

/**
 * Application entry point.
 *
 * Startup flow (per spec):
 *   1. Show a branded splash screen.
 *   2. If setup.completed is false in local config -> Setup Wizard.
 *   3. Otherwise -> open directly to the public Viewer Dashboard
 *      (no login required). Admin Login / Salesperson Login buttons on
 *      that screen lead into the authenticated area.
 *   4. An idle-timeout watcher runs for the lifetime of the app and
 *      auto-logs-out an authenticated session after inactivity.
 */
public class MainApp extends Application {

    @Override
    public void start(Stage primaryStage) {
        SceneManager.setPrimaryStage(primaryStage);
        primaryStage.setMinWidth(1024);
        primaryStage.setMinHeight(700);

        showSplash(primaryStage);
        startIdleWatcher();
    }

    private void showSplash(Stage stage) {
        VBox root = new VBox(14);
        root.setAlignment(Pos.CENTER);
        root.setStyle("-fx-background-color: #0B1220;");
        Label title = new Label("Enterprise Sales Manager");
        title.setStyle("-fx-text-fill: #C9A961; -fx-font-size: 26px; -fx-font-weight: bold;");
        Label subtitle = new Label("Loading your business…");
        subtitle.setStyle("-fx-text-fill: #8A93AC; -fx-font-size: 13px;");
        ProgressIndicator spinner = new ProgressIndicator();
        spinner.setMaxSize(46, 46);
        root.getChildren().addAll(title, spinner, subtitle);

        Scene scene = new Scene(root, 1024, 700);
        stage.setTitle("Enterprise Sales Manager");
        stage.setScene(scene);
        stage.show();

        PauseTransition pause = new PauseTransition(Duration.seconds(1.4));
        pause.setOnFinished(e -> routeToStartScreen());
        pause.play();
    }

    private void routeToStartScreen() {
        boolean setupDone;
        try {
            setupDone = AppConfig.getInstance().isSetupCompleted() && new StoreDAO().isSetupCompleted();
        } catch (Exception dbUnreachable) {
            // DB not reachable yet (e.g. credentials not saved / MySQL not running) -> force setup wizard,
            // which lets the user (re)enter working database credentials.
            setupDone = false;
        }

        if (!setupDone) {
            SceneManager.switchScene("/fxml/setup-wizard.fxml", "Setup Wizard — Enterprise Sales Manager", 760, 640);
        } else {
            SceneManager.switchScene("/fxml/viewer-dashboard.fxml", "Enterprise Sales Manager", 1200, 780);
        }
    }

    /** Ticks once a minute; auto-logs-out an idle authenticated session. */
    private void startIdleWatcher() {
        SessionManager.getInstance().setOnAutoLogout(v ->
                SceneManager.switchScene("/fxml/viewer-dashboard.fxml", "Enterprise Sales Manager", 1200, 780));

        javafx.animation.Timeline watcher = new javafx.animation.Timeline(
                new javafx.animation.KeyFrame(Duration.seconds(30),
                        e -> SessionManager.getInstance().checkAndHandleIdleTimeout()));
        watcher.setCycleCount(javafx.animation.Timeline.INDEFINITE);
        watcher.play();
    }

    public static void main(String[] args) {
        launch(args);
    }
}

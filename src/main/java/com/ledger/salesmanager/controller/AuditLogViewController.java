package com.ledger.salesmanager.controller;

import com.ledger.salesmanager.dao.ActivityLogDAO;
import com.ledger.salesmanager.model.ActivityLog;
import com.ledger.salesmanager.util.AlertUtil;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Read-only, Owner-only view over activity_logs — covers the spec's
 * "Audit Logs" requirement. Every login, sale, product/user change,
 * and password reset already gets written here by the various
 * services (AuthService, ProductService, SalesService, UsersView, etc.);
 * this screen just makes that history searchable and exportable.
 */
public class AuditLogViewController {

    @FXML private TextField userFilterField;
    @FXML private ComboBox<String> actionFilterCombo;
    @FXML private DatePicker fromDatePicker, toDatePicker;
    @FXML private Label resultCountLabel;
    @FXML private TableView<ActivityLog> logTable;
    @FXML private TableColumn<ActivityLog, String> colTimestamp, colUser, colAction, colDetails;

    private final ActivityLogDAO activityLogDAO = new ActivityLogDAO();
    private final ObservableList<ActivityLog> data = FXCollections.observableArrayList();
    private static final DateTimeFormatter DT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @FXML
    public void initialize() {
        colTimestamp.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(
                c.getValue().getCreatedAt() != null ? c.getValue().getCreatedAt().format(DT) : ""));
        colUser.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(
                c.getValue().getUserName() != null ? c.getValue().getUserName() : "System"));
        colAction.setCellValueFactory(new PropertyValueFactory<>("action"));
        colDetails.setCellValueFactory(new PropertyValueFactory<>("details"));

        List<String> actions = safeLoadActions();
        actionFilterCombo.setItems(FXCollections.observableArrayList(actions));
        actionFilterCombo.getItems().add(0, "ALL");
        actionFilterCombo.getSelectionModel().select("ALL");

        toDatePicker.setValue(LocalDate.now());
        fromDatePicker.setValue(LocalDate.now().minusDays(29));

        logTable.setItems(data);
        refresh();
    }

    private List<String> safeLoadActions() {
        try {
            return activityLogDAO.distinctActions();
        } catch (Exception e) {
            return List.of();
        }
    }

    @FXML
    private void onApplyFilters() {
        refresh();
    }

    @FXML
    private void onResetFilters() {
        userFilterField.clear();
        actionFilterCombo.getSelectionModel().select("ALL");
        toDatePicker.setValue(LocalDate.now());
        fromDatePicker.setValue(LocalDate.now().minusDays(29));
        refresh();
    }

    private void refresh() {
        List<ActivityLog> results = activityLogDAO.search(
                userFilterField.getText(),
                actionFilterCombo.getValue(),
                fromDatePicker.getValue(),
                toDatePicker.getValue(),
                1000
        );
        data.setAll(results);
        resultCountLabel.setText(results.size() + " entries" + (results.size() == 1000 ? " (showing latest 1000)" : ""));
    }

    @FXML
    private void onExportCsv() {
        FileChooser chooser = new FileChooser();
        chooser.setInitialFileName("audit_log.csv");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV", "*.csv"));
        File file = chooser.showSaveDialog(logTable.getScene().getWindow());
        if (file == null) return;

        StringBuilder sb = new StringBuilder("Timestamp,User,Action,Details\n");
        for (ActivityLog log : data) {
            sb.append(csv(log.getCreatedAt() != null ? log.getCreatedAt().format(DT) : "")).append(',')
              .append(csv(log.getUserName())).append(',')
              .append(csv(log.getAction())).append(',')
              .append(csv(log.getDetails())).append('\n');
        }
        try {
            Files.writeString(file.toPath(), sb.toString());
            AlertUtil.info("Exported", "Audit log save ho gaya: " + file.getName());
        } catch (IOException e) {
            AlertUtil.error("Export Failed", e.getMessage());
        }
    }

    private String csv(String value) {
        if (value == null) return "";
        String escaped = value.replace("\"", "\"\"");
        return escaped.contains(",") ? "\"" + escaped + "\"" : escaped;
    }
}

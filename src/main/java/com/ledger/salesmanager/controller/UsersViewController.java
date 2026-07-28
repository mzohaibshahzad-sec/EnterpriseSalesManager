package com.ledger.salesmanager.controller;

import com.ledger.salesmanager.dao.ActivityLogDAO;
import com.ledger.salesmanager.dao.UserDAO;
import com.ledger.salesmanager.model.Role;
import com.ledger.salesmanager.model.User;
import com.ledger.salesmanager.util.AlertUtil;
import com.ledger.salesmanager.util.PasswordUtil;
import com.ledger.salesmanager.util.SceneManager;
import com.ledger.salesmanager.util.SessionManager;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.net.URL;
import java.time.format.DateTimeFormatter;

public class UsersViewController {

    @FXML private TableView<User> userTable;
    @FXML private TableColumn<User, String> colName, colUsername, colGmail, colRole, colStatus, colLastLogin;
    @FXML private TableColumn<User, Void> colActions;

    private final UserDAO userDAO = new UserDAO();
    private final ActivityLogDAO activityLogDAO = new ActivityLogDAO();
    private final ObservableList<User> data = FXCollections.observableArrayList();
    private static final DateTimeFormatter DT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @FXML
    public void initialize() {
        colName.setCellValueFactory(new PropertyValueFactory<>("fullName"));
        colUsername.setCellValueFactory(new PropertyValueFactory<>("username"));
        colGmail.setCellValueFactory(new PropertyValueFactory<>("gmail"));
        colRole.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().getRole().name()));
        colStatus.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(
                c.getValue().isActive() ? "Active" : "Deactivated"));
        colLastLogin.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(
                c.getValue().getLastLogin() != null ? c.getValue().getLastLogin().format(DT) : "Never"));

        colActions.setCellFactory(col -> new TableCell<>() {
            private final Button toggleBtn = new Button();
            private final Button resetBtn = new Button("Reset Password");
            private final Button deleteBtn = new Button("Delete");
            private final HBox box = new HBox(6, toggleBtn, resetBtn, deleteBtn);
            {
                deleteBtn.getStyleClass().add("danger-button");
                toggleBtn.setOnAction(e -> onToggleActive(getTableView().getItems().get(getIndex())));
                resetBtn.setOnAction(e -> onResetPassword(getTableView().getItems().get(getIndex())));
                deleteBtn.setOnAction(e -> onDelete(getTableView().getItems().get(getIndex())));
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) { setGraphic(null); return; }
                User u = getTableView().getItems().get(getIndex());
                toggleBtn.setText(u.isActive() ? "Deactivate" : "Activate");
                boolean isSelf = u.getId() == SessionManager.getInstance().getCurrentUser().getId();
                deleteBtn.setDisable(isSelf);
                toggleBtn.setDisable(isSelf);
                setGraphic(box);
            }
        });

        userTable.setItems(data);
        refresh();
    }

    private void refresh() {
        data.setAll(userDAO.findAll());
    }

    @FXML
    private void onAddUser() {
        try {
            URL url = getClass().getResource("/fxml/user-form-dialog.fxml");
            FXMLLoader loader = new FXMLLoader(url);
            Parent root = loader.load();
            UserFormController controller = loader.getController();
            controller.setOnSaved(this::refresh);

            Stage dialog = new Stage();
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.setTitle("Add User");
            javafx.scene.Scene scene = new javafx.scene.Scene(root);
            SceneManager.applyTheme(scene);
            dialog.setScene(scene);
            dialog.showAndWait();
        } catch (Exception e) {
            AlertUtil.error("Error", "User form open nahi ho saka: " + e.getMessage());
        }
    }

    private void onToggleActive(User user) {
        user.setActive(!user.isActive());
        userDAO.update(user);
        activityLogDAO.log(SessionManager.getInstance().getCurrentUser().getId(),
                user.isActive() ? "USER_ACTIVATED" : "USER_DEACTIVATED", user.getUsername());
        refresh();
    }

    private void onResetPassword(User user) {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Reset Password");
        dialog.setHeaderText("New password for " + user.getFullName());
        dialog.setContentText("New password:");
        dialog.showAndWait().ifPresent(newPassword -> {
            if (newPassword.length() < 8) {
                AlertUtil.error("Invalid Password", "Password kam az kam 8 characters ka ho.");
                return;
            }
            userDAO.updatePassword(user.getId(), PasswordUtil.hash(newPassword));
            activityLogDAO.log(SessionManager.getInstance().getCurrentUser().getId(),
                    "PASSWORD_RESET", "Reset password for " + user.getUsername());
            AlertUtil.info("Done", "Password reset ho gaya.");
        });
    }

    private void onDelete(User user) {
        if (user.getRole() == Role.OWNER) {
            long ownerCount = userDAO.countByRole(Role.OWNER);
            if (ownerCount <= 1) {
                AlertUtil.error("Not Allowed", "Kam az kam ek Owner account zaroori hai.");
                return;
            }
        }
        boolean confirmed = AlertUtil.confirm("Delete User?", user.getFullName() + " ko delete karna chahte hain?");
        if (confirmed) {
            userDAO.delete(user.getId());
            refresh();
        }
    }
}

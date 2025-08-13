package billsplit;

import java.sql.*;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import java.util.ArrayList;
import java.util.List;

public class Main extends Application {

    static class Person {

        String name;
        double amount;
        Person(String name, double amount) {
            this.name = name;
            this.amount = amount;
        }
        @Override
        public String toString() {
            return name + " owes $" + String.format("%.2f", amount);
        }
    }
    private final List<Person> people = new ArrayList<>();
    private final List<SessionRecord> allSessions = new ArrayList<>();
    private final ListView<String> listView = new ListView<>();

    public static void main(String[] args) {
        launch();
    }

    @Override
    public void start(Stage stage) {
        showLoginPage(stage);
    }

    public void showMainApp(Stage stage) {
        Label billLabel = new Label("Total Bill");
        billLabel.setStyle("-fx-text-fill: #f1f1f1;");

        TextField billField = new TextField();
        billField.setPromptText("Enter total amount");
        billField.setStyle("""
        -fx-text-fill: white;
        -fx-prompt-text-fill: #bbbbbb;
        -fx-font-size: 14px;
        -fx-background-color: #3a3a3a;
        -fx-background-radius: 8;
        -fx-border-radius: 8;
        -fx-border-color: #555;
        -fx-padding: 8;
    """);

        Label nameLabel = new Label("Person's Name");
        nameLabel.setStyle("-fx-text-fill: #f1f1f1;");

        TextField nameField = new TextField();
        nameField.setPromptText("e.g. Alice");
        nameField.setStyle("""
        -fx-text-fill: white;
        -fx-prompt-text-fill: #bbbbbb;
        -fx-font-size: 14px;
        -fx-background-color: #3a3a3a;
        -fx-background-radius: 8;
        -fx-border-radius: 8;
        -fx-border-color: #555;
        -fx-padding: 8;
    """);

        Button addPersonButton = new Button("Add Person");
        styleButton(addPersonButton);

        Label peopleCountLabel = new Label("People added: 0");
        peopleCountLabel.setStyle("-fx-text-fill: #f1f1f1;");

        Button splitButton = new Button("Split Bill");
        styleButton(splitButton);

        Label resultLabel = new Label();
        resultLabel.setStyle("-fx-text-fill: #f1f1f1;");

        listView.setPrefHeight(200);
        listView.setStyle("-fx-control-inner-background: #333; -fx-text-fill: #f1f1f1;");

        addPersonButton.setOnAction(e -> {
            String name = nameField.getText().trim();
            if (!name.isEmpty()) {
                people.add(new Person(name, 0));
                nameField.clear();
                peopleCountLabel.setText("People added: " + people.size());
                resultLabel.setText("");
            }
        });

        splitButton.setOnAction(e -> {
            try {
                double total = Double.parseDouble(billField.getText());
                if (people.isEmpty()) {
                    resultLabel.setText("Please add at least one person.");
                    return;
                }
                openSplitWindow(total);
            } catch (NumberFormatException ex) {
                resultLabel.setText("Enter a valid total bill amount.");
            }
        });

        Button historyButton = new Button("View History");
        styleButton(historyButton);

        historyButton.setOnAction(e -> {
            StringBuilder historyText = new StringBuilder();
            for (SessionRecord session : allSessions) {
                historyText.append("Session on ").append(session.timestamp).append(":\n");
                for (Person p : session.entries) {
                    historyText.append(" - ").append(p.toString()).append("\n");
                }
                historyText.append("\n");
            }

            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("History");
            alert.setHeaderText("Past Sessions");
            alert.setContentText(historyText.toString());
            alert.getDialogPane().setStyle("-fx-background-color: #2e2e2e;");
            alert.getDialogPane().lookup(".content.label").setStyle("-fx-text-fill: white;");
            alert.getDialogPane().setPrefWidth(400);
            alert.showAndWait();
        });

        VBox inputSection = new VBox(10, billLabel, billField, nameLabel, nameField, addPersonButton, peopleCountLabel);
        inputSection.setAlignment(Pos.CENTER_LEFT);
        inputSection.setPadding(new Insets(10));
        HBox buttonRow = new HBox(15, splitButton, historyButton);
        buttonRow.setAlignment(Pos.CENTER);
        VBox actionSection = new VBox(10, buttonRow, resultLabel, listView);
        actionSection.setPadding(new Insets(10));
        VBox root = new VBox(30, inputSection, actionSection);
        root.setPadding(new Insets(20));
        root.setStyle("-fx-background-color: #1e1e1e;");
        Scene scene = new Scene(root, 400, 550);
        stage.setTitle("Bill Splitter");
        stage.setScene(scene);
        stage.setOnCloseRequest(e -> saveHistory());
    }
    private void openSplitWindow(double total) {
        Stage popup = new Stage();
        popup.setTitle("Choose Split Method");

        ToggleGroup group = new ToggleGroup();
        RadioButton evenBtn = new RadioButton("Split Evenly");
        RadioButton customBtn = new RadioButton("Custom Split");
        evenBtn.setToggleGroup(group);
        customBtn.setToggleGroup(group);

        Button confirmBtn = new Button("Confirm");
        styleButton(confirmBtn);

        VBox root = new VBox(10, evenBtn, customBtn, confirmBtn);
        root.setPadding(new Insets(15));
        root.setStyle("-fx-background-color: #2e2e2e;");
        evenBtn.setStyle("-fx-text-fill: white;");
        customBtn.setStyle("-fx-text-fill: white;");

        confirmBtn.setOnAction(ev -> {
            RadioButton selected = (RadioButton) group.getSelectedToggle();
            if (selected == evenBtn) {
                double share = total / people.size();
                listView.getItems().clear();
                for (Person person : people) {
                    person.amount = share;
                    listView.getItems().add(person.toString());
                }
            } else if (selected == customBtn) {
                popup.close();
                openCustomSplitWindow(total);
                return;
            }
            popup.close();
        });

        Scene scene = new Scene(root, 250, 150);
        popup.setScene(scene);
        popup.show();
    }
    private void openCustomSplitWindow(double total) {
        Stage popup = new Stage();
        popup.setTitle("Custom Split");

        VBox inputArea = new VBox(10);
        List<TextField> amountFields = new ArrayList<>();

        for (Person p : people) {
            HBox row = new HBox(10);
            Label name = new Label(p.name);
            name.setPrefWidth(100);
            name.setStyle("-fx-text-fill: white;");
            TextField amountField = new TextField();
            amountField.setPromptText("Amount for " + p.name);
            amountFields.add(amountField);
            row.getChildren().addAll(name, amountField);
            inputArea.getChildren().add(row);
        }

        Button confirmBtn = new Button("Confirm");
        styleButton(confirmBtn);
        Label errorLabel = new Label();
        errorLabel.setStyle("-fx-text-fill: red;");

        confirmBtn.setOnAction(e -> {
            double sum = 0;
            List<Double> values = new ArrayList<>();
            try {
                for (TextField field : amountFields) {
                    double val = Double.parseDouble(field.getText());
                    values.add(val);
                    sum += val;
                }

                if (Math.abs(sum - total) > 0.01) {
                    errorLabel.setText("Sum must equal total (" + total + ")");
                    return;
                }

                for (int i = 0; i < people.size(); i++) {
                    people.get(i).amount = values.get(i);
                }

                listView.getItems().clear();
                for (Person person : people) {
                    listView.getItems().add(person.toString());
                }
                popup.close();
            } catch (NumberFormatException ex) {
                errorLabel.setText("Enter valid amounts for all people.");
            }
        });

        VBox root = new VBox(10, inputArea, errorLabel, confirmBtn);
        root.setPadding(new Insets(15));
        root.setStyle("-fx-background-color: #2e2e2e;");

        Scene scene = new Scene(root, 300, 60 + people.size() * 40);
        popup.setScene(scene);
        popup.show();
    }
    private void styleButton(Button btn) {
        btn.setStyle(
                "-fx-background-color: #4CAF50;" +
                        "-fx-text-fill: white;" +
                        "-fx-background-radius: 20px;" +
                        "-fx-padding: 8 16;" +
                        "-fx-font-weight: bold;"
        );
    }

    private void saveHistory() {
        if (loggedInUser == null || people.isEmpty()) return;

        String query = "INSERT INTO history (username, person_name, amount, timestamp) VALUES (?, ?, ?, NOW())";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            for (Person p : people) {
                stmt.setString(1, loggedInUser);
                stmt.setString(2, p.name);
                stmt.setDouble(3, p.amount);
                stmt.executeUpdate();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void loadHistory() {
        if (loggedInUser == null) return;

        String query = "SELECT person_name, amount, timestamp FROM history WHERE username = ? ORDER BY timestamp DESC";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, loggedInUser);
            ResultSet rs = stmt.executeQuery();

            allSessions.clear();
            while (rs.next()) {
                String timestamp = rs.getTimestamp("timestamp").toString();
                List<Person> singleEntry = new ArrayList<>();
                singleEntry.add(new Person(rs.getString("person_name"), rs.getDouble("amount")));
                allSessions.add(new SessionRecord(timestamp, singleEntry));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    static class SessionRecord {
        String timestamp;
        List<Person> entries;

        SessionRecord(String timestamp, List<Person> entries) {
            this.timestamp = timestamp;
            this.entries = entries;
        }
    }

    class User {
        String username;
        String password;

        User(String username, String password) {
            this.username = username;
            this.password = password;
        }
    }

    private void showLoginPage(Stage stage) {
        TextField usernameField = new TextField();
        usernameField.setPromptText("Username");
        usernameField.setStyle("""
        -fx-text-fill: white;
        -fx-prompt-text-fill: #bbbbbb;
        -fx-font-size: 14px;
        -fx-background-color: #3a3a3a;
        -fx-background-radius: 8;
        -fx-border-radius: 8;
        -fx-border-color: #555;
        -fx-padding: 8;
    """);

        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Password");
        passwordField.setStyle("""
        -fx-text-fill: white;
        -fx-prompt-text-fill: #bbbbbb;
        -fx-font-size: 14px;
        -fx-background-color: #3a3a3a;
        -fx-background-radius: 8;
        -fx-border-radius: 8;
        -fx-border-color: #555;
        -fx-padding: 8;
    """);

        Button loginButton = new Button("Login");
        loginButton.setStyle("""
        -fx-background-color: #1e90ff;
        -fx-text-fill: white;
        -fx-font-size: 14px;
        -fx-background-radius: 8;
        -fx-cursor: hand;
        -fx-padding: 8 16;
    """);

        Button signupButton = new Button("Sign Up");
        signupButton.setStyle("""
        -fx-background-color: #555555;
        -fx-text-fill: white;
        -fx-font-size: 14px;
        -fx-background-radius: 8;
        -fx-cursor: hand;
        -fx-padding: 8 16;
    """);
        Label statusLabel = new Label();
        statusLabel.setStyle("-fx-text-fill: #ff4d4d; -fx-font-size: 12px;");

        loginButton.setOnAction(e -> {
            String user = usernameField.getText().trim();
            String pass = passwordField.getText().trim();

            if (user.isEmpty() || pass.isEmpty()) {
                statusLabel.setText("Username and password cannot be empty.");
                return;
            }

            if (validateUser(user, pass)) {
                loggedInUser = user;
                loadHistory();
                showMainApp(stage);
            } else {
                statusLabel.setText("Invalid username or password.");
            }
        });


        signupButton.setOnAction(e -> {
            String user = usernameField.getText().trim();
            String pass = passwordField.getText().trim();

            if (user.isEmpty() || pass.isEmpty()) {
                statusLabel.setText("Username and password cannot be empty.");
                return;
            }

            if (registerUser(user, pass)) {
                statusLabel.setText("Account created. You can now login.");
            } else {
                statusLabel.setText("Username already exists.");
            }
        });


        VBox layout = new VBox(15, usernameField, passwordField, loginButton, signupButton, statusLabel);
        layout.setPadding(new Insets(30));
        layout.setAlignment(Pos.CENTER);
        layout.setStyle("-fx-background-color: #1e1e1e;"); // Dark background
        Scene scene = new Scene(layout, 350, 300);
        stage.setScene(scene);
        stage.setTitle("Login");
        stage.show();
    }

    private String loggedInUser = null;

    private boolean registerUser(String username, String password) {
        String query = "INSERT INTO users (username, password) VALUES (?, ?)";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, username);
            stmt.setString(2, password);
            stmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            return false;
        }
    }

    private boolean validateUser(String username, String password) {
        String query = "SELECT * FROM users WHERE username = ? AND password = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, username);
            stmt.setString(2, password);
            ResultSet rs = stmt.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

}

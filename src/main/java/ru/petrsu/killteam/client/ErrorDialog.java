package ru.petrsu.killteam.client;

import javafx.application.Platform;
import javafx.scene.control.*;
import javafx.stage.Modality;

public class ErrorDialog {

    private static final String DEFAULT_MSG = "Сервер запретил действие";
    private static final String OFFLINE_MSG = "Сервер недоступен. Проверь, запущен ли он.";

    public static void show(Exception e) {
        e.printStackTrace();
        Platform.runLater(() -> showStyled(extractMessage(e)));
    }

    public static void show(String message) {
        Platform.runLater(() -> showStyled(message));
    }

    private static void showStyled(String message) {
        Alert alert = new Alert(Alert.AlertType.NONE);
        alert.setTitle("Ошибка");
        alert.setHeaderText("Не удалось выполнить действие");

        TextArea area = new TextArea(message);
        area.setEditable(false);
        area.setWrapText(true);
        area.setPrefRowCount(3);
        area.setPrefColumnCount(46);
        area.setMaxWidth(500);
        area.setStyle(
                "-fx-control-inner-background: #0f0b06;" +
                        "-fx-text-fill: #e8d9b0;" +
                        "-fx-font-size: 13px;" +
                        "-fx-border-color: #5c4a1f;" +
                        "-fx-border-radius: 3;"
        );
        alert.getDialogPane().setContent(area);
        alert.getDialogPane().setPrefWidth(520);
        alert.getDialogPane().setMaxWidth(560);
        alert.getButtonTypes().setAll(ButtonType.OK);
        alert.initModality(Modality.APPLICATION_MODAL);
        alert.setResizable(false);

        DialogStyler.apply(alert);
        alert.getDialogPane().getStyleClass().add("error-dialog");

        alert.showAndWait();
    }

    private static String extractMessage(Exception e) {
        if (e == null) return DEFAULT_MSG;

        Throwable t = e;
        while (t != null) {
            if (t instanceof java.io.IOException) return OFFLINE_MSG;
            String name = t.getClass().getName().toLowerCase();
            if (name.contains("connectexception")
                    || name.contains("sockettimeout")
                    || name.contains("unknownhost")
                    || name.contains("httptimeout")) {
                return OFFLINE_MSG;
            }
            t = t.getCause();
        }

        String raw = e.getMessage();
        if (raw == null || raw.isBlank()) return DEFAULT_MSG;

        if (raw.contains("\"message\"")) {
            String parsed = parseJsonMessage(raw);
            if (parsed != null && !parsed.isBlank()) return parsed;
        }

        if (raw.startsWith("HTTP ")) {
            int colon = raw.indexOf(": ");
            if (colon > 0) {
                String after = raw.substring(colon + 2).trim();
                if (after.startsWith("{")) {
                    String parsed = parseJsonMessage(after);
                    if (parsed != null && !parsed.isBlank()) return parsed;
                } else if (!after.isBlank()) {
                    return after;
                }
                return DEFAULT_MSG;
            }
        }

        return raw;
    }

    private static String parseJsonMessage(String json) {
        try {
            int key = json.indexOf("\"message\"");
            if (key < 0) return null;
            int colon = json.indexOf(':', key);
            if (colon < 0) return null;
            int start = json.indexOf('"', colon + 1);
            if (start < 0) return null;
            StringBuilder sb = new StringBuilder();
            boolean esc = false;
            for (int i = start + 1; i < json.length(); i++) {
                char c = json.charAt(i);
                if (esc) { sb.append(c); esc = false; continue; }
                if (c == '\\') { esc = true; continue; }
                if (c == '"') break;
                sb.append(c);
            }
            return sb.toString();
        } catch (Exception ex) {
            return null;
        }
    }
}
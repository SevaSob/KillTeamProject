package ru.petrsu.killteam.client;

import javafx.scene.control.Alert;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;

public class DialogStyler {

    public static void apply(Dialog<?> dialog) {
        DialogPane pane = dialog.getDialogPane();
        addStylesheet(pane);
        pane.getStyleClass().add("app-dialog");
        pane.setStyle("-fx-background-color: #1c160d;");
    }

    public static void apply(Alert alert) {
        DialogPane pane = alert.getDialogPane();
        addStylesheet(pane);
        pane.getStyleClass().add("app-dialog");
        pane.setStyle("-fx-background-color: #1c160d;");
    }

    private static void addStylesheet(DialogPane pane) {
        try {
            var res = DialogStyler.class.getResource("/styles.css");
            if (res != null) pane.getStylesheets().add(res.toExternalForm());
        } catch (Exception ignored) {}
    }
}
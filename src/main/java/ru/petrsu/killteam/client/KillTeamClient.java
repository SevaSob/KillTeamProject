package ru.petrsu.killteam.client;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class KillTeamClient extends Application {

    @Override
    public void start(Stage stage) {
        MainView2 view = new MainView2();
        Scene scene = new Scene(view, 1250, 720);
        scene.getStylesheets().add(
                getClass().getResource("/styles.css").toExternalForm()
        );
        stage.setTitle("Kill Team — Client");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
package ru.petrsu.killteam.client;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import ru.petrsu.killteam.dto.KillTeamDto;
import ru.petrsu.killteam.dto.OperativeDto;
import ru.petrsu.killteam.dto.WeaponDto;

public class MainView extends BorderPane {

    private final ApiClient api = new ApiClient();

    private final ListView<KillTeamDto> teamList = new ListView<>();
    private final ListView<OperativeDto> operativeList = new ListView<>();
    private final TextArea details = new TextArea();

    public MainView() {
        details.setEditable(false);
        details.setWrapText(true);

        teamList.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(KillTeamDto item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.name());
            }
        });

        operativeList.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(OperativeDto item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.name());
            }
        });

        teamList.getSelectionModel().selectedItemProperty().addListener((obs, o, team) -> {
            operativeList.getItems().clear();
            details.clear();
            if (team == null) return;
            try {
                operativeList.getItems().setAll(api.getOperatives(team.id()));
                details.setText(team.name() + "\nФракция: " + team.factionName()
                        + "\n\nПРАВИЛА:\n" + team.rulesText());
            } catch (Exception e) {
                details.setText("Ошибка: " + e.getMessage());
            }
        });

        operativeList.getSelectionModel().selectedItemProperty().addListener((obs, o, op) -> {
            if (op == null) return;
            StringBuilder sb = new StringBuilder();
            sb.append(op.name()).append("\n");
            sb.append("APL: ").append(op.apl())
                    .append("  Move: ").append(op.move()).append("\"")
                    .append("  Save: ").append(op.save())
                    .append("  Wounds: ").append(op.wounds()).append("\n\n");
            sb.append("ОРУЖИЕ:\n");
            for (WeaponDto w : op.weapons()) {
                sb.append(String.format("%-32s ATK %d  HIT %s  DMG %s  WR %s%n",
                        w.name(), w.atk(), w.hit(), w.dmg(), w.special()));
            }
            sb.append("\nСПОСОБНОСТЬ:\n").append(op.ability());
            details.setText(sb.toString());
        });

        VBox left = new VBox(6, new Label("Kill Team"), teamList);
        VBox center = new VBox(6, new Label("Оперативники"), operativeList);
        VBox right = new VBox(6, new Label("Детали"), details);

        left.setPrefWidth(220);
        center.setPrefWidth(240);
        VBox.setVgrow(teamList, Priority.ALWAYS);
        VBox.setVgrow(operativeList, Priority.ALWAYS);
        VBox.setVgrow(details, Priority.ALWAYS);

        HBox content = new HBox(10, left, center, right);
        content.setPadding(new Insets(10));
        setCenter(content);

        loadTeams();
    }

    private void loadTeams() {
        new Thread(() -> {
            try {
                var teams = api.getKillTeams();
                Platform.runLater(() -> teamList.getItems().setAll(teams));
            } catch (Exception e) {
                Platform.runLater(() -> details.setText("Не удалось загрузить команды: " + e.getMessage()));
            }
        }).start();
    }
}
package ru.petrsu.killteam.client;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.layout.Region;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import ru.petrsu.killteam.dto.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class MainView2 extends BorderPane {

    private final ApiClient api = new ApiClient();
    private final TabPane tabs = new TabPane();
    private final Label statusLabel = new Label("Готов");

    private final ListView<FactionDto> factionList = new ListView<>();
    private final TextArea factionDetails = new TextArea();
    private final List<FactionDto> allFactions = new ArrayList<>();

    private final ListView<KillTeamDto> teamList = new ListView<>();
    private final ListView<OperativeDto> operativeList = new ListView<>();
    private final TextArea details = new TextArea();
    private final List<KillTeamDto> allTeams = new ArrayList<>();

    private final ListView<MatchDto> matchList = new ListView<>();
    private final TextArea matchDetails = new TextArea();
    private final HBox playerControls = new HBox(12);
    private MatchDto currentMatch;

    public MainView2() {
        Tab factionsTab = new Tab("Фракции", buildFactionsPane());
        factionsTab.setClosable(false);

        Tab teamsTab = new Tab("Команды", buildTeamsPane());
        teamsTab.setClosable(false);

        Tab matchesTab = new Tab("Матчи", buildMatchesPane());
        matchesTab.setClosable(false);

        tabs.getTabs().addAll(factionsTab, teamsTab, matchesTab);
        setCenter(tabs);
        setBottom(buildStatusBar());
    }

    // ======================== ФРАКЦИИ ========================

    private BorderPane buildFactionsPane() {
        BorderPane pane = new BorderPane();
        factionDetails.setEditable(false);
        factionDetails.setWrapText(true);

        factionList.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(FactionDto item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); return; }
                setText(item.parentName() != null ? "    └ " + item.name() : item.name());
                setStyle(item.parentName() != null
                        ? "-fx-text-fill: #c9b98a;"
                        : "-fx-text-fill: #f0c75e; -fx-font-weight: bold;");
            }
        });

        factionList.getSelectionModel().selectedItemProperty().addListener((o, old, f) -> {
            if (f == null) { factionDetails.clear(); return; }
            var sb = new StringBuilder();
            sb.append(f.name()).append("\n");
            if (f.parentName() != null) sb.append("Родитель: ").append(f.parentName()).append("\n");
            sb.append("\n").append(f.description() != null ? f.description() : "");
            factionDetails.setText(sb.toString());
        });

        Button createBtn = new Button("Создать");
        Button editBtn = new Button("Редактировать");
        Button deleteBtn = new Button("Удалить");
        deleteBtn.getStyleClass().add("danger");
        Button refreshBtn = new Button("Обновить");
        refreshBtn.getStyleClass().add("ghost");

        createBtn.setOnAction(e -> openFactionDialog(null));
        editBtn.setOnAction(e -> {
            var sel = factionList.getSelectionModel().getSelectedItem();
            if (sel != null) openFactionDialog(sel);
        });
        deleteBtn.setOnAction(e -> {
            var sel = factionList.getSelectionModel().getSelectedItem();
            if (sel == null) return;
            if (!confirm("Удалить фракцию \"" + sel.name() + "\"?")) return;
            new Thread(() -> {
                try {
                    api.deleteFaction(sel.id());
                    Platform.runLater(this::loadFactions);
                } catch (Exception ex) { showError(ex); }
            }).start();
        });
        refreshBtn.setOnAction(e -> loadFactions());

        GridPane buttons = buildButtonGrid(createBtn, editBtn, deleteBtn, refreshBtn);

        Label listHeader = new Label("Список фракций");
        listHeader.getStyleClass().add("section-header");
        Label detailsHeader = new Label("Описание");
        detailsHeader.getStyleClass().add("section-header");

        VBox left = new VBox(6, listHeader, factionList, buttons);
        left.setPrefWidth(340); left.setMinWidth(280);
        VBox.setVgrow(factionList, Priority.ALWAYS);

        VBox right = new VBox(6, detailsHeader, factionDetails);
        VBox.setVgrow(factionDetails, Priority.ALWAYS);

        HBox content = new HBox(12, left, right);
        content.setPadding(new Insets(12));
        pane.setCenter(content);

        loadFactions();
        return pane;
    }

    private void loadFactions() {
        setStatus("Загрузка фракций...", "wait");
        new Thread(() -> {
            try {
                var list = api.getFactions();
                Platform.runLater(() -> {
                    allFactions.clear();
                    allFactions.addAll(list);
                    factionList.getItems().setAll(list);
                    setStatus("Фракций: " + list.size(), "ok");
                });
            } catch (Exception e) { showError(e); }
        }).start();
    }

    private void openFactionDialog(FactionDto existing) {
        new Thread(() -> {
            try {
                var factions = api.getFactions();
                Platform.runLater(() -> showFactionDialog(existing, factions));
            } catch (Exception e) { showError(e); }
        }).start();
    }

    private void showFactionDialog(FactionDto existing, List<FactionDto> factions) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(existing == null ? "Новая фракция" : "Редактировать фракцию");
        dialog.setResizable(false);
        DialogStyler.apply(dialog);

        TextField nameField = new TextField(existing != null ? existing.name() : "");
        TextArea descArea = new TextArea(existing != null ? existing.description() : "");
        descArea.setPrefRowCount(6);
        descArea.setWrapText(true);

        ComboBox<FactionDto> parentBox = new ComboBox<>();
        parentBox.getItems().add(null);
        factions.stream()
                .filter(f -> existing == null || !f.id().equals(existing.id()))
                .forEach(parentBox.getItems()::add);
        parentBox.setConverter(new javafx.util.StringConverter<>() {
            @Override public String toString(FactionDto f) {
                return f == null ? "(без родителя)" : f.name();
            }
            @Override public FactionDto fromString(String s) { return null; }
        });
        if (existing != null && existing.parentId() != null) {
            parentBox.getItems().stream()
                    .filter(f -> f != null && f.id().equals(existing.parentId()))
                    .findFirst().ifPresent(parentBox::setValue);
        } else {
            parentBox.setValue(null);
        }

        GridPane grid = buildFormGrid();
        grid.add(new Label("Название:"), 0, 0); grid.add(nameField, 1, 0);
        grid.add(new Label("Родитель:"), 0, 1); grid.add(parentBox, 1, 1);
        grid.add(new Label("Описание:"), 0, 2); grid.add(descArea, 1, 2);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().setPrefWidth(480);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.showAndWait().ifPresent(bt -> {
            if (bt != ButtonType.OK) return;
            if (nameField.getText().isBlank()) {
                ErrorDialog.show("Укажи название фракции");
                return;
            }
            var parent = parentBox.getValue();
            var dto = new FactionDto(
                    existing != null ? existing.id() : null,
                    nameField.getText(),
                    descArea.getText(),
                    parent != null ? parent.id() : null,
                    parent != null ? parent.name() : null
            );
            new Thread(() -> {
                try {
                    if (existing == null) api.createFaction(dto);
                    else api.updateFaction(existing.id(), dto);
                    Platform.runLater(this::loadFactions);
                } catch (Exception ex) { showError(ex); }
            }).start();
        });
    }

    // ======================== КОМАНДЫ ========================

    private BorderPane buildTeamsPane() {
        BorderPane pane = new BorderPane();
        details.setEditable(false);
        details.setWrapText(true);

        TextField searchField = new TextField();
        searchField.setPromptText("Поиск команды...");
        searchField.textProperty().addListener((o, old, n) -> applyTeamFilter(n));

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

        teamList.getSelectionModel().selectedItemProperty()
                .addListener((o, old, t) -> onTeamSelected(t));
        operativeList.getSelectionModel().selectedItemProperty()
                .addListener((o, old, op) -> onOperativeSelected(op));

        Button createBtn = new Button("Создать");
        Button editBtn = new Button("Редактировать");
        Button deleteBtn = new Button("Удалить");
        deleteBtn.getStyleClass().add("danger");
        Button refreshBtn = new Button("Обновить");
        refreshBtn.getStyleClass().add("ghost");

        createBtn.setOnAction(e -> openTeamDialog(null));
        editBtn.setOnAction(e -> {
            var sel = teamList.getSelectionModel().getSelectedItem();
            if (sel != null) openTeamDialog(sel);
        });
        deleteBtn.setOnAction(e -> {
            var sel = teamList.getSelectionModel().getSelectedItem();
            if (sel == null) return;
            if (!confirm("Удалить команду \"" + sel.name() + "\"?")) return;
            new Thread(() -> {
                try {
                    api.deleteKillTeam(sel.id());
                    Platform.runLater(this::loadTeams);
                } catch (Exception ex) { showError(ex); }
            }).start();
        });
        refreshBtn.setOnAction(e -> loadTeams());

        GridPane buttons = buildButtonGrid(createBtn, editBtn, deleteBtn, refreshBtn);

        Label teamHeader = new Label("Kill Team");
        teamHeader.getStyleClass().add("section-header");
        Label opHeader = new Label("Оперативники");
        opHeader.getStyleClass().add("section-header");
        Label detailsHeader = new Label("Детали");
        detailsHeader.getStyleClass().add("section-header");

        VBox left = new VBox(6, teamHeader, searchField, teamList, buttons);
        VBox center = new VBox(6, opHeader, operativeList);
        VBox right = new VBox(6, detailsHeader, details);
        left.setPrefWidth(320); left.setMinWidth(280);
        center.setPrefWidth(260);
        VBox.setVgrow(teamList, Priority.ALWAYS);
        VBox.setVgrow(operativeList, Priority.ALWAYS);
        VBox.setVgrow(details, Priority.ALWAYS);

        HBox content = new HBox(12, left, center, right);
        content.setPadding(new Insets(12));
        pane.setCenter(content);

        loadTeams();
        return pane;
    }

    private void applyTeamFilter(String query) {
        if (query == null || query.isBlank()) {
            teamList.getItems().setAll(allTeams);
            return;
        }
        String q = query.toLowerCase();
        teamList.getItems().setAll(
                allTeams.stream().filter(t -> t.name().toLowerCase().contains(q)).toList()
        );
    }

    private void loadTeams() {
        setStatus("Загрузка команд...", "wait");
        new Thread(() -> {
            try {
                var teams = api.getKillTeams();
                Platform.runLater(() -> {
                    allTeams.clear();
                    allTeams.addAll(teams);
                    teamList.getItems().setAll(teams);
                    setStatus("Команд: " + teams.size(), "ok");
                });
            } catch (Exception e) { showError(e); }
        }).start();
    }

    private void onTeamSelected(KillTeamDto team) {
        operativeList.getItems().clear();
        details.clear();
        if (team == null) return;
        new Thread(() -> {
            try {
                var ops = api.getOperatives(team.id());
                Platform.runLater(() -> {
                    operativeList.getItems().setAll(ops);
                    details.setText(team.name() + "\nФракция: " + team.factionName()
                            + "\nВерсия: " + team.version()
                            + "\n\nПРАВИЛА:\n" + team.rulesText());
                });
            } catch (Exception e) { showError(e); }
        }).start();
    }

    private void onOperativeSelected(OperativeDto op) {
        if (op == null) return;
        var sb = new StringBuilder();
        sb.append(op.name()).append("\n");
        sb.append("ОД: ").append(op.apl())
                .append("   Движение: ").append(op.move()).append("\"")
                .append("   Спас: ").append(op.save())
                .append("   Раны: ").append(op.wounds()).append("\n\n");
        sb.append("ОРУЖИЕ:\n");
        if (op.weapons().isEmpty()) {
            sb.append("  (нет оружия)\n");
        } else {
            for (WeaponDto w : op.weapons()) {
                sb.append(String.format("%-32s Ат.%d  Точ.%s  Урон %s  %s%n",
                        w.name(), w.atk(), w.hit(), w.dmg(), w.special()));
            }
        }
        sb.append("\nСПОСОБНОСТЬ:\n").append(op.ability());
        details.setText(sb.toString());
    }

    private void openTeamDialog(KillTeamDto existing) {
        new Thread(() -> {
            try {
                var factions = api.getFactions();
                var operatives = existing != null
                        ? api.getOperatives(existing.id())
                        : List.<OperativeDto>of();
                Platform.runLater(() -> showTeamDialog(existing, factions, operatives));
            } catch (Exception e) { showError(e); }
        }).start();
    }

    private void showTeamDialog(KillTeamDto existing, List<FactionDto> factions,
                                List<OperativeDto> operatives) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(existing == null ? "Новая команда" : "Редактировать команду");
        dialog.setResizable(false);
        DialogStyler.apply(dialog);

        TextField nameField = new TextField(existing != null ? existing.name() : "");
        TextField versionField = new TextField(existing != null ? existing.version() : "");
        TextArea rulesArea = new TextArea(existing != null ? existing.rulesText() : "");
        rulesArea.setPrefRowCount(5);
        rulesArea.setWrapText(true);

        ComboBox<FactionDto> factionBox = new ComboBox<>();
        factionBox.getItems().setAll(factions);
        factionBox.setConverter(new javafx.util.StringConverter<>() {
            @Override public String toString(FactionDto f) { return f == null ? "" : f.name(); }
            @Override public FactionDto fromString(String s) { return null; }
        });
        if (existing != null) {
            factions.stream().filter(f -> f.id().equals(existing.factionId()))
                    .findFirst().ifPresent(factionBox::setValue);
        } else if (!factions.isEmpty()) {
            factionBox.setValue(factions.get(0));
        }

        GridPane grid = buildFormGrid();
        grid.add(new Label("Название:"), 0, 0); grid.add(nameField, 1, 0);
        grid.add(new Label("Версия:"), 0, 1); grid.add(versionField, 1, 1);
        grid.add(new Label("Фракция:"), 0, 2); grid.add(factionBox, 1, 2);
        grid.add(new Label("Правила:"), 0, 3); grid.add(rulesArea, 1, 3);

        ListView<OperativeDto> opList = new ListView<>();
        opList.getItems().setAll(operatives);
        opList.setPrefHeight(180);
        opList.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(OperativeDto item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.name());
            }
        });

        VBox opBox = new VBox(6);
        Label opLabel = new Label("Оперативники:");
        opLabel.getStyleClass().add("section-header");
        Button addOpBtn = new Button("Добавить");
        Button editOpBtn = new Button("Редактировать");
        Button delOpBtn = new Button("Удалить");
        delOpBtn.getStyleClass().add("danger");
        opBox.setDisable(existing == null);

        Runnable reloadOps = () -> {
            try {
                var fresh = api.getOperatives(existing.id());
                Platform.runLater(() -> opList.getItems().setAll(fresh));
            } catch (Exception ex) { showError(ex); }
        };

        addOpBtn.setOnAction(e -> {
            if (existing == null) return;
            openOperativeDialog(null, existing.id(), reloadOps);
        });
        editOpBtn.setOnAction(e -> {
            var sel = opList.getSelectionModel().getSelectedItem();
            if (sel == null) return;
            openOperativeDialog(sel, existing.id(), reloadOps);
        });
        delOpBtn.setOnAction(e -> {
            var sel = opList.getSelectionModel().getSelectedItem();
            if (sel == null) return;
            if (!confirm("Удалить оперативника \"" + sel.name() + "\"?")) return;
            new Thread(() -> {
                try {
                    api.deleteOperative(sel.id());
                    reloadOps.run();
                } catch (Exception ex) { showError(ex); }
            }).start();
        });

        HBox opButtons = new HBox(6, addOpBtn, editOpBtn, delOpBtn);
        opBox.getChildren().addAll(opLabel, opList, opButtons);

        VBox root = new VBox(10, grid, new Separator(), opBox);
        dialog.getDialogPane().setContent(root);
        dialog.getDialogPane().setPrefWidth(560);
        dialog.getDialogPane().setMaxWidth(600);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.showAndWait().ifPresent(bt -> {
            if (bt != ButtonType.OK) return;
            var faction = factionBox.getValue();
            if (faction == null || nameField.getText().isBlank()) {
                ErrorDialog.show("Укажи название и фракцию");
                return;
            }
            var dto = new KillTeamDto(
                    existing != null ? existing.id() : null,
                    faction.id(), faction.name(),
                    nameField.getText(),
                    versionField.getText(),
                    rulesArea.getText(),
                    existing != null ? existing.operatives() : List.of()
            );
            new Thread(() -> {
                try {
                    if (existing == null) api.createKillTeam(dto);
                    else api.updateKillTeam(existing.id(), dto);
                    Platform.runLater(this::loadTeams);
                } catch (Exception ex) { showError(ex); }
            }).start();
        });
    }

    // ======================== ОПЕРАТИВНИК ========================

    private void openOperativeDialog(OperativeDto existing, Long killTeamId, Runnable onSaved) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(existing == null ? "Новый оперативник" : "Редактировать оперативника");
        dialog.setResizable(false);
        DialogStyler.apply(dialog);

        TextField nameField = new TextField(existing != null ? existing.name() : "");
        TextField aplField = new TextField(existing != null ? String.valueOf(existing.apl()) : "3");
        TextField moveField = new TextField(existing != null ? String.valueOf(existing.move()) : "6");
        TextField saveField = new TextField(existing != null ? existing.save() : "3+");
        TextField woundsField = new TextField(existing != null ? String.valueOf(existing.wounds()) : "10");
        TextArea abilityArea = new TextArea(existing != null ? existing.ability() : "");
        abilityArea.setPrefRowCount(3);
        abilityArea.setWrapText(true);

        GridPane grid = buildFormGrid();
        grid.add(new Label("Имя:"), 0, 0); grid.add(nameField, 1, 0);
        grid.add(new Label("ОД:"), 0, 1); grid.add(aplField, 1, 1);
        grid.add(new Label("Движение:"), 0, 2); grid.add(moveField, 1, 2);
        grid.add(new Label("Спас:"), 0, 3); grid.add(saveField, 1, 3);
        grid.add(new Label("Раны:"), 0, 4); grid.add(woundsField, 1, 4);
        grid.add(new Label("Способ.:"), 0, 5); grid.add(abilityArea, 1, 5);

        // Оружие
        ListView<WeaponDto> weaponList = new ListView<>();
        weaponList.setPrefHeight(140);
        weaponList.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(WeaponDto item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); return; }
                setText(item.name() + "  [Ат." + item.atk() + " / Точ."
                        + item.hit() + " / Урон " + item.dmg() + "]");
            }
        });

        Button addWeaponBtn = new Button("Добавить");
        Button editWeaponBtn = new Button("Редактировать");
        Button delWeaponBtn = new Button("Удалить");
        delWeaponBtn.getStyleClass().add("danger");

        boolean persist = existing != null;
        if (persist) weaponList.getItems().setAll(existing.weapons());

        Runnable reloadWeapons = () -> {
            try {
                var fresh = api.getOperative(existing.id());
                Platform.runLater(() -> weaponList.getItems().setAll(fresh.weapons()));
            } catch (Exception ex) { showError(ex); }
        };

        addWeaponBtn.setOnAction(e -> {
            if (!persist) {
                ErrorDialog.show("Сначала сохрани оперативника");
                return;
            }
            openWeaponDialog(null, existing.id(), reloadWeapons);
        });
        editWeaponBtn.setOnAction(e -> {
            if (!persist) return;
            var sel = weaponList.getSelectionModel().getSelectedItem();
            if (sel == null) return;
            openWeaponDialog(sel, existing.id(), reloadWeapons);
        });
        delWeaponBtn.setOnAction(e -> {
            if (!persist) return;
            var sel = weaponList.getSelectionModel().getSelectedItem();
            if (sel == null) return;
            if (!confirm("Удалить оружие \"" + sel.name() + "\"?")) return;
            new Thread(() -> {
                try {
                    api.deleteWeapon(sel.id());
                    reloadWeapons.run();
                } catch (Exception ex) { showError(ex); }
            }).start();
        });

        Label wLabel = new Label("Оружие:");
        wLabel.getStyleClass().add("section-header");
        HBox wButtons = new HBox(6, addWeaponBtn, editWeaponBtn, delWeaponBtn);
        VBox weaponBox = new VBox(6, wLabel, weaponList, wButtons);

        VBox root = new VBox(10, grid, new Separator(), weaponBox);
        dialog.getDialogPane().setContent(root);
        dialog.getDialogPane().setPrefWidth(540);
        dialog.getDialogPane().setMaxWidth(580);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.showAndWait().ifPresent(bt -> {
            if (bt != ButtonType.OK) return;
            try {
                var dto = new OperativeDto(
                        existing != null ? existing.id() : null,
                        nameField.getText(),
                        Integer.parseInt(aplField.getText().trim()),
                        Integer.parseInt(moveField.getText().trim()),
                        saveField.getText().trim(),
                        Integer.parseInt(woundsField.getText().trim()),
                        abilityArea.getText(),
                        weaponList.getItems()
                );
                new Thread(() -> {
                    try {
                        if (existing == null) api.createOperative(killTeamId, dto);
                        else api.updateOperative(existing.id(), dto);
                        Platform.runLater(onSaved);
                    } catch (Exception ex) { showError(ex); }
                }).start();
            } catch (NumberFormatException ex) {
                ErrorDialog.show("Проверь числовые поля (ОД, Движение, Раны)");
            }
        });
    }

    // ======================== ОРУЖИЕ ========================

    private void openWeaponDialog(WeaponDto existing, Long operativeId, Runnable onSaved) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(existing == null ? "Новое оружие" : "Редактировать оружие");
        dialog.setResizable(false);
        DialogStyler.apply(dialog);

        TextField nameField = new TextField(existing != null ? existing.name() : "");
        TextField atkField = new TextField(existing != null ? String.valueOf(existing.atk()) : "4");
        TextField hitField = new TextField(existing != null ? existing.hit() : "3+");
        TextField dmgField = new TextField(existing != null ? existing.dmg() : "3/4");
        TextField specialField = new TextField(existing != null ? existing.special() : "");

        GridPane grid = buildFormGrid();
        grid.add(new Label("Название:"), 0, 0); grid.add(nameField, 1, 0);
        grid.add(new Label("Атаки:"), 0, 1); grid.add(atkField, 1, 1);
        grid.add(new Label("Точность:"), 0, 2); grid.add(hitField, 1, 2);
        grid.add(new Label("Урон (об/крит):"), 0, 3); grid.add(dmgField, 1, 3);
        grid.add(new Label("Свойства:"), 0, 4); grid.add(specialField, 1, 4);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().setPrefWidth(460);
        dialog.getDialogPane().setMaxWidth(500);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.showAndWait().ifPresent(bt -> {
            if (bt != ButtonType.OK) return;
            try {
                var dto = new WeaponDto(
                        existing != null ? existing.id() : null,
                        nameField.getText(),
                        Integer.parseInt(atkField.getText().trim()),
                        hitField.getText().trim(),
                        dmgField.getText().trim(),
                        specialField.getText()
                );
                new Thread(() -> {
                    try {
                        if (existing == null) api.createWeapon(operativeId, dto);
                        else api.updateWeapon(existing.id(), dto);
                        Platform.runLater(onSaved);
                    } catch (Exception ex) { showError(ex); }
                }).start();
            } catch (NumberFormatException ex) {
                ErrorDialog.show("Атаки должны быть числом");
            }
        });
    }

    // ======================== МАТЧИ ========================

    private BorderPane buildMatchesPane() {
        BorderPane pane = new BorderPane();
        matchDetails.setEditable(false);
        matchDetails.setWrapText(true);

        matchList.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(MatchDto item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null
                        : item.name() + "  [" + translateStatus(item.status()) + "]");
            }
        });

        matchList.getSelectionModel().selectedItemProperty().addListener((o, old, m) -> {
            currentMatch = m;
            if (m == null) { matchDetails.clear(); playerControls.getChildren().clear(); return; }
            renderMatch(m);
        });

        Button newMatchBtn = new Button("Новый матч");
        Button advanceBtn = new Button("Следующая фаза");
        Button eventBtn = new Button("Добавить событие");
        Button fieldBtn = new Button("Поле боя");
        fieldBtn.getStyleClass().add("success");
        Button deleteBtn = new Button("Удалить матч");
        deleteBtn.getStyleClass().add("danger");
        Button refreshBtn = new Button("Обновить");
        refreshBtn.getStyleClass().add("ghost");

        newMatchBtn.setOnAction(e -> openNewMatchDialog());
        advanceBtn.setOnAction(e -> {
            if (currentMatch == null) return;
            Long id = currentMatch.id();
            new Thread(() -> {
                try {
                    api.advancePhase(id);
                    Platform.runLater(this::loadMatches);
                } catch (Exception ex) { showError(ex); }
            }).start();
        });
        eventBtn.setOnAction(e -> {
            if (currentMatch == null) return;
            openNewEventDialog(currentMatch);
        });
        fieldBtn.setOnAction(e -> {
            if (currentMatch == null) {
                ErrorDialog.show("Сначала выбери матч");
                return;
            }
            try {
                var w = new BattlefieldWindow(api, currentMatch, this::loadMatches);
                w.show();
            } catch (Throwable t) {
                t.printStackTrace();
                ErrorDialog.show(t instanceof Exception ex ? ex
                        : new RuntimeException(t.getMessage()));
            }
        });
        deleteBtn.setOnAction(e -> {
            if (currentMatch == null) return;
            if (!confirm("Удалить матч \"" + currentMatch.name() + "\"?")) return;
            Long id = currentMatch.id();
            new Thread(() -> {
                try {
                    api.deleteMatch(id);
                    Platform.runLater(() -> { currentMatch = null; loadMatches(); });
                } catch (Exception ex) { showError(ex); }
            }).start();
        });
        refreshBtn.setOnAction(e -> loadMatches());

        GridPane buttons = buildButtonGrid(newMatchBtn, advanceBtn, eventBtn, fieldBtn, deleteBtn, refreshBtn);

        Label matchHeader = new Label("Матчи");
        matchHeader.getStyleClass().add("section-header");
        Label detailsHeader = new Label("Детали матча");
        detailsHeader.getStyleClass().add("section-header");

        VBox left = new VBox(6, matchHeader, matchList, buttons);
        left.setPrefWidth(340); left.setMinWidth(300);
        VBox.setVgrow(matchList, Priority.ALWAYS);

        playerControls.setPadding(new Insets(6, 0, 6, 0));
        VBox right = new VBox(6, detailsHeader, matchDetails, playerControls);
        VBox.setVgrow(matchDetails, Priority.ALWAYS);

        SplitPane split = new SplitPane(left, right);
        split.setDividerPositions(0.32);
        pane.setCenter(split);

        loadMatches();
        return pane;
    }

    private void loadMatches() {
        setStatus("Загрузка матчей...", "wait");
        new Thread(() -> {
            try {
                var matches = api.getMatches();
                Platform.runLater(() -> {
                    Long selectedId = currentMatch != null ? currentMatch.id() : null;
                    matchList.getItems().setAll(matches);
                    if (selectedId != null) {
                        boolean found = matches.stream()
                                .filter(m -> m.id().equals(selectedId))
                                .findFirst()
                                .map(m -> { matchList.getSelectionModel().select(m); return true; })
                                .orElse(false);
                        if (!found) currentMatch = null;
                    } else if (!matches.isEmpty()) {
                        matchList.getSelectionModel().select(0);
                    }
                    setStatus("Матчей: " + matches.size(), "ok");
                });
            } catch (Exception e) { showError(e); }
        }).start();
    }

    private void renderMatch(MatchDto m) {
        new Thread(() -> {
            try {
                var events = api.getEvents(m.id());
                Platform.runLater(() -> {
                    var sb = new StringBuilder();
                    sb.append("Матч: ").append(m.name()).append("\n");
                    sb.append("Миссия: ").append(m.mission())
                            .append("   Зона: ").append(m.killzone()).append("\n");
                    sb.append("Статус: ").append(translateStatus(m.status())).append("\n");
                    if (m.deploymentPhase()) {
                        sb.append("Фаза: РАССТАНОВКА\n");
                    } else {
                        var current = m.turningPoints().stream()
                                .filter(tp -> !"FINISHED".equals(tp.phase()))
                                .findFirst().orElse(null);
                        if (current == null) {
                            sb.append("Фаза: МАТЧ ЗАВЕРШЁН\n");
                        } else {
                            sb.append("Раунд ").append(current.number())
                                    .append(" — ").append(translatePhase(current.phase())).append("\n");
                            if ("FIREFIGHT".equals(current.phase()) && m.activePlayerId() != null) {
                                m.players().stream()
                                        .filter(p -> p.id().equals(m.activePlayerId()))
                                        .findFirst()
                                        .ifPresent(p -> sb.append("Ход: ").append(p.name()).append("\n"));
                            }
                        }
                    }
                    sb.append("\n");

                    sb.append("ИГРОКИ:\n");
                    for (MatchPlayerDto p : m.players()) {
                        sb.append("  ").append(p.name())
                                .append(" — ").append(p.killTeamName())
                                .append(" | КО: ").append(p.cp())
                                .append(" | Очки: ").append(p.score())
                                .append(p.initiative() ? "  ★ ИНИЦИАТИВА" : "")
                                .append("\n");
                    }

                    sb.append("\nРАУНДЫ:\n");
                    for (TurningPointDto tp : m.turningPoints()) {
                        sb.append("  Раунд ").append(tp.number())
                                .append(" — ").append(translatePhase(tp.phase())).append("\n");
                    }

                    sb.append("\nСОБЫТИЯ:\n");
                    if (events.isEmpty()) sb.append("  (пока нет)\n");
                    else for (GameEventDto e : events) {
                        sb.append("  [").append(translateEventType(e.eventType())).append("] ")
                                .append(e.description() != null ? e.description() : "").append("\n");
                    }
                    matchDetails.setText(sb.toString());
                    buildPlayerControls(m);
                });
            } catch (Exception e) { showError(e); }
        }).start();
    }

    private void buildPlayerControls(MatchDto m) {
        playerControls.getChildren().clear();
        for (MatchPlayerDto p : m.players()) {
            VBox box = new VBox(4);
            box.setPadding(new Insets(10));
            box.setStyle(
                    "-fx-background-color: linear-gradient(to bottom, #2a2012, #1c160d);" +
                            "-fx-border-color: #8b6f2a;" +
                            "-fx-border-radius: 4;" +
                            "-fx-background-radius: 4;" +
                            "-fx-border-width: 1;"
            );
            Label name = new Label(p.name());
            name.setStyle("-fx-font-weight: bold; -fx-text-fill: #f0c75e; -fx-font-size: 14px;");
            Label team = new Label(p.killTeamName());
            team.setStyle("-fx-text-fill: #c9b98a;");
            Label stats = new Label("КО: " + p.cp() + "   Очки: " + p.score());
            stats.setStyle("-fx-text-fill: #d4a72c; -fx-font-weight: bold;");

            Button cpPlus = new Button("+КО");
            Button cpMinus = new Button("-КО");
            cpMinus.getStyleClass().add("ghost");
            Button scorePlus = new Button("+1 очко");
            scorePlus.getStyleClass().add("success");
            Button scoreMinus = new Button("-1 очко");
            scoreMinus.getStyleClass().add("ghost");

            cpPlus.setOnAction(e -> changeCp(p.id(), 1));
            cpMinus.setOnAction(e -> changeCp(p.id(), -1));
            scorePlus.setOnAction(e -> changeScore(p.id(), 1));
            scoreMinus.setOnAction(e -> changeScore(p.id(), -1));

            HBox btns = new HBox(6, cpPlus, cpMinus, scorePlus, scoreMinus);
            box.getChildren().addAll(name, team, stats, btns);
            playerControls.getChildren().add(box);
        }
    }

    private void changeCp(Long playerId, int delta) {
        if (currentMatch == null) return;
        Long matchId = currentMatch.id();
        new Thread(() -> {
            try {
                var updated = api.changeCp(matchId, playerId, delta);
                Platform.runLater(() -> { currentMatch = updated; renderMatch(updated); });
            } catch (Exception ex) { showError(ex); }
        }).start();
    }

    private void changeScore(Long playerId, int delta) {
        if (currentMatch == null) return;
        Long matchId = currentMatch.id();
        new Thread(() -> {
            try {
                var updated = api.changeScore(matchId, playerId, delta);
                Platform.runLater(() -> { currentMatch = updated; renderMatch(updated); });
            } catch (Exception ex) { showError(ex); }
        }).start();
    }

    private void openNewMatchDialog() {
        new Thread(() -> {
            try {
                var teams = api.getKillTeams();
                Platform.runLater(() -> showNewMatchDialog(teams));
            } catch (Exception e) { showError(e); }
        }).start();
    }

    private void showNewMatchDialog(List<KillTeamDto> teams) {
        if (teams.size() < 2) {
            ErrorDialog.show("Нужно минимум 2 команды для матча");
            return;
        }
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Новый матч");
        dialog.setResizable(false);
        DialogStyler.apply(dialog);

        TextField nameField = new TextField("Новый бой");
        TextField missionField = new TextField("Loot");
        TextField killzoneField = new TextField("30x22");

        TextField p1Name = new TextField("Игрок 1");
        ComboBox<KillTeamDto> p1Team = teamCombo(teams, 0);
        TextField p2Name = new TextField("Игрок 2");
        ComboBox<KillTeamDto> p2Team = teamCombo(teams, Math.min(1, teams.size() - 1));

        GridPane grid = buildFormGrid();
        grid.add(new Label("Название:"), 0, 0); grid.add(nameField, 1, 0);
        grid.add(new Label("Миссия:"), 0, 1); grid.add(missionField, 1, 1);
        grid.add(new Label("Зона боя:"), 0, 2); grid.add(killzoneField, 1, 2);
        grid.add(new Separator(), 0, 3, 2, 1);
        grid.add(new Label("Игрок 1:"), 0, 4); grid.add(p1Name, 1, 4);
        grid.add(new Label("Команда 1:"), 0, 5); grid.add(p1Team, 1, 5);
        grid.add(new Label("Игрок 2:"), 0, 6); grid.add(p2Name, 1, 6);
        grid.add(new Label("Команда 2:"), 0, 7); grid.add(p2Team, 1, 7);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().setPrefWidth(480);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.showAndWait().ifPresent(bt -> {
            if (bt != ButtonType.OK) return;
            var kt1 = p1Team.getValue();
            var kt2 = p2Team.getValue();
            if (kt1 == null || kt2 == null) return;

            if (!killzoneField.getText().matches("\\s*\\d+(\\.\\d+)?\\s*[xX×]\\s*\\d+(\\.\\d+)?\\s*")) {
                ErrorDialog.show("Зона боя должна быть в формате 30x22");
                return;
            }

            Map<String, Object> req = new HashMap<>();
            req.put("name", nameField.getText());
            req.put("mission", missionField.getText());
            req.put("killzone", killzoneField.getText());
            req.put("player1Name", p1Name.getText());
            req.put("killTeam1Id", kt1.id());
            req.put("player2Name", p2Name.getText());
            req.put("killTeam2Id", kt2.id());

            new Thread(() -> {
                try {
                    var created = api.createMatch(req);
                    Platform.runLater(() -> { currentMatch = created; loadMatches(); });
                } catch (Exception ex) { showError(ex); }
            }).start();
        });
    }

    private void openNewEventDialog(MatchDto match) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Добавить событие");
        dialog.setResizable(false);
        DialogStyler.apply(dialog);

        ComboBox<String> typeBox = new ComboBox<>();
        typeBox.getItems().setAll("Активация", "Стрельба", "Ближний бой",
                "Движение", "Трата КО", "Заметка");
        typeBox.setValue("Заметка");

        ComboBox<MatchPlayerDto> playerBox = new ComboBox<>();
        playerBox.getItems().setAll(match.players());
        playerBox.setConverter(new javafx.util.StringConverter<>() {
            @Override public String toString(MatchPlayerDto p) { return p == null ? "" : p.name(); }
            @Override public MatchPlayerDto fromString(String s) { return null; }
        });
        if (!match.players().isEmpty()) playerBox.setValue(match.players().get(0));

        ComboBox<TurningPointDto> tpBox = new ComboBox<>();
        tpBox.getItems().setAll(match.turningPoints());
        tpBox.setConverter(new javafx.util.StringConverter<>() {
            @Override public String toString(TurningPointDto tp) {
                return tp == null ? "" : "Раунд " + tp.number() + " (" + translatePhase(tp.phase()) + ")";
            }
            @Override public TurningPointDto fromString(String s) { return null; }
        });
        if (!match.turningPoints().isEmpty()) tpBox.setValue(match.turningPoints().get(0));

        TextArea descArea = new TextArea();
        descArea.setPrefRowCount(4);
        descArea.setWrapText(true);

        GridPane grid = buildFormGrid();
        grid.add(new Label("Тип:"), 0, 0); grid.add(typeBox, 1, 0);
        grid.add(new Label("Игрок:"), 0, 1); grid.add(playerBox, 1, 1);
        grid.add(new Label("Раунд:"), 0, 2); grid.add(tpBox, 1, 2);
        grid.add(new Label("Описание:"), 0, 3); grid.add(descArea, 1, 3);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().setPrefWidth(480);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.showAndWait().ifPresent(bt -> {
            if (bt != ButtonType.OK) return;
            Map<String, Object> req = new HashMap<>();
            req.put("turningPointId", tpBox.getValue() != null ? tpBox.getValue().id() : null);
            req.put("playerId", playerBox.getValue() != null ? playerBox.getValue().id() : null);
            req.put("operativeId", null);
            req.put("eventType", switch (typeBox.getValue()) {
                case "Активация" -> "ACTIVATE";
                case "Стрельба" -> "SHOOT";
                case "Ближний бой" -> "FIGHT";
                case "Движение" -> "MOVE";
                case "Трата КО" -> "CP_SPEND";
                default -> "NOTE";
            });
            req.put("description", descArea.getText());

            Long matchId = match.id();
            new Thread(() -> {
                try {
                    api.addEvent(matchId, req);
                    var fresh = api.getMatch(matchId);
                    Platform.runLater(() -> { currentMatch = fresh; renderMatch(fresh); });
                } catch (Exception ex) { showError(ex); }
            }).start();
        });
    }

    private ComboBox<KillTeamDto> teamCombo(List<KillTeamDto> teams, int initialIndex) {
        ComboBox<KillTeamDto> box = new ComboBox<>();
        box.getItems().setAll(teams);
        box.setConverter(new javafx.util.StringConverter<>() {
            @Override public String toString(KillTeamDto t) { return t == null ? "" : t.name(); }
            @Override public KillTeamDto fromString(String s) { return null; }
        });
        if (initialIndex >= 0 && initialIndex < teams.size()) box.setValue(teams.get(initialIndex));
        return box;
    }

    // ======================== УТИЛИТЫ ========================

    private GridPane buildFormGrid() {
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(15));
        grid.setStyle("-fx-background-color: #1c160d;");

        ColumnConstraints labelCol = new ColumnConstraints();
        labelCol.setMinWidth(110);
        labelCol.setPrefWidth(110);
        ColumnConstraints fieldCol = new ColumnConstraints();
        fieldCol.setHgrow(Priority.ALWAYS);
        fieldCol.setFillWidth(true);
        grid.getColumnConstraints().addAll(labelCol, fieldCol);

        return grid;
    }

    private String translateStatus(String s) {
        if (s == null) return "";
        return switch (s) {
            case "PLANNED" -> "Запланирован";
            case "IN_PROGRESS" -> "В процессе";
            case "FINISHED" -> "Завершён";
            default -> s;
        };
    }

    private String translatePhase(String p) {
        if (p == null) return "";
        return switch (p) {
            case "STRATEGY" -> "Стратегия";
            case "FIREFIGHT" -> "Бой";
            case "FINISHED" -> "Завершено";
            default -> p;
        };
    }

    private String translateEventType(String t) {
        if (t == null) return "";
        return switch (t) {
            case "ACTIVATE" -> "Активация";
            case "SHOOT" -> "Стрельба";
            case "FIGHT" -> "Ближний бой";
            case "MOVE" -> "Движение";
            case "CP_SPEND" -> "Трата КО";
            case "NOTE" -> "Заметка";
            case "CP_GAIN" -> "Получение КО";
            default -> t;
        };
    }

    private HBox buildStatusBar() {
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button resetDataBtn = new Button("Заполнить тестовыми данными");
        resetDataBtn.getStyleClass().add("ghost");
        resetDataBtn.setOnAction(e -> {
            if (!confirm("Очистить всю базу и залить тестовые данные?\n"
                    + "Все твои команды, бойцы и матчи будут удалены.")) return;
            new Thread(() -> {
                try {
                    api.resetTestData();
                    Platform.runLater(() -> {
                        loadFactions();
                        loadTeams();
                        loadMatches();
                        setStatus("База перезалита тестовыми данными", "ok");
                    });
                } catch (Exception ex) { showError(ex); }
            }).start();
        });

        HBox bar = new HBox(10, statusLabel, spacer, resetDataBtn);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(6, 12, 6, 12));
        bar.getStyleClass().add("status-bar");
        return bar;
    }

    private void setStatus(String text, String kind) {
        Platform.runLater(() -> {
            statusLabel.setText(text);
            statusLabel.getStyleClass().removeAll("status-ok", "status-err", "status-wait");
            switch (kind) {
                case "ok" -> statusLabel.getStyleClass().add("status-ok");
                case "err" -> statusLabel.getStyleClass().add("status-err");
                case "wait" -> statusLabel.getStyleClass().add("status-wait");
            }
        });
    }

    private GridPane buildButtonGrid(Button... buttons) {
        GridPane grid = new GridPane();
        grid.setHgap(6); grid.setVgap(6); grid.setPadding(new Insets(6));
        ColumnConstraints c1 = new ColumnConstraints();
        ColumnConstraints c2 = new ColumnConstraints();
        c1.setPercentWidth(50); c2.setPercentWidth(50);
        grid.getColumnConstraints().addAll(c1, c2);
        for (int i = 0; i < buttons.length; i++) {
            Button b = buttons[i];
            b.setMaxWidth(Double.MAX_VALUE);
            b.setMinWidth(80);
            grid.add(b, i % 2, i / 2);
        }
        return grid;
    }

    private boolean confirm(String text) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, text, ButtonType.OK, ButtonType.CANCEL);
        alert.setResizable(false);
        DialogStyler.apply(alert);
        return alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK;
    }

    private void showError(Exception e) {
        ErrorDialog.show(e);
        setStatus("Ошибка", "err");
    }
}
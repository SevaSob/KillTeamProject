package ru.petrsu.killteam.client;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;
import ru.petrsu.killteam.dto.*;

import java.util.ArrayList;
import java.util.List;

public class BattlefieldWindow {

    private static final double CANVAS_SIZE = 4000;

    private final double boardW;
    private final double boardH;

    private final ApiClient api;
    private MatchDto match;
    private final Stage stage;
    private final Runnable onMatchChanged;

    private double scale = 25;
    private boolean gridVisible = true;
    private boolean labelsVisible = true;

    private double panX = 0, panY = 0;
    private boolean panning = false;
    private double panStartScreenX, panStartScreenY;
    private double panStartOffsetX, panStartOffsetY;

    private boolean rulerMode = false;
    private Double rulerStartX, rulerStartY, rulerEndX, rulerEndY;
    private RulerHandle rulerDragHandle = RulerHandle.NONE;
    private enum RulerHandle { NONE, START, END }

    private final List<Gauge> gauges = new ArrayList<>();
    private String pendingGaugeSize = null;
    private boolean eraserMode = false;

    private static class Gauge {
        double x, y, diameter;
    }

    private final List<MatchOperativeDto> fieldUnits = new ArrayList<>();
    private MatchOperativeDto selected, dragged;
    private double dragOffsetX, dragOffsetY;
    private double dragStartX, dragStartY;
    private double dragMaxDist;
    private ContextMenu activeMenu;

    private final Canvas canvas = new Canvas(CANVAS_SIZE, CANVAS_SIZE);
    private Pane canvasPane;
    private final ListView<OperativeDto> rosterList = new ListView<>();
    private final ComboBox<String> activePlayerBox = new ComboBox<>();
    private final Label coordLabel = new Label("x: 0.0\"  y: 0.0\"");
    private final Label modeLabel = new Label("");
    private final Label phaseLabel = new Label("");
    private final Label turnLabel = new Label("");
    private final Label headerLabel = new Label("");

    private Button mainActionBtn, nextTurnBtn, restartBtn, resetAllAplBtn, clearFieldBtn;

    public BattlefieldWindow(ApiClient api, MatchDto match, Runnable onMatchChanged) {
        this.api = api;
        this.match = match;
        this.onMatchChanged = onMatchChanged;

        double[] dims = parseKillzone(match.killzone());
        this.boardW = dims[0];
        this.boardH = dims[1];

        this.stage = new Stage();
        stage.setTitle("Поле боя — " + match.name());

        BorderPane root = new BorderPane();
        root.setCenter(buildCenter());
        root.setRight(buildSidebar());
        root.setTop(buildHeader());

        Scene scene = new Scene(root, 1400, 900);
        try {
            var res = getClass().getResource("/styles.css");
            if (res != null) scene.getStylesheets().add(res.toExternalForm());
        } catch (Exception ignored) {}
        stage.setScene(scene);

        Platform.runLater(this::centerCanvas);
        refreshMatchAndRedraw();
        loadField();
        loadRoster();
        stage.setOnShown(e -> Platform.runLater(this::centerCanvas));
        stage.setOnHidden(e -> notifyChanged());
    }

    public void show() { stage.show(); }

    private void notifyChanged() {
        if (onMatchChanged != null) {
            Platform.runLater(onMatchChanged);
        }
    }

    private double[] parseKillzone(String kz) {
        if (kz != null && kz.matches("\\s*\\d+(\\.\\d+)?\\s*[xX×]\\s*\\d+(\\.\\d+)?\\s*")) {
            String[] parts = kz.toLowerCase().split("[xX×]");
            try {
                double w = Double.parseDouble(parts[0].trim());
                double h = Double.parseDouble(parts[1].trim());
                if (w > 0 && h > 0 && w <= 500 && h <= 500) return new double[]{w, h};
            } catch (Exception ignored) {}
        }
        return new double[]{30, 22};
    }

    private HBox buildHeader() {
        headerLabel.setStyle("-fx-text-fill: #f0c75e; -fx-font-size: 16px; -fx-font-weight: bold;");
        HBox h = new HBox(headerLabel);
        h.setPadding(new Insets(10, 16, 10, 16));
        h.setStyle("-fx-background-color: #1c160d; -fx-border-color: transparent transparent #8b6f2a transparent; -fx-border-width: 0 0 2 0;");
        return h;
    }

    private void refreshMatchAndRedraw() {
        new Thread(() -> {
            try {
                var fresh = api.getMatch(match.id());
                Platform.runLater(() -> {
                    this.match = fresh;
                    updatePhaseUI();
                    redraw();
                });
            } catch (Exception e) { showError(e); }
        }).start();
    }

    private void updatePhaseUI() {
        String header;
        String phase;
        String turn = "";

        if (match.deploymentPhase()) {
            header = "РАССТАНОВКА";
            phase = "Выставь бойцов и нажми «Начать бой»";
            modeLabel.setText("Свободное перемещение");
        } else {
            TurningPointDto current = match.turningPoints().stream()
                    .filter(tp -> !"FINISHED".equals(tp.phase()))
                    .findFirst().orElse(null);
            if (current == null) {
                header = "МАТЧ ЗАВЕРШЁН";
                phase = "Все 4 раунда отыграны";
                modeLabel.setText("");
            } else if ("STRATEGY".equals(current.phase())) {
                header = "РАУНД " + current.number() + " — СТРАТЕГИЯ";
                phase = "Фаза стратегии. Тратьте КО и начинайте фазу боя.";
                modeLabel.setText("");
            } else {
                String active = match.players().stream()
                        .filter(p -> p.id().equals(match.activePlayerId()))
                        .map(MatchPlayerDto::name)
                        .findFirst().orElse("—");
                header = "РАУНД " + current.number() + " — БОЙ";
                phase = "Ход: " + active;
                turn = active;
                modeLabel.setText("");
            }
        }

        headerLabel.setText(header);
        phaseLabel.setText(phase);
        turnLabel.setText(turn);

        if (match.deploymentPhase()) {
            mainActionBtn.setText("Начать бой");
            mainActionBtn.setVisible(true);
            mainActionBtn.setManaged(true);
            mainActionBtn.setDisable(false);
            nextTurnBtn.setVisible(false);
            nextTurnBtn.setManaged(false);
            clearFieldBtn.setVisible(true);
            clearFieldBtn.setManaged(true);
            resetAllAplBtn.setVisible(false);
            resetAllAplBtn.setManaged(false);
        } else {
            TurningPointDto current = match.turningPoints().stream()
                    .filter(tp -> !"FINISHED".equals(tp.phase()))
                    .findFirst().orElse(null);

            if (current == null) {
                mainActionBtn.setVisible(false);
                mainActionBtn.setManaged(false);
                nextTurnBtn.setVisible(false);
                nextTurnBtn.setManaged(false);
            } else if ("STRATEGY".equals(current.phase())) {
                mainActionBtn.setText("Начать фазу боя");
                mainActionBtn.setVisible(true);
                mainActionBtn.setManaged(true);
                mainActionBtn.setDisable(false);
                nextTurnBtn.setVisible(false);
                nextTurnBtn.setManaged(false);
            } else {
                mainActionBtn.setText("Завершить раунд");
                mainActionBtn.setVisible(true);
                mainActionBtn.setManaged(true);
                mainActionBtn.setDisable(false);
                nextTurnBtn.setVisible(true);
                nextTurnBtn.setManaged(true);
                nextTurnBtn.setDisable(false);
            }

            clearFieldBtn.setVisible(false);
            clearFieldBtn.setManaged(false);
            resetAllAplBtn.setVisible(true);
            resetAllAplBtn.setManaged(true);
        }

        restartBtn.setVisible(!match.deploymentPhase());
        restartBtn.setManaged(!match.deploymentPhase());
    }

    private Pane buildCenter() {
        canvasPane = new Pane(canvas);
        canvasPane.setStyle("-fx-background-color: #0f0b06;");

        Rectangle clip = new Rectangle();
        clip.widthProperty().bind(canvasPane.widthProperty());
        clip.heightProperty().bind(canvasPane.heightProperty());
        canvasPane.setClip(clip);

        canvas.setOnMouseMoved(e -> {
            double[] inch = screenToInch(e.getX(), e.getY());
            coordLabel.setText(String.format("x: %.1f\"  y: %.1f\"", inch[0], inch[1]));
        });

        canvas.setOnScroll(e -> {
            double delta = e.getDeltaY();
            if (delta == 0) return;
            double factor = delta > 0 ? 1.1 : 0.9;
            double newScale = clamp(scale * factor, 5, 100);
            if (Math.abs(newScale - scale) < 0.01) return;

            double[] field = screenToInch(e.getX(), e.getY());
            double sx = panX + e.getX();
            double sy = panY + e.getY();
            scale = newScale;

            double fX = (canvas.getWidth() - boardW * scale) / 2;
            double fY = (canvas.getHeight() - boardH * scale) / 2;
            panX = sx - (fX + field[0] * scale);
            panY = sy - (fY + field[1] * scale);
            updateCanvasPosition();
            redraw();
            e.consume();
        });

        canvas.setOnMousePressed(e -> {
            double[] inch = screenToInch(e.getX(), e.getY());

            if (e.getButton() == MouseButton.SECONDARY) {
                if (eraserMode) return;
                if (rulerMode) {
                    rulerStartX = rulerStartY = rulerEndX = rulerEndY = null;
                    redraw();
                    return;
                }
                MatchOperativeDto hit = hitTest(inch[0], inch[1]);
                if (hit != null) showContextMenu(hit, e.getScreenX(), e.getScreenY());
                else removeGaugeAt(inch[0], inch[1]);
                return;
            }

            if (e.getButton() != MouseButton.PRIMARY) return;

            if (activeMenu != null) { activeMenu.hide(); activeMenu = null; }

            if (eraserMode) { removeGaugeAt(inch[0], inch[1]); return; }

            if (pendingGaugeSize != null) {
                try {
                    Gauge g = new Gauge();
                    g.x = inch[0]; g.y = inch[1];
                    g.diameter = Double.parseDouble(pendingGaugeSize);
                    gauges.add(g);
                    pendingGaugeSize = null;
                    updateModeLabel();
                    redraw();
                } catch (NumberFormatException ignored) {}
                return;
            }

            if (rulerMode) {
                if (nearPoint(inch[0], inch[1], rulerStartX, rulerStartY)) {
                    rulerDragHandle = RulerHandle.START; return;
                }
                if (nearPoint(inch[0], inch[1], rulerEndX, rulerEndY)) {
                    rulerDragHandle = RulerHandle.END; return;
                }
                if (rulerStartX == null || rulerEndX != null) {
                    rulerStartX = inch[0]; rulerStartY = inch[1];
                    rulerEndX = null; rulerEndY = null;
                    rulerDragHandle = RulerHandle.START;
                } else {
                    rulerEndX = inch[0]; rulerEndY = inch[1];
                    rulerDragHandle = RulerHandle.END;
                }
                redraw();
                return;
            }

            MatchOperativeDto hit = hitTest(inch[0], inch[1]);
            if (hit != null) {
                selected = hit;

                boolean canDrag = hit.woundsCurrent() > 0;
                if (match.deploymentPhase()) {
                    // Свободно
                } else {
                    TurningPointDto current = match.turningPoints().stream()
                            .filter(tp -> !"FINISHED".equals(tp.phase()))
                            .findFirst().orElse(null);
                    if (current == null || !"FIREFIGHT".equals(current.phase())) {
                        canDrag = false;
                    }
                    if (match.activePlayerId() != null
                            && !match.activePlayerId().equals(hit.playerId())) {
                        canDrag = false;
                    }
                    if ("EXPENDED".equals(hit.state()) || hit.currentApl() <= 0) {
                        canDrag = false;
                    }
                }

                if (canDrag) {
                    dragged = hit;
                    dragOffsetX = inch[0] - hit.posX();
                    dragOffsetY = inch[1] - hit.posY();
                    dragStartX = hit.posX();
                    dragStartY = hit.posY();
                    dragMaxDist = match.deploymentPhase() ? 0 : hit.move();
                }
                redraw();
            } else {
                panning = true;
                panStartScreenX = e.getSceneX();
                panStartScreenY = e.getSceneY();
                panStartOffsetX = panX;
                panStartOffsetY = panY;
                selected = null;
                redraw();
            }
        });

        canvas.setOnMouseDragged(e -> {
            if (panning) {
                panX = panStartOffsetX + (e.getSceneX() - panStartScreenX);
                panY = panStartOffsetY + (e.getSceneY() - panStartScreenY);
                updateCanvasPosition();
                return;
            }
            double[] inch = screenToInch(e.getX(), e.getY());

            if (rulerMode && rulerDragHandle != RulerHandle.NONE) {
                if (rulerDragHandle == RulerHandle.START) {
                    rulerStartX = inch[0]; rulerStartY = inch[1];
                } else {
                    rulerEndX = inch[0]; rulerEndY = inch[1];
                }
                redraw();
                return;
            }

            if (dragged == null) return;

            double rawNX = inch[0] - dragOffsetX;
            double rawNY = inch[1] - dragOffsetY;

            double dx = rawNX - dragStartX;
            double dy = rawNY - dragStartY;
            double dist = Math.hypot(dx, dy);
            double nx, ny;
            if (dragMaxDist > 0 && dist > dragMaxDist) {
                double k = dragMaxDist / dist;
                nx = dragStartX + dx * k;
                ny = dragStartY + dy * k;
            } else {
                nx = rawNX;
                ny = rawNY;
            }
            nx = clamp(nx, 0, boardW);
            ny = clamp(ny, 0, boardH);

            MatchOperativeDto updated = new MatchOperativeDto(
                    dragged.id(), dragged.matchId(), dragged.playerId(), dragged.playerName(),
                    dragged.operativeId(), dragged.operativeName(),
                    nx, ny, dragged.orderType(), dragged.state(),
                    dragged.woundsCurrent(), dragged.woundsMax(),
                    dragged.currentApl(), dragged.maxApl(), dragged.move()
            );
            dragged = updated;
            replaceInField(updated);
            redraw();
        });

        canvas.setOnMouseReleased(e -> {
            if (panning) { panning = false; return; }
            rulerDragHandle = RulerHandle.NONE;

            if (dragged != null && !rulerMode) {
                MatchOperativeDto toSave = dragged;
                double movedDist = Math.hypot(toSave.posX() - dragStartX, toSave.posY() - dragStartY);
                double sX = dragStartX, sY = dragStartY;
                dragged = null;

                if (movedDist > 0.05) {
                    new Thread(() -> {
                        try {
                            var updated = api.reposition(toSave.id(), toSave.posX(), toSave.posY());
                            Platform.runLater(() -> {
                                replaceInField(updated);
                                redraw();
                                notifyChanged();
                            });
                        } catch (Exception ex) {
                            Platform.runLater(() -> {
                                MatchOperativeDto reverted = new MatchOperativeDto(
                                        toSave.id(), toSave.matchId(), toSave.playerId(),
                                        toSave.playerName(), toSave.operativeId(),
                                        toSave.operativeName(),
                                        sX, sY,
                                        toSave.orderType(), toSave.state(),
                                        toSave.woundsCurrent(), toSave.woundsMax(),
                                        toSave.currentApl(), toSave.maxApl(), toSave.move()
                                );
                                replaceInField(reverted);
                                redraw();
                                showError(ex);
                            });
                        }
                    }).start();
                } else {
                    redraw();
                }
            }
        });

        return canvasPane;
    }

    private void updateCanvasPosition() {
        canvas.setLayoutX(panX);
        canvas.setLayoutY(panY);
    }

    private void centerCanvas() {
        if (canvasPane == null) return;
        double viewW = canvasPane.getWidth();
        double viewH = canvasPane.getHeight();
        if (viewW < 50 || viewH < 50) return;
        double newScale = Math.min((viewW - 80) / boardW, (viewH - 80) / boardH);
        scale = clamp(newScale, 8, 80);

        double fX = (canvas.getWidth() - boardW * scale) / 2;
        double fY = (canvas.getHeight() - boardH * scale) / 2;
        panX = (viewW - boardW * scale) / 2 - fX;
        panY = (viewH - boardH * scale) / 2 - fY;
        updateCanvasPosition();
        redraw();
    }

    private ScrollPane buildSidebar() {
        VBox box = new VBox(12);
        box.setPadding(new Insets(15));
        box.setPrefWidth(340);
        box.setStyle("-fx-background-color: #1c160d;");

        Label phaseHeader = new Label("Состояние");
        phaseHeader.getStyleClass().add("section-header");
        phaseLabel.setStyle("-fx-text-fill: #f0c75e; -fx-font-weight: bold; -fx-font-size: 13px;");
        phaseLabel.setWrapText(true);
        turnLabel.setStyle("-fx-text-fill: #c9b98a; -fx-font-size: 12px;");

        mainActionBtn = new Button("Начать бой");
        mainActionBtn.getStyleClass().add("success");
        mainActionBtn.setMaxWidth(Double.MAX_VALUE);
        mainActionBtn.setOnAction(e -> mainAction());

        nextTurnBtn = new Button("Следующий ход");
        nextTurnBtn.setMaxWidth(Double.MAX_VALUE);
        nextTurnBtn.setOnAction(e -> nextTurn());

        restartBtn = new Button("Перезапустить бой");
        restartBtn.getStyleClass().add("danger");
        restartBtn.setMaxWidth(Double.MAX_VALUE);
        restartBtn.setOnAction(e -> restartDeployment());

        VBox phaseBox = new VBox(6, phaseHeader, phaseLabel, turnLabel,
                mainActionBtn, nextTurnBtn, restartBtn);

        Label rosterHeader = new Label("Игрок и команда");
        rosterHeader.getStyleClass().add("section-header");

        activePlayerBox.getItems().setAll(
                match.players().stream().map(MatchPlayerDto::name).toList()
        );
        if (!match.players().isEmpty()) activePlayerBox.setValue(match.players().get(0).name());
        activePlayerBox.setOnAction(e -> loadRoster());

        rosterList.setPrefHeight(160);
        rosterList.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(OperativeDto item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); return; }
                boolean deployed = isDeployed(item.id(), activePlayerBox.getValue());
                setText(item.name() + (deployed ? "  ✓" : ""));
                setStyle(deployed ? "-fx-text-fill: #6b5a35;" : "");
            }
        });

        Button deployBtn = new Button("Выставить в центр");
        deployBtn.setMaxWidth(Double.MAX_VALUE);
        deployBtn.setOnAction(e -> deploySelected());

        VBox rosterBox = new VBox(6, rosterHeader,
                new Label("Активный игрок:"), activePlayerBox,
                rosterList, deployBtn);

        Label viewHeader = new Label("Настройки вида");
        viewHeader.getStyleClass().add("section-header");

        CheckBox gridCheck = new CheckBox("Сетка");
        gridCheck.setSelected(gridVisible);
        gridCheck.setOnAction(e -> { gridVisible = gridCheck.isSelected(); redraw(); });

        CheckBox labelsCheck = new CheckBox("Подписи");
        labelsCheck.setSelected(labelsVisible);
        labelsCheck.setOnAction(e -> { labelsVisible = labelsCheck.isSelected(); redraw(); });

        Button centerBtn = new Button("Центрировать");
        centerBtn.getStyleClass().add("ghost");
        centerBtn.setMaxWidth(Double.MAX_VALUE);
        centerBtn.setOnAction(e -> centerCanvas());

        VBox viewBox = new VBox(6, viewHeader, gridCheck, labelsCheck, centerBtn);

        Label toolsHeader = new Label("Измерительные инструменты");
        toolsHeader.getStyleClass().add("section-header");

        ToggleButton rulerBtn = new ToggleButton("Линейка");
        rulerBtn.getStyleClass().add("tool-toggle");
        rulerBtn.setMaxWidth(Double.MAX_VALUE);
        rulerBtn.setOnAction(e -> {
            rulerMode = rulerBtn.isSelected();
            if (rulerMode) { eraserMode = false; pendingGaugeSize = null; }
            else { rulerStartX = rulerStartY = rulerEndX = rulerEndY = null; rulerDragHandle = RulerHandle.NONE; }
            updateModeLabel();
            redraw();
        });

        TextField gaugeField = new TextField("3");
        gaugeField.setPromptText("диаметр, \"");
        gaugeField.setPrefWidth(70);
        Button gaugeAddBtn = new Button("Шаблон");
        gaugeAddBtn.setOnAction(e -> {
            String val = gaugeField.getText().trim().replace(",", ".");
            try {
                double d = Double.parseDouble(val);
                if (d <= 0 || d > 100) throw new NumberFormatException();
                pendingGaugeSize = val;
                eraserMode = false;
                rulerBtn.setSelected(false);
                rulerMode = false;
                updateModeLabel();
            } catch (NumberFormatException ex) {
                ErrorDialog.show("Введи положительное число (диаметр в дюймах)");
            }
        });
        HBox gaugeRow = new HBox(6, gaugeField, gaugeAddBtn);

        ToggleButton eraserBtn = new ToggleButton("Ластик");
        eraserBtn.getStyleClass().add("tool-toggle");
        eraserBtn.setMaxWidth(Double.MAX_VALUE);
        eraserBtn.setOnAction(e -> {
            eraserMode = eraserBtn.isSelected();
            if (eraserMode) {
                rulerBtn.setSelected(false);
                rulerMode = false;
                pendingGaugeSize = null;
            }
            updateModeLabel();
            redraw();
        });

        Button clearGauges = new Button("Убрать все шаблоны");
        clearGauges.getStyleClass().add("ghost");
        clearGauges.setMaxWidth(Double.MAX_VALUE);
        clearGauges.setOnAction(e -> { gauges.clear(); redraw(); });

        VBox toolsBox = new VBox(6, toolsHeader, rulerBtn, gaugeRow, eraserBtn, clearGauges);

        Label serviceHeader = new Label("Обслуживание");
        serviceHeader.getStyleClass().add("section-header");

        Button refreshBtn = new Button("Обновить");
        refreshBtn.getStyleClass().add("ghost");
        refreshBtn.setMaxWidth(Double.MAX_VALUE);
        refreshBtn.setOnAction(e -> {
            refreshMatchAndRedraw();
            loadField();
            loadRoster();
        });

        resetAllAplBtn = new Button("Сбросить все ОД");
        resetAllAplBtn.setMaxWidth(Double.MAX_VALUE);
        resetAllAplBtn.setOnAction(e -> {
            if (!confirm("Сбросить ОД у всех бойцов на поле?")) return;
            new Thread(() -> {
                try {
                    api.resetAllApl(match.id());
                    Platform.runLater(() -> {
                        loadField();
                        notifyChanged();
                    });
                } catch (Exception ex) { showError(ex); }
            }).start();
        });

        clearFieldBtn = new Button("Очистить поле");
        clearFieldBtn.getStyleClass().add("danger");
        clearFieldBtn.setMaxWidth(Double.MAX_VALUE);
        clearFieldBtn.setOnAction(e -> {
            if (!confirm("Убрать всех бойцов с поля?")) return;
            new Thread(() -> {
                try {
                    api.clearField(match.id());
                    Platform.runLater(() -> {
                        fieldUnits.clear();
                        loadRoster();
                        redraw();
                        notifyChanged();
                    });
                } catch (Exception ex) { showError(ex); }
            }).start();
        });

        VBox serviceBox = new VBox(6, serviceHeader, refreshBtn, resetAllAplBtn, clearFieldBtn);

        modeLabel.setStyle("-fx-text-fill: #f0c75e; -fx-font-weight: bold; -fx-font-size: 11px;");

        Label hintLabel = new Label(
                "ЛКМ по бойцу — выделить/тащить\n" +
                        "ЛКМ по пустому — двигать карту\n" +
                        "ПКМ — меню бойца / удалить шаблон"
        );
        hintLabel.setStyle("-fx-text-fill: #6b5a35; -fx-font-size: 10px;");

        box.getChildren().addAll(
                phaseBox,
                new Separator(), rosterBox,
                new Separator(), viewBox,
                new Separator(), toolsBox,
                new Separator(), serviceBox,
                new Separator(),
                coordLabel, modeLabel, hintLabel
        );

        ScrollPane scroll = new ScrollPane(box);
        scroll.setFitToWidth(true);
        scroll.setPrefWidth(360);
        scroll.setStyle("-fx-background: #1c160d; -fx-background-color: #1c160d;");
        return scroll;
    }

    private void updateModeLabel() {
        if (pendingGaugeSize != null) modeLabel.setText("Кликни на поле для шаблона " + pendingGaugeSize + "\"");
        else if (rulerMode) modeLabel.setText("Режим: линейка");
        else if (eraserMode) modeLabel.setText("Режим: ластик");
        else modeLabel.setText(match.deploymentPhase() ? "Свободное перемещение" : "");
    }

    private void mainAction() {
        if (match.deploymentPhase()) {
            startBattle();
            return;
        }
        TurningPointDto current = match.turningPoints().stream()
                .filter(tp -> !"FINISHED".equals(tp.phase()))
                .findFirst().orElse(null);
        if (current == null) return;
        if ("STRATEGY".equals(current.phase())) {
            transitionPhase("Начинаем фазу боя раунда " + current.number());
        } else {
            transitionPhase("Раунд " + current.number() + " завершён");
        }
    }

    private void startBattle() {
        new Thread(() -> {
            try {
                var updated = api.startBattle(match.id());
                Platform.runLater(() -> {
                    match = updated;
                    updatePhaseUI();
                    loadField();
                    redraw();
                    notifyChanged();
                    showInfo("Бой начался",
                            "Раунд 1 — Стратегия.\n\n"
                                    + "Нажми «Начать фазу боя», когда будешь готов.");
                });
            } catch (Exception ex) { showError(ex); }
        }).start();
    }

    private void transitionPhase(String info) {
        new Thread(() -> {
            try {
                var updated = api.nextPhase(match.id());
                Platform.runLater(() -> {
                    match = updated;
                    updatePhaseUI();
                    loadField();
                    redraw();
                    notifyChanged();
                    if (info != null) showInfo("Фаза сменилась", info);
                });
            } catch (Exception ex) { showError(ex); }
        }).start();
    }

    private void nextTurn() {
        new Thread(() -> {
            try {
                var updated = api.nextTurn(match.id());
                Platform.runLater(() -> {
                    match = updated;
                    updatePhaseUI();
                    loadField();
                    redraw();
                    notifyChanged();
                });
            } catch (Exception ex) { showError(ex); }
        }).start();
    }

    private void restartDeployment() {
        if (!confirm("Перезапустить бой и вернуться к расстановке?\n"
                + "Все бойцы будут убраны с поля.")) return;
        new Thread(() -> {
            try {
                var updated = api.restartDeployment(match.id());
                Platform.runLater(() -> {
                    match = updated;
                    updatePhaseUI();
                    fieldUnits.clear();
                    loadField();
                    loadRoster();
                    redraw();
                    notifyChanged();
                });
            } catch (Exception ex) { showError(ex); }
        }).start();
    }

    private void loadField() {
        new Thread(() -> {
            try {
                var list = api.getField(match.id());
                Platform.runLater(() -> {
                    fieldUnits.clear();
                    fieldUnits.addAll(list);
                    redraw();
                });
            } catch (Exception e) { showError(e); }
        }).start();
    }

    private void loadRoster() {
        String playerName = activePlayerBox.getValue();
        MatchPlayerDto player = match.players().stream()
                .filter(p -> p.name().equals(playerName))
                .findFirst().orElse(null);
        if (player == null) return;
        new Thread(() -> {
            try {
                var ops = api.getOperatives(player.killTeamId());
                Platform.runLater(() -> rosterList.getItems().setAll(ops));
            } catch (Exception e) { showError(e); }
        }).start();
    }

    private void deploySelected() {
        if (!match.deploymentPhase()) {
            ErrorDialog.show("Выставление бойцов доступно только в фазе расстановки");
            return;
        }
        OperativeDto op = rosterList.getSelectionModel().getSelectedItem();
        if (op == null) { ErrorDialog.show("Выбери бойца в составе"); return; }
        String playerName = activePlayerBox.getValue();
        MatchPlayerDto player = match.players().stream()
                .filter(p -> p.name().equals(playerName))
                .findFirst().orElse(null);
        if (player == null) return;

        boolean deployed = fieldUnits.stream()
                .anyMatch(mo -> mo.operativeId().equals(op.id())
                        && mo.playerId().equals(player.id()));
        if (deployed) { ErrorDialog.show("Этот боец уже на поле"); return; }

        double rawCx = boardW / 2 + (Math.random() * 6 - 3);
        double rawCy = boardH / 2 + (Math.random() * 6 - 3);
        final double cx = clamp(rawCx, 0, boardW);
        final double cy = clamp(rawCy, 0, boardH);
        final Long playerId = player.id();
        final Long operativeId = op.id();
        final Long matchId = match.id();

        new Thread(() -> {
            try {
                var mo = api.deploy(matchId, playerId, operativeId, cx, cy);
                Platform.runLater(() -> {
                    fieldUnits.add(mo);
                    loadRoster();
                    redraw();
                    notifyChanged();
                });
            } catch (Exception ex) { showError(ex); }
        }).start();
    }

    private boolean isDeployed(Long operativeId, String playerName) {
        MatchPlayerDto player = match.players().stream()
                .filter(p -> p.name().equals(playerName))
                .findFirst().orElse(null);
        if (player == null) return false;
        return fieldUnits.stream()
                .anyMatch(mo -> mo.operativeId().equals(operativeId)
                        && mo.playerId().equals(player.id()));
    }

    private void replaceInField(MatchOperativeDto updated) {
        for (int i = 0; i < fieldUnits.size(); i++) {
            if (fieldUnits.get(i).id().equals(updated.id())) {
                fieldUnits.set(i, updated);
                return;
            }
        }
    }

    private void removeGaugeAt(double x, double y) {
        for (int i = gauges.size() - 1; i >= 0; i--) {
            Gauge g = gauges.get(i);
            double r = g.diameter / 2;
            if (Math.hypot(g.x - x, g.y - y) <= r) { gauges.remove(i); redraw(); return; }
        }
    }

    private void showContextMenu(MatchOperativeDto mo, double screenX, double screenY) {
        if (activeMenu != null) { activeMenu.hide(); activeMenu = null; }

        ContextMenu menu = new ContextMenu();
        boolean alive = mo.woundsCurrent() > 0;
        boolean deployment = match.deploymentPhase();

        TurningPointDto currentTp = match.turningPoints().stream()
                .filter(tp -> !"FINISHED".equals(tp.phase()))
                .findFirst().orElse(null);
        boolean firefight = currentTp != null && "FIREFIGHT".equals(currentTp.phase());

        boolean myTurn = match.activePlayerId() == null
                || match.activePlayerId().equals(mo.playerId());

        boolean canAct = alive && !deployment && firefight && myTurn;

        Menu orderMenu = new Menu("Приказ");
        MenuItem engageItem = new MenuItem("Engage — атака");
        MenuItem concealItem = new MenuItem("Conceal — скрытность");
        engageItem.setOnAction(e -> changeOrder(mo, "ENGAGE"));
        concealItem.setOnAction(e -> changeOrder(mo, "CONCEAL"));
        orderMenu.getItems().addAll(engageItem, concealItem);
        orderMenu.setDisable(deployment ? false : !canAct);

        Menu stateMenu = new Menu("Состояние");
        MenuItem readyItem = new MenuItem("Готов");
        MenuItem expendedItem = new MenuItem("Потрачен");
        readyItem.setOnAction(e -> changeState(mo, "READY"));
        expendedItem.setOnAction(e -> changeState(mo, "EXPENDED"));
        stateMenu.getItems().addAll(readyItem, expendedItem);
        stateMenu.setDisable(deployment ? false : !canAct);

        Menu actionsMenu = new Menu("Действия бойца (ОД)");
        MenuItem shootAct = new MenuItem("Стрельба (1 ОД)");
        MenuItem fightAct = new MenuItem("Ближний бой (1 ОД)");
        shootAct.setOnAction(e -> openCombatDialog(mo, "SHOOT"));
        fightAct.setOnAction(e -> openCombatDialog(mo, "FIGHT"));
        actionsMenu.getItems().addAll(shootAct, fightAct);
        actionsMenu.setDisable(!canAct);

        MenuItem resetAplItem = new MenuItem("Сбросить ОД");
        resetAplItem.setOnAction(e -> resetApl(mo));

        MenuItem wound = new MenuItem("Изменить раны...");
        wound.setOnAction(e -> editWounds(mo));

        MenuItem remove = new MenuItem("Убрать с поля");
        remove.setOnAction(e -> removeUnit(mo));
        remove.setDisable(!deployment);

        menu.getItems().addAll(
                orderMenu, stateMenu,
                new SeparatorMenuItem(),
                actionsMenu, resetAplItem,
                new SeparatorMenuItem(),
                wound,
                new SeparatorMenuItem(),
                remove
        );

        activeMenu = menu;
        menu.show(canvas, screenX, screenY);
    }

    private void openCombatDialog(MatchOperativeDto attacker, String type) {
        List<MatchOperativeDto> enemies = fieldUnits.stream()
                .filter(u -> !u.playerId().equals(attacker.playerId()))
                .filter(u -> u.woundsCurrent() > 0)
                .toList();
        if (enemies.isEmpty()) { ErrorDialog.show("Нет живых врагов на поле"); return; }

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(type.equals("SHOOT") ? "Стрельба" : "Ближний бой");
        dialog.setResizable(false);
        DialogStyler.apply(dialog);

        ComboBox<MatchOperativeDto> targetBox = new ComboBox<>();
        targetBox.getItems().setAll(enemies);
        targetBox.setConverter(new javafx.util.StringConverter<>() {
            @Override public String toString(MatchOperativeDto u) {
                if (u == null) return "";
                double d = Math.hypot(u.posX() - attacker.posX(), u.posY() - attacker.posY());
                return u.operativeName() + String.format(" (%.1f\")", d)
                        + "  Раны " + u.woundsCurrent() + "/" + u.woundsMax();
            }
            @Override public MatchOperativeDto fromString(String s) { return null; }
        });
        targetBox.setValue(enemies.get(0));

        Label info = new Label(
                "Атакующий: " + attacker.operativeName()
                        + "\nОД: " + attacker.currentApl() + "/" + attacker.maxApl()
                        + "\nПриказ: " + (attacker.orderType().equals("ENGAGE") ? "Engage" : "Conceal")
                        + "\nРаны: " + attacker.woundsCurrent() + "/" + attacker.woundsMax()
        );
        info.setStyle("-fx-text-fill: #c9b98a;");

        VBox vbox = new VBox(10, info, new Separator(), new Label("Цель:"), targetBox);
        vbox.setPadding(new Insets(15));
        vbox.setStyle("-fx-background-color: #1c160d;");
        dialog.getDialogPane().setContent(vbox);
        dialog.getDialogPane().setPrefWidth(480);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.showAndWait().ifPresent(bt -> {
            if (bt != ButtonType.OK) return;
            MatchOperativeDto target = targetBox.getValue();
            if (target == null) return;
            new Thread(() -> {
                try {
                    var result = type.equals("SHOOT")
                            ? api.shoot(attacker.id(), target.id())
                            : api.fight(attacker.id(), target.id());
                    Platform.runLater(() -> {
                        showCombatResult(result);
                        loadField();
                        notifyChanged();
                    });
                } catch (Exception ex) { showError(ex); }
            }).start();
        });
    }

    private void showCombatResult(CombatResultDto r) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(r.type().equals("SHOOT") ? "Результат стрельбы" : "Результат ближнего боя");
        alert.setHeaderText(r.attackerName() + "  →  " + r.targetName());

        TextArea area = new TextArea(r.log());
        area.setEditable(false);
        area.setWrapText(true);
        area.setPrefRowCount(18);
        area.setPrefColumnCount(50);
        area.setMaxWidth(500);
        area.setStyle("-fx-font-family: 'Consolas', monospace; -fx-font-size: 12px;");
        alert.getDialogPane().setContent(area);
        alert.getDialogPane().setPrefWidth(520);
        alert.setResizable(false);
        DialogStyler.apply(alert);
        alert.showAndWait();
    }

    private void showInfo(String title, String text) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(text);
        alert.setResizable(false);
        DialogStyler.apply(alert);
        alert.showAndWait();
    }

    private void changeOrder(MatchOperativeDto mo, String order) {
        new Thread(() -> {
            try {
                var updated = api.setOrder(mo.id(), order);
                Platform.runLater(() -> {
                    replaceInField(updated);
                    redraw();
                    notifyChanged();
                });
            } catch (Exception ex) { showError(ex); }
        }).start();
    }

    private void changeState(MatchOperativeDto mo, String state) {
        new Thread(() -> {
            try {
                var updated = api.setState(mo.id(), state);
                Platform.runLater(() -> {
                    replaceInField(updated);
                    redraw();
                    notifyChanged();
                });
            } catch (Exception ex) { showError(ex); }
        }).start();
    }

    private void resetApl(MatchOperativeDto mo) {
        new Thread(() -> {
            try {
                var updated = api.resetApl(mo.id());
                Platform.runLater(() -> {
                    replaceInField(updated);
                    redraw();
                    notifyChanged();
                });
            } catch (Exception ex) { showError(ex); }
        }).start();
    }

    private void editWounds(MatchOperativeDto mo) {
        TextInputDialog dlg = new TextInputDialog(String.valueOf(mo.woundsCurrent()));
        dlg.setTitle("Раны");
        dlg.setHeaderText("Текущие раны для " + mo.operativeName());
        dlg.setContentText("Ран (0.." + mo.woundsMax() + "):");
        dlg.setResizable(false);
        DialogStyler.apply(dlg);
        dlg.showAndWait().ifPresent(s -> {
            try {
                int w = Integer.parseInt(s.trim());
                if (w < 0 || w > mo.woundsMax()) {
                    ErrorDialog.show("Допустимо 0.." + mo.woundsMax());
                    return;
                }
                new Thread(() -> {
                    try {
                        var updated = api.setWounds(mo.id(), w);
                        Platform.runLater(() -> {
                            replaceInField(updated);
                            redraw();
                            notifyChanged();
                        });
                    } catch (Exception ex) { showError(ex); }
                }).start();
            } catch (NumberFormatException ignored) {}
        });
    }

    private void removeUnit(MatchOperativeDto mo) {
        new Thread(() -> {
            try {
                api.removeFromField(mo.id());
                Platform.runLater(() -> {
                    fieldUnits.removeIf(u -> u.id().equals(mo.id()));
                    loadRoster();
                    redraw();
                    notifyChanged();
                });
            } catch (Exception ex) { showError(ex); }
        }).start();
    }

    private void redraw() {
        GraphicsContext g = canvas.getGraphicsContext2D();
        double w = canvas.getWidth(), h = canvas.getHeight();
        g.setFill(Color.web("#2a0808"));
        g.fillRect(0, 0, w, h);

        double fieldPx = boardW * scale;
        double fieldPy = boardH * scale;
        double fieldX = (w - fieldPx) / 2;
        double fieldY = (h - fieldPy) / 2;

        drawHatching(g, w, h, fieldX, fieldY, fieldPx, fieldPy);
        g.setFill(Color.web("#241a0c"));
        g.fillRect(fieldX, fieldY, fieldPx, fieldPy);

        if (gridVisible) drawGrid(g, fieldX, fieldY, fieldPx, fieldPy);

        if (dragged != null && dragMaxDist > 0) {
            double cx = fieldX + dragStartX * scale;
            double cy = fieldY + dragStartY * scale;
            double r = dragMaxDist * scale;
            g.setFill(Color.web("#2196f3", 0.06));
            g.fillOval(cx - r, cy - r, r * 2, r * 2);
            g.setStroke(Color.web("#2196f3", 0.55));
            g.setLineWidth(1.5);
            g.setLineDashes(8, 6);
            g.strokeOval(cx - r, cy - r, r * 2, r * 2);
            g.setLineDashes();
        }

        drawGauges(g, fieldX, fieldY);
        drawRuler(g, fieldX, fieldY);
        for (MatchOperativeDto mo : fieldUnits) drawUnit(g, mo, fieldX, fieldY);

        g.setStroke(Color.web("#d4a72c"));
        g.setLineWidth(3);
        g.strokeRect(fieldX, fieldY, fieldPx, fieldPy);
    }

    private void drawHatching(GraphicsContext g, double w, double h,
                              double fX, double fY, double fW, double fH) {
        for (double i = -h; i < w + h; i += 22) {
            drawLineClipped(g, i, 0, i + h, h, fX, fY, fW, fH);
        }
    }

    private void drawLineClipped(GraphicsContext g,
                                 double x1, double y1, double x2, double y2,
                                 double fx, double fy, double fw, double fh) {
        double dx = x2 - x1, dy = y2 - y1;
        List<Double> ts = new ArrayList<>();
        ts.add(0.0); ts.add(1.0);
        if (Math.abs(dx) > 1e-9) for (double bx : new double[]{fx, fx + fw}) {
            double t = (bx - x1) / dx;
            if (t > 0 && t < 1) ts.add(t);
        }
        if (Math.abs(dy) > 1e-9) for (double by : new double[]{fy, fy + fh}) {
            double t = (by - y1) / dy;
            if (t > 0 && t < 1) ts.add(t);
        }
        ts.sort(Double::compare);
        g.setStroke(Color.web("#8b1a1a", 0.45));
        g.setLineWidth(1);
        for (int i = 0; i < ts.size() - 1; i++) {
            double t0 = ts.get(i), t1 = ts.get(i + 1);
            double mx = x1 + (t0 + t1) / 2 * dx;
            double my = y1 + (t0 + t1) / 2 * dy;
            boolean inside = mx > fx && mx < fx + fw && my > fy && my < fy + fh;
            if (!inside) g.strokeLine(x1 + t0 * dx, y1 + t0 * dy, x1 + t1 * dx, y1 + t1 * dy);
        }
    }

    private void drawGrid(GraphicsContext g, double fieldX, double fieldY,
                          double fieldPx, double fieldPy) {
        g.setStroke(Color.web("#2a2012"));
        g.setLineWidth(0.5);
        for (double x = 1; x < boardW; x += 1)
            g.strokeLine(fieldX + x * scale, fieldY, fieldX + x * scale, fieldY + fieldPy);
        for (double y = 1; y < boardH; y += 1)
            g.strokeLine(fieldX, fieldY + y * scale, fieldX + fieldPx, fieldY + y * scale);
        g.setStroke(Color.web("#4a3a1a"));
        g.setLineWidth(1);
        for (double x = 6; x < boardW; x += 6)
            g.strokeLine(fieldX + x * scale, fieldY, fieldX + x * scale, fieldY + fieldPy);
        for (double y = 6; y < boardH; y += 6)
            g.strokeLine(fieldX, fieldY + y * scale, fieldX + fieldPx, fieldY + y * scale);
    }

    private void drawGauges(GraphicsContext g, double fieldX, double fieldY) {
        for (Gauge gauge : gauges) {
            double r = (gauge.diameter / 2) * scale;
            double cx = fieldX + gauge.x * scale;
            double cy = fieldY + gauge.y * scale;
            g.setFill(Color.web("#d4a72c", 0.25));
            g.fillOval(cx - r, cy - r, r * 2, r * 2);
            g.setStroke(Color.web("#f0c75e", 0.9));
            g.setLineWidth(2);
            g.strokeOval(cx - r, cy - r, r * 2, r * 2);
            if (r > 12) {
                g.setFill(Color.web("#f0c75e"));
                g.setFont(Font.font("Segoe UI", 11));
                g.setTextAlign(TextAlignment.CENTER);
                g.fillText(String.format("%.1f\"", gauge.diameter), cx, cy + 4);
            }
        }
    }

    private void drawUnit(GraphicsContext g, MatchOperativeDto mo,
                          double fieldX, double fieldY) {
        double px = fieldX + mo.posX() * scale;
        double py = fieldY + mo.posY() * scale;
        double r = Math.max(10, scale * 0.55);

        boolean isPlayer1 = !match.players().isEmpty()
                && mo.playerId().equals(match.players().get(0).id());
        Color baseColor = isPlayer1 ? Color.web("#2196f3") : Color.web("#e53935");

        boolean isActivePlayer = match.deploymentPhase()
                || match.activePlayerId() == null
                || match.activePlayerId().equals(mo.playerId());
        if (!isActivePlayer) baseColor = baseColor.deriveColor(0, 0.6, 0.7, 1);

        boolean dead = mo.woundsCurrent() <= 0;
        if (dead) baseColor = Color.web("#4a4a4a");

        boolean isSelected = selected != null && mo.id().equals(selected.id());
        Color strokeColor = isSelected ? Color.web("#f0c75e") : Color.web("#d4a72c");

        boolean expended = "EXPENDED".equals(mo.state());
        g.setGlobalAlpha(dead ? 0.35 : (expended ? 0.55 : 1.0));

        g.setFill(baseColor);
        g.fillOval(px - r, py - r, r * 2, r * 2);
        g.setStroke(strokeColor);
        g.setLineWidth(isSelected ? 3 : 2);
        g.strokeOval(px - r, py - r, r * 2, r * 2);

        g.setFill(Color.WHITE);
        g.setFont(Font.font("Segoe UI", Math.max(11, r * 0.9)));
        g.setTextAlign(TextAlignment.CENTER);
        String icon;
        if (dead) icon = "✖";
        else icon = "ENGAGE".equals(mo.orderType()) ? "⚔" : "👁";
        g.fillText(icon, px, py + r * 0.35);

        if (labelsVisible) {
            g.setFill(Color.web("#e8d9b0"));
            g.setFont(Font.font("Segoe UI", 10));
            g.fillText(shortName(mo.operativeName()), px, py - r - 4);
            g.setFill(dead ? Color.web("#8b1a1a") : Color.web("#f0c75e"));
            g.fillText(dead ? "ВЫВЕДЕН" : (mo.woundsCurrent() + "/" + mo.woundsMax()),
                    px, py + r + 12);
            if (!dead) {
                g.setFill(Color.web("#8b6f2a"));
                g.setFont(Font.font("Segoe UI", 9));
                g.fillText("ОД " + mo.currentApl() + "/" + mo.maxApl(), px, py + r + 24);
            }
        }

        g.setGlobalAlpha(1.0);
    }

    private void drawRuler(GraphicsContext g, double fieldX, double fieldY) {
        if (rulerStartX == null) return;
        double x1 = fieldX + rulerStartX * scale;
        double y1 = fieldY + rulerStartY * scale;
        g.setFill(Color.web("#d4a72c"));
        g.fillOval(x1 - 6, y1 - 6, 12, 12);
        g.setStroke(Color.web("#0f0b06"));
        g.setLineWidth(2);
        g.strokeOval(x1 - 6, y1 - 6, 12, 12);

        if (rulerEndX == null) return;
        double x2 = fieldX + rulerEndX * scale;
        double y2 = fieldY + rulerEndY * scale;

        g.setStroke(Color.web("#d4a72c"));
        g.setLineWidth(2);
        g.strokeLine(x1, y1, x2, y2);
        g.setFill(Color.web("#d4a72c"));
        g.fillOval(x2 - 6, y2 - 6, 12, 12);
        g.setStroke(Color.web("#0f0b06"));
        g.strokeOval(x2 - 6, y2 - 6, 12, 12);

        double dist = Math.hypot(rulerEndX - rulerStartX, rulerEndY - rulerStartY);
        double midX = (x1 + x2) / 2;
        double midY = (y1 + y2) / 2;
        g.setFill(Color.web("#0f0b06", 0.9));
        g.fillRoundRect(midX - 34, midY - 22, 68, 22, 6, 6);
        g.setFill(Color.web("#f0c75e"));
        g.setFont(Font.font("Segoe UI", 12));
        g.setTextAlign(TextAlignment.CENTER);
        g.fillText(String.format("%.1f\"", dist), midX, midY - 6);
    }

    private double[] screenToInch(double px, double py) {
        double fX = (canvas.getWidth() - boardW * scale) / 2;
        double fY = (canvas.getHeight() - boardH * scale) / 2;
        return new double[]{(px - fX) / scale, (py - fY) / scale};
    }

    private MatchOperativeDto hitTest(double x, double y) {
        double rInch = Math.max(10, scale * 0.55) / scale;
        for (int i = fieldUnits.size() - 1; i >= 0; i--) {
            MatchOperativeDto mo = fieldUnits.get(i);
            if (Math.hypot(mo.posX() - x, mo.posY() - y) <= rInch) return mo;
        }
        return null;
    }

    private boolean nearPoint(double x, double y, Double px, Double py) {
        if (px == null || py == null) return false;
        return Math.hypot(x - px, y - py) <= 10.0 / scale;
    }

    private double clamp(double v, double min, double max) {
        return Math.max(min, Math.min(max, v));
    }

    private String shortName(String name) {
        if (name == null) return "";
        String[] parts = name.split(" ");
        if (parts.length >= 2) return parts[0].charAt(0) + ". " + parts[parts.length - 1];
        return name;
    }

    private boolean confirm(String text) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, text, ButtonType.OK, ButtonType.CANCEL);
        alert.setResizable(false);
        DialogStyler.apply(alert);
        return alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK;
    }

    private void showError(Exception e) {
        ErrorDialog.show(e);
        refreshMatchAndRedraw();
    }
}
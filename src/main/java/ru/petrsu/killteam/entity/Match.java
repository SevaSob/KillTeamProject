package ru.petrsu.killteam.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "match_game")
@Getter
@Setter
@NoArgsConstructor
public class Match {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private String mission;
    private String killzone;

    @Column(nullable = false)
    private String status;

    private LocalDateTime createdAt;

    @Column(nullable = false)
    private boolean deploymentPhase = true;

    private Long activePlayerId;

    @OneToMany(mappedBy = "match", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MatchPlayer> players = new ArrayList<>();

    @OneToMany(mappedBy = "match", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TurningPoint> turningPoints = new ArrayList<>();

    @OneToMany(mappedBy = "match", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<GameEvent> events = new ArrayList<>();

    @OneToMany(mappedBy = "match", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MatchOperative> matchOperatives = new ArrayList<>();

    public Match(String name, String mission, String killzone) {
        this.name = name;
        this.mission = mission;
        this.killzone = killzone;
        this.status = "PLANNED";
        this.createdAt = LocalDateTime.now();
        this.deploymentPhase = true;
    }

    public void addPlayer(MatchPlayer p) {
        players.add(p);
        p.setMatch(this);
    }

    public void addTurningPoint(TurningPoint tp) {
        turningPoints.add(tp);
        tp.setMatch(this);
    }
}
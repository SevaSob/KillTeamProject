package ru.petrsu.killteam.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "game_event")
@Getter
@Setter
@NoArgsConstructor
public class GameEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "match_id", nullable = false)
    private Match match;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "turning_point_id")
    private TurningPoint turningPoint;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player_id")
    private MatchPlayer player;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "operative_id")
    private Operative operative;

    @Column(nullable = false)
    private String eventType; // ACTIVATE, SHOOT, FIGHT, MOVE, CP_SPEND, NOTE

    @Column(columnDefinition = "TEXT")
    private String description;

    private LocalDateTime createdAt;

    public GameEvent(String eventType, String description) {
        this.eventType = eventType;
        this.description = description;
        this.createdAt = LocalDateTime.now();
    }
}
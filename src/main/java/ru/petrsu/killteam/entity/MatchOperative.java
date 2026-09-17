package ru.petrsu.killteam.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "match_operative")
@Getter
@Setter
@NoArgsConstructor
public class MatchOperative {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "match_id", nullable = false)
    private Match match;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player_id", nullable = false)
    private MatchPlayer player;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "operative_id", nullable = false)
    private Operative operative;

    private double posX;
    private double posY;

    @Column(nullable = false)
    private String orderType; // ENGAGE, CONCEAL

    @Column(nullable = false)
    private String state; // READY, EXPENDED

    private int woundsCurrent;

    private int currentApl;

    public MatchOperative(Match match, MatchPlayer player, Operative operative, double x, double y) {
        this.match = match;
        this.player = player;
        this.operative = operative;
        this.posX = x;
        this.posY = y;
        this.orderType = "CONCEAL";
        this.state = "READY";
        this.woundsCurrent = operative.getWounds();
        this.currentApl = operative.getApl();
    }
}
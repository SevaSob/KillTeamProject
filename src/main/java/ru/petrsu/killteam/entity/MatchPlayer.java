package ru.petrsu.killteam.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "match_player")
@Getter
@Setter
@NoArgsConstructor
public class MatchPlayer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "match_id", nullable = false)
    private Match match;

    @Column(nullable = false)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "kill_team_id", nullable = false)
    private KillTeam killTeam;

    private int cp;
    private int score;
    private boolean initiative;

    public MatchPlayer(String name, KillTeam killTeam) {
        this.name = name;
        this.killTeam = killTeam;
        this.cp = 2;
        this.score = 0;
        this.initiative = false;
    }
}
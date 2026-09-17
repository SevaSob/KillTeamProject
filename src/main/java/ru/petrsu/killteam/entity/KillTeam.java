package ru.petrsu.killteam.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "kill_team")
@Getter
@Setter
@NoArgsConstructor
public class KillTeam {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "faction_id", nullable = false)
    private Faction faction;

    @Column(nullable = false)
    private String name;

    private String version;

    @Column(columnDefinition = "TEXT")
    private String rulesText;

    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "killTeam", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Operative> operatives = new ArrayList<>();

    public KillTeam(Faction faction, String name, String version, String rulesText) {
        this.faction = faction;
        this.name = name;
        this.version = version;
        this.rulesText = rulesText;
        this.updatedAt = LocalDateTime.now();
    }

    public void addOperative(Operative op) {
        operatives.add(op);
        op.setKillTeam(this);
    }
}
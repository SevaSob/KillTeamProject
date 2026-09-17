package ru.petrsu.killteam.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "turning_point")
@Getter
@Setter
@NoArgsConstructor
public class TurningPoint {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "match_id", nullable = false)
    private Match match;

    @Column(nullable = false)
    private int number;

    @Column(nullable = false)
    private String phase; // STRATEGY, FIREFIGHT, FINISHED

    public TurningPoint(int number) {
        this.number = number;
        this.phase = "STRATEGY";
    }
}
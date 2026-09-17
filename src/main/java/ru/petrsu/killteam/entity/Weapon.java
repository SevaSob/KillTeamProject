package ru.petrsu.killteam.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "weapon")
@Getter
@Setter
@NoArgsConstructor
public class Weapon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "operative_id", nullable = false)
    private Operative operative;

    @Column(nullable = false)
    private String name;

    private int atk;
    private String hit;
    private String dmg;

    @Column(length = 500)
    private String special;

    public Weapon(String name, int atk, String hit, String dmg, String special) {
        this.name = name;
        this.atk = atk;
        this.hit = hit;
        this.dmg = dmg;
        this.special = special;
    }
}
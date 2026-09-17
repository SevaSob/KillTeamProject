package ru.petrsu.killteam.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "operative")
@Getter
@Setter
@NoArgsConstructor
public class Operative {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "kill_team_id", nullable = false)
    private KillTeam killTeam;

    @Column(nullable = false)
    private String name;

    private int apl;
    private int move;
    private String save;
    private int wounds;

    @Column(columnDefinition = "TEXT")
    private String ability;

    @OneToMany(mappedBy = "operative", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Weapon> weapons = new ArrayList<>();

    public Operative(String name, int apl, int move, String save, int wounds, String ability) {
        this.name = name;
        this.apl = apl;
        this.move = move;
        this.save = save;
        this.wounds = wounds;
        this.ability = ability;
    }

    public void addWeapon(Weapon w) {
        weapons.add(w);
        w.setOperative(this);
    }
}
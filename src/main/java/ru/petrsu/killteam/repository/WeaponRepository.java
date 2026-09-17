package ru.petrsu.killteam.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.petrsu.killteam.entity.Weapon;

public interface WeaponRepository extends JpaRepository<Weapon, Long> {
}
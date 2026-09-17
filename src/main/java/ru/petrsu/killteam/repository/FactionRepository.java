package ru.petrsu.killteam.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.petrsu.killteam.entity.Faction;

public interface FactionRepository extends JpaRepository<Faction, Long> {
}
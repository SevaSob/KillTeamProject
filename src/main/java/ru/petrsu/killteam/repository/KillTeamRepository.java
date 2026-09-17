package ru.petrsu.killteam.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.petrsu.killteam.entity.KillTeam;

import java.util.List;

public interface KillTeamRepository extends JpaRepository<KillTeam, Long> {
    List<KillTeam> findByFactionId(Long factionId);
}
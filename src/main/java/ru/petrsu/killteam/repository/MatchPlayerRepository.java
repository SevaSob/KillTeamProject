package ru.petrsu.killteam.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.petrsu.killteam.entity.MatchPlayer;

import java.util.List;

public interface MatchPlayerRepository extends JpaRepository<MatchPlayer, Long> {
    List<MatchPlayer> findByMatchId(Long matchId);
}
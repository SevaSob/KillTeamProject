package ru.petrsu.killteam.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.petrsu.killteam.entity.MatchOperative;

import java.util.List;

public interface MatchOperativeRepository extends JpaRepository<MatchOperative, Long> {
    List<MatchOperative> findByMatchId(Long matchId);
    void deleteByMatchId(Long matchId);
}
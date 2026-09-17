package ru.petrsu.killteam.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.petrsu.killteam.entity.Match;

public interface MatchRepository extends JpaRepository<Match, Long> {
}
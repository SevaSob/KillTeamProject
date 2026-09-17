package ru.petrsu.killteam.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.petrsu.killteam.entity.GameEvent;

import java.util.List;

public interface GameEventRepository extends JpaRepository<GameEvent, Long> {
    List<GameEvent> findByMatchIdOrderByCreatedAtAsc(Long matchId);
}
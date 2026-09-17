package ru.petrsu.killteam.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.petrsu.killteam.entity.TurningPoint;

import java.util.List;

public interface TurningPointRepository extends JpaRepository<TurningPoint, Long> {
    List<TurningPoint> findByMatchIdOrderByNumberAsc(Long matchId);
}
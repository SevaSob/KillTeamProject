package ru.petrsu.killteam.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.petrsu.killteam.entity.Operative;

import java.util.List;

public interface OperativeRepository extends JpaRepository<Operative, Long> {
    List<Operative> findByKillTeamId(Long killTeamId);
}
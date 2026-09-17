package ru.petrsu.killteam.dto;

import java.time.LocalDateTime;
import java.util.List;

public record MatchDto(
        Long id,
        String name,
        String mission,
        String killzone,
        String status,
        boolean deploymentPhase,
        Long activePlayerId,
        LocalDateTime createdAt,
        List<MatchPlayerDto> players,
        List<TurningPointDto> turningPoints,
        List<GameEventDto> events
) {}
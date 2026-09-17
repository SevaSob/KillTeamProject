package ru.petrsu.killteam.dto;

import java.time.LocalDateTime;

public record GameEventDto(
        Long id,
        Long matchId,
        Long turningPointId,
        Long playerId,
        Long operativeId,
        String eventType,
        String description,
        LocalDateTime createdAt
) {}
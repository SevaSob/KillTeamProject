package ru.petrsu.killteam.dto;

public record MatchOperativeDto(
        Long id,
        Long matchId,
        Long playerId,
        String playerName,
        Long operativeId,
        String operativeName,
        double posX,
        double posY,
        String orderType,
        String state,
        int woundsCurrent,
        int woundsMax,
        int currentApl,
        int maxApl,
        int move
) {}
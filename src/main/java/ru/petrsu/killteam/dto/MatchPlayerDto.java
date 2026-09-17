package ru.petrsu.killteam.dto;

public record MatchPlayerDto(
        Long id,
        String name,
        Long killTeamId,
        String killTeamName,
        int cp,
        int score,
        boolean initiative
) {}
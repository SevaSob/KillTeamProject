package ru.petrsu.killteam.dto;

import java.util.List;

public record KillTeamDto(
        Long id,
        Long factionId,
        String factionName,
        String name,
        String version,
        String rulesText,
        List<OperativeDto> operatives
) {}
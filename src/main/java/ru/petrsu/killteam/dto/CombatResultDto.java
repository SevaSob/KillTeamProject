package ru.petrsu.killteam.dto;

import java.util.List;

public record CombatResultDto(
        String type,              // "SHOOT" или "FIGHT"
        String attackerName,
        String targetName,
        String weaponName,
        List<Integer> attackRolls,
        List<Integer> defenseRolls,
        int attackHits,
        int attackCrits,
        int defenseHits,
        int defenseCrits,
        int netHits,
        int netCrits,
        int totalDamage,
        int targetWoundsBefore,
        int targetWoundsAfter,
        String injuryResult,
        String log
) {}
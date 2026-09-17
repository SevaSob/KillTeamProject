package ru.petrsu.killteam.dto;

import java.util.List;

public record OperativeDto(
        Long id,
        String name,
        int apl,
        int move,
        String save,
        int wounds,
        String ability,
        List<WeaponDto> weapons
) {}
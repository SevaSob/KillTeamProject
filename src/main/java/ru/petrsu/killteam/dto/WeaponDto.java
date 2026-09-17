package ru.petrsu.killteam.dto;

public record WeaponDto(
        Long id,
        String name,
        int atk,
        String hit,
        String dmg,
        String special
) {}
package ru.petrsu.killteam.dto;

public record FactionDto(
        Long id,
        String name,
        String description,
        Long parentId,
        String parentName
) {}
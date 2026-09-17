package ru.petrsu.killteam.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.petrsu.killteam.dto.KillTeamDto;
import ru.petrsu.killteam.service.KillTeamService;

import java.util.List;

@RestController
@RequestMapping("/api/kill-teams")
@RequiredArgsConstructor
public class KillTeamController {

    private final KillTeamService service;

    @GetMapping
    public List<KillTeamDto> getAll(@RequestParam(required = false) Long factionId) {
        return service.getAll(factionId);
    }

    @GetMapping("/{id}")
    public KillTeamDto getById(@PathVariable Long id) {
        return service.getById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public KillTeamDto create(@RequestBody KillTeamDto dto) {
        return service.create(dto);
    }

    @PutMapping("/{id}")
    public KillTeamDto update(@PathVariable Long id, @RequestBody KillTeamDto dto) {
        return service.update(id, dto);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}